package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.service.MasterListService;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 总表 Service 实现
 */
@Service
@RequiredArgsConstructor
public class MasterListServiceImpl implements MasterListService {

    private final StudentMapper studentMapper;

    @Override
    public IPage<MasterListViewVO> getMasterList(Integer page, Integer size,
                                                 String keyword, Long classId, String identity) {
        Page<MasterListViewVO> pageParam = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String idt = StringUtils.hasText(identity) ? identity.trim() : null;
        return studentMapper.selectMasterListPage(pageParam, kw, classId, idt);
    }
}
