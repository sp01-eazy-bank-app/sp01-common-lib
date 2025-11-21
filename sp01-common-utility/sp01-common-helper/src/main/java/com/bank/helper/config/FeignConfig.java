package com.bank.helper.config;

import com.bank.helper.util.CommonUtil;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor jwtPropagationInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("Auth in context: {}", authentication);

            // propagate JWT if present
            if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
                Object credentials = authToken.getCredentials();
                if (credentials instanceof String token) {
                    requestTemplate.header("Authorization", "Bearer " + token);
                }
            } else {
                log.error("No JWT found in SecurityContext for propagation");
            }
            // propagate trace ID if present for IO logging
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // Try header first, then attribute
                String traceId = request.getHeader(CommonUtil.TRACE_ID);
                if (traceId == null) {
                    Object attr = request.getAttribute(CommonUtil.TRACE_ID);
                    if (attr != null) traceId = attr.toString();
                }
                if (traceId != null) {
                    requestTemplate.header(CommonUtil.TRACE_ID, traceId);
                    log.debug("Propagating trace ID: {}", traceId);
                } else {
                    log.debug("No trace ID found in request to propagate");
                }
            } else {
                log.warn("No servlet request attributes available for trace propagation");
            }
        };
    }
}
