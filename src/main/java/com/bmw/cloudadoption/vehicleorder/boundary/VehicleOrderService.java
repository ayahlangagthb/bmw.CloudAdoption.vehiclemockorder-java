package com.bmw.cloudadoption.vehicleorder.boundary;

import com.bmw.cloudadoption.vehicleorder.entity.VehicleOrder;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path(VehicleOrderResource.ENTITY_PATH)
@RegisterRestClient
@Produces(MediaType.APPLICATION_JSON)
@RegisterClientHeaders
public interface VehicleOrderService {

    /**
     * Used to fetch an individual entry from another databackend instance.
     *
     * @param key that should be fetched
     * @return entry for given key
     */
    @GET
    @Path("{key}")
    VehicleOrder getEntry(@PathParam("key") String key);

    /**
     * Used to fetch all entries from another databackend instance.
     *
     * @param localOnly always true since we just want the local data from the
     * called instance
     * @return all entries from the called instance
     */
    @GET
    CompletionStage<List<VehicleOrder>> getAll(@QueryParam("local") boolean localOnly);
}
