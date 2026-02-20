package com.demo.multitenancy.user.api;

import java.util.ArrayList;
import java.util.List;

public class RoleResponse {
  private String name;
  private List<String> permissionCodes = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getPermissionCodes() {
    return permissionCodes;
  }

  public void setPermissionCodes(List<String> permissionCodes) {
    this.permissionCodes = permissionCodes;
  }
}
