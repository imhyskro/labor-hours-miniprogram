package com.labor.management.service.impl;

import com.labor.management.exception.BusinessException;
import com.labor.management.service.ClassesService;
import com.labor.management.service.CompanyService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.CompanyStaffVO;
import com.labor.management.vo.CompanyVO;
import com.labor.management.vo.StaffMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final JdbcTemplate jdbcTemplate;
    private final ClassesService classesService;

    @Override
    public List<CompanyVO> listAll() {
        return jdbcTemplate.query("""
                SELECT c.id, c.company_name, c.created_at,
                       (SELECT COUNT(*) FROM teaching_group g WHERE g.company_id=c.id AND g.status<>'ARCHIVED') class_count
                FROM company c ORDER BY c.id
                """, (rs, n) -> {
            CompanyVO vo = new CompanyVO(); vo.setId(rs.getLong("id")); vo.setName(rs.getString("company_name"));
            vo.setSortOrder((int) rs.getLong("id")); vo.setStatus(1); vo.setClassCount(rs.getLong("class_count"));
            vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime()); return vo;
        });
    }

    @Override
    @Transactional
    public void create(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) throw new BusinessException("公司名称不能为空");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company WHERE company_name=?", Integer.class, value);
        if (count != null && count > 0) throw new BusinessException("公司名称已存在：" + value);
        String code = "WEB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO company(company_code, company_name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, code); ps.setString(2, value); return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new BusinessException("公司创建失败");
        classesService.ensureDefaultClasses(key.longValue());
    }

    @Override
    @Transactional
    public void rename(Long id, String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) throw new BusinessException("公司名称不能为空");
        requireCompany(id);
        Integer duplicate = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company WHERE company_name=? AND id<>?", Integer.class, value, id);
        if (duplicate != null && duplicate > 0) throw new BusinessException("公司名称已存在：" + value);
        jdbcTemplate.update("UPDATE company SET company_name=? WHERE id=?", value, id);
        jdbcTemplate.update("UPDATE teaching_group SET company_name_snapshot=? WHERE company_id=? AND status<>'ARCHIVED'", value, id);
        jdbcTemplate.update("""
                UPDATE student_course_enrollment e JOIN teaching_group g ON g.id=e.teaching_group_id
                SET e.company_name_snapshot=? WHERE g.company_id=? AND e.status='ACTIVE'
                """, value, id);
        jdbcTemplate.update("""
                UPDATE assistant_group_assignment a JOIN teaching_group g ON g.id=a.teaching_group_id
                SET a.company_name_snapshot=? WHERE g.company_id=? AND a.unassigned_at IS NULL
                """, value, id);
    }

    @Override
    public void delete(Long id) {
        requireCompany(id);
        Integer groups = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teaching_group WHERE company_id=?", Integer.class, id);
        if (groups != null && groups > 0) throw new BusinessException("该公司已有关联教学班，不能删除");
        jdbcTemplate.update("DELETE FROM company WHERE id=?", id);
    }

    @Override
    public CompanyStaffVO getCompanyStaff(Long companyId) {
        requireCompany(companyId);
        List<StaffMemberVO> teachers = jdbcTemplate.query("""
                SELECT DISTINCT u.id, u.real_name, u.username FROM teaching_group g
                JOIN sys_user u ON u.id=g.current_teacher_user_id
                WHERE g.company_id=? AND g.status<>'ARCHIVED' ORDER BY u.username
                """, (rs, n) -> staff(rs.getLong(1), rs.getString(2), rs.getString(3)), companyId);
        List<StaffMemberVO> assistants = jdbcTemplate.query("""
                SELECT DISTINCT s.id, p.student_name_snapshot, p.student_no_snapshot
                FROM assistant_group_assignment a JOIN teaching_group g ON g.id=a.teaching_group_id
                JOIN assistant_profile p ON p.id=a.assistant_profile_id LEFT JOIN student s ON s.id=p.student_id
                WHERE g.company_id=? AND a.unassigned_at IS NULL AND p.status='ACTIVE'
                ORDER BY p.student_no_snapshot
                """, (rs, n) -> staff((Long) rs.getObject(1), rs.getString(2), rs.getString(3)), companyId);
        CompanyStaffVO vo = new CompanyStaffVO(); vo.setTeachers(teachers); vo.setAssistants(assistants); return vo;
    }

    @Override
    @Transactional
    public void assignTeacherClassesByCompany(Long companyId, Long userId, List<Long> classIds) {
        requireCompany(companyId); requireRole(userId, "TEACHER", "教师不存在或账号没有教师角色");
        Set<Long> selected = validateGroups(companyId, classIds);
        Long operator = SecurityUtil.getCurrentUserId();
        List<Long> current = jdbcTemplate.queryForList("SELECT id FROM teaching_group WHERE company_id=? AND current_teacher_user_id=?",
                Long.class, companyId, userId);
        for (Long groupId : current) if (!selected.contains(groupId)) {
            jdbcTemplate.update("UPDATE teacher_assignment_history SET unassigned_at=CURRENT_TIMESTAMP(3) WHERE teaching_group_id=? AND unassigned_at IS NULL", groupId);
            jdbcTemplate.update("UPDATE teaching_group SET current_teacher_user_id=NULL WHERE id=?", groupId);
        }
        for (Long groupId : selected) {
            Long old = jdbcTemplate.query("SELECT current_teacher_user_id FROM teaching_group WHERE id=?", rs -> rs.next() ? (Long) rs.getObject(1) : null, groupId);
            if (Objects.equals(old, userId)) continue;
            jdbcTemplate.update("UPDATE teacher_assignment_history SET unassigned_at=CURRENT_TIMESTAMP(3) WHERE teaching_group_id=? AND unassigned_at IS NULL", groupId);
            jdbcTemplate.update("UPDATE teaching_group SET current_teacher_user_id=? WHERE id=?", userId, groupId);
            jdbcTemplate.update("""
                    INSERT INTO teacher_assignment_history(teaching_group_id, teacher_user_id,
                      teacher_username_snapshot, teacher_name_snapshot, assigned_at, assigned_by_user_id)
                    SELECT ?, u.id, u.username, u.real_name, CURRENT_TIMESTAMP(3), ? FROM sys_user u WHERE u.id=?
                    """, groupId, operator, userId);
        }
    }

    @Override
    @Transactional
    public void assignAssistantClassesByCompany(Long companyId, Long assistantStudentId, List<Long> classIds) {
        requireCompany(companyId);
        Long profileId = jdbcTemplate.query("SELECT id FROM assistant_profile WHERE student_id=? AND status='ACTIVE'",
                rs -> rs.next() ? rs.getLong(1) : null, assistantStudentId);
        if (profileId == null) throw new BusinessException("助教不存在或身份未启用");
        Set<Long> selected = validateGroups(companyId, classIds);
        Long operator = SecurityUtil.getCurrentUserId();
        List<Long> current = jdbcTemplate.queryForList("""
                SELECT a.teaching_group_id FROM assistant_group_assignment a
                JOIN teaching_group g ON g.id=a.teaching_group_id
                WHERE a.assistant_profile_id=? AND a.unassigned_at IS NULL AND g.company_id=?
                """, Long.class, profileId, companyId);
        for (Long groupId : current) if (!selected.contains(groupId)) {
            jdbcTemplate.update("UPDATE assistant_group_assignment SET unassigned_at=CURRENT_TIMESTAMP(3) WHERE assistant_profile_id=? AND teaching_group_id=? AND unassigned_at IS NULL",
                    profileId, groupId);
        }
        for (Long groupId : selected) if (!current.contains(groupId)) {
            jdbcTemplate.update("""
                    INSERT INTO assistant_group_assignment(assistant_profile_id, teaching_group_id,
                      assistant_no_snapshot, assistant_name_snapshot, company_name_snapshot,
                      class_code_snapshot, assigned_by_user_id)
                    SELECT p.id, g.id, p.student_no_snapshot, p.student_name_snapshot,
                           g.company_name_snapshot, g.class_code, ?
                    FROM assistant_profile p JOIN teaching_group g ON g.id=? WHERE p.id=?
                    """, operator, groupId, profileId);
        }
    }

    private Set<Long> validateGroups(Long companyId, List<Long> ids) {
        Set<Long> result = new LinkedHashSet<>();
        if (ids == null) return result;
        for (Long id : ids) {
            if (id == null) continue;
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teaching_group WHERE id=? AND company_id=? AND status<>'ARCHIVED'",
                    Integer.class, id, companyId);
            if (count == null || count == 0) throw new BusinessException("班级不存在或不属于当前公司：" + id);
            result.add(id);
        }
        return result;
    }
    private void requireCompany(Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company WHERE id=?", Integer.class, id);
        if (count == null || count == 0) throw new BusinessException("公司不存在");
    }
    private void requireRole(Long userId, String role, String message) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_user u JOIN user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
                WHERE u.id=? AND u.status='ACTIVE' AND r.role_code=?
                """, Integer.class, userId, role);
        if (count == null || count == 0) throw new BusinessException(message);
    }
    private StaffMemberVO staff(Long id, String name, String identifier) {
        StaffMemberVO vo = new StaffMemberVO(); vo.setId(id); vo.setName(name); vo.setIdentifier(identifier); return vo;
    }
}
