package com.labor.management.service;

import com.labor.management.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel 导入 Service
 */
public interface ExcelImportService {

    /**
     * 导入学生
     *
     * <p>规则：学号必须不存在；班级名称必须存在；批量插入 student 记录（is_assistant=0）。</p>
     *
     * @param file Excel 文件
     * @return 导入结果
     */
    ImportResultVO importStudents(MultipartFile file);

    /**
     * 导入助教
     *
     * <p>规则：学号存在 → 标记助教 + 创建账号（已是助教则跳过）；
     * 学号不存在 → 自动创建 student 记录 + 创建账号。
     * 账号 username=学号，初始密码 cdjcc123456（BCrypt），first_login=1，分配 ASSISTANT 角色。</p>
     *
     * @param file Excel 文件
     * @return 导入结果
     */
    ImportResultVO importAssistants(MultipartFile file);
}
