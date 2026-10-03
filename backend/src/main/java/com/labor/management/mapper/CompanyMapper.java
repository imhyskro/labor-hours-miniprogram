package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.Company;
import com.labor.management.vo.StaffMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公司 Mapper
 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {

    /**
     * 按公司聚合老师名单（去重，仅未逻辑删除的关联与账号）
     *
     * <p>关联路径：teacher_class → classes(=companyId) → sys_user。
     * 显式带 deleted=0 条件以应对 @TableLogic 自动过滤（原生 SQL 中需手动指定）。</p>
     */
    @Select("SELECT DISTINCT u.id, u.real_name AS name, u.username AS identifier " +
            "FROM teacher_class tc " +
            "JOIN sys_user u ON u.id = tc.user_id AND u.deleted = 0 " +
            "JOIN classes c ON c.id = tc.class_id AND c.deleted = 0 " +
            "WHERE c.company_id = #{companyId} " +
            "ORDER BY u.username")
    List<StaffMemberVO> selectCompanyTeachers(@Param("companyId") Long companyId);

    /**
     * 按公司聚合助教名单（去重，仅未逻辑删除的关联与学生）
     *
     * <p>关联路径：assistant_class → classes(=companyId) → student。
     * 注意 assistant_class / teacher_class 为物理删（关联表无 @TableLogic），
     * 因此不需要带 deleted=0 条件，但 student / classes 仍需显式 deleted=0。</p>
     */
    @Select("SELECT DISTINCT s.id, s.name, s.student_id AS identifier " +
            "FROM assistant_class ac " +
            "JOIN student s ON s.id = ac.assistant_student_id AND s.deleted = 0 " +
            "JOIN classes c ON c.id = ac.class_id AND c.deleted = 0 " +
            "WHERE c.company_id = #{companyId} " +
            "ORDER BY s.student_id")
    List<StaffMemberVO> selectCompanyAssistants(@Param("companyId") Long companyId);
}
