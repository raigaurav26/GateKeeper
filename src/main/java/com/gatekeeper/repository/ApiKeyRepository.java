package com.gatekeeper.repository;

import com.gatekeeper.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyValue(String keyValue);

    Optional<ApiKey> findByKeyValueAndActiveTrue(String keyValue);
}
