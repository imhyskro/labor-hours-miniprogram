package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Company;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.CompanyMapper;
import com.labor.management.service.CompanyService;
import com.labor.management.vo.CompanyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公司 Service 实现
 */
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;
    private final ClassesMapper classesMapper;

    @Override
    public List<CompanyVO> listAll() {
        List<Company> list = companyMapper.selectList(
                new LambdaQueryWrapper<Company>()
                        .eq(Company::getStatus, 1)
                        .orderByAsc(Company::getSortOrder)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void create(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("公司名称不能为空");
        }
        String trimmed = name.trim();
        Long exists = companyMapper.selectCount(
                new LambdaQueryWrapper<Company>().eq(Company::getName, trimmed)
        );
        if (exists != null && exists > 0) {
            throw new BusinessException("公司名称已存在：" + trimmed);
        }
        // 排序号取当前最大值 +1
        List<Company> all = companyMapper.selectList(null);
        int maxOrder = all.stream().mapToInt(c -> c.getSortOrder() == null ? 0 : c.getSortOrder()).max().orElse(0);

        Company company = new Company();
        company.setName(trimmed);
        company.setSortOrder(maxOrder + 1);
        company.setStatus(1);
        companyMapper.insert(company);
    }

    @Override
    public void rename(Long id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("公司名称不能为空");
        }
        Company company = companyMapper.selectById(id);
        if (company == null) {
            throw new BusinessException("公司不存在");
        }
        String trimmed = name.trim();
        Long exists = companyMapper.selectCount(
                new LambdaQueryWrapper<Company>()
                        .eq(Company::getName, trimmed)
                        .ne(Company::getId, id)
        );
        if (exists != null && exists > 0) {
            throw new BusinessException("公司名称已存在：" + trimmed);
        }
        company.setName(trimmed);
        companyMapper.updateById(company);
    }

    @Override
    public void delete(Long id) {
        Company company = companyMapper.selectById(id);
        if (company == null) {
            throw new BusinessException("公司不存在");
        }
        // 公司下存在班级则禁止删除
        Long classCount = classesMapper.selectCount(
                new LambdaQueryWrapper<Classes>().eq(Classes::getCompanyId, id)
        );
        if (classCount != null && classCount > 0) {
            throw new BusinessException("该公司下存在班级，无法删除（请先删除班级）");
        }
        companyMapper.deleteById(id);
    }

    private CompanyVO toVO(Company entity) {
        CompanyVO vo = new CompanyVO();
        BeanUtils.copyProperties(entity, vo);
        Long classCount = classesMapper.selectCount(
                new LambdaQueryWrapper<Classes>().eq(Classes::getCompanyId, entity.getId())
        );
        vo.setClassCount(classCount);
        return vo;
    }
}
