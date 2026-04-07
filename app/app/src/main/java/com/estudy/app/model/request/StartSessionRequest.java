package com.estudy.app.model.request;

public class StartSessionRequest {
    private String setId;
    private String mode;

    public StartSessionRequest(String setId, String mode) {
        this.setId = setId;
        this.mode  = mode;
    }

    public String getSetId() { return setId; }
    public String getMode()  { return mode; }
}
