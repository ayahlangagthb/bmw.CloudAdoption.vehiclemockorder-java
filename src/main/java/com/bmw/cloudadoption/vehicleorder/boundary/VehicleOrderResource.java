package com.bmw.cloudadoption.vehicleorder.boundary;

import com.bmw.cloudadoption.vehicleorder.control.VehicleOrderInteractiveQueries;
import com.bmw.cloudadoption.vehicleorder.entity.VehicleOrder;

import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path(VehicleOrderResource.ENTITY_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@OpenAPIDefinition(info = @Info(
        title = "VehicleOrder API",
        description = "Allows CRUD operation on Vehicle Order",
        version = "1.0.0"
))
@Tag(name = "Vehicle Order API", description = "This REST API enables operations on Vehicle order resources that are backed by a Kafka topic")
public class VehicleOrderResource extends ResourceBase<VehicleOrder> {

    public static final String ENTITY_PATH = "vehicleorder";

    @Inject
    VehicleOrderInteractiveQueries interactiveQueries;

    @ConfigProperty(name = "KAFKA_VEHICLE_ORDER_TOPIC")
    String entityTopic;

    @Inject
    @Channel("kafka")
    Emitter<VehicleOrder> emitter;

    /**
     * Get individual entity by key.
     *
     * @param key to search for
     * @return Response containing the entity, otherwise not found response
     */
    @GET
    @Path("{key}")
    @Operation(operationId = "getVehicleOrder", summary = "get single VehicleOrder entry")
    @APIResponses(value = {
        @APIResponse(
                responseCode = "200",
                description = "Entry for the requested key.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleOrder.class))),
        @APIResponse(
                responseCode = "204",
                description = "Entry for requested key not found.")})
    public Response getEntry(@PathParam("key") String key) {
        VehicleOrder entry = interactiveQueries.getEntry(key);
        Response.ResponseBuilder response = entry != null ? Response.ok(entry) : Response.status(Response.Status.NOT_FOUND);
        return response.build();
    }

    /**
     * Get all entities of this type.
     *
     * @param localOnly if true, only return local data, if false, returns data
     * from all running instances
     * @return List of all available entities of this type
     */
    @GET
    @Operation(operationId = "getAllVehicleOrder", summary = "get all VehicleOrder entries")
    @APIResponses(value = {
        @APIResponse(
                responseCode = "200",
                description = "All entries for the requested entity.",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(type = SchemaType.ARRAY, implementation = VehicleOrder.class)))})
    public CompletionStage<List<VehicleOrder>> getAll(@QueryParam("local") boolean localOnly) {
        return CompletableFuture.supplyAsync(() -> interactiveQueries.getAll(localOnly));
    }

    @Override
    public Response postEntry(VehicleOrder entity) {
        // FE sends PUT and POST with UpdateEntityWrapper
        String entityKey = entity.getKey();

        // Build metadata..
        OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(entityKey)
                .withTopic(entityTopic)
                .build();
        // .. and send the record
        emitter.send(Message.of(entity, Metadata.of(metadata)));
        // Verify that our created or updated entity already made it back to our state store
        Response.ResponseBuilder response = interactiveQueries.verifyEntryInStateStore(entityKey, entity)
                ? Response.created(URI.create(ENTITY_PATH + SLASH + entityKey)) : Response.status(Response.Status.NOT_ACCEPTABLE);
        return response.header("key", entityKey).build();
    }

    /**
     * Updates a entity by deleting it via its old key and creating it with the
     * new state.
     *
     * @param entity that should be updated
     * @return Response whether the entity was updated or not
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateVehicleOrder", summary = "update vehicle order entry")
    @APIResponses(value = {
        @APIResponse(
                responseCode = "201",
                description = "Entry updated.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleOrder.class))),
        @APIResponse(
                responseCode = "406",
                description = "Entry not updated.")})
    public Response updateEntry(VehicleOrder entity) {
        return updateGenericEntry(entity, ENTITY_PATH);
    }

    @Override
    public Response deleteEntry(String key) {
        return interactiveQueries.deleteEntry(key, entityTopic, interactiveQueries);
    }

}
