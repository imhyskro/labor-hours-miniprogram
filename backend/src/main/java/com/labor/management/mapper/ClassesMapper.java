package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.Classes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 班级 Mapper
 */
@Mapper
public interface ClassesMapper extends BaseMapper<Classes> {

    /**
     * 按「公司+周几+开始节次+结束节次」查询班级（含已逻辑删除记录）。
     *
     * <p>原生 SQL 绕过 @TableLogic 自动追加的 deleted=0 过滤。
     * 联合唯一键 uk_classes_company_session (company_id, week, start_session, end_session)
     * 不含 deleted 列，因此同一组合最多一条记录（含 deleted=1 的）。</p>
     */
    @Select("SELECT * FROM classes " +
            "WHERE company_id = #{companyId} AND week = #{week} " +
            "AND start_session = #{start} AND end_session = #{end} LIMIT 1")
    Classes selectByCompanySessionIncludeDeleted(@Param("companyId") Long companyId,
                                                  @Param("week") Integer week,
                                                  @Param("start") Integer start,
                                                  @Param("end") Integer end);

    /**
     * 复活已逻辑删除的班级（原生 SQL 绕过 @TableLogic），同时刷新名称、节次编码与启用状态。
     */
    @Update("UPDATE classes SET deleted = 0, status = 1, class_name = #{className}, " +
            "class_code = #{classCode}, updated_at = NOW() WHERE id = #{id}")
    int reviveById(@Param("id") Long id,
                   @Param("className") String className,
                   @Param("classCode") String classCode);
}
