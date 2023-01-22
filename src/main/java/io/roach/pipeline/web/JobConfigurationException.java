package io.roach.pipeline.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class JobConfigurationException extends RuntimeException {
    public JobConfigurationException(String message) {
        super(message);
    }

    public JobConfigurationException(Throwable cause) {
        super(cause);
    }
}
