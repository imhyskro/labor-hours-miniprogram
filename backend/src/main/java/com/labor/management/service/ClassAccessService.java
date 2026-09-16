package com.labor.management.service;

/**
 * 考勤与成绩模块的数据范围服务。
 *
 * <p>管理员可访问全部班级；教师仅可访问 teacher_class 中的班级；
 * 助教仅可访问 assistant_class 中分配给自己的班级。</p>
 */
public interface ClassAccessService {

    /** 校验管理员或教师是否可访问指定班级。 */
    void checkTeacherAccess(Long classId);

    /** 校验管理员、教师或助教是否可访问指定班级。 */
    void checkAttendanceAccess(Long classId);

    /** 当前用户关联的助教学生 ID；非助教返回 null。 */
    Long getCurrentAssistantStudentId();
}
