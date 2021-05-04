package me.soels.tocairn.api.dtos;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class ErrorResponseDto {
    String message;
    ZonedDateTime timestamp;
    String exceptionClass;
}
