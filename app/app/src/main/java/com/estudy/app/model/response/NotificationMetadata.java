package com.estudy.app.model.response;

public class NotificationMetadata {
    private String status;     // "approved" | "pending" — dùng cho join_request
    private String classId;    // dùng cho join_request, new_set
    private String setId;      // dùng cho new_set, new_comment
    private String commentId;  // dùng cho new_comment

    public String getStatus()    { return status; }
    public String getClassId()   { return classId; }
    public String getSetId()     { return setId; }
    public String getCommentId() { return commentId; }
}
