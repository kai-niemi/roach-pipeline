package io.roach.pipeline.web.home;

import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"links", "embedded"})
public class DataSourceTableModel<T> extends CollectionModel<T> {
    private String url;

    private String message;

    private final List<String> topologyOrder;

    public DataSourceTableModel(Iterable<T> content, List<String> topologyOrder) {
        super(content);
        this.topologyOrder = topologyOrder;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTopologyOrder() {
        return StringUtils.collectionToCommaDelimitedString(topologyOrder);
    }
}
