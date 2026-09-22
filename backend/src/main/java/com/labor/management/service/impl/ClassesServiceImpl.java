package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.ClassCreateDTO;
import com.labor.management.dto.ClassUpdateDTO;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Company;
import com.labor.management.entity.Student;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.CompanyMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.service.ClassesService;
import com.labor.management.vo.ClassStudentVO;
import com.labor.management.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级 Service 实现
 *
 * <p>班级 = 公司 + 周次 + 开始节次 + 结束节次；编码 W-S-E 与名称自动生成。</p>
 * <p>查询/单条操作方法支持数据隔离：当 scopeClassIds 非 null 时，仅返回/操作范围内的班级。</p>
 */
@Service
@RequiredArgsConstructor
public class ClassesServiceImpl implements ClassesService {

    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;
    private final CompanyMapper companyMapper;

    @Override
    public IPage<ClassVO> pageQuery(Integer page, Integer size, List<Long> scopeClassIds) {
        Page<Classes> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Classes> wrapper = new LambdaQueryWrapper<Classes>()
                .orderByAsc(Classes::getCompanyId)
                .orderByAsc(Classes::getWeek)
                .orderByAsc(Classes::getStartSession);
        applyScope(wrapper, scopeClassIds);
        IPage<Classes> entityPage = classesMapper.selectPage(pageParam, wrapper);

        Page<ClassVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    public ClassVO getById(Long id, List<Long> scopeClassIds) {
        Classes entity = classesMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("班级不存在");
        }
        checkScope(entity.getId(), scopeClassIds);
        return toVO(entity);
    }

    @Override
    public void create(ClassCreateDTO dto) {
        validateSessions(dto.getWeek(), dto.getStartSession(), dto.getEndSession());
        Company company = companyMapper.selectById(dto.getCompanyId());
        if (company == null) {
            throw new BusinessException("所选公司不存在");
        }
        checkDuplicate(dto.getCompanyId(), dto.getWeek(), dto.getStartSession(), dto.getEndSession(), null);

        Classes entity = new Classes();
        entity.setCompanyId(company.getId());
        entity.setWeek(dto.getWeek());
        entity.setStartSession(dto.getStartSession());
        entity.setEndSession(dto.getEndSession());
        entity.setClassCode(buildClassCode(dto.getWeek(), dto.getStartSession(), dto.getEndSession()));
        entity.setClassName(buildClassName(company.getName(), dto.getWeek(), dto.getStartSession(), dto.getEndSession()));
        entity.setAcademicYear(StringUtils.hasText(dto.getAcademicYear()) ? dto.getAcademicYear() : "24-25");
        entity.setStatus(1);
        classesMapper.insert(entity);
    }

    @Override
    public void update(ClassUpdateDTO dto, List<Long> scopeClassIds) {
        Classes existing = classesMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("班级不存在");
        }
        checkScope(existing.getId(), scopeClassIds);
        validateSessions(dto.getWeek(), dto.getStartSession(), dto.getEndSession());
        checkDuplicate(existing.getCompanyId(), dto.getWeek(), dto.getStartSession(), dto.getEndSession(), dto.getId());

