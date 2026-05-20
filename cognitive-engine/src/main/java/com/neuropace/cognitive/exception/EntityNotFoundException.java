package com.neuropace.cognitive.exception;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {
    private final String entityName;
    private final Object entityId;

    public EntityNotFoundException(String entityName, Object entityId) {
        super(String.format("%s with ID '%s' not found.", entityName, entityId));
        this.entityName = entityName;
        this.entityId = entityId;
    }
}
