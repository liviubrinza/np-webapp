package com.brinza.notary.repository;

import com.brinza.notary.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    java.util.List<Service> findByActiveTrue();
}
