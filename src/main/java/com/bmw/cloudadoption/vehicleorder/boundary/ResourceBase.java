package com.bmw.cloudadoption.vehicleorder.boundary;

import com.bmw.cloudadoption.vehicleorder.entity.EntityBase;
import com.bmw.cloudadoption.vehicleorder.entity.VehicleOrder;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Generic base class for databackend REST endpoints.
 *
 * @param <T> entity type of this REST endpoint
 */
public abstract class ResourceBase<T extends EntityBase> {

    protected static final String SLASH = "/";

    /**
     * Deletes a entity in the backing topic via its key.
     *
     * @param key of the entity to be deleted
     * @return Response whether the entity was deleted or not
     */
    @DELETE
    @Path("{key}")
    @Operation(summary = "delete entry via its key")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "204",
                        description = "Entry deleted."
                ),
                @APIResponse(
                        responseCode = "406",
                        description = "Entry not deleted."
                )
            }
    )
    public abstract Response deleteEntry(@PathParam("key") String key);

    /**
     * Creates or overwrites the given entity in the backing topic.
     *
     * @param entity that should be created or updated
     * @return Response whether the entity was created/updated or not
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "create", summary = "create entry")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "201",
                        description = "Entry created.",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleOrder.class))
                ),
                @APIResponse(
                        responseCode = "406",
                        description = "Entry not created."
                )
            }
    )
    public abstract Response postEntry(T entity);

    /**
     * Updates the given entity by deleting it by its key and then creating it
     * with its new state.
     *
     * @param entity wrapped entity to be updated
     * @param entityPath URI path for this entity
     * @return Response 204
     */
    public Response updateGenericEntry(T entity, String entityPath) {
        boolean successful = deleteEntry(entity.getKey()).getStatus() == 204;
        if (successful) {
            successful = postEntry(entity).getStatus() == 201;
        }
        // If both operations, delete and create, were successful, then the whole operation is successful
        Response.ResponseBuilder response = successful
                ? Response.created(URI.create(entityPath + SLASH + entity.getKey())) : Response.status(Response.Status.NOT_ACCEPTABLE);
        return response.header("key", entity.getKey()).build();
    }
}
