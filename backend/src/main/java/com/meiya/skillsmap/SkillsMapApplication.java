package com.meiya.skillsmap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SkillsMap 应用启动入口
 *
 * @author SkillsMap Team
 */
@EnableScheduling
@MapperScan("com.meiya.skillsmap.mapper")
@SpringBootApplication(exclude = {
        // 与 dos-backend 保持一致：显式排除 JPA，本项目使用 MyBatis-Plus
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
public class SkillsMapApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillsMapApplication.class, args);
    }
}
