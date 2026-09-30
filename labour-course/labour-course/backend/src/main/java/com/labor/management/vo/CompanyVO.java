package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公司响应 VO
 */
@Data
public class CompanyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 公司名称 */
    private String name;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态: 1=启用, 0=停用 */
    private Integer status;

    /** 公司下班级数 */
    private Long classCount;

    private LocalDateTime createdAt;
}
