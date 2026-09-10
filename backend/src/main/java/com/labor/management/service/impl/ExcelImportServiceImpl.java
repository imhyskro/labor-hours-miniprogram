package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.dto.AssistantImportDTO;
import com.labor.management.dto.StudentImportDTO;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.entity.SysRole;
import com.labor.management.entity.SysUser;
import com.labor.management.entity.UserRole;
import com.labor.management.enums.RoleCodeEnum;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.mapper.SysRoleMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.mapper.UserRoleMapper;
import com.labor.management.service.ExcelImportService;
import com.labor.management.vo.ImportErrorVO;
import com.labor.management.vo.ImportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    /** 助教账号初始密码 */
    private static final String ASSISTANT_DEFAULT_PASSWORD = "cdjcc123456";

    private final StudentMapper studentMapper;
    private final ClassesMapper classesMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    // ==================== 学生导入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importStudents(MultipartFile file) {
        List<StudentImportDTO> rows = parseStudentExcel(file);
        ImportResultVO result = new ImportResultVO();
        result.setTotal(rows.size());

        // 预加载所有班级（按名称查ID），避免逐行查库
        Map<String, Classes> classNameMap = loadClassNameMap();

        int success = 0;
        List<ImportErrorVO> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            StudentImportDTO row = rows.get(i);
            int rowNum = i + 2; // 数据从第2行起
            String sid = trim(row.getStudentId());
            String name = trim(row.getName());
            String cls = trim(row.getClassName());
            String gender = trim(row.getGender());

            // 基础字段校验
            if (!StringUtils.hasText(sid)) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号不能为空"));
                continue;
            }
            if (!StringUtils.hasText(name)) {
                errors.add(new ImportErrorVO(rowNum, sid, "姓名不能为空"));
                continue;
            }
            if (!StringUtils.hasText(cls)) {
                errors.add(new ImportErrorVO(rowNum, sid, "班级名称不能为空"));
                continue;
            }

            // 学号唯一性校验
            Long existsCount = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentId, sid)
            );
            if (existsCount != null && existsCount > 0) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号已存在"));
                continue;
            }

            // 班级存在校验
            Classes classes = classNameMap.get(cls);
            if (classes == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "班级不存在：" + cls));
                continue;
            }

            // 性别转换
            Integer genderCode = parseGender(gender);

            Student student = new Student();
            student.setStudentId(sid);
            student.setName(name);
            student.setClassId(classes.getId());
            student.setGender(genderCode);
            student.setStatus(1);
            student.setIsAssistant(0);
            student.setAssignedClassId(null);
            studentMapper.insert(student);
            success++;
        }

        result.setSuccessCount(success);
        result.setFailCount(errors.size());
        result.setErrors(errors);
        return result;
    }

    // ==================== 助教导入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importAssistants(MultipartFile file) {
        List<AssistantImportDTO> rows = parseAssistantExcel(file);
        ImportResultVO result = new ImportResultVO();
        result.setTotal(rows.size());

        // 预加载班级（按名称查ID）
        Map<String, Classes> classNameMap = loadClassNameMap();

        // 预加载 ASSISTANT 角色
        SysRole assistantRole = loadRole(RoleCodeEnum.ASSISTANT.getCode());
        String passwordHash = passwordEncoder.encode(ASSISTANT_DEFAULT_PASSWORD);

        int success = 0;
        List<ImportErrorVO> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            AssistantImportDTO row = rows.get(i);
            int rowNum = i + 2;
            String sid = trim(row.getStudentId());
            String name = trim(row.getName());
            String cls = trim(row.getClassName());

            // 基础字段校验
            if (!StringUtils.hasText(sid)) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号不能为空"));
                continue;
            }
            if (!StringUtils.hasText(name)) {
                errors.add(new ImportErrorVO(rowNum, sid, "姓名不能为空"));
                continue;
            }

            // 班级校验：助教导入的班级字段为"所属班级"，可能为空（非本学期学生）
            Classes classes = null;
            if (StringUtils.hasText(cls)) {
                classes = classNameMap.get(cls);
                if (classes == null) {
                    errors.add(new ImportErrorVO(rowNum, sid, "班级不存在：" + cls));
                    continue;
                }
            }

            // 查询是否已存在该学号的学生记录
            Student student = studentMapper.selectOne(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentId, sid)
            );

            if (student == null) {
                // 学号不存在 → 自动创建 student 记录（带所属班级）+ 创建账号
                student = new Student();
                student.setStudentId(sid);
                student.setName(name);
                student.setClassId(classes != null ? classes.getId() : null);
                student.setGender(0);
                student.setStatus(1);
                student.setIsAssistant(1);
                student.setAssignedClassId(null);
                studentMapper.insert(student);
                success++;
                createAssistantAccount(student, name, passwordHash, assistantRole);
            } else {
                // 学号已存在 → 已是助教则跳过；否则标记助教 + 创建账号
                if (student.getIsAssistant() != null && student.getIsAssistant() == 1) {
                    errors.add(new ImportErrorVO(rowNum, sid, "该学号已是助教，跳过"));
                    continue;
                }
                // 更新姓名与班级（按导入数据覆盖）
                student.setName(name);
                if (classes != null) {
                    student.setClassId(classes.getId());
                }
                student.setIsAssistant(1);
                studentMapper.updateById(student);
                success++;
                createAssistantAccount(student, name, passwordHash, assistantRole);
            }
        }

        result.setSuccessCount(success);
        result.setFailCount(errors.size());
        result.setErrors(errors);
        return result;
    }

    /**
     * 创建助教账号（username=学号 + 分配 ASSISTANT 角色）
     */
    private void createAssistantAccount(Student student, String realName,
                                        String passwordHash, SysRole assistantRole) {
        // 防止重复创建账号（按 username 唯一性）
        Long exists = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, student.getStudentId())
        );
        if (exists != null && exists > 0) {
            log.warn("助教账号已存在，跳过创建：{}", student.getStudentId());
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(student.getStudentId());
        user.setPasswordHash(passwordHash);
        user.setRealName(StringUtils.hasText(realName) ? realName : student.getName());
        user.setStatus(1);
        user.setFirstLogin(1);
        user.setStudentId(student.getId());
        sysUserMapper.insert(user);

        // 分配 ASSISTANT 角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(assistantRole.getId());
        userRoleMapper.insert(userRole);
    }

    // ==================== Excel 解析 ====================

    /**
     * 解析学生导入 Excel
     * 列顺序：学号 / 姓名 / 班级名称 / 性别
     */
    private List<StudentImportDTO> parseStudentExcel(MultipartFile file) {
        return parse(file, 4, (row, rowNum) -> {
            StudentImportDTO dto = new StudentImportDTO();
            dto.setStudentId(getStringCell(row, 0));
            dto.setName(getStringCell(row, 1));
            dto.setClassName(getStringCell(row, 2));
            dto.setGender(getStringCell(row, 3));
            return dto;
        });
    }

    /**
     * 解析助教导入 Excel
     * 列顺序：学号 / 姓名 / 班级名称
     */
    private List<AssistantImportDTO> parseAssistantExcel(MultipartFile file) {
        return parse(file, 3, (row, rowNum) -> {
            AssistantImportDTO dto = new AssistantImportDTO();
            dto.setStudentId(getStringCell(row, 0));
            dto.setName(getStringCell(row, 1));
            dto.setClassName(getStringCell(row, 2));
            return dto;
        });
    }

    /**
     * 通用 Excel 解析：跳过表头，按预期列数读取数据行
     */
    private <T> List<T> parse(MultipartFile file, int expectedCols,
                               RowParser<T> parser) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("导入文件为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null ||
            (!originalName.toLowerCase().endsWith(".xlsx") && !originalName.toLowerCase().endsWith(".xls"))) {
            throw new BusinessException("文件格式不支持，仅支持 .xlsx / .xls");
        }

        List<T> result = new ArrayList<>();
        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel 文件无工作表");
            }
            int lastRow = sheet.getLastRowNum();
            // 第 0 行为表头，从 1 起读数据
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                // 空行检测：所有预期列均空则跳过
                boolean allEmpty = true;
                for (int c = 0; c < expectedCols; c++) {
                    if (StringUtils.hasText(getStringCell(row, c))) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) continue;
                result.add(parser.parse(row, i + 1));
            }
        } catch (IOException e) {
            log.error("Excel 解析失败", e);
            throw new BusinessException("Excel 解析失败：" + e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excel 解析异常", e);
            throw new BusinessException("Excel 解析异常：" + e.getMessage());
        }
        return result;
    }

    @FunctionalInterface
    private interface RowParser<T> {
        T parse(Row row, int rowNum);
    }

    /**
     * 安全读取单元格为字符串
     */
    private String getStringCell(Row row, int colIdx) {
        if (row == null) return null;
        Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        CellType type = cell.getCellType();
        // 兼容公式
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        switch (type) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
            case ERROR:
            case _NONE:
            default:
                return null;
        }
    }

    // ==================== 工具方法 ====================

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * 性别文本转码：男→1, 女→2, 其他→0
     */
    private Integer parseGender(String text) {
        if (!StringUtils.hasText(text)) return 0;
        String t = text.trim();
        if (t.contains("男")) return 1;
        if (t.contains("女")) return 2;
        return 0;
    }

    /**
     * 加载所有班级，按 class_name 索引（仅在导入流程内部使用，数据量可控）
     */
    private Map<String, Classes> loadClassNameMap() {
        List<Classes> list = classesMapper.selectList(null);
        Map<String, Classes> map = new HashMap<>();
        for (Classes c : list) {
            if (c.getClassName() != null) {
                map.put(c.getClassName(), c);
            }
        }
        return map;
    }

    /**
     * 加载角色（按 roleCode）
     */
    private SysRole loadRole(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode)
        );
        if (role == null) {
            throw new BusinessException("角色不存在：" + roleCode);
        }
        return role;
    }
}
