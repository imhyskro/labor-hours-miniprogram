package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.dto.StudentQueryDTO;
import com.labor.management.dto.StudentUpdateDTO;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.StudentService;
import com.labor.management.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Override
    public IPage<StudentVO> pageQuery(StudentQueryDTO dto, List<Long> scopeClassIds) {
        int current = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int size = dto.getSize() == null || dto.getSize() < 1 ? 10 : dto.getSize();
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("offset", (current - 1) * size).addValue("size", size);
        String where = buildWhere(dto, scopeClassIds, params);
        Long total = namedJdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM student s JOIN student_course_enrollment e ON e.student_id=s.id
                JOIN teaching_group g ON g.id=e.teaching_group_id WHERE e.status<>'WITHDRAWN'
                """ + where, params, Long.class);
        List<StudentVO> rows = namedJdbcTemplate.query(studentSelect() + " WHERE e.status<>'WITHDRAWN'" + where +
                " ORDER BY e.company_name_snapshot, e.class_code_snapshot, e.in_class_no LIMIT :offset, :size", params, this::mapStudent);
        Page<StudentVO> result = new Page<>(current, size, total == null ? 0 : total); result.setRecords(rows); return result;
    }

    @Override
    public StudentVO getById(Long id, List<Long> scopeClassIds) {
        List<StudentVO> rows = jdbcTemplate.query(studentSelect() + " WHERE s.id=? AND e.status<>'WITHDRAWN' ORDER BY e.id DESC LIMIT 1",
                this::mapStudent, id);
        if (rows.isEmpty()) throw new BusinessException("学生不存在");
        checkScope(rows.get(0).getClassId(), scopeClassIds); return rows.get(0);
    }

    @Override
    @Transactional
    public Long create(StudentCreateDTO dto, List<Long> scopeClassIds) {
        checkScope(dto.getClassId(), scopeClassIds);
        GroupRow group = group(dto.getClassId());
        Integer duplicate = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student WHERE student_no=?", Integer.class, dto.getStudentId());
        if (duplicate != null && duplicate > 0) throw new BusinessException("学号已存在");
        Long adminClassId = adminClass(dto.getOriginalMajor());
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO student(student_no,student_name,gender,administrative_class_id) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getStudentId()); ps.setString(2, dto.getName()); ps.setString(3, genderText(dto.getGender()));
            if (adminClassId == null) ps.setNull(4, java.sql.Types.BIGINT); else ps.setLong(4, adminClassId); return ps;
        }, keyHolder);
        if (keyHolder.getKey() == null) throw new BusinessException("学生创建失败");
        Long studentId = keyHolder.getKey().longValue();
        int inClassNo = dto.getStudentNoInClass() == null ? nextNo(dto.getClassId()) : dto.getStudentNoInClass();
        jdbcTemplate.update("""
                INSERT INTO student_course_enrollment(student_id,term_id,teaching_group_id,
                  student_no_snapshot,student_name_snapshot,gender_snapshot,admin_class_snapshot,
                  company_name_snapshot,class_code_snapshot,in_class_no,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,'ACTIVE')
                """, studentId, group.termId(), group.id(), dto.getStudentId(), dto.getName(), genderText(dto.getGender()),
                adminClassName(dto.getOriginalMajor()), group.companyName(), group.classCode(), inClassNo);
        return studentId;
    }

    @Override
    @Transactional
    public void update(StudentUpdateDTO dto, List<Long> scopeClassIds) {
        StudentVO old = getById(dto.getId(), scopeClassIds);
        checkScope(dto.getClassId(), scopeClassIds);
        GroupRow group = group(dto.getClassId());
        Integer duplicate = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student WHERE student_no=? AND id<>?", Integer.class, dto.getStudentId(), dto.getId());
        if (duplicate != null && duplicate > 0) throw new BusinessException("学号已存在");
        Long adminClassId = adminClass(dto.getOriginalMajor());
        jdbcTemplate.update("UPDATE student SET student_no=?,student_name=?,gender=?,administrative_class_id=? WHERE id=?",
                dto.getStudentId(), dto.getName(), genderText(dto.getGender()), adminClassId, dto.getId());
        String status = dto.getStatus() != null && dto.getStatus() == 0 ? "WITHDRAWN" : "ACTIVE";
        jdbcTemplate.update("""
                UPDATE student_course_enrollment SET term_id=?, teaching_group_id=?, student_no_snapshot=?,
                  student_name_snapshot=?, gender_snapshot=?, admin_class_snapshot=?, company_name_snapshot=?,
                  class_code_snapshot=?, in_class_no=?, status=?
                WHERE student_id=? AND status<>'COMPLETED'
                """, group.termId(), group.id(), dto.getStudentId(), dto.getName(), genderText(dto.getGender()),
                adminClassName(dto.getOriginalMajor()), group.companyName(), group.classCode(),
                dto.getStudentNoInClass() == null ? nextNo(dto.getClassId()) : dto.getStudentNoInClass(), status, dto.getId());
        if (old.getClassId() == null) throw new BusinessException("学生修读关系不存在");
    }

    @Override
    @Transactional
    public void deleteById(Long id, List<Long> scopeClassIds) {
        getById(id, scopeClassIds);
        jdbcTemplate.update("UPDATE student_course_enrollment SET status='WITHDRAWN', completed_at=CURRENT_TIMESTAMP(3) WHERE student_id=? AND status='ACTIVE'", id);
        jdbcTemplate.update("UPDATE assistant_profile SET status='INACTIVE' WHERE student_id=? AND status='ACTIVE'", id);
    }

    private String studentSelect() {
        return """
                SELECT s.id, s.student_no, s.student_name, e.teaching_group_id,
                       CONCAT(e.company_name_snapshot, '-', e.class_code_snapshot) class_name,
                       s.gender, CASE WHEN e.status='WITHDRAWN' THEN 0 ELSE 1 END web_status, s.created_at
                FROM student s JOIN student_course_enrollment e ON e.student_id=s.id
                JOIN teaching_group g ON g.id=e.teaching_group_id
                """;
    }
    private StudentVO mapStudent(ResultSet rs, int n) throws SQLException {
        StudentVO vo = new StudentVO(); vo.setId(rs.getLong("id")); vo.setStudentId(rs.getString("student_no"));
        vo.setName(rs.getString("student_name")); vo.setClassId((Long) rs.getObject("teaching_group_id"));
        vo.setClassName(rs.getString("class_name")); vo.setGender(genderNumber(rs.getString("gender")));
        vo.setStatus(rs.getInt("web_status")); vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime()); return vo;
    }
    private String buildWhere(StudentQueryDTO dto, List<Long> scope, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder();
        if (StringUtils.hasText(dto.getKeyword())) {
            params.addValue("keyword", "%" + dto.getKeyword().trim() + "%");
            sql.append(" AND (s.student_no LIKE :keyword OR s.student_name LIKE :keyword)");
        }
        if (dto.getClassId() != null) { params.addValue("classId", dto.getClassId()); sql.append(" AND e.teaching_group_id=:classId"); }
        if (scope != null) {
            if (scope.isEmpty()) sql.append(" AND 1=0");
            else { params.addValue("scopeIds", scope); sql.append(" AND e.teaching_group_id IN (:scopeIds)"); }
        }
        return sql.toString();
    }
    private GroupRow group(Long id) {
        List<GroupRow> rows = jdbcTemplate.query("""
                SELECT id,term_id,company_name_snapshot,class_code FROM teaching_group
                WHERE id=? AND status<>'ARCHIVED'
                """, (rs, n) -> new GroupRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)), id);
        if (rows.isEmpty()) throw new BusinessException("班级不存在或已归档"); return rows.get(0);
    }
    private Long adminClass(String name) {
        if (!StringUtils.hasText(name)) return null;
        String value = name.trim();
        jdbcTemplate.update("INSERT INTO administrative_class(administrative_class_name) VALUES (?) ON DUPLICATE KEY UPDATE administrative_class_name=VALUES(administrative_class_name)", value);
        return jdbcTemplate.queryForObject("SELECT id FROM administrative_class WHERE administrative_class_name=?", Long.class, value);
    }
    private String adminClassName(String value) { return StringUtils.hasText(value) ? value.trim() : "未填写"; }
    private int nextNo(Long groupId) {
        Integer n = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(in_class_no),0)+1 FROM student_course_enrollment WHERE teaching_group_id=?", Integer.class, groupId);
        return n == null ? 1 : n;
    }
    private void checkScope(Long groupId, List<Long> scope) {
        if (scope != null && !scope.contains(groupId)) throw new BusinessException("无权访问该学生所属班级");
    }
    private String genderText(Integer v) { return v != null && v == 1 ? "男" : v != null && v == 2 ? "女" : "未知"; }
    private int genderNumber(String v) { return "男".equals(v) ? 1 : "女".equals(v) ? 2 : 0; }
    private record GroupRow(Long id, Long termId, String companyName, String classCode) {}
}