        existing.setWeek(dto.getWeek());
        existing.setStartSession(dto.getStartSession());
        existing.setEndSession(dto.getEndSession());
        existing.setClassCode(buildClassCode(dto.getWeek(), dto.getStartSession(), dto.getEndSession()));
        Company company = companyMapper.selectById(existing.getCompanyId());
        String companyName = company != null ? company.getName() : "";
        existing.setClassName(buildClassName(companyName, dto.getWeek(), dto.getStartSession(), dto.getEndSession()));
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        classesMapper.updateById(existing);
    }

    @Override
    public void deleteById(Long id, List<Long> scopeClassIds) {
        Classes existing = classesMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("班级不存在");
        }
        checkScope(id, scopeClassIds);
        // 班级下有学生则禁止删除
        Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, id)
        );
        if (count != null && count > 0) {
            throw new BusinessException("该班级下存在学生，无法删除（请先移除学生）");
        }
        classesMapper.deleteById(id);
    }

    @Override
    public List<ClassVO> listAll(List<Long> scopeClassIds) {
        LambdaQueryWrapper<Classes> wrapper = new LambdaQueryWrapper<Classes>()
                .orderByAsc(Classes::getCompanyId)
                .orderByAsc(Classes::getWeek)
                .orderByAsc(Classes::getStartSession);
        applyScope(wrapper, scopeClassIds);
        List<Classes> list = classesMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ClassVO> listByCompany(Long companyId, List<Long> scopeClassIds) {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("公司不存在");
        }
        LambdaQueryWrapper<Classes> wrapper = new LambdaQueryWrapper<Classes>()
                .eq(Classes::getCompanyId, companyId)
                .orderByAsc(Classes::getWeek)
                .orderByAsc(Classes::getStartSession)
                .orderByAsc(Classes::getEndSession);
        applyScope(wrapper, scopeClassIds);
        List<Classes> list = classesMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ClassStudentVO> getClassStudents(Long classId, List<Long> scopeClassIds) {
        Classes classes = classesMapper.selectById(classId);
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }
        checkScope(classId, scopeClassIds);
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, classId)
                        .orderByAsc(Student::getStudentNoInClass)
        );
        String code = classes.getClassCode();
        List<ClassStudentVO> result = new ArrayList<>();
        for (Student s : students) {
            ClassStudentVO vo = new ClassStudentVO();
            BeanUtils.copyProperties(s, vo);
            if (s.getStudentNoInClass() != null) {
                vo.setFullNo(code + "-" + s.getStudentNoInClass());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public Classes findOrCreateClass(Long companyId, Integer week, Integer startSession, Integer endSession) {
        validateSessions(week, startSession, endSession);
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("公司不存在");
        }
        Classes existing = classesMapper.selectOne(
                new LambdaQueryWrapper<Classes>()
                        .eq(Classes::getCompanyId, companyId)
                        .eq(Classes::getWeek, week)
                        .eq(Classes::getStartSession, startSession)
                        .eq(Classes::getEndSession, endSession)
        );
        if (existing != null) {
            return existing;
        }
        Classes entity = new Classes();
        entity.setCompanyId(companyId);
        entity.setWeek(week);
        entity.setStartSession(startSession);
        entity.setEndSession(endSession);
        entity.setClassCode(buildClassCode(week, startSession, endSession));
        entity.setClassName(buildClassName(company.getName(), week, startSession, endSession));
        entity.setAcademicYear("24-25");
        entity.setStatus(1);
        classesMapper.insert(entity);
        return entity;
    }

    // ==================== 数据范围工具 ====================

    /**
     * 在查询 wrapper 上应用数据范围过滤（仅当 scopeClassIds 非 null 时）
     */
    private void applyScope(LambdaQueryWrapper<Classes> wrapper, List<Long> scopeClassIds) {
        if (scopeClassIds != null) {
            if (scopeClassIds.isEmpty()) {
                // 老师无任何负责班级：永远查不到
                wrapper.eq(Classes::getId, -1L);
            } else {
                wrapper.in(Classes::getId, scopeClassIds);
            }
        }
    }

    /**
     * 校验某班级 ID 是否在当前用户数据范围内
     *
     * @param classId       要操作的班级ID
     * @param scopeClassIds null=不限制；非 null 时必须在列表内
     */
    private void checkScope(Long classId, List<Long> scopeClassIds) {
        if (scopeClassIds == null) {
            return;
        }
        if (!scopeClassIds.contains(classId)) {
            throw new BusinessException("无权限操作该班级（不在您负责范围内）");
        }
    }

    // ==================== 私有工具 ====================

    private void validateSessions(Integer week, Integer start, Integer end) {
        if (week == null || week <= 0) {
            throw new BusinessException("周次必须为正整数");
        }
        if (start == null || start <= 0 || end == null || end <= 0) {
            throw new BusinessException("节次必须为正整数");
        }
        if (start > end) {
            throw new BusinessException("开始节次不能大于结束节次");
        }
    }

    private void checkDuplicate(Long companyId, Integer week, Integer start, Integer end, Long excludeId) {
        LambdaQueryWrapper<Classes> wrapper = new LambdaQueryWrapper<Classes>()
                .eq(Classes::getCompanyId, companyId)
                .eq(Classes::getWeek, week)
                .eq(Classes::getStartSession, start)
                .eq(Classes::getEndSession, end);
        if (excludeId != null) {
            wrapper.ne(Classes::getId, excludeId);
        }
        Long count = classesMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该公司下已存在相同周次和节次的班级（" + buildClassCode(week, start, end) + "）");
        }
    }

    private String buildClassCode(Integer week, Integer start, Integer end) {
        return week + "-" + start + "-" + end;
    }

    private String buildClassName(String companyName, Integer week, Integer start, Integer end) {
        return companyName + "-第" + week + "周-" + start + "~" + end + "节";
    }

    private ClassVO toVO(Classes entity) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(entity, vo);
        // 公司名
        if (entity.getCompanyId() != null) {
            Company company = companyMapper.selectById(entity.getCompanyId());
            if (company != null) {
                vo.setCompanyName(company.getName());
            }
        }
        // 学生数
        Long studentCount = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, entity.getId())
        );
        vo.setStudentCount(studentCount);
        return vo;
    }
}
