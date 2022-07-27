package com.bmw.cloudadoption.vehicleorder.control;

import com.bmw.cloudadoption.vehicleorder.TopologyProducer;
import com.bmw.cloudadoption.vehicleorder.boundary.VehicleOrderService;
import com.bmw.cloudadoption.vehicleorder.entity.VehicleOrder;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

/**
 * Discrete implementation of {@link InteractiveQueriesBase}. Handles
 * interactions with entities of type {@link VehicleOrder}.
 */
@ApplicationScoped
public class VehicleOrderInteractiveQueries extends InteractiveQueriesBase<String, VehicleOrder, VehicleOrderService> {

    // function returning the concrete rest-client
    private static final Function<URL, VehicleOrderService> GET_REST_CLIENT
            = url -> RestClientBuilder.newBuilder().baseUrl(url).build(VehicleOrderService.class);

    // function for fetching one message by the key
    private static final BiFunction<VehicleOrderService, String, VehicleOrder> GET_ENTRY = VehicleOrderService::getEntry;

    // function for fetching all messages from the local state-store
    private static final Function<VehicleOrderService, CompletionStage<List<VehicleOrder>>> GET_ALL
            = restClient -> restClient.getAll(true);

    /**
     * Instantiates our class for our concrete entity type.
     */
    public VehicleOrderInteractiveQueries() {
        super(TopologyProducer.VEHICLE_ORDER_STORE_NAME, String.class, GET_REST_CLIENT, GET_ENTRY, GET_ALL);
    }

}
