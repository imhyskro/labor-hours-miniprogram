package com.labor.management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 启动上下文测试
 *
 * <p>阶段0：验证 Spring 容器可以正常加载。</p>
 */
@SpringBootTest
class LaborManagementApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext, "Spring 应用上下文加载失败");
    }
}
