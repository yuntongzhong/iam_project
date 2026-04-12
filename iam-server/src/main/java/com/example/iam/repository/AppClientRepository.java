package com.example.iam.repository;

import com.example.iam.domain.AppClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppClientRepository extends JpaRepository<AppClient, Long> {

    Optional<AppClient> findByClientId(String clientId);
}
