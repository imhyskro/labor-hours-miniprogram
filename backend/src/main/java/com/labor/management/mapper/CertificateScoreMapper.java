package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.CertificateScore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 换证考试成绩 Mapper。 */
@Mapper
public interface CertificateScoreMapper extends BaseMapper<CertificateScore> {

    /** 物理删除成绩，避免逻辑删除记录占用“学生+学年+学期”唯一键。 */
    @Delete("DELETE FROM certificate_score " +
            "WHERE student_id=#{studentId} AND academic_year=#{academicYear} AND semester=#{semester}")
    int physicalDelete(@Param("studentId") Long studentId,
                       @Param("academicYear") String academicYear,
                       @Param("semester") Integer semester);
}
