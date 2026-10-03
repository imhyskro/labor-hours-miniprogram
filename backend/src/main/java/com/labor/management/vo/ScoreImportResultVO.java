package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 成绩导入结果 VO
 *
 * <p>事务性导入：要么全部成功（successCount=total, failCount=0），要么整批拒绝（successCount=0, failCount=total + 错误列表）。</p>
 */
@Data
public class ScoreImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总行数（不含表头） */
    private Integer total;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 错误详情列表（含预期值 vs 实际值） */
    private List<ScoreImportErrorVO> errors = new ArrayList<>();
}
