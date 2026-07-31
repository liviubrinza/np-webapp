package com.brinza.notary.repository;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatus(AppointmentStatus status);

    boolean existsByServiceId(Long serviceId);

    boolean existsByStatus(AppointmentStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Appointment a
            WHERE a.status = :status
              AND a.id <> :excludeId
              AND a.requestedAt < :endedAt
              AND a.endedAt > :requestedAt
            """)
    boolean existsOverlapping(@Param("status") AppointmentStatus status,
                              @Param("excludeId") Long excludeId,
                              @Param("requestedAt") LocalDateTime requestedAt,
                              @Param("endedAt") LocalDateTime endedAt);

    @Query("""
            SELECT a FROM Appointment a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:from IS NULL OR a.requestedAt >= :from)
              AND (:to IS NULL OR a.requestedAt <= :to)
              AND (:name IS NULL OR LOWER(a.clientName) LIKE LOWER(CONCAT('%', :name, '%')))
            ORDER BY a.requestedAt DESC
            """)
    List<Appointment> search(@Param("status") AppointmentStatus status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("name") String name);

    @Query("""
            SELECT a FROM Appointment a
            WHERE (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            """)
    List<Appointment> findAllByCreatedAtRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
