package com.demo.multitenancy.security.authorization;

public final class PermissionCodes {
  private PermissionCodes() {
  }

  public static final String TODO_READ = "todo.read";
  public static final String TODO_WRITE = "todo.write";

  public static final String USER_READ = "user.read";
  public static final String USER_WRITE = "user.write";

  public static final String SUBSCRIPTION_READ = "subscription.read";
  public static final String SUBSCRIPTION_WRITE = "subscription.write";

  public static final String BILLING_WRITE = "billing.write";

  public static final String TENANT_INVITE_WRITE = "tenant.invite.write";

  public static final String AUTHZ_READ = "authz.read";
  public static final String AUTHZ_WRITE = "authz.write";

  public static final String ADMIN = "admin";
}
