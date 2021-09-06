package me.soels.tocairn.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID id) {
        this("Could not find resource with id " + id.toString());
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
