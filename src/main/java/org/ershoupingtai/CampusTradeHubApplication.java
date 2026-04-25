package org.ershoupingtai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@MapperScan("org.ershoupingtai.mapper")
public class CampusTradeHubApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(CampusTradeHubApplication.class, args);

        // Test database connection
        try {
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.UserLogin", Integer.class);
            System.out.println("Database connection successful. User count: " + count);
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("🚀 校园二手交易平台启动成功！");
        System.out.println("📌 访问地址：http://localhost:8081/user/login");
    }
}