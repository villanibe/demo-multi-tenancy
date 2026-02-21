package com.demo.multitenancy.billing.webhook.virtual;

public class VirtualWebhookRequest {

  private String eventId;
  private String type;
  private String sessionId;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
}
