package com.demo.multitenancy.metering.service;

import java.time.Clock;
import java.time.Instant;

import com.demo.multitenancy.metering.domain.UsageEvent;
import com.demo.multitenancy.metering.domain.UsageEventRepository;
import com.demo.multitenancy.user.service.TenantGuard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeteringService {
  private final UsageEventRepository repository;
  private final TenantGuard tenantGuard;
  private final Clock clock;

  public MeteringService(UsageEventRepository repository, TenantGuard tenantGuard, Clock clock) {
    this.repository = repository;
    this.tenantGuard = tenantGuard;
    this.clock = clock;
  }

  @Transactional
  public UsageEvent record(String metricKey, long quantity) {
    tenantGuard.requireTenantId();
    Instant now = Instant.now(clock);
    return repository.save(new UsageEvent(metricKey, quantity, now));
  }
}
