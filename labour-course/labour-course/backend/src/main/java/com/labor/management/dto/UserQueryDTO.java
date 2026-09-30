package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户分页查询 DTO
 */
@Data
public class UserQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关键词（用户名/姓名模糊搜索） */
    private String keyword;

    /** 状态筛选: 1=启用, 0=停用, null=全部 */
    private Integer status;

    /** 当前页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}
