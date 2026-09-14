package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.dto.StudentQueryDTO;
import com.labor.management.dto.StudentUpdateDTO;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.entity.SysUser;
import com.labor.management.entity.UserRole;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.AssistantClassMapper;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.mapper.UserRoleMapper;
import com.labor.management.service.StudentService;
import com.labor.management.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 学生 Service 实现
 *
 * <p>查询/单条操作方法支持数据隔离：当 scopeClassIds 非 null 时，限制学生 class_id 必须在范围内。</p>
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final ClassesMapper classesMapper;
    private final AssistantClassMapper assistantClassMapper;
    private final SysUserMapper sysUserMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public IPage<StudentVO> pageQuery(StudentQueryDTO queryDTO, List<Long> scopeClassIds) {
        Page<StudentVO> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        String keyword = StringUtils.hasText(queryDTO.getKeyword())
                ? queryDTO.getKeyword().trim()
                : null;
        Long classId = queryDTO.getClassId();
        return studentMapper.selectStudentPage(pageParam, keyword, classId, scopeClassIds);
    }

    @Override
    public StudentVO getById(Long id, List<Long> scopeClassIds) {
        Student entity = studentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("学生不存在");
        }
        checkScope(entity.getClassId(), scopeClassIds);
        return toVO(entity);
    }

    @Override
    public void create(StudentCreateDTO dto, List<Long> scopeClassIds) {
        // 校验 studentId 唯一
        Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentId, dto.getStudentId())
        );
        if (count != null && count > 0) {
            throw new BusinessException("学号已存在");
        }

        // 校验班级存在
        Classes classes = classesMapper.selectById(dto.getClassId());
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }
        checkScope(dto.getClassId(), scopeClassIds);

        Student entity = new Student();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1);
        studentMapper.insert(entity);
    }

    @Override
    public void update(StudentUpdateDTO dto, List<Long> scopeClassIds) {
        Student existing = studentMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }
        // 旧班级范围校验
        checkScope(existing.getClassId(), scopeClassIds);

        // 校验 studentId 唯一，排除自身
        Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentId, dto.getStudentId())
                        .ne(Student::getId, dto.getId())
        );
        if (count != null && count > 0) {
            throw new BusinessException("学号已存在");
        }

        // 校验班级存在
        Classes classes = classesMapper.selectById(dto.getClassId());
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }
        // 新班级范围校验
        checkScope(dto.getClassId(), scopeClassIds);

        BeanUtils.copyProperties(dto, existing);
        studentMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id, List<Long> scopeClassIds) {
        Student existing = studentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }
        checkScope(existing.getClassId(), scopeClassIds);
        // 清理助教负责班级关联
        assistantClassMapper.physicalDeleteByAssistant(id);
        // 清理登录账号与角色关联（若为助教）
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentId, id)
        );
        for (SysUser user : users) {
            userRoleMapper.delete(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId())
            );
            sysUserMapper.deleteById(user.getId());
        }
        studentMapper.deleteById(id);
    }

    /**
     * 实体转 VO，补充班级名称
     */
    private StudentVO toVO(Student entity) {
        StudentVO vo = new StudentVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getClassId() != null) {
            Classes classes = classesMapper.selectById(entity.getClassId());
            if (classes != null) {
                vo.setClassName(classes.getClassName());
            }
        }
        return vo;
    }

    /**
     * 校验某班级 ID 是否在当前用户数据范围内
     *
     * @param classId       要操作的班级ID（可能为 null，如助教学生非本学期）
     * @param scopeClassIds null=不限制；非 null 时 classId 必须在列表内（classId 为 null 时直接拒绝）
     */
    private void checkScope(Long classId, List<Long> scopeClassIds) {
        if (scopeClassIds == null) {
            return;
        }
        if (classId == null || !scopeClassIds.contains(classId)) {
            throw new BusinessException("无权限操作该班级的学生（不在您负责范围内）");
        }
    }
}
