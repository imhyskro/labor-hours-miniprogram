package com.labor.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 劳动学时管理小程序 - 后端服务启动入口
 *
 * <p>阶段0：项目初始化，仅保证项目可启动，暂不包含任何业务功能。</p>
 */
@SpringBootApplication
@MapperScan("com.labor.management.mapper")
public class LaborManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaborManagementApplication.class, args);
        System.out.println("劳动学时管理后端服务启动成功");
    }
}
