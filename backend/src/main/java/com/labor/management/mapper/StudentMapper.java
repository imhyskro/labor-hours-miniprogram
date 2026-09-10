package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.entity.Student;
import com.labor.management.vo.MasterListViewVO;
import com.labor.management.vo.StudentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生 Mapper
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 分页查询学生（关联班级表获取班级名称，支持关键词模糊搜索、班级筛选）
     *
     * @param page    分页参数
     * @param keyword 关键词（学号或姓名模糊搜索）
     * @param classId 班级ID（可选）
     * @return 分页结果
     */
    IPage<StudentVO> selectStudentPage(IPage<StudentVO> page,
                                       @Param("keyword") String keyword,
                                       @Param("classId") Long classId);

    /**
     * 总表分页查询（关联 classes + sys_user）
     *
     * @param page     分页参数
     * @param keyword  关键词（学号或姓名模糊搜索）
     * @param classId  所属班级ID（可选）
     * @param identity 身份筛选：null=全部, "STUDENT"=普通学生, "ASSISTANT"=助教
     * @return 分页结果
     */
    IPage<MasterListViewVO> selectMasterListPage(IPage<MasterListViewVO> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("classId") Long classId,
                                                 @Param("identity") String identity);

    /**
     * 查询未分配（负责班级）的助教列表
     *
     * @return 助教列表（assigned_class_id IS NULL）
     */
    List<MasterListViewVO> selectUnassignedAssistants();
}

