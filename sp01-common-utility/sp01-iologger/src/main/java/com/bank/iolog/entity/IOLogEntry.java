package com.bank.iolog.entity;

import com.bank.iolog.enums.ChannelType;
import com.bank.iolog.enums.IOType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "io_log_entries")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IOLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", length = 10, nullable = false)
    private String traceId;

    @Column(name = "source_application", length = 100, nullable = false)
    private String sourceApplication;

    @Column(name = "resource", length = 255, nullable = false)
    private String resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "io_type", length = 10, nullable = false)
    private IOType ioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_channel", length = 10, nullable = false)
    private ChannelType communicationChannel;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String header;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private Instant timestamp;
}
