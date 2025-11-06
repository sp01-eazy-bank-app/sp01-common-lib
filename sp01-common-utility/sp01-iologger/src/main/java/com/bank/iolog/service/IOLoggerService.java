package com.bank.iolog.service;

import com.bank.iolog.entity.IOLogEntry;
import com.bank.iolog.enums.ChannelType;
import com.bank.iolog.enums.IOType;
import com.bank.iolog.repository.IOLogEntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
            IOLogEntry entry = buildLogEntry(
                    traceId, sourceApplication, resource, IOType.INBOUND,
                    serialize(extractHeaders(request)), extractPayload(request), null, ChannelType.REST
            );
            ioLogEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("Error while logging HTTP inbound request", e);
        }
    }

    // --- HTTP outbound ---
    public void logHttpOutboundResponse(ContentCachingResponseWrapper response, String traceId,
                                        String sourceApplication, String resource,
                                        Integer httpStatus, Map<String, String> responseHeaders) {
        try {
            IOLogEntry entry = buildLogEntry(
                    traceId, sourceApplication, resource, IOType.OUTBOUND,
                    serialize(responseHeaders), extractPayload(response), httpStatus, ChannelType.REST
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
                    serialize(headers), payload, null, ChannelType.RABBITMQ
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
                    serialize(headers), serializeResponse(response), httpStatus, ChannelType.RABBITMQ
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
        return toStringSafe(request.getContentAsByteArray(), request.getCharacterEncoding());
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
}
