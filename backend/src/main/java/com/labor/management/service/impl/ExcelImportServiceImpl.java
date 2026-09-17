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
import com.labor.management.service.OperationLogService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入 Service 实现
 *
 * <p>学生/助教统一使用六列表格：公司 / 节次 / 学号 / 姓名 / 性别 / 行政班。
 * 节次使用 W-S-E-N（周次-开始节次-结束节次-班内编号）；公司必须预先存在，
 * 班级（W-S-E）在导入时按 公司+周次+节次 自动查找或创建。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    /** 助教账号初始密码 */
    private static final String ASSISTANT_DEFAULT_PASSWORD = "cdjcc123456";

    /** 学生和助教使用同一套导入表头 */
    private static final String[] IMPORT_HEADERS = {
            "公司", "节次", "学号", "姓名", "性别", "行政班"
    };

    private final StudentMapper studentMapper;
    private final CompanyMapper companyMapper;
    private final ClassesService classesService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

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
            String sessionCode = trim(row.getSessionCode());
            String administrativeClass = trim(row.getAdministrativeClass());
            String gender = trim(row.getGender());

            if (!StringUtils.hasText(sid)) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号不能为空"));
                continue;
            }
            if (!StringUtils.hasText(name)) {
                errors.add(new ImportErrorVO(rowNum, sid, "姓名不能为空"));
                continue;
            }
            if (!StringUtils.hasText(companyName)) {
                errors.add(new ImportErrorVO(rowNum, sid, "公司不能为空"));
                continue;
            }
            if (!StringUtils.hasText(administrativeClass)) {
                errors.add(new ImportErrorVO(rowNum, sid, "行政班不能为空"));
                continue;
            }

            Integer genderCode = parseRequiredGender(gender);
            if (genderCode == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "性别必须填写男或女"));
                continue;
            }

            SessionParts session = parseSessionCode(sessionCode);
            if (session == null) {
                errors.add(new ImportErrorVO(rowNum, sid,
                        "节次格式必须为 周次-开始节次-结束节次-班内编号，例如 1-1-2-1"));
                continue;
            }

            // 公司解析
            Company company = resolveCompany(companyName, companyCache);
            if (company == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "公司不存在：" + companyName + "（请先在班级管理中创建公司）"));
                continue;
            }

            if (session.startSession > session.endSession) {
                errors.add(new ImportErrorVO(rowNum, sid, "开始节次不能大于结束节次"));
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
                classes = classesService.findOrCreateClass(
                        company.getId(), session.week, session.startSession, session.endSession);
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(rowNum, sid, e.getMessage()));
                continue;
            }

            // 同班级内班内编号唯一
            Long noExists = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>()
                            .eq(Student::getClassId, classes.getId())
                            .eq(Student::getStudentNoInClass, session.studentNoInClass)
            );
            if (noExists != null && noExists > 0) {
                errors.add(new ImportErrorVO(rowNum, sid,
                        "该班级中班内编号 " + session.studentNoInClass + " 已存在"));
                continue;
            }

            Student student = new Student();
            student.setStudentId(sid);
            student.setName(name);
            student.setClassId(classes.getId());
            student.setStudentNoInClass(session.studentNoInClass);
            student.setOriginalMajor(administrativeClass);
            student.setGender(genderCode);
            student.setStatus(1);
            student.setIsAssistant(0);
            studentMapper.insert(student);
            success++;
        }

        result.setSuccessCount(success);
        result.setFailCount(errors.size());
        result.setErrors(errors);
        operationLogService.record("STUDENT", "IMPORT", "STUDENT", null,
                "导入学生：成功 " + success + " 条，失败 " + errors.size() + " 条",
                null, result);
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
            String sessionCode = trim(row.getSessionCode());
            String administrativeClass = trim(row.getAdministrativeClass());
            String gender = trim(row.getGender());

            if (!StringUtils.hasText(sid)) {
                errors.add(new ImportErrorVO(rowNum, sid, "学号不能为空"));
                continue;
            }
            if (!StringUtils.hasText(name)) {
                errors.add(new ImportErrorVO(rowNum, sid, "姓名不能为空"));
                continue;
            }
            if (!StringUtils.hasText(companyName)) {
                errors.add(new ImportErrorVO(rowNum, sid, "公司不能为空"));
                continue;
            }
            if (!StringUtils.hasText(administrativeClass)) {
                errors.add(new ImportErrorVO(rowNum, sid, "行政班不能为空"));
                continue;
            }

            Integer genderCode = parseRequiredGender(gender);
            if (genderCode == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "性别必须填写男或女"));
                continue;
            }

            SessionParts session = parseSessionCode(sessionCode);
            if (session == null) {
                errors.add(new ImportErrorVO(rowNum, sid,
                        "节次格式必须为 周次-开始节次-结束节次-班内编号，例如 1-1-2-1"));
                continue;
            }
            if (session.startSession > session.endSession) {
                errors.add(new ImportErrorVO(rowNum, sid, "开始节次不能大于结束节次"));
                continue;
            }

            Student student = studentMapper.selectOne(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentId, sid)
            );

            // 公司与节次用于定位助教作为学生时所属的班级
            Company company = resolveCompany(companyName, companyCache);
            if (company == null) {
                errors.add(new ImportErrorVO(rowNum, sid, "公司不存在：" + companyName + "（请先在班级管理中创建公司）"));
                continue;
            }

            Classes classes;
            try {
                classes = classesService.findOrCreateClass(
                        company.getId(), session.week, session.startSession, session.endSession);
            } catch (BusinessException e) {
                errors.add(new ImportErrorVO(rowNum, sid, e.getMessage()));
                continue;
            }

            LambdaQueryWrapper<Student> noQuery = new LambdaQueryWrapper<Student>()
                    .eq(Student::getClassId, classes.getId())
                    .eq(Student::getStudentNoInClass, session.studentNoInClass);
            if (student != null) {
                noQuery.ne(Student::getId, student.getId());
            }
            Long noExists = studentMapper.selectCount(noQuery);
            if (noExists != null && noExists > 0) {
                errors.add(new ImportErrorVO(rowNum, sid,
                        "该班级中班内编号 " + session.studentNoInClass + " 已存在"));
                continue;
            }

            if (student == null) {
                // 学号不存在 → 新建学生 + 助教账号
                student = new Student();
                student.setStudentId(sid);
                student.setName(name);
                student.setClassId(classes.getId());
                student.setStudentNoInClass(session.studentNoInClass);
                student.setOriginalMajor(administrativeClass);
                student.setGender(genderCode);
                student.setStatus(1);
                student.setIsAssistant(1);
                studentMapper.insert(student);
                success++;
                createAssistantAccount(student, name, passwordHash, assistantRole);
            } else {
                // 学号已存在（包括已经标记为助教）→ 更新资料并确保账号、角色完整
                student.setName(name);
                student.setClassId(classes.getId());
                student.setStudentNoInClass(session.studentNoInClass);
                student.setOriginalMajor(administrativeClass);
                student.setGender(genderCode);
                student.setIsAssistant(1);
                studentMapper.updateById(student);
                success++;
                createAssistantAccount(student, name, passwordHash, assistantRole);
            }
        }

        result.setSuccessCount(success);
        result.setFailCount(errors.size());
        result.setErrors(errors);
        operationLogService.record("ASSISTANT", "IMPORT", "ASSISTANT", null,
                "导入助教：成功 " + success + " 条，失败 " + errors.size() + " 条",
                null, result);
        return result;
    }

    /**
     * 创建、恢复或补全助教账号（username=学号 + 分配 ASSISTANT 角色）。
     */
    private void createAssistantAccount(Student student, String realName,
                                        String passwordHash, SysRole assistantRole) {
        String displayName = StringUtils.hasText(realName) ? realName : student.getName();
        SysUser user = sysUserMapper.selectByUsernameIncludeDeleted(student.getStudentId());

        if (user == null) {
            // 全新账号：写入 sys_user，用户管理页面即可查询到。
            user = new SysUser();
            user.setUsername(student.getStudentId());
            user.setPasswordHash(passwordHash);
            user.setRealName(displayName);
            user.setStatus(1);
            user.setFirstLogin(1);
            user.setLastPasswordChangeTime(LocalDateTime.now());
            user.setStudentId(student.getId());
            sysUserMapper.insert(user);
        } else if (user.getDeleted() != null && user.getDeleted() == 1) {
            // 同名账号是之前取消助教后留下的逻辑删除记录，复活并重新关联学生。
            sysUserMapper.reviveAndReset(
                    user.getId(),
                    passwordHash,
                    displayName,
                    student.getId(),
                    LocalDateTime.now()
            );
        } else {
            // 正常账号只同步资料和学生关联，不重置用户已经修改过的密码。
            user.setRealName(displayName);
            user.setStatus(1);
            user.setStudentId(student.getId());
            sysUserMapper.updateById(user);
        }

        // user_role 也可能存在逻辑删除记录，需要复活而不是重复插入。
        UserRole existingRole = userRoleMapper.selectByUserAndRoleIncludeDeleted(
                user.getId(), assistantRole.getId());
        if (existingRole == null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(assistantRole.getId());
            userRoleMapper.insert(userRole);
        } else {
            userRoleMapper.reviveByUserAndRole(user.getId(), assistantRole.getId());
        }

        log.info("助教导入账号已创建或恢复: studentId={}, userId={}",
                student.getStudentId(), user.getId());
    }

    // ==================== Excel 解析 ====================

    /**
     * 解析学生导入 Excel
     * 列顺序：公司 / 节次 / 学号 / 姓名 / 性别 / 行政班
     */
    private List<StudentImportDTO> parseStudentExcel(MultipartFile file) {
        return parse(file, IMPORT_HEADERS, (row, rowNum) -> {
            StudentImportDTO dto = new StudentImportDTO();
            dto.setCompanyName(getStringCell(row, 0));
            dto.setSessionCode(getStringCell(row, 1));
            dto.setStudentId(getStringCell(row, 2));
            dto.setName(getStringCell(row, 3));
            dto.setGender(getStringCell(row, 4));
            dto.setAdministrativeClass(getStringCell(row, 5));
            return dto;
        });
    }

    /**
     * 解析助教导入 Excel
     * 列顺序：公司 / 节次 / 学号 / 姓名 / 性别 / 行政班
     */
    private List<AssistantImportDTO> parseAssistantExcel(MultipartFile file) {
        return parse(file, IMPORT_HEADERS, (row, rowNum) -> {
            AssistantImportDTO dto = new AssistantImportDTO();
            dto.setCompanyName(getStringCell(row, 0));
            dto.setSessionCode(getStringCell(row, 1));
            dto.setStudentId(getStringCell(row, 2));
            dto.setName(getStringCell(row, 3));
            dto.setGender(getStringCell(row, 4));
            dto.setAdministrativeClass(getStringCell(row, 5));
            return dto;
        });
    }

    /**
     * 通用 Excel 解析：跳过表头，按预期列数读取数据行
     */
    private <T> List<T> parse(MultipartFile file, String[] expectedHeaders,
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
            validateHeaders(sheet, expectedHeaders);
            int expectedCols = expectedHeaders.length;
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

    /**
     * 校验第一行表头，避免旧模板或列错位造成字段误导入。
     */
    private void validateHeaders(Sheet sheet, String[] expectedHeaders) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new BusinessException("Excel 第1行必须是表头");
        }
        for (int i = 0; i < expectedHeaders.length; i++) {
            String actual = trim(getStringCell(header, i));
            if (!expectedHeaders[i].equals(actual)) {
                throw new BusinessException(
                        "第" + (i + 1) + "列表头应为“" + expectedHeaders[i] + "”，实际为“"
                                + (actual == null ? "" : actual) + "”");
            }
        }
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
     * 解析节次编码 W-S-E-N（周次-开始节次-结束节次-班内编号）。
     */
    private SessionParts parseSessionCode(String text) {
        if (!StringUtils.hasText(text)) return null;
        String[] parts = text.trim().split("\\s*-\\s*", -1);
        if (parts.length != 4) return null;
        Integer week = parseInt(parts[0]);
        Integer startSession = parseInt(parts[1]);
        Integer endSession = parseInt(parts[2]);
        Integer studentNoInClass = parseInt(parts[3]);
        if (week == null || startSession == null || endSession == null || studentNoInClass == null) {
            return null;
        }
        return new SessionParts(week, startSession, endSession, studentNoInClass);
    }

    /**
     * 性别文本转码：男→1，女→2；其他值返回 null 作为格式错误。
     */
    private Integer parseRequiredGender(String text) {
        if (!StringUtils.hasText(text)) return null;
        String value = text.trim();
        if ("男".equals(value)) return 1;
        if ("女".equals(value)) return 2;
        return null;
    }

    private static final class SessionParts {
        private final Integer week;
        private final Integer startSession;
        private final Integer endSession;
        private final Integer studentNoInClass;

        private SessionParts(Integer week, Integer startSession,
                             Integer endSession, Integer studentNoInClass) {
            this.week = week;
            this.startSession = startSession;
            this.endSession = endSession;
            this.studentNoInClass = studentNoInClass;
        }
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
