package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 公司聚合人员 VO（老师或助教）
 *
 * <p>用于公司详情首行展示"该公司所有班级涉及的老师/助教名单"。</p>
 */
@Data
public class StaffMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键：老师=sys_user.id，助教=student.id */
    private Long id;

    /** 姓名：老师=real_name，助教=student.name */
    private String name;

    /** 标识：老师=username，助教=student_id（学号） */
    private String identifier;
}
