package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 助教导入 DTO（Excel 单行解析结果）
 *
 * <p>列顺序：学号 / 姓名 / 公司名称 / 周次 / 开始节次 / 结束节次 / 班内编号 / 原始专业。
 * 助教本质是学生 + 登录账号，导入后自动开通账号（用户名=学号，初始密码）。</p>
 */
@Data
public class AssistantImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学号（同时作为登录账号） */
    private String studentId;
    /** 姓名 */
    private String name;
    /** 公司名称 */
    private String companyName;
    /** 周次 */
    private String week;
    /** 开始节次 */
    private String startSession;
    /** 结束节次 */
    private String endSession;
    /** 班级内编号 */
    private String studentNoInClass;
    /** 原始专业 */
    private String originalMajor;
}
