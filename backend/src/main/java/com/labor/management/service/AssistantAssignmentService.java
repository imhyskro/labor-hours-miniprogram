package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.MasterListViewVO;

import java.util.List;

/**
 * 助教管理 Service
 *
 * <p>助教 = is_assistant=1 的学生。老师可：查看助教列表、为助教分配多个负责班级
 * （assistant_class 多对多）、取消助教身份（恢复普通学生）。</p>
 */
public interface AssistantAssignmentService {

    /**
     * 助教分页列表（含负责班级数量与聚合名称）
     */
    IPage<AssistantVO> pageAssistants(Integer page, Integer size, String keyword);

    /**
     * 查询助教当前负责的班级ID列表
     */
    List<Long> getAssistantClassIds(Long assistantStudentId);

    /**
     * 设置助教负责的班级（全量覆盖：重复自动跳过）
     *
     * @param assistantStudentId 助教 student 主键
     * @param classIds           负责班级ID列表（空列表表示清空）
     */
    void assignClasses(Long assistantStudentId, List<Long> classIds);

    /**
     * 取消助教身份，恢复为普通学生（清理负责班级关联与登录账号）
     *
     * @param notEnrolledThisTerm true 时同时清空当前所属班级和班内编号
     */
    void revokeAssistant(Long assistantStudentId, boolean notEnrolledThisTerm);

    /**
     * 设置或取消助教身份
     *
     * <p>设置助教（isAssistant=true）：student.is_assistant=1，并创建登录账号
     * （username=学号，初始密码 cdjcc123456，first_login=1 强制首登改密），
     * 关联 ASSISTANT 角色。</p>
     * <p>取消助教（isAssistant=false）：等价于 {@link #revokeAssistant}，
     * 清理负责班级关联 + 删除登录账号 + student.is_assistant=0。</p>
     *
     * @param studentId   student 表主键
     * @param isAssistant true=设为助教，false=取消助教
     */
    void setIdentity(Long studentId, boolean isAssistant);

    /**
     * 查询尚未分配任何负责班级的助教列表
     */
    List<MasterListViewVO> getUnassignedAssistants();
}
