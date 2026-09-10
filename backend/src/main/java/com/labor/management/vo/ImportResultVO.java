package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果 VO
 */
@Data
public class ImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总行数（不含表头） */
    private Integer total;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 错误详情列表 */
    private List<ImportErrorVO> errors = new ArrayList<>();
}
