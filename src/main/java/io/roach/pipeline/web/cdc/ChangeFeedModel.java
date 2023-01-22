package io.roach.pipeline.web.cdc;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"links", "embedded", "templates"})
public class ChangeFeedModel extends RepresentationModel<ChangeFeedModel> {
    private String message;

    private String simpleChangeFeedStatement;

    private String advancedChangeFeedStatement;

    public ChangeFeedModel() {
    }

    public String getMessage() {
        return message;
    }

    public ChangeFeedModel setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getSimpleChangeFeedStatement() {
        return simpleChangeFeedStatement;
    }

    public void setSimpleChangeFeedStatement(String simpleChangeFeedStatement) {
        this.simpleChangeFeedStatement = simpleChangeFeedStatement;
    }

    public String getAdvancedChangeFeedStatement() {
        return advancedChangeFeedStatement;
    }

    public void setAdvancedChangeFeedStatement(String advancedChangeFeedStatement) {
        this.advancedChangeFeedStatement = advancedChangeFeedStatement;
    }
}
