package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.entity.Student;
import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.MasterListViewVO;
import com.labor.management.vo.ScoreSummaryVO;
import com.labor.management.vo.StudentVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 学生 Mapper
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 按学号查询学生（含已逻辑删除的记录，绕过 @TableLogic 自动过滤）
     *
     * <p>用于导入场景：学号唯一键 uk_student_student_id 不含 deleted 列，
     * 已删除学号仍占用唯一键，直接 INSERT 会撞键。需先查含已删记录判断是否复活。</p>
     */
    @Select("SELECT * FROM student WHERE student_id = #{studentId} LIMIT 1")
    Student selectByStudentIdIncludeDeleted(@Param("studentId") String studentId);

    /**
     * 复活已逻辑删除的学生记录（原生 SQL 绕过 @TableLogic）
     */
    @Update("UPDATE student SET deleted = 0, status = 1, updated_at = NOW() WHERE id = #{id}")
    int reviveById(@Param("id") Long id);

    /**
     * 批量插入学生（一次 INSERT 多行 VALUES，提升 8000+ 行导入性能）
     *
     * <p>用于 Excel 批量导入场景：单条 insert 循环 8000 次会产生 8000 次 DB 往返，
     * 改为分批（建议每批 500 行）批量 INSERT 可将 DB 调用次数降到几十次。</p>
     *
     * <p>注意：不显式设置 created_at / updated_at / deleted，由数据库默认值填充
     * （created_at/updated_at 默认 NOW()，deleted 默认 0）。
     * 调用方需保证 list 非空，且单批 ≤ 1000 行（受 MySQL max_allowed_packet 限制）。</p>
     *
     * @param list 待插入的学生列表（Student 实体的 studentId/name/classId/gender/
     *             originalMajor/studentNoInClass/status/isAssistant 字段会被写入）
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO student (student_id, name, class_id, gender, original_major, " +
            "student_no_in_class, status, is_assistant) VALUES " +
            "<foreach collection='list' item='s' separator=','>" +
            "(#{s.studentId}, #{s.name}, #{s.classId}, #{s.gender}, #{s.originalMajor}, " +
            "#{s.studentNoInClass}, #{s.status}, #{s.isAssistant})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<Student> students);

    /**
     * 批量查询已存在的学号（含已逻辑删除记录，绕过 @TableLogic）
     *
     * <p>用于 Excel 导入前的学号唯一性预查：把所有待导入学号一次性查出，
     * 避免循环里逐行 selectByStudentIdIncludeDeleted 产生 N 次 DB 调用。</p>
     *
     * <p>返回的 Student 仅填充业务字段（id/studentId/deleted/name/classId/studentNoInClass/
     * originalMajor/gender/status/isAssistant），其余字段为 null。
     * 调用方按 studentId 建立 Map 索引后即可在循环里 O(1) 命中。</p>
     *
     * @param studentIds 学号列表（建议去重后再调用）
     * @return 已存在的学生记录列表（含已逻辑删除的）
     */
    @Select("<script>" +
            "SELECT id, student_id, name, class_id, gender, original_major, " +
            "student_no_in_class, status, is_assistant, deleted " +
            "FROM student WHERE student_id IN " +
            "<foreach collection='list' item='sid' open='(' separator=',' close=')'>" +
            "#{sid}" +
            "</foreach>" +
            "</script>")
    List<Student> selectByStudentIdsIncludeDeleted(@Param("list") List<String> studentIds);

    /**
     * 批量查询同班级已占用的班内编号
     *
     * <p>用于 Excel 导入前的班内编号唯一性预查：把所有 (classId, noInClass) 对一次性查出，
     * 避免循环里逐行 selectCount 产生 N 次 DB 调用。</p>
     *
     * <p>查询逻辑：对每个出现的 classId，一次性查出该班级下所有已占用的 student_no_in_class
     * 集合，调用方按 classId 分组建立 Set&lt;Integer&gt; 索引后在循环里 O(1) 命中。
     * 返回的 Student 仅填充 classId 和 studentNoInClass 两个字段（依赖 MyBatis-Plus
     * 默认开启的 mapUnderscoreToCamelCase 驼峰映射），其余字段为 null。</p>
     *
     * @param classIds 班级ID列表（去重后传入）
     * @return 占用记录列表：每行 classId + 占用的 noInClass（仅 deleted=0 记录）
     */
    @Select("<script>" +
            "SELECT class_id, student_no_in_class FROM student WHERE deleted = 0 AND class_id IN " +
            "<foreach collection='list' item='cid' open='(' separator=',' close=')'>" +
            "#{cid}" +
            "</foreach>" +
            "</script>")
    List<Student> selectOccupiedNoInClass(@Param("list") List<Long> classIds);

    /**
     * 分页查询学生（关联班级表获取班级名称，支持关键词模糊搜索、班级筛选）
     *
     * @param scopeClassIds 数据范围班级ID列表（null=不限制；非空列表=限定 class_id IN 列表）
     */
    IPage<StudentVO> selectStudentPage(IPage<StudentVO> page,
                                       @Param("keyword") String keyword,
                                       @Param("classId") Long classId,
                                       @Param("scopeClassIds") List<Long> scopeClassIds);

    /**
     * 总表分页查询（公司 → 班级 W-S-E → 班内编号）
     *
     * @param page      分页参数
     * @param keyword   关键词（学号或姓名模糊搜索）
     * @param companyId 公司ID（可选）
     * @param classId   班级ID（可选）
     * @param identity  身份筛选：null=全部, "STUDENT"=普通学生, "ASSISTANT"=助教
     * @param scopeClassIds 数据范围班级ID列表（null=不限制；非空列表=限定 class_id IN 列表）
     */
    IPage<MasterListViewVO> selectMasterListPage(IPage<MasterListViewVO> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("companyId") Long companyId,
                                                 @Param("classId") Long classId,
                                                 @Param("identity") String identity,
                                                 @Param("scopeClassIds") List<Long> scopeClassIds);

    /**
     * 助教分页查询（is_assistant=1，含负责班级数量/聚合名）
     */
    IPage<AssistantVO> selectAssistantPage(IPage<AssistantVO> page,
                                           @Param("keyword") String keyword);

    /**
     * 查询尚未分配任何负责班级的助教列表（assistant_class 无记录）
     */
    List<MasterListViewVO> selectUnassignedAssistants();

    /**
     * 查询指定班级下所有在读学生的ID列表（成绩导入「班级齐全校验」用）
     *
     * <p>仅返回 deleted=0 的学生主键ID。导入 Excel 时按班级分组，
     * 与 Excel 中出现的学号集合对比，缺一即报「班级不齐全」错误。</p>
     */
    @Select("SELECT id FROM student WHERE class_id = #{classId} AND deleted = 0")
    List<Long> selectClassStudentIds(@Param("classId") Long classId);

    /**
     * 查询指定班级当前最大班内编号（不含已逻辑删除的记录）
     *
     * <p>用于单条新增学生/助教时自动分配末尾编号：前端不传 studentNoInClass，
     * 后端取 max + 1 作为新编号；若班级尚无学生返回 null，Service 层归 0 后 +1=1。</p>
     */
    @Select("SELECT MAX(student_no_in_class) FROM student WHERE class_id = #{classId} AND deleted = 0")
    Integer selectMaxNoInClass(@Param("classId") Long classId);

    /**
     * 成绩汇总分页查询（总表字段 + 4 项分数，LEFT JOIN 分数表）
     *
     * <p>总成绩 finalScore 由 Service 层按比例加权计算（避免 SQL 中硬编码比例）；
     * 此处仅查 4 项原始分数，缺项以 NULL 返回，Service 层标记 missingItems。</p>
     *
     * @param page           分页参数
     * @param keyword        关键词（学号或姓名模糊搜索）
     * @param companyId      公司ID（可选）
     * @param classId        班级ID（可选）
     * @param identity       身份筛选：null=全部, "STUDENT"=普通学生, "ASSISTANT"=助教
     * @param scopeClassIds  数据范围班级ID列表（null=不限制；非空列表=限定 class_id IN 列表）
     */
    IPage<ScoreSummaryVO> selectScoreSummaryPage(IPage<ScoreSummaryVO> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("companyId") Long companyId,
                                                 @Param("classId") Long classId,
                                                 @Param("identity") String identity,
                                                 @Param("scopeClassIds") List<Long> scopeClassIds);
}
