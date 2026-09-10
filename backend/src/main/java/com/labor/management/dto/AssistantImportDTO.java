package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 助教导入 DTO（Excel 单行解析结果）
 *
 * <p>字段顺序对应助教导入模板：学号 / 姓名 / 班级名称。
 * 班级名称为助教"所属班级"，非"负责班级"。</p>
 */
@Data
public class AssistantImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学号 */
    private String studentId;

    /** 姓名 */
    private String name;

    /** 所属班级名称（按名称匹配 classes.class_name） */
    private String className;
}
