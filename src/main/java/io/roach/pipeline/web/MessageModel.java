package io.roach.pipeline.web;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"links"})
public class MessageModel extends RepresentationModel<MessageModel> {
    public static MessageModel from(String message) {
        return new MessageModel(message);
    }

    private String message;

    private String notice;

    private String important;

    public MessageModel() {
    }

    public MessageModel(String message) {
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getImportant() {
        return important;
    }

    public void setImportant(String important) {
        this.important = important;
    }
}
