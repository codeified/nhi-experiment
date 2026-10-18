package com.nhi.experiment.model;

public class NhiIdentity {

    private String nhiId;
    private String name;
    private String type;
    private String application;
    private String environment;
    private String owner;
    private String status;

    public NhiIdentity() {
    }

    public NhiIdentity(
            String nhiId,
            String name,
            String type,
            String application,
            String environment,
            String owner,
            String status) {

        this.nhiId = nhiId;
        this.name = name;
        this.type = type;
        this.application = application;
        this.environment = environment;
        this.owner = owner;
        this.status = status;
    }

    public String getNhiId() {
        return nhiId;
    }

    public void setNhiId(String nhiId) {
        this.nhiId = nhiId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}