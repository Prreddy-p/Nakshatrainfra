package com.realestate.repository;

import com.realestate.model.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentRecord, Long> { }
