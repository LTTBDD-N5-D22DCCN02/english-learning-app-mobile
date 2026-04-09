package com.estudy.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ErrorCode {

    // ==========================
    // COMMON / SYSTEM
    // ==========================
    UNCATEGORIZED_EXCEPTION(9999, "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),

    // ==========================
    // AUTHENTICATION & AUTHORIZATION
    // ==========================
    UNAUTHENTICATED(1100, "Authentication required", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1101, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),

    // ==========================
    // VALIDATION - USER INPUT
    // ==========================
    USERNAME_REQUIRED(1200, "Username must not be blank", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1201, "Username must be between 3 and 50 characters", HttpStatus.BAD_REQUEST),

    PASSWORD_REQUIRED(1202, "Password must not be blank", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1203, "Password must be between 8 and 100 characters", HttpStatus.BAD_REQUEST),

    EMAIL_REQUIRED(1215, "Email must not be blank", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1216, "Invalid email format", HttpStatus.BAD_REQUEST),

    FULLNAME_REQUIRED(1204, "Full name must not be blank", HttpStatus.BAD_REQUEST),
    FULLNAME_INVALID(1205, "Full name must not exceed 100 characters", HttpStatus.BAD_REQUEST),

    DOB_REQUIRED(1206, "Date of birth must not be null", HttpStatus.BAD_REQUEST),
    INVALID_DOB(1207, "Age must be at least {min}", HttpStatus.BAD_REQUEST),

    GENDER_REQUIRED(1208, "Gender must not be blank", HttpStatus.BAD_REQUEST),

    PHONE_REQUIRED(1209, "Phone number must not be blank", HttpStatus.BAD_REQUEST),
    PHONE_INVALID(1210, "Phone number must be 10 to 11 digits", HttpStatus.BAD_REQUEST),

    ADDRESS_REQUIRED(1211, "Address must not be blank", HttpStatus.BAD_REQUEST),
    ADDRESS_INVALID(1212, "Address must not exceed 255 characters", HttpStatus.BAD_REQUEST),

    // ==========================
    // USER
    // ==========================
    USER_EXISTED(1300, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1301, "User not found", HttpStatus.NOT_FOUND),
    INVALID_OLD_PASSWORD(1302, "Old password is incorrect", HttpStatus.BAD_REQUEST),

    // ==========================
    // FLASHCARD SET
    // ==========================
    FLASHCARD_SET_NOT_FOUND(2200, "Flashcard set not found", HttpStatus.NOT_FOUND),
    FLASHCARD_SET_NOT_OWNED(2201, "You do not have permission to modify this flashcard set", HttpStatus.FORBIDDEN),
    FLASHCARD_SET_NAME_REQUIRED(2202, "Flashcard set name must not be blank", HttpStatus.BAD_REQUEST),
    FLASHCARD_SET_NAME_INVALID(2203, "Flashcard set name must not exceed 255 characters", HttpStatus.BAD_REQUEST),
    FLASHCARD_SET_PRIVACY_REQUIRED(2204, "Privacy must not be null", HttpStatus.BAD_REQUEST),

    // ==========================
    // FLASHCARD
    // ==========================
    FLASHCARD_NOT_EXISTED(2400, "Flashcard not found", HttpStatus.NOT_FOUND),

    // ==========================
    // COMMENT
    // ==========================
    COMMENT_NOT_FOUND(2300, "Comment not found", HttpStatus.NOT_FOUND),
    COMMENT_NOT_OWNED(2301, "You do not have permission to delete this comment", HttpStatus.FORBIDDEN),
    COMMENT_CONTENT_REQUIRED(2302, "Comment content must not be blank", HttpStatus.BAD_REQUEST),
    COMMENT_CONTENT_INVALID(2303, "Comment content must not exceed 1000 characters", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatusCode getStatusCode() { return statusCode; }
}
