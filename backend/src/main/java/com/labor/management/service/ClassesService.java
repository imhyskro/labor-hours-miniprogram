package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.ClassCreateDTO;
import com.labor.management.dto.ClassUpdateDTO;
import com.labor.management.vo.ClassVO;

import java.util.List;

/**
 * 班级 Service 接口
 */
public interface ClassesService {

    /**
     * 分页查询班级（按创建时间倒序）
     */
    IPage<ClassVO> pageQuery(Integer page, Integer size);

    /**
     * 根据 ID 查询班级
     */
    ClassVO getById(Long id);

    /**
     * 新增班级（校验 classCode 唯一性）
     */
    void create(ClassCreateDTO dto);

    /**
     * 修改班级（校验 classCode 唯一性，排除自身）
     */
    void update(ClassUpdateDTO dto);

    /**
     * 逻辑删除班级
     */
    void deleteById(Long id);

    /**
     * 查询所有启用的班级（下拉框用）
     */
    List<ClassVO> listAll();
}
