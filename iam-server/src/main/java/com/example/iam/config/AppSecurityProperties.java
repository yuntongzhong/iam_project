package com.example.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private String issuer;
    private String jwkPath;
    private List<String> allowedPostLogoutRedirects = new ArrayList<>();
    private List<String> frontChannelLogoutUris = new ArrayList<>();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwkPath() {
        return jwkPath;
    }

    public void setJwkPath(String jwkPath) {
        this.jwkPath = jwkPath;
    }

    public List<String> getAllowedPostLogoutRedirects() {
        return allowedPostLogoutRedirects;
    }

    public void setAllowedPostLogoutRedirects(List<String> allowedPostLogoutRedirects) {
        this.allowedPostLogoutRedirects = allowedPostLogoutRedirects;
    }

    public List<String> getFrontChannelLogoutUris() {
        return frontChannelLogoutUris;
    }

    public void setFrontChannelLogoutUris(List<String> frontChannelLogoutUris) {
        this.frontChannelLogoutUris = frontChannelLogoutUris;
    }
}
