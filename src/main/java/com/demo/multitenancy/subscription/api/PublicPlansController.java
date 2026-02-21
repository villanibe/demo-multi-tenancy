package com.demo.multitenancy.subscription.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.subscription.domain.Plan;
import com.demo.multitenancy.subscription.domain.PlanPrice;
import com.demo.multitenancy.subscription.domain.PlanPriceRepository;
import com.demo.multitenancy.subscription.domain.PlanRepository;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/plans")
public class PublicPlansController {

  private final PlanRepository planRepository;
  private final PlanPriceRepository planPriceRepository;

  public PublicPlansController(PlanRepository planRepository, PlanPriceRepository planPriceRepository) {
    this.planRepository = planRepository;
    this.planPriceRepository = planPriceRepository;
  }

  @Operation(summary = "List available plans (public catalog)")
  @GetMapping
  public ResponseEntity<List<PlanResponse>> list() {
    List<Plan> plans = planRepository.findAll().stream()
        .sorted(Comparator.comparing(Plan::getCode))
        .toList();

    List<PlanResponse> response = new ArrayList<>();
    for (Plan plan : plans) {
      List<PlanPrice> prices = planPriceRepository.findAllByPlan_Code(plan.getCode());
      response.add(toResponse(plan, prices));
    }

    return ResponseEntity.ok(response);
  }

  private static PlanResponse toResponse(Plan plan, List<PlanPrice> prices) {
    PlanResponse response = new PlanResponse();
    response.setCode(plan.getCode());
    response.setName(plan.getName());
    response.setTrialDays(plan.getTrialDays());
    response.setEntitlements(new ArrayList<>(plan.getPlanFeatureCodes()));

    List<PlanPriceResponse> priceResponses = prices.stream()
        .sorted(Comparator.comparing(PlanPrice::getInterval))
        .map(p -> {
          PlanPriceResponse pr = new PlanPriceResponse();
          pr.setInterval(p.getInterval());
          pr.setCurrency(p.getCurrency());
          pr.setUnitAmountCents(p.getUnitAmountCents());
          return pr;
        })
        .toList();

    response.setPrices(priceResponses);
    return response;
  }

  public static class PlanResponse {
    private String code;
    private String name;
    private int trialDays;
    private List<String> entitlements;
    private List<PlanPriceResponse> prices;

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getTrialDays() {
      return trialDays;
    }

    public void setTrialDays(int trialDays) {
      this.trialDays = trialDays;
    }

    public List<String> getEntitlements() {
      return entitlements;
    }

    public void setEntitlements(List<String> entitlements) {
      this.entitlements = entitlements;
    }

    public List<PlanPriceResponse> getPrices() {
      return prices;
    }

    public void setPrices(List<PlanPriceResponse> prices) {
      this.prices = prices;
    }
  }

  public static class PlanPriceResponse {
    private BillingInterval interval;
    private String currency;
    private long unitAmountCents;

    public BillingInterval getInterval() {
      return interval;
    }

    public void setInterval(BillingInterval interval) {
      this.interval = interval;
    }

    public String getCurrency() {
      return currency;
    }

    public void setCurrency(String currency) {
      this.currency = currency;
    }

    public long getUnitAmountCents() {
      return unitAmountCents;
    }

    public void setUnitAmountCents(long unitAmountCents) {
      this.unitAmountCents = unitAmountCents;
    }
  }
}
