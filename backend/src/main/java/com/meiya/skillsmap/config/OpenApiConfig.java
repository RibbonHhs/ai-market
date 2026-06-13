package com.meiya.skillsmap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Knife4j 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skillsMapOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkillsMap API")
                        .description("Claude Skills 集市与管理平台 接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillsMap Team").email("dev@skillsmap.local"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
