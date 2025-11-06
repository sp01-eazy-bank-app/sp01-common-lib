package com.bank.iolog.service;

import com.bank.iolog.entity.IOLogEntry;
import com.bank.iolog.enums.ChannelType;
import com.bank.iolog.enums.IOType;
import com.bank.iolog.repository.IOLogEntryRepository;
import com.bank.iolog.util.IOLoggerConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class IOLoggerService {

    private final IOLogEntryRepository ioLogEntryRepository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- HTTP inbound ---
    public void logHttpInboundRequest(ContentCachingRequestWrapper request, String traceId,
                                      String sourceApplication, String resource) {
        try {
            // try to use recorded start time if present so ordering is accurate
            Instant ts;
            Object attr = request.getAttribute(IOLoggerConstant.REQUEST_START_TIME);
            if (attr instanceof Instant) {
                ts = (Instant) attr;
            } else {
                ts = Instant.now();
            }

            // Prefer the cached request body attribute if present (set by RequestWrappingFilter)
            Object cachedBody = request.getAttribute(IOLoggerConstant.REQUEST_BODY);
            String payload;
            if (cachedBody instanceof String s && !s.isEmpty()) {
                payload = s;
            } else {
                payload = extractPayload(request);
            }

            IOLogEntry entry = buildLogEntry(
                    traceId, sourceApplication, resource, IOType.INBOUND,
                    serialize(extractHeaders(request)), payload, null, ChannelType.REST,
                    ts
            );
            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging HTTP inbound request", e);
        }
    }

    // New: accept payload and timestamp directly
    public void logHttpInboundWithPayload(String payload, ContentCachingRequestWrapper request, String traceId,
                                          String sourceApplication, String resource, Instant timestamp) {
        try {
            IOLogEntry entry = buildLogEntry(
                    traceId, sourceApplication, resource, IOType.INBOUND,
                    serialize(extractHeaders(request)), payload, null, ChannelType.REST,
                    timestamp != null ? timestamp : Instant.now()
            );
            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging HTTP inbound request with payload", e);
        }
    }

    // --- HTTP outbound ---
    public void logHttpOutboundResponse(ContentCachingResponseWrapper response, String traceId,
                                        String sourceApplication, String resource,
                                        Integer httpStatus, Map<String, String> responseHeaders) {
        try {
            // Attempt to get the request start time from the current request attributes so outbound is ordered after inbound
            Instant outboundTs = Instant.now();
            try {
                RequestAttributes ra = RequestContextHolder.getRequestAttributes();
                if (ra != null) {
                    Object attr = ra.getAttribute(IOLoggerConstant.REQUEST_START_TIME, RequestAttributes.SCOPE_REQUEST);
                    if (attr instanceof Instant) {
                        // make outbound timestamp slightly after inbound so sorting is deterministic
                        outboundTs = ((Instant) attr).plus(1, ChronoUnit.MILLIS);
                    }
                }
            } catch (Exception ignored) {
                outboundTs = Instant.now();
            }

            IOLogEntry entry = buildLogEntry(
                    traceId, sourceApplication, resource, IOType.OUTBOUND,
                    serialize(responseHeaders), extractPayload(response), httpStatus, ChannelType.REST,
                    outboundTs
            );
            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging HTTP outbound response", e);
        }
    }

    // --- Rabbit inbound ---
    public void logRabbitInboundRequest(Map<String, String> headers, String payload, String traceId,
                                        String sourceApplication, String resource) {
        try {
            IOLogEntry entry = buildLogEntry(
                    traceId, sourceApplication, resource, IOType.INBOUND,
                    serialize(headers), payload, null, ChannelType.RABBITMQ,
                    Instant.now()
            );
            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging HTTP inbound request", e);
        }
    }

    // --- Rabbit outbound ---
    public void logRabbitOutboundResponse(Object response, String traceId, String sourceApplication,
                                          String resource, Integer httpStatus, Map<String, String> headers) {
        try {
            IOLogEntry entry = buildLogEntry(
                    traceId, sourceApplication, resource, IOType.OUTBOUND,
                    serialize(headers), serializeResponse(response), httpStatus, ChannelType.RABBITMQ,
                    Instant.now()
            );
            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging HTTP outbound response", e);
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames())
                .stream().collect(Collectors.toMap(h -> h, request::getHeader));
    }

    private String extractPayload(ContentCachingRequestWrapper request) {
        String body = toStringSafe(request.getContentAsByteArray(), request.getCharacterEncoding());
        if (!"{}".equals(body) && body != null) return body;

        // fallback: build payload from parameters (form data or query params)
        try {
            Map<String, String[]> params = request.getParameterMap();
            if (params != null && !params.isEmpty()) {
                // convert to single-value map where possible
                Map<String, Object> single = params.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().length == 1 ? e.getValue()[0] : e.getValue()));
                return MAPPER.writeValueAsString(single);
            }
            String query = request.getQueryString();
            if (query != null && !query.isEmpty()) return query;
        } catch (Exception ignored) {
        }

        return "{}";
    }

    private String extractPayload(ContentCachingResponseWrapper response) {
        return toStringSafe(response.getContentAsByteArray(), response.getCharacterEncoding());
    }

    private String toStringSafe(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) return "{}";
        Charset charset = getCharset(encoding);
        return new String(buf, charset);
    }

    private static Charset getCharset(String encoding) {
        try {
            return encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private String serialize(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String serializeResponse(Object response) {
        try {
            if (response instanceof ResponseEntity<?> entity) {
                return MAPPER.writeValueAsString(entity.getBody());
            }
            return response != null ? MAPPER.writeValueAsString(response) : "{}";
        } catch (Exception e) {
            log.warn("Could not serialize response body", e);
            return "{}";
        }
    }

    private IOLogEntry buildLogEntry(String traceId, String sourceApp, String resource, IOType ioType,
                                     String header, String payload, Integer httpStatus, ChannelType channelType,
                                     Instant timestamp) {
        return IOLogEntry.builder()
                .traceId(traceId)
                .sourceApplication(sourceApp)
                .resource(resource)
                .ioType(ioType)
                .header(header)
                .payload(payload)
                .httpStatus(httpStatus)
                .communicationChannel(channelType)
                .timestamp(timestamp)
                .build();
    }
}
