package ml.echelon133.common.exception;

import java.util.UUID;

/**
 * Exception thrown when a resource with the specified {@link UUID} is not found in the database.
 */
public class ResourceNotFoundException extends Exception {

    private final Class resourceClass;
    private final UUID resourceId;

    public ResourceNotFoundException(Class cls, UUID id) {
        super(String.format("%s %s could not be found", cls.getSimpleName().toLowerCase(), id));
        this.resourceClass = cls;
        this.resourceId = id;
    }

    public Class getResourceClass() {
        return resourceClass;
    }

    public UUID getResourceId() {
        return resourceId;
    }
}
