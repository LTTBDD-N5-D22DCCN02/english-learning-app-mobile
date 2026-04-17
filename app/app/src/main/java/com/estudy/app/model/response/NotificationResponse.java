package com.estudy.app.model.response;

public class NotificationResponse {
    private String id;
    private String type;       // "vocab_reminder" | "join_request" | "new_set" | "new_comment"
    private String title;
    private String content;
    private boolean isRead;
    private String createdAt;
    private NotificationMetadata metadata;

    public String getId()                    { return id; }
    public String getType()                  { return type; }
    public String getTitle()                 { return title; }
    public String getContent()               { return content; }
    public boolean isRead()                  { return isRead; }
    public String getCreatedAt()             { return createdAt; }
    public NotificationMetadata getMetadata(){ return metadata; }

    public void setRead(boolean read)        { this.isRead = read; }
}
