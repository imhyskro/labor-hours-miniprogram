package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 公司聚合人员响应 VO
 *
 * <p>按公司聚合展示该公司所有班级涉及的老师名单与助教名单（去重）。</p>
 */
@Data
public class CompanyStaffVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 老师名单（去重，按 username 排序） */
    private List<StaffMemberVO> teachers;

    /** 助教名单（去重，按学号排序） */
    private List<StaffMemberVO> assistants;
}
