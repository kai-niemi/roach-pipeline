package io.roach.pipeline.web.admin;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.web.LinkRels;

@Relation(value = LinkRels.JOB_INSTANCE_REL,
        collectionRelation = LinkRels.JOB_INSTANCES_REL)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"links", "embedded"})
public class JobInstanceModel extends RepresentationModel<JobInstanceModel> {
    private String name;

    private Long id;

    public String getName() {
        return name;
    }

    public JobInstanceModel setName(String name) {
        this.name = name;
        return this;
    }

    public Long getId() {
        return id;
    }

    public JobInstanceModel setId(Long id) {
        this.id = id;
        return this;
    }
}
