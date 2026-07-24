package com.brinza.notary.repository;

import com.brinza.notary.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByActiveTrue();

    Optional<Service> findByCode(String code);
}
