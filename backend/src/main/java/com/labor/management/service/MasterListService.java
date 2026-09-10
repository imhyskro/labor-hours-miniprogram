package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.vo.MasterListViewVO;

/**
 * 总表 Service
 */
public interface MasterListService {

    /**
     * 总表分页查询（所有学生记录：普通学生 + 助教）
     *
     * @param page     页码（1 起）
     * @param size     每页条数
     * @param keyword  关键词（学号/姓名模糊）
     * @param classId  所属班级ID（可选）
     * @param identity 身份筛选：null=全部, "STUDENT"=学生, "ASSISTANT"=助教
     * @return 分页结果
     */
    IPage<MasterListViewVO> getMasterList(Integer page, Integer size,
                                          String keyword, Long classId, String identity);
}
