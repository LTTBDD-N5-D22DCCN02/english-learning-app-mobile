package com.estudy.app.model.request;

import com.google.gson.annotations.SerializedName;

public class JoinClassRequest {

    // Backend expect field name "code" (khớp với @NotBlank String code trong DTO)
    @SerializedName("code")
    private String code;

    public JoinClassRequest(String code) { this.code = code; }
    public String getCode() { return code; }
}