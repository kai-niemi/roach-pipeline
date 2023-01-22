package io.roach.pipeline.web.admin;

import org.springframework.batch.core.StepExecution;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class StepExecutionModelAssembler
        extends RepresentationModelAssemblerSupport<StepExecution, StepExecutionModel> {
    public StepExecutionModelAssembler() {
        super(JobController.class, StepExecutionModel.class);
    }

    @Override
    public StepExecutionModel toModel(StepExecution entity) {
        StepExecutionModel model = new StepExecutionModel();
        model.setStepName(entity.getStepName());
        model.setStatus(entity.getStatus());
        model.setReadCount(entity.getReadCount());
        model.setWriteCount(entity.getWriteCount());
        model.setCommitCount(entity.getCommitCount());
        model.setRollbackCount(entity.getRollbackCount());
        model.setReadSkipCount(entity.getReadSkipCount());
        model.setProcessSkipCount(entity.getProcessSkipCount());
        model.setWriteSkipCount(entity.getWriteSkipCount());
        model.setStartTime(entity.getStartTime());
        model.setEndTime(entity.getEndTime());
        model.setLastUpdated(entity.getLastUpdated());
        model.setExitStatus(entity.getExitStatus());
        model.setTerminateOnly(entity.isTerminateOnly());
        model.setFilterCount(entity.getFilterCount());
        model.setFailureExceptions(entity.getFailureExceptions());

        model.add(linkTo(methodOn(JobController.class)
                .getStepExecution(entity.getJobExecutionId(), entity.getId()))
                .withSelfRel());

        return model;
    }
}
