package com.labor.management.service;

import com.labor.management.vo.CompanyVO;

import java.util.List;

/**
 * 公司 Service
 */
public interface CompanyService {

    /** 查询所有公司（含班级数，按排序号） */
    List<CompanyVO> listAll();

    /** 新增公司（只需名字） */
    void create(String name);

    /** 公司改名 */
    void rename(Long id, String name);

    /** 删除公司（公司下存在班级时禁止删除） */
    void delete(Long id);
}
