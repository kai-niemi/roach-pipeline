package io.roach.pipeline.web.admin;

import java.util.Optional;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import io.roach.pipeline.web.LinkRels;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class JobInstanceModelAssembler extends RepresentationModelAssemblerSupport<JobInstance, JobInstanceModel> {
    @Autowired
    private JobExplorer jobExplorer;

    public JobInstanceModelAssembler() {
        super(JobController.class, JobInstanceModel.class);
    }

    @Override
    public JobInstanceModel toModel(JobInstance entity) {
        JobInstanceModel model = new JobInstanceModel();
        model.setName(entity.getJobName());
        model.setId(entity.getId());

        model.add(linkTo(methodOn(JobController.class)
                .getJobInstance(entity.getId()))
                .withSelfRel());

        int executions = jobExplorer.getJobExecutions(entity).size();
        if (executions > 0) {
            model.add(linkTo(methodOn(JobController.class)
                    .listJobExecutions(entity.getId()))
                    .withRel(LinkRels.JOB_EXECUTIONS_REL));

            Optional<JobExecution> lastJobExecution = Optional.ofNullable(jobExplorer.getLastJobExecution(entity));
            lastJobExecution.ifPresent(jobExecution -> model.add(linkTo(methodOn(JobController.class)
                    .getJobExecution(jobExecution.getId()))
                    .withRel(LinkRels.JOB_LAST_EXECUTION_REL)));
        }

        return model;
    }
}
