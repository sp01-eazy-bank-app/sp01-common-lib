package com.bank.iolog.filter;

import com.bank.iolog.service.IOLoggerService;
import com.bank.iolog.util.IOLoggerConstant;
import com.bank.iolog.util.IOLoggerUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequestWrappingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestWrappingFilter.class);

    private final IOLoggerService ioLoggerService;
    private final String sourceApplication;

    public RequestWrappingFilter(IOLoggerService ioLoggerService, String sourceApplication) {
        this.ioLoggerService = ioLoggerService;
        this.sourceApplication = sourceApplication;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper
                ? (ContentCachingRequestWrapper) request
                : new ContentCachingRequestWrapper(request);

        // if wrappedRequest has no bytes (maybe already consumed), use CachedBodyHttpServletRequest
        if (wrappedRequest.getContentAsByteArray().length == 0) {
            try {
                CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);
                wrappedRequest = new ContentCachingRequestWrapper(cached);
            } catch (IOException e) {
                // fallback to original wrappedRequest
            }
        }

        ContentCachingResponseWrapper wrappedResponse = response instanceof ContentCachingResponseWrapper
                ? (ContentCachingResponseWrapper) response
                : new ContentCachingResponseWrapper(response);

        // record start time for ordering
        Instant startTs = Instant.now();
        wrappedRequest.setAttribute(IOLoggerConstant.REQUEST_START_TIME, startTs);

        // set request attributes so RequestContextHolder-based lookups see the wrapped request
        ServletRequestAttributes previousAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes wrappedAttributes = new ServletRequestAttributes(wrappedRequest, wrappedResponse);
        RequestContextHolder.setRequestAttributes(wrappedAttributes);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            // After processing, cache the request body string into an attribute for consumers that use RequestContextHolder
            String cachedBody = null;
            try {
                byte[] content = wrappedRequest.getContentAsByteArray();
                if (content.length > 0) {
                    String encoding = Optional.ofNullable(wrappedRequest.getCharacterEncoding()).orElse(StandardCharsets.UTF_8.name());
                    cachedBody = new String(content, encoding);
                    wrappedRequest.setAttribute(IOLoggerConstant.REQUEST_BODY, cachedBody);
                }
            } catch (Exception ignored) {
            }

            // Build trace id (prefer header or existing attribute)
            String traceId = Optional.ofNullable(wrappedRequest.getHeader(IOLoggerConstant.TRACE_ID))
                    .filter(s -> !s.isEmpty())
                    .orElse(null);
            if (traceId == null) {
                Object attrObj = wrappedRequest.getAttribute(IOLoggerConstant.TRACE_ID);
                if (attrObj != null) traceId = attrObj.toString();
            }
            if (traceId == null) traceId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            wrappedRequest.setAttribute(IOLoggerConstant.TRACE_ID, traceId);

            String resource = IOLoggerUtil.buildFullResource(wrappedRequest);

            // collect response headers (replace nulls with empty string)
            Map<String, String> responseHeaders = wrappedResponse.getHeaderNames().stream()
                    .collect(Collectors.toMap(h -> h, h -> Optional.ofNullable(wrappedResponse.getHeader(h)).orElse("")));

            // Debug logging to help diagnose payload / ordering problems
            try {
                int reqLen = cachedBody != null ? cachedBody.length() : wrappedRequest.getContentAsByteArray().length;
                int respLen = wrappedResponse.getContentAsByteArray().length;
                log.debug("IOLogger: traceId={} resource={} reqLen={} respLen={}", traceId, resource, reqLen, respLen);
            } catch (Exception e) {
                log.debug("IOLogger: unable to compute body lengths", e);
            }

            // Log inbound (prefer passing the cached body and start ts directly to avoid timing/visibility issues)
            try {
                if (cachedBody != null) {
                    ioLoggerService.logHttpInboundWithPayload(cachedBody, wrappedRequest, traceId, sourceApplication, resource, startTs);
                } else {
                    ioLoggerService.logHttpInboundRequest(wrappedRequest, traceId, sourceApplication, resource);
                }
            } catch (Exception e) {
                // best effort
                log.debug("IOLogger: inbound logging failed for traceId={}", traceId, e);
            }

            try {
                ioLoggerService.logHttpOutboundResponse(wrappedResponse, traceId, sourceApplication, resource,
                        wrappedResponse.getStatus(), responseHeaders);
            } catch (Exception e) {
                // best effort
                log.debug("IOLogger: outbound logging failed for traceId={}", traceId, e);
            }

        } finally {
            try {
                wrappedResponse.copyBodyToResponse();
            } finally {
                // restore previous attributes
                if (previousAttributes != null) {
                    RequestContextHolder.setRequestAttributes(previousAttributes);
                } else {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        }
    }
}
