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
        // 校验班内编号唯一（同班同编号，含助教记录，防止助教恢复后编号冲突）
        checkNoInClassUnique(entity.getClassId(), entity.getStudentNoInClass(), null);
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
        // 校验班内编号唯一（同班同编号，含助教记录，排除自身，防止助教恢复后编号冲突）
        checkNoInClassUnique(existing.getClassId(), existing.getStudentNoInClass(), existing.getId());
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
     * 校验班内编号唯一性
     *
     * <p>同一班级内，班内编号（student_no_in_class）必须唯一，且校验范围包含助教记录
     * （is_assistant=1 的学生仍占用其班级内的编号位）。这样可防止以下场景：</p>
     * <ol>
     *   <li>助教 A 原为班1编号5（取消助教后 classId/no 仍保留）</li>
     *   <li>助教期间给班1新增/编辑学生 B 时误用编号5</li>
     *   <li>取消 A 助教身份后，班1出现两个编号5</li>
     * </ol>
     *
     * <p>编号或班级 ID 为 null 时不校验（允许助教不归属本学期班级、或不设编号）。</p>
     *
     * @param classId         目标班级 ID
     * @param studentNoInClass 班内编号
     * @param excludeId       排除的学生 ID（更新时传自身 ID，新增时传 null）
     */
    private void checkNoInClassUnique(Long classId, Integer studentNoInClass, Long excludeId) {
        if (studentNoInClass == null || classId == null) {
            return;
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, classId)
                .eq(Student::getStudentNoInClass, studentNoInClass);
        if (excludeId != null) {
            wrapper.ne(Student::getId, excludeId);
        }
        Long count = studentMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该班级内编号 " + studentNoInClass + " 已被占用（含助教记录），请使用其他编号");
        }
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
