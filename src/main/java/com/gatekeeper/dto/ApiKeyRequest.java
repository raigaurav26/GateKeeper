package com.gatekeeper.dto;

public class ApiKeyRequest {

    private String name;
    private String owner;
    private String tier;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
}
