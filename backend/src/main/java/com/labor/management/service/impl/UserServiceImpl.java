package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.UserCreateDTO;
import com.labor.management.dto.UserQueryDTO;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Company;
import com.labor.management.entity.Student;
import com.labor.management.entity.SysRole;
import com.labor.management.entity.SysUser;
import com.labor.management.entity.TeacherClass;
import com.labor.management.entity.UserRole;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.CompanyMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.mapper.SysRoleMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.mapper.TeacherClassMapper;
import com.labor.management.mapper.UserRoleMapper;
import com.labor.management.security.UserDetailsServiceImpl;
import com.labor.management.service.UserService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.ClassVO;
import com.labor.management.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户 Service 实现（仅超级管理员可操作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleMapper sysRoleMapper;
    private final UserRoleMapper userRoleMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final ClassesMapper classesMapper;
    private final CompanyMapper companyMapper;
    private final StudentMapper studentMapper;

    /** 初始默认密码，从配置读取（便于统一调整） */
    @Value("${app.default-password:cdjcc123456}")
    private String defaultPassword;

    @Override
    public IPage<UserVO> pageQuery(UserQueryDTO queryDTO) {
        Page<SysUser> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getRealName, kw));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, queryDTO.getStatus());
        }

        IPage<SysUser> entityPage = sysUserMapper.selectPage(pageParam, wrapper);

        Page<UserVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    public UserVO getById(Long id) {
        SysUser entity = sysUserMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(entity);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        // 校验不能操作自己
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException("不能停用或重置自己的账号");
        }

        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(existing);
        log.info("超级管理员修改用户状态: targetUserId={}, status={}", userId, status);
    }

    @Override
    public void resetPassword(Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        // 校验不能操作自己
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException("不能停用或重置自己的账号");
        }

        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        existing.setPasswordHash(passwordEncoder.encode(defaultPassword));
        existing.setFirstLogin(1);
        existing.setLastPasswordChangeTime(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(existing);
        log.info("超级管理员重置用户密码: targetUserId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTeacher(UserCreateDTO dto) {
        // 校验用户名不重复
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 查 TEACHER 角色
        SysRole teacherRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "TEACHER")
        );
        if (teacherRole == null) {
            throw new BusinessException("TEACHER 角色未初始化，请先执行 init_schema.sql");
        }

        // 插入 sys_user
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        user.setRealName(dto.getRealName());
        user.setStatus(1);
        user.setFirstLogin(1);
        user.setLastPasswordChangeTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        // 绑定 TEACHER 角色
        UserRole ur = new UserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(teacherRole.getId());
        userRoleMapper.insert(ur);

        log.info("超级管理员创建教师账号: userId={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    public List<ClassVO> getTeacherClasses(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 查 teacher_class 关联
        List<TeacherClass> rels = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getUserId, userId)
        );
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> classIds = rels.stream().map(TeacherClass::getClassId).distinct().collect(Collectors.toList());
        List<Classes> classes = classesMapper.selectBatchIds(classIds);
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        // 按 classIds 顺序输出，并补充公司名/学生数
        return classes.stream()
                .sorted(java.util.Comparator.comparingInt(c -> classIds.indexOf(c.getId())))
                .map(this::toClassVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignClassesToTeacher(Long userId, List<Long> classIds) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 校验班级存在并去重
        Set<Long> uniqueClassIds = new LinkedHashSet<>();
        if (classIds != null) {
            for (Long classId : classIds) {
                if (classId == null) continue;
                if (classesMapper.selectById(classId) == null) {
                    throw new BusinessException("班级不存在：" + classId);
                }
                uniqueClassIds.add(classId);
            }
        }
        // 物理清空旧关联，再重新插入（关联表无逻辑删价值）
        teacherClassMapper.physicalDeleteByUser(userId);
        for (Long classId : uniqueClassIds) {
            TeacherClass tc = new TeacherClass();
            tc.setUserId(userId);
            tc.setClassId(classId);
            teacherClassMapper.insert(tc);
        }
        log.info("超级管理员设置教师负责班级: userId={}, classCount={}", userId, uniqueClassIds.size());
    }

    /**
     * 实体转 VO，补充角色列表和 firstLogin 布尔值
     */
    private UserVO toVO(SysUser entity) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setRoles(userDetailsService.loadRoleCodes(entity.getId()));
        vo.setFirstLogin(entity.getFirstLogin() != null && entity.getFirstLogin() == 1);
        return vo;
    }

    /**
     * Classes 实体转 ClassVO，补充公司名和学生数
     */
    private ClassVO toClassVO(Classes entity) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getCompanyId() != null) {
            Company company = companyMapper.selectById(entity.getCompanyId());
            if (company != null) {
                vo.setCompanyName(company.getName());
            }
        }
        Long studentCount = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, entity.getId())
        );
        vo.setStudentCount(studentCount);
        return vo;
    }
}
