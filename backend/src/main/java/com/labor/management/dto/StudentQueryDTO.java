package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生分页查询 DTO
 */
@Data
public class StudentQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关键词（姓名/学号模糊搜索） */
    private String keyword;

    /** 班级ID（可选筛选） */
    private Long classId;

    /** 当前页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}
