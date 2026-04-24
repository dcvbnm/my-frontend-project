package org.ershoupingtai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.ershoupingtai.mapper")
public class CampusTradeHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusTradeHubApplication.class, args);
        System.out.println("🚀 校园二手交易平台启动成功！");
        System.out.println("📌 访问地址：http://localhost:8081");
    }
}