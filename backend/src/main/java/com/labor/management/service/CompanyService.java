package com.labor.management.service;

import com.labor.management.vo.CompanyStaffVO;
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

    /**
     * 按公司聚合老师与助教名单（去重）
     *
     * <p>用于公司详情首行展示。仅返回未逻辑删除的账号与学生。</p>
     */
    CompanyStaffVO getCompanyStaff(Long companyId);

    /**
     * 按公司维度分配教师负责班级（增量覆盖，不影响该教师在其他公司的分配）
     *
     * <p>事务内：① 校验公司存在 ② 校验每个 classId 存在且 classes.company_id = companyId
     * （防跨公司注入）③ 去重 ④ 物理删除该教师在 companyId 范围内的旧关联 ⑤ 逐条插入新关联。
     * 空数组合法：仅执行 DELETE 步骤，等于"清空本公司分配"。</p>
     *
     * @param companyId 公司 ID
     * @param userId    教师 sys_user 主键
     * @param classIds  本次本公司内勾选的班级 ID 列表（去重，可空）
     */
    void assignTeacherClassesByCompany(Long companyId, Long userId, List<Long> classIds);

    /**
     * 按公司维度分配助教负责班级（增量覆盖，不影响该助教在其他公司的分配）
     *
     * <p>语义同 {@link #assignTeacherClassesByCompany}，作用于 assistant_class 关联表。</p>
     *
     * @param companyId          公司 ID
     * @param assistantStudentId 助教的 student.id
     * @param classIds           本次本公司内勾选的班级 ID 列表
     */
    void assignAssistantClassesByCompany(Long companyId, Long assistantStudentId, List<Long> classIds);
}
