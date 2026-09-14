package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.dto.AssistantImportDTO;
import com.labor.management.dto.StudentImportDTO;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Company;
import com.labor.management.entity.Student;
import com.labor.management.entity.SysRole;
import com.labor.management.entity.SysUser;
import com.labor.management.entity.UserRole;
import com.labor.management.enums.RoleCodeEnum;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.CompanyMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.mapper.SysRoleMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.mapper.UserRoleMapper;
import com.labor.management.service.ClassesService;
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
 *
 * <p>学生/助教通过 Excel 批量导入，列含：公司 / 周次 / 开始节次 / 结束节次 / 班内编号 / 原始专业。
 * 公司必须预先存在（班级管理中维护）；班级（W-S-E）在导入时按 公司+周次+节次 自动查找或创建。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    /** 助教账号初始密码 */
    private static final String ASSISTANT_DEFAULT_PASSWORD = "cdjcc123456";

    private final StudentMapper studentMapper;
    private final CompanyMapper companyMapper;
    private final ClassesService classesService;
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

        Map<String, Company> companyCache = new HashMap<>();

        int success = 0;
        List<ImportErrorVO> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            StudentImportDTO row = rows.get(i);
            int rowNum = i + 2; // 数据从第2行起
            String sid = trim(row.getStudentId());
            String name = trim(row.getName());
            String companyName = trim(row.getCompanyName());
            String major = trim(row.getOriginalMajor());
            String gender = trim(row.getGender());

            if (!StringUtils.hasText(sid)) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号不能为空"));
                continue;
            }
            if (!StringUtils.hasText(name)) {
                errors.add(new ImportErrorVO(rowNum, sid, "姓名不能为空"));
                continue;
            }

            // 公司解析
            Company company = resolveCompany(companyName, companyCache);
            if (company == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "公司不存在：" + companyName + "（请先在班级管理中创建公司）"));
                continue;
            }

            // 周次/节次/班内编号解析
            Integer week = parseInt(row.getWeek());
            Integer start = parseInt(row.getStartSession());
            Integer end = parseInt(row.getEndSession());
            Integer noInClass = parseInt(row.getStudentNoInClass());
            if (week == null || start == null || end == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "周次/开始节次/结束节次必须为正整数"));
                continue;
            }
            if (start > end) {
                errors.add(new ImportErrorVO(rowNum, sid, "开始节次不能大于结束节次"));
                continue;
            }
            if (noInClass == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "班内编号必须为正整数"));
                continue;
            }

            // 学号唯一性
            Long existsCount = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentId, sid)
            );
            if (existsCount != null && existsCount > 0) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号已存在"));
                continue;
            }

            // 班级自动查找或创建
            Classes classes;
            try {
                classes = classesService.findOrCreateClass(company.getId(), week, start, end);
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(rowNum, sid, e.getMessage()));
                continue;
            }

            // 同班级内班内编号唯一
            Long noExists = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>()
                            .eq(Student::getClassId, classes.getId())
                            .eq(Student::getStudentNoInClass, noInClass)
            );
            if (noExists != null && noExists > 0) {
                errors.add(new ImportErrorVO(rowNum, sid, "该班级中班内编号 " + noInClass + " 已存在"));
                continue;
            }

            Student student = new Student();
            student.setStudentId(sid);
            student.setName(name);
            student.setClassId(classes.getId());
            student.setStudentNoInClass(noInClass);
            student.setOriginalMajor(major);
            student.setGender(parseGender(gender));
            student.setStatus(1);
            student.setIsAssistant(0);
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

        Map<String, Company> companyCache = new HashMap<>();
        SysRole assistantRole = loadRole(RoleCodeEnum.ASSISTANT.getCode());
        String passwordHash = passwordEncoder.encode(ASSISTANT_DEFAULT_PASSWORD);

        int success = 0;
        List<ImportErrorVO> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            AssistantImportDTO row = rows.get(i);
            int rowNum = i + 2;
            String sid = trim(row.getStudentId());
            String name = trim(row.getName());
            String companyName = trim(row.getCompanyName());
            String major = trim(row.getOriginalMajor());

            if (!StringUtils.hasText(sid)) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号不能为空"));
                continue;
            }
            if (!StringUtils.hasText(name)) {
                errors.add(new ImportErrorVO(rowNum, sid, "姓名不能为空"));
                continue;
            }

            // 公司/周次/节次/班内编号（助教所属班级，用于定位学生所在班）
            Company company = resolveCompany(companyName, companyCache);
            Classes classes = null;
            Integer noInClass = null;
            if (company != null) {
                Integer week = parseInt(row.getWeek());
                Integer start = parseInt(row.getStartSession());
                Integer end = parseInt(row.getEndSession());
                noInClass = parseInt(row.getStudentNoInClass());
                if (week != null && start != null && end != null && start <= end) {
                    try {
                        classes = classesService.findOrCreateClass(company.getId(), week, start, end);
                    } catch (BusinessException e) {
                        errors.add(new ImportErrorVO(rowNum, sid, e.getMessage()));
                        continue;
                    }
                }
            } else if (StringUtils.hasText(companyName)) {
                errors.add(new ImportErrorVO(rowNum, sid, "公司不存在：" + companyName + "（请先在班级管理中创建公司）"));
                continue;
            }

            Student student = studentMapper.selectOne(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentId, sid)
            );

            if (student == null) {
                // 学号不存在 → 新建学生 + 助教账号
                student = new Student();
                student.setStudentId(sid);
                student.setName(name);
                student.setClassId(classes != null ? classes.getId() : null);
                student.setStudentNoInClass(noInClass);
                student.setOriginalMajor(major);
                student.setGender(0);
                student.setStatus(1);
                student.setIsAssistant(1);
                studentMapper.insert(student);
                success++;
                createAssistantAccount(student, name, passwordHash, assistantRole);
            } else {
                // 学号已存在 → 已是助教则跳过；否则升为助教 + 建账号
                if (student.getIsAssistant() != null && student.getIsAssistant() == 1) {
                    errors.add(new ImportErrorVO(rowNum, sid, "该学号已是助教，跳过"));
                    continue;
                }
                student.setName(name);
                if (classes != null) {
                    student.setClassId(classes.getId());
                }
                if (noInClass != null) {
                    student.setStudentNoInClass(noInClass);
                }
                if (StringUtils.hasText(major)) {
                    student.setOriginalMajor(major);
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

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(assistantRole.getId());
        userRoleMapper.insert(userRole);
    }

    // ==================== Excel 解析 ====================

    /**
     * 解析学生导入 Excel
     * 列顺序：学号 / 姓名 / 公司名称 / 周次 / 开始节次 / 结束节次 / 班内编号 / 原始专业 / 性别
     */
    private List<StudentImportDTO> parseStudentExcel(MultipartFile file) {
        return parse(file, 9, (row, rowNum) -> {
            StudentImportDTO dto = new StudentImportDTO();
            dto.setStudentId(getStringCell(row, 0));
            dto.setName(getStringCell(row, 1));
            dto.setCompanyName(getStringCell(row, 2));
            dto.setWeek(getStringCell(row, 3));
            dto.setStartSession(getStringCell(row, 4));
            dto.setEndSession(getStringCell(row, 5));
            dto.setStudentNoInClass(getStringCell(row, 6));
            dto.setOriginalMajor(getStringCell(row, 7));
            dto.setGender(getStringCell(row, 8));
            return dto;
        });
    }

    /**
     * 解析助教导入 Excel
     * 列顺序：学号 / 姓名 / 公司名称 / 周次 / 开始节次 / 结束节次 / 班内编号 / 原始专业
     */
    private List<AssistantImportDTO> parseAssistantExcel(MultipartFile file) {
        return parse(file, 8, (row, rowNum) -> {
            AssistantImportDTO dto = new AssistantImportDTO();
            dto.setStudentId(getStringCell(row, 0));
            dto.setName(getStringCell(row, 1));
            dto.setCompanyName(getStringCell(row, 2));
            dto.setWeek(getStringCell(row, 3));
            dto.setStartSession(getStringCell(row, 4));
            dto.setEndSession(getStringCell(row, 5));
            dto.setStudentNoInClass(getStringCell(row, 6));
            dto.setOriginalMajor(getStringCell(row, 7));
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
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
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
     * 字符串转正整数，失败/空返回 null
     */
    private Integer parseInt(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            // 兼容 "1.0" 形式
            try {
                double d = Double.parseDouble(s.trim());
                return d > 0 && d == Math.floor(d) ? (int) d : null;
            } catch (NumberFormatException e2) {
                return null;
            }
        }
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
     * 按公司名解析公司（带缓存）；公司名空或不存在返回 null
     */
    private Company resolveCompany(String companyName, Map<String, Company> cache) {
        if (!StringUtils.hasText(companyName)) {
            return null;
        }
        String key = companyName.trim();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Company company = companyMapper.selectOne(
                new LambdaQueryWrapper<Company>().eq(Company::getName, key)
        );
        cache.put(key, company);
        return company;
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
