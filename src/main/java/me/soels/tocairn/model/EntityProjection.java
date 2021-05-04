package me.soels.tocairn.model;

import lombok.Value;

import java.util.UUID;

@Value
public class EntityProjection {
    long identity;
    UUID id;
}
