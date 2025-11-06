package com.bank.iolog.service;

import com.bank.iolog.entity.IOLogEntry;
import com.bank.iolog.enums.ChannelType;
import com.bank.iolog.enums.IOType;
import com.bank.iolog.repository.IOLogEntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.boot.web.servlet.server.Encoding.DEFAULT_CHARSET;

@RequiredArgsConstructor
@Slf4j
public class IOLoggerService {

    private final IOLogEntryRepository ioLogEntryRepository;

    // HTTP inbound
    public void logHttpInboundRequest(ContentCachingRequestWrapper request, String traceId, String sourceApplication,
                                      String resource) {
        try {
            Map<String, String> headers = extractRequestHeaders(request);
            String payload = extractRequestPayload(request);

            IOLogEntry entry = buildLogEntry(traceId, sourceApplication, resource, IOType.INBOUND,
                    serialize(headers), payload, null, ChannelType.REST);

            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging request", e);
        }
    }

    public void logHttpOutboundResponse(ContentCachingResponseWrapper response, String traceId, String sourceApplication,
                                        String resource, Integer httpStatus, Map<String, String> responseHeaders) {
        try {
            Map<String, String> headers = extractResponseHeaders(response);
            String payload = extractResponsePayload(response);

            IOLogEntry entry = buildLogEntry(traceId, sourceApplication, resource, IOType.OUTBOUND,
                    serialize(headers), payload, httpStatus, ChannelType.REST);

            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging request", e);
        }
    }

    // Rabbit inbound
    public void logRabbitInboundRequest(Map<String, String> headers, String payload, String traceId,
                                  String sourceApplication, String resource) {
        try {
            IOLogEntry entry = buildLogEntry(traceId, sourceApplication, resource, IOType.INBOUND,
                    serialize(headers), payload, null, ChannelType.RABBITMQ);
            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging RabbitMQ inbound message", e);
        }
    }

    public void logRabbitOutboundResponse(Object response, String traceId, String sourceApplication,
                                          String resource, Integer httpStatus, Map<String, String> headers) {
        try {
            IOLogEntry entry = buildLogEntry(traceId, sourceApplication, resource, IOType.OUTBOUND,
                    serialize(headers), serializeResponse(response), httpStatus, ChannelType.RABBITMQ);

            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging response", e);
        }
    }

    private Map<String, String> extractRequestHeaders(ContentCachingRequestWrapper request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private String extractRequestPayload(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) return "{}";
        return new String(buf, getCharset(request.getCharacterEncoding()));
    }

    private static Charset getCharset(String encoding) {
        try {
            return encoding != null ? Charset.forName(encoding) : DEFAULT_CHARSET;
        } catch (Exception e) {
            return DEFAULT_CHARSET;
        }
    }

    private Map<String, String> extractResponseHeaders(ContentCachingResponseWrapper response) {
        Map<String, String> headers = new HashMap<>();
        response.getHeaderNames().forEach(h -> headers.put(h, response.getHeader(h)));
        return headers;
    }

    private String extractResponsePayload(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length == 0) return "{}";
        return new String(buf, getCharset(response.getCharacterEncoding()));
    }

    private IOLogEntry buildLogEntry(String traceId, String sourceApp, String resource, IOType ioType,
                                     String header, String payload, Integer httpStatus, ChannelType channelType) {
        return IOLogEntry.builder()
                .traceId(traceId)
                .sourceApplication(sourceApp)
                .resource(resource)
                .ioType(ioType)
                .header(header)
                .payload(payload)
                .httpStatus(httpStatus)
                .communicationChannel(channelType)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Converts Java object or Map to JSON string safely
     */
    private String serialize(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * Serializes response payload to JSON
     */
    private String serializeResponse(Object response) {
        try {
            if (response == null) return null;

            // If it's a ResponseEntity, extract only the actual body
            if (response instanceof ResponseEntity<?> responseEntity) {
                return new ObjectMapper().writeValueAsString(responseEntity.getBody());
            }
            // otherwise just serialize the object directly
            return new ObjectMapper().writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Could not serialize response body", e);
            return "{}";
        }
    }
}
