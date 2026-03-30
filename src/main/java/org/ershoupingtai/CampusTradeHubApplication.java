package org.ershoupingtai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusTradeHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusTradeHubApplication.class, args);
        System.out.println("🚀 校园二手交易平台启动成功！");
        System.out.println("📌 访问地址：http://localhost:8080");
    }
}