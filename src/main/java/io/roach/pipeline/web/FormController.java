package io.roach.pipeline.web;

import java.util.Map;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;

public interface FormController<T extends FormModel<? extends T>> {
    @GetMapping(value = {"/forms/bundle"}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<StreamingResponseBody> getFormTemplatesBundle(
            @RequestParam Map<String, String> requestParams, HttpServletResponse response) throws JobExecutionException;

    @GetMapping(value = {"/forms"})
    ResponseEntity<CollectionModel<T>> getFormTemplates(@RequestParam Map<String, String> requestParams) throws JobExecutionException;

    @GetMapping(value = {"/form"})
    ResponseEntity<T> getFormTemplate(@RequestParam Map<String, String> requestParams) throws JobExecutionException;

    @PostMapping
    ResponseEntity<?> submitForm(@RequestBody T form) throws JobExecutionException;
}
