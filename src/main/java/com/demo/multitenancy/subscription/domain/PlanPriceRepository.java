package com.demo.multitenancy.subscription.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, UUID> {
  Optional<PlanPrice> findByPlan_CodeAndInterval(String planCode, BillingInterval interval);

  List<PlanPrice> findAllByPlan_Code(String planCode);
}
