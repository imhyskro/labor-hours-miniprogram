package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 助教分数批量保存 DTO
 */
@Data
public class ScoreSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待保存的分数条目 */
    private List<Item> items;

    /**
     * 单条分数
     */
    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 助教（student 表主键） */
        private Long assistantStudentId;

        /** 打分对应的班级ID（assistant_class.class_id） */
        private Long classId;

        /** 第几次课 */
        private Integer sessionNo;

        /** 本次课分数（0~10） */
        private BigDecimal score;
    }
}
