package com.example.ui_service.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

@Component
public class TokenRelayInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TokenRelayInterceptor.class);

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        try {
            HttpServletRequest currentRequest = resolveCurrentRequest();
            if (currentRequest != null) {
                String authorizationHeader = resolveAuthorizationHeader(currentRequest);
                if (StringUtils.hasText(authorizationHeader)) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                }
            }
        } catch (Exception ex) {
            log.debug("Không thể relay JWT cho RestTemplate: {}", ex.getMessage());
        }
        return execution.execute(request, body);
    }

    @Nullable
    private HttpServletRequest resolveCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    @Nullable
    private String resolveAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader)) {
            return authHeader;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("jwtToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                String token = cookie.getValue();
                return token.startsWith("Bearer ") ? token : "Bearer " + token;
            }
        }
        return null;
    }
}

