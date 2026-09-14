package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.entity.AssistantClass;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.entity.SysRole;
import com.labor.management.entity.SysUser;
import com.labor.management.entity.UserRole;
import com.labor.management.enums.RoleCodeEnum;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.AssistantClassMapper;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.mapper.SysRoleMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.mapper.UserRoleMapper;
import com.labor.management.service.AssistantAssignmentService;
import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 助教管理 Service 实现
 *
 * <p>助教与负责班级为多对多（assistant_class）：一个助教可负责多个班级，
 * 一个班级也可分配多个助教。分配采用全量覆盖（物理清旧 + 重新插入）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantAssignmentServiceImpl implements AssistantAssignmentService {

    private final StudentMapper studentMapper;
    private final ClassesMapper classesMapper;
    private final AssistantClassMapper assistantClassMapper;
    private final SysUserMapper sysUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /** 助教账号初始密码 */
    @Value("${app.default-password:cdjcc123456}")
    private String defaultPassword;

    @Override
    public IPage<AssistantVO> pageAssistants(Integer page, Integer size, String keyword) {
        Page<AssistantVO> pageParam = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return studentMapper.selectAssistantPage(pageParam, kw);
    }

    @Override
    public List<Long> getAssistantClassIds(Long assistantStudentId) {
        List<AssistantClass> list = assistantClassMapper.selectList(
                new LambdaQueryWrapper<AssistantClass>()
                        .eq(AssistantClass::getAssistantStudentId, assistantStudentId)
        );
        return list.stream()
                .map(AssistantClass::getClassId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignClasses(Long assistantStudentId, List<Long> classIds) {
        Student student = studentMapper.selectById(assistantStudentId);
        if (student == null) {
            throw new BusinessException("助教不存在");
        }
        if (student.getIsAssistant() == null || student.getIsAssistant() != 1) {
            throw new BusinessException("该学生不是助教，无法分配班级");
        }

        // 校验班级存在并去重
        Set<Long> uniqueClassIds = new LinkedHashSet<>();
        if (classIds != null) {
            for (Long classId : classIds) {
                if (classId == null) continue;
                Classes classes = classesMapper.selectById(classId);
                if (classes == null) {
                    throw new BusinessException("班级不存在：" + classId);
                }
                uniqueClassIds.add(classId);
            }
        }

        // 物理清空旧关联，再重新插入（关联表无逻辑删价值）
        assistantClassMapper.physicalDeleteByAssistant(assistantStudentId);
        for (Long classId : uniqueClassIds) {
            AssistantClass ac = new AssistantClass();
            ac.setAssistantStudentId(assistantStudentId);
            ac.setClassId(classId);
            assistantClassMapper.insert(ac);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAssistant(Long assistantStudentId) {
        Student student = studentMapper.selectById(assistantStudentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        if (student.getIsAssistant() == null || student.getIsAssistant() != 1) {
            throw new BusinessException("该学生不是助教");
        }

        // 清理负责班级关联
        assistantClassMapper.physicalDeleteByAssistant(assistantStudentId);

        // 删除助教登录账号与角色关联（学生记录保留，恢复为普通学生）
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentId, assistantStudentId)
        );
        for (SysUser user : users) {
            userRoleMapper.delete(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId())
            );
            sysUserMapper.deleteById(user.getId());
        }

        // 恢复普通学生身份
        student.setIsAssistant(0);
        studentMapper.updateById(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setIdentity(Long studentId, boolean isAssistant) {
        if (isAssistant) {
            promoteToAssistant(studentId);
        } else {
            revokeAssistant(studentId);
        }
    }

    /**
     * 设置助教身份：is_assistant=1 + 创建/复活登录账号 + 关联 ASSISTANT 角色
     *
     * <p>关键点：取消助教身份时账号被逻辑删除（deleted=1），但 username 仍占 UNIQUE 约束。
     * 重新设置助教身份时不能再 INSERT（会触发 uk_sys_user_username 唯一键冲突），
     * 必须查询到旧账号（含 deleted=1 的）并复活：UPDATE deleted=0、重置密码、重置 student_id 等。</p>
     */
    private void promoteToAssistant(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        if (!StringUtils.hasText(student.getStudentId())) {
            throw new BusinessException("学生学号为空，无法创建助教账号");
        }
        // 已是助教：幂等返回
        if (student.getIsAssistant() != null && student.getIsAssistant() == 1) {
            return;
        }

        // 查 ASSISTANT 角色
        SysRole assistantRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, RoleCodeEnum.ASSISTANT.getCode())
        );
        if (assistantRole == null) {
            throw new BusinessException("ASSISTANT 角色未初始化，请先执行 init_schema.sql");
        }

        // 查 username 是否已有账号（含已逻辑删除的），决定 INSERT 还是复活
        SysUser existingUser = sysUserMapper.selectByUsernameIncludeDeleted(student.getStudentId());
        if (existingUser != null) {
            // 复活旧账号：直接用原生 SQL UPDATE 绕过 @TableLogic 自动加 deleted=0 过滤
            sysUserMapper.reviveAndReset(
                    existingUser.getId(),
                    passwordEncoder.encode(defaultPassword),
                    student.getName(),
                    student.getId(),
                    LocalDateTime.now()
            );

            // 复活/新增 ASSISTANT 角色关联（user_role 也是逻辑删除，需用原生 SQL 绕过过滤）
            UserRole existingRole = userRoleMapper.selectByUserAndRoleIncludeDeleted(
                    existingUser.getId(), assistantRole.getId());
            if (existingRole != null) {
                // 历史有记录（含 deleted=1），复活
                userRoleMapper.reviveByUserAndRole(existingUser.getId(), assistantRole.getId());
            } else {
                // 无任何历史记录，直接插入
                UserRole userRole = new UserRole();
                userRole.setUserId(existingUser.getId());
                userRole.setRoleId(assistantRole.getId());
                userRoleMapper.insert(userRole);
            }
        } else {
            // 无任何同名账号（含 deleted=1），新建
            SysUser user = new SysUser();
            user.setUsername(student.getStudentId());
            user.setPasswordHash(passwordEncoder.encode(defaultPassword));
            user.setRealName(student.getName());
            user.setStatus(1);
            user.setFirstLogin(1);
            user.setStudentId(student.getId());
            user.setLastPasswordChangeTime(LocalDateTime.now());
            sysUserMapper.insert(user);

            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(assistantRole.getId());
            userRoleMapper.insert(userRole);
        }

        student.setIsAssistant(1);
        studentMapper.updateById(student);
        log.info("设置助教身份: studentId={}, studentNo={}", studentId, student.getStudentId());
    }

    @Override
    public List<MasterListViewVO> getUnassignedAssistants() {
        return studentMapper.selectUnassignedAssistants();
    }
}
