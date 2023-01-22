package io.roach.pipeline.web.admin;

import org.springframework.batch.core.JobExecution;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import io.roach.pipeline.web.LinkRels;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class JobExecutionModelAssembler extends RepresentationModelAssemblerSupport<JobExecution, JobExecutionModel> {
    public JobExecutionModelAssembler() {
        super(JobController.class, JobExecutionModel.class);
    }

    @Override
    public JobExecutionModel toModel(JobExecution entity) {
        JobExecutionModel model = new JobExecutionModel();
        model.setJobName(entity.getJobInstance().getJobName());
        model.setStartTime(entity.getStartTime());
        model.setEndTime(entity.getEndTime());
        model.setBatchStatus(entity.getStatus());
        model.setExitStatus(entity.getExitStatus());
        model.setFailures(entity.getFailureExceptions());
        model.setJobParameters(entity.getJobParameters());

        model.add(linkTo(methodOn(JobController.class)
                .getJobExecution(entity.getId()))
                .withSelfRel());

        if (entity.isRunning()) {
            model.add(linkTo(methodOn(JobController.class)
                    .stopJobExecution(entity.getId()))
                    .withRel(LinkRels.STOP_REL));
        }

        model.add(linkTo(methodOn(JobController.class)
                .listStepExecutions(entity.getId()))
                .withRel(LinkRels.STEP_EXECUTIONS_REL));

        return model;
    }

}
