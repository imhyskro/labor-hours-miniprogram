package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 公司新增/改名 DTO（加公司只需一个名字）
 */
@Data
public class CompanyCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "公司名称不能为空")
    private String name;
}
