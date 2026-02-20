package com.demo.multitenancy.metering.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {
}
