package com.labor.management.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 助教打分表行 VO
 *
 * <p>在总表行（助教原表字段）基础上，附带该助教每次课的分数：
 * key=第几次课，value=本次课分数（0~10）。前端在原表右侧渲染分数列。</p>
 *
 * <p>注意：父类 MasterListViewVO.classId 是助教自己作为学生所在的班（student.class_id），
 * 不能用作打分维度。打分维度是 {@link #scoringClassId}——该助教在当前老师视角下取的
 * 「负责班级」主班ID（assistant_class 与 teacher_class 交集里 id 最小的那个班），
 * 前端保存分数时必须把 scoringClassId 回带到 ScoreItem.classId。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssistantScoreRowVO extends MasterListViewVO {

    /** 打分主班ID（assistant_class ∩ teacher_class 交集里 id 最小的班），前端提交分数时回带 */
    private Long scoringClassId;

    /** 每次课分数：key=第几次课，value=分数 */
    private Map<Integer, BigDecimal> scores = new HashMap<>();
}
