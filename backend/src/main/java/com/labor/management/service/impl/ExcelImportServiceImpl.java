package com.labor.management.service.impl;

import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.entity.Classes;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.*;
import com.labor.management.vo.ImportErrorVO;
import com.labor.management.vo.ImportResultVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Imports the existing Web templates into V2 master/enrollment/profile tables. */
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {
    private final JdbcTemplate jdbcTemplate;
    private final ClassesService classesService;
    private final StudentService studentService;
    private final AssistantAssignmentService assistantService;
    private final DataScopeService dataScopeService;

    @Override public ImportResultVO importStudents(MultipartFile file){return importRows(file,false);}
    @Override public ImportResultVO importAssistants(MultipartFile file){return importRows(file,true);}

    private ImportResultVO importRows(MultipartFile file,boolean assistant){
        List<String[]> rows=parse(file,assistant?8:9);List<ImportErrorVO> errors=new ArrayList<>();int success=0;
        List<Long> scope=dataScopeService.getCurrentUserScopeClassIds();
        for(int i=0;i<rows.size();i++){
            String[] r=rows.get(i);String no=trim(r[0]);
            try{
                if(!StringUtils.hasText(no)||!no.matches("\\d{9}"))throw new BusinessException("学号必须是9位数字");
                if(!StringUtils.hasText(trim(r[1])))throw new BusinessException("姓名不能为空");
                Long companyId=jdbcTemplate.query("SELECT id FROM company WHERE company_name=?",rs->rs.next()?rs.getLong(1):null,trim(r[2]));
                if(companyId==null)throw new BusinessException("公司不存在："+trim(r[2]));
                int week=number(r[3],"周次"),start=number(r[4],"开始节次"),end=number(r[5],"结束节次"),inClassNo=number(r[6],"班内编号");
                Classes group=classesService.findOrCreateClass(companyId,week,start,end);
                if(scope!=null&&!scope.contains(group.getId()))throw new BusinessException("该班级不在您的管理范围内");
                Long studentId=jdbcTemplate.query("SELECT id FROM student WHERE student_no=?",rs->rs.next()?rs.getLong(1):null,no);
                if(studentId==null){StudentCreateDTO dto=new StudentCreateDTO();dto.setStudentId(no);dto.setName(trim(r[1]));dto.setClassId(group.getId());dto.setStudentNoInClass(inClassNo);dto.setOriginalMajor(trim(r[7]));dto.setGender(assistant?0:gender(r[8]));studentId=studentService.create(dto,scope);}
                else if(!assistant)throw new BusinessException("学号已存在");
                if(assistant)assistantService.setIdentity(studentId,true);success++;
            }catch(Exception e){errors.add(new ImportErrorVO(i+2,no,e.getMessage()==null?"导入失败":e.getMessage()));}
        }
        ImportResultVO result=new ImportResultVO();result.setTotal(rows.size());result.setSuccessCount(success);result.setFailCount(errors.size());result.setErrors(errors);return result;
    }

    private List<String[]> parse(MultipartFile file,int columns){
        if(file==null||file.isEmpty())throw new BusinessException("导入文件为空");List<String[]> result=new ArrayList<>();
        try(InputStream in=file.getInputStream();Workbook workbook=WorkbookFactory.create(in)){
            Sheet sheet=workbook.getSheetAt(0);for(int i=1;i<=sheet.getLastRowNum();i++){Row row=sheet.getRow(i);if(row==null)continue;String[] values=new String[columns];boolean empty=true;for(int c=0;c<columns;c++){values[c]=cell(row.getCell(c,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));if(StringUtils.hasText(values[c]))empty=false;}if(!empty)result.add(values);}
        }catch(Exception e){throw new BusinessException("Excel解析失败："+e.getMessage());}return result;
    }
    private String cell(Cell c){if(c==null)return null;CellType t=c.getCellType()==CellType.FORMULA?c.getCachedFormulaResultType():c.getCellType();if(t==CellType.STRING)return c.getStringCellValue();if(t==CellType.NUMERIC){double v=c.getNumericCellValue();return v==Math.floor(v)?String.valueOf((long)v):String.valueOf(v);}return t==CellType.BOOLEAN?String.valueOf(c.getBooleanCellValue()):null;}
    private int number(String value,String name){try{int n=(int)Double.parseDouble(trim(value));if(n<=0)throw new Exception();return n;}catch(Exception e){throw new BusinessException(name+"必须为正整数");}}
    private int gender(String value){return value!=null&&value.contains("男")?1:value!=null&&value.contains("女")?2:0;}
    private String trim(String value){return value==null?null:value.trim();}
}
