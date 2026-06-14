package com.meiya.skillsmap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Knife4j 配置
 * <p>S38: 增加 bearer-jwt SecurityScheme（沿用 Lead 决策 #5），让 Knife4j /doc.html 的
 * POST /api/skills 等受保护端点可在 UI 里点 "Authorize" 按钮粘贴 JWT 直接调试。
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
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        // S38: bearer-jwt 鉴权方案
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("粘贴完整 JWT（无 'Bearer ' 前缀），"
                                                + "受保护端点将自动附加 Authorization 头")));
    }
}
