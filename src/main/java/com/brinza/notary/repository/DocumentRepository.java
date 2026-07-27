package com.brinza.notary.repository;

import com.brinza.notary.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByAppointmentIdOrderByUploadedAtAsc(Long appointmentId);
}
