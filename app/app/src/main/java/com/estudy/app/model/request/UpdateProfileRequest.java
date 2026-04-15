package com.estudy.app.model.request;

public class UpdateProfileRequest {
    private String fullName;
    private String email;
    private String phone;
    private String dob;  // format: "yyyy-MM-dd"

    public UpdateProfileRequest(String fullName, String email, String phone, String dob) {
        this.fullName = fullName;
        this.email    = email;
        this.phone    = phone;
        this.dob      = dob;
    }

    public String getFullName() { return fullName; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }
    public String getDob()      { return dob; }
}
