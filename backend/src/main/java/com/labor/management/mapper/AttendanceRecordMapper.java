package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 考勤记录 Mapper。 */
@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    /**
     * 物理删除一条考勤记录，使同一学生之后仍可在同一课次重新登记。
     * attendance_record 的唯一键未包含逻辑删除字段，因此这里不能使用逻辑删除。
     */
    @Delete("DELETE FROM attendance_record WHERE session_id=#{sessionId} AND student_id=#{studentId}")
    int physicalDelete(@Param("sessionId") Long sessionId,
                       @Param("studentId") Long studentId);
}
