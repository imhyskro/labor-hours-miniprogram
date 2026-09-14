package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.vo.MasterListViewVO;

import java.util.List;

/**
 * 总表 Service
 *
 * <p>查询方法支持数据隔离：当 scopeClassIds 非 null 时，限制学生 class_id 必须在范围内。</p>
 */
public interface MasterListService {

    /**
     * 总表分页查询（所有学生记录：普通学生 + 助教）
     *
     * @param page      页码（1 起）
     * @param size      每页条数
     * @param keyword   关键词（学号/姓名模糊）
     * @param companyId 公司ID（可选）
     * @param classId   所属班级ID（可选）
     * @param identity  身份筛选：null=全部, "STUDENT"=学生, "ASSISTANT"=助教
     * @param scopeClassIds 数据范围班级ID（null=不限制；非 null=限定 class_id IN 列表）
     * @return 分页结果
     */
    IPage<MasterListViewVO> getMasterList(Integer page, Integer size,
                                          String keyword, Long companyId, Long classId, String identity,
                                          List<Long> scopeClassIds);
}
