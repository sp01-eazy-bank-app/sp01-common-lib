package com.bank.iolog.repository;

import com.bank.iolog.entity.IOLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOLogEntryRepository extends JpaRepository<IOLogEntry, Long> {
}
