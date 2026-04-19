package com.estudy.app.bot;

public class BotMessage {

    public enum Type { USER, BOT }

    private String text;
    private final Type type;

    public BotMessage(String text, Type type) {
        this.text = text;
        this.type = type;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Type getType() { return type; }
}
