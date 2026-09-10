package com.labor.management.service;

import com.labor.management.vo.MasterListViewVO;

import java.util.List;

/**
 * 助教分配 Service
 *
 * <p>分配/取消分配助教到班级；不限制班级唯一性（一个班级可分配多个助教）。</p>
 */
public interface AssistantAssignmentService {

    /**
     * 查询未分配（负责班级）的助教列表
     */
    List<MasterListViewVO> getUnassignedAssistants();

    /**
     * 分配助教到班级
     *
     * @param studentId 助教的 student 主键ID（注意：非学号字符串）
     * @param classId   班级ID
     */
    void assignToClass(Long studentId, Long classId);

    /**
     * 取消助教的班级分配
     *
     * @param studentId 助教的 student 主键ID
     */
    void unassign(Long studentId);
}
