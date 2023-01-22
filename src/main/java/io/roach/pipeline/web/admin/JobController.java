package io.roach.pipeline.web.admin;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Entity;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.web.NotFoundException;
import io.roach.pipeline.config.ApplicationProfiles;
import io.roach.pipeline.item.BatchJobRepository;
import io.roach.pipeline.web.MessageModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/jobs")
@ApplicationProfiles.Online
public class JobController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private BatchJobRepository batchRepository;

    @Autowired
    private JobInstanceModelAssembler jobInstanceModelAssembler;

    @Autowired
    private JobExecutionModelAssembler jobExecutionModelAssembler;

    @Autowired
    private StepExecutionModelAssembler stepExecutionModelAssembler;

    @DeleteMapping
    public ResponseEntity<MessageModel> purgeJobs() {
        int rows = batchRepository.purgeAll();
        return ResponseEntity.ok().body(MessageModel.from("All job metadata deleted (" + rows + " rows)"));
    }

    @GetMapping
    public ResponseEntity<RepresentationModel<?>> listJobs() {
        HalModelBuilder modelBuilder = HalModelBuilder.emptyHalModel()
                .link(linkTo(methodOn(getClass())
                        .listJobs())
                        .withSelfRel());

        jobExplorer.getJobNames().forEach(name -> {
            List<JobInstance> jobInstances = jobExplorer.getJobInstances(name, 0, Integer.MAX_VALUE);
            jobInstances.forEach(jobInstance -> {
                modelBuilder.preview(jobInstanceModelAssembler.toModel(jobInstance))
                        .forLink(linkTo(methodOn(JobController.class)
                                .getJobInstance(jobInstance.getInstanceId()))
                                .withRel(HalLinkRelation.curied(LinkRels.CURIE_NAMESPACE,
                                        LinkRels.JOB_INSTANCE_REL)));
            });

            Set<JobExecution> jobExecutionList = jobExplorer.findRunningJobExecutions(name);
            jobExecutionList.forEach(jobExecution -> {
                modelBuilder.preview(jobExecutionModelAssembler.toModel(jobExecution))
                        .forLink(linkTo(methodOn(JobController.class)
                                .getJobExecution(jobExecution.getId()))
                                .withRel(HalLinkRelation.curied(LinkRels.CURIE_NAMESPACE,
                                        LinkRels.JOB_EXECUTION_REL)));
            });

            Optional<JobInstance> lastJobInstance = Optional.ofNullable(
                    jobExplorer.getLastJobInstance(name));

            lastJobInstance.ifPresent(
                    jobInstance -> modelBuilder.preview(jobInstanceModelAssembler.toModel(jobInstance))
                            .forLink(linkTo(methodOn(JobController.class)
                                    .getJobInstance(jobInstance.getInstanceId()))
                                    .withRel(HalLinkRelation.curied(LinkRels.CURIE_NAMESPACE,
                                            LinkRels.JOB_LAST_INSTANCE_REL))));
        });

        return ResponseEntity.ok().body(modelBuilder.build());
    }

    @GetMapping(value = {"/instance/{jobInstanceId}"})
    public ResponseEntity<JobInstanceModel> getJobInstance(@PathVariable("jobInstanceId") Long jobInstanceId) {
        JobInstance jobInstance = jobExplorer.getJobInstance(jobInstanceId);
        if (jobInstance == null) {
            throw new NotFoundException("No job found with instance id: " + jobInstanceId);
        }
        return ResponseEntity.ok().body(jobInstanceModelAssembler.toModel(jobInstance));
    }

    @GetMapping(value = {"/instance/{jobInstanceId}/executions"})
    public ResponseEntity<CollectionModel<JobExecutionModel>> listJobExecutions(
            @PathVariable("jobInstanceId") Long jobInstanceId) {
        JobInstance jobInstance = jobExplorer.getJobInstance(jobInstanceId);
        if (jobInstance == null) {
            throw new NotFoundException("No job found with instance id: " + jobInstanceId);
        }

        List<JobExecution> jobExecutionList = jobExplorer.getJobExecutions(jobInstance);
        jobExecutionList.sort(Comparator.comparing(Entity::getId));

        return ResponseEntity.ok()
                .body(jobExecutionModelAssembler.toCollectionModel(jobExecutionList)
                        .add(linkTo(methodOn(getClass())
                                .listJobExecutions(jobInstanceId))
                                .withSelfRel()));
    }

    @GetMapping(value = {"/execution/{jobExecutionId}"})
    public ResponseEntity<?> getJobExecution(@PathVariable("jobExecutionId") Long jobExecutionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
        if (jobExecution == null) {
            throw new NotFoundException("No job execution found with id: " + jobExecutionId);
        }

        HalModelBuilder modelBuilder = HalModelBuilder
                .halModelOf(jobExecutionModelAssembler.toModel(jobExecution))
                .link(linkTo(methodOn(getClass())
                        .getJobExecution(jobExecutionId))
                        .withSelfRel());

        jobExecution.getStepExecutions().forEach(stepExecution -> {
            modelBuilder
                    .embed(stepExecutionModelAssembler.toModel(stepExecution))
                    .link(linkTo(methodOn(JobController.class)
                            .getStepExecution(jobExecutionId, stepExecution.getId()))
                            .withRel(HalLinkRelation.curied(LinkRels.CURIE_NAMESPACE,
                                    LinkRels.STEP_EXECUTION_REL)));
        });

        return ResponseEntity.ok().body(modelBuilder.build());
    }

    @DeleteMapping(value = {"/execution/{jobExecutionId}/stop"})
    @PutMapping(value = {"/execution/{jobExecutionId}/stop"})
    public ResponseEntity<JobExecutionModel> stopJobExecution(@PathVariable("jobExecutionId") Long jobExecutionId) {
        try {
            jobOperator.stop(jobExecutionId);
        } catch (NoSuchJobExecutionException e) {
            throw new NotFoundException("No job execution found with id: " + jobExecutionId);
        } catch (JobExecutionNotRunningException e) {
            logger.info(e.toString());
        }

        URI uri = linkTo(methodOn(JobController.class)
                .getJobExecution(jobExecutionId))
                .withRel(LinkRels.JOB_EXECUTION_REL)
                .toUri();

        return ResponseEntity.ok()
                .location(uri).build();
    }

    @GetMapping(value = {"/execution/future/{id}"})
    public ResponseEntity<JobExecutionModel> getFutureJobExecution(@PathVariable("id") UUID id) {
        AtomicReference<JobExecution> match = new AtomicReference<>();

        jobExplorer.getJobNames().forEach(jobName -> {
            jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE).forEach(jobInstance -> {
                jobExplorer.getJobExecutions(jobInstance).forEach(jobExecution -> {
                    String jobId = jobExecution.getJobParameters().getString("jobId");
                    if (id.toString().equals(jobId)) {
                        match.set(jobExecution);
                    }
                });
            });
        });

        if (match.get() == null) {
            throw new NotFoundException("No job execution with id: " + id);
        }

        return ResponseEntity.ok(jobExecutionModelAssembler.toModel(match.get()));
    }

    @GetMapping(value = {"/execution/{jobExecutionId}/steps"})
    public ResponseEntity<CollectionModel<StepExecutionModel>> listStepExecutions(
            @PathVariable("jobExecutionId") Long jobExecutionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
        if (jobExecution == null) {
            throw new NotFoundException("No job execution found with id: " + jobExecutionId);
        }
        return ResponseEntity.ok().body(stepExecutionModelAssembler.toCollectionModel(jobExecution.getStepExecutions())
                .add(linkTo(methodOn(getClass())
                        .listStepExecutions(jobExecutionId))
                        .withSelfRel()));
    }

    @GetMapping(value = {"/execution/{jobExecutionId}/step/{stepExecutionId}"})
    public ResponseEntity<StepExecutionModel> getStepExecution(@PathVariable("jobExecutionId") Long jobExecutionId,
                                                               @PathVariable("stepExecutionId") Long stepExecutionId
    ) {
        StepExecution stepExecution = jobExplorer.getStepExecution(jobExecutionId, stepExecutionId);
        if (stepExecution == null) {
            throw new NotFoundException("No step execution found with id: " + stepExecutionId);
        }
        return ResponseEntity.ok().body(stepExecutionModelAssembler.toModel(stepExecution));
    }
}
