package com.labor.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 班级新增 DTO
 *
 * <p>班级 = 公司 + 周几(1~5) + 开始节次 + 结束节次；节次固定四档 1-2/3-4/5-6/7-8，
 * 编码与名称由后端自动生成。</p>
 */
@Data
public class ClassCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择公司")
    private Long companyId;

    @NotNull(message = "周几不能为空")
    private Integer week;

    @NotNull(message = "开始节次不能为空")
    private Integer startSession;

    @NotNull(message = "结束节次不能为空")
    private Integer endSession;

    /** 学年（可空，默认 24-25） */
    private String academicYear;
}
