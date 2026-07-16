package com.hunesion.assets_management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hunesion.assets_management.device.repository")
public class AssetsManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetsManagementApplication.class, args);
    }

}
