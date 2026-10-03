package com.labor.management.vo;

import lombok.Data;

import java.util.List;

/**
 * 助教打分表 VO
 *
 * <p>打分页一次返回：上课次数（分数列数）+ 当前页助教行（含各次课分数）。</p>
 */
@Data
public class AssistantScoreTableVO {

    /** 上课次数（分数列数，教师可手动调整） */
    private Integer sessionCount;

    /** 当前页助教行（原表字段 + scores 分数表） */
    private List<AssistantScoreRowVO> records;

    /** 助教总数 */
    private Long total;

    /** 当前页码 */
    private Long current;

    /** 每页条数 */
    private Long size;
}
