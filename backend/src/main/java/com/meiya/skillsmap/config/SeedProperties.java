package com.meiya.skillsmap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "skillsmap")
public class SeedProperties {

    private Jwt jwt = new Jwt();
    private Seed seed = new Seed();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Seed getSeed() { return seed; }
    public void setSeed(Seed seed) { this.seed = seed; }

    public static class Jwt {
        private String secret;
        private long expirationSeconds = 604800;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
    }

    public static class Seed {
        private boolean enabled = true;
        private String localSkillsPath;
        private String localPluginsPath;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getLocalSkillsPath() { return localSkillsPath; }
        public void setLocalSkillsPath(String localSkillsPath) { this.localSkillsPath = localSkillsPath; }
        public String getLocalPluginsPath() { return localPluginsPath; }
        public void setLocalPluginsPath(String localPluginsPath) { this.localPluginsPath = localPluginsPath; }
    }
}
