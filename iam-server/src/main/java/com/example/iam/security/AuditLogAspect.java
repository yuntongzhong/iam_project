package com.example.iam.security;

import com.example.iam.domain.enums.AuditStatus;
import com.example.iam.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        HttpServletRequest request = currentRequest();
        String username = request != null && request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
        String ipAddress = request != null ? request.getRemoteAddr() : "unknown";
        String details = maskSensitivePayload(joinPoint);

        try {
            Object result = joinPoint.proceed();
            if (request != null) {
                auditLogService.save(username, auditLog.action(), request, AuditStatus.SUCCESS, details);
            } else {
                auditLogService.save(username, auditLog.action(), ipAddress, AuditStatus.SUCCESS, details);
            }
            return result;
        } catch (Throwable ex) {
            if (request != null) {
                auditLogService.save(username, auditLog.action(), request, AuditStatus.FAILURE, details + " | error=" + ex.getMessage());
            } else {
                auditLogService.save(username, auditLog.action(), ipAddress, AuditStatus.FAILURE, details + " | error=" + ex.getMessage());
            }
            throw ex;
        }
    }

    private String maskSensitivePayload(ProceedingJoinPoint joinPoint) {
        CodeSignature signature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] values = joinPoint.getArgs();
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < parameterNames.length; i++) {
            Object value = values[i];
            if (value == null
                    || value instanceof HttpServletRequest
                    || value instanceof jakarta.servlet.http.HttpServletResponse
                    || value instanceof MultipartFile
                    || value instanceof byte[]) {
                continue;
            }
            payload.put(parameterNames[i], value);
        }
        try {
            String raw = objectMapper.writeValueAsString(payload);
            String maskedPassword = raw.replaceAll("(?i)(\"[^\"]*(?:password|secret)[^\"]*\"\\s*:\\s*\")([^\"]*)(\")", "$1***$3");
            return maskedPassword.replaceAll("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)", "$1****$2");
        } catch (JsonProcessingException ex) {
            return payload.values().stream().filter(Objects::nonNull).map(Object::toString).reduce((left, right) -> left + "," + right).orElse("");
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}
