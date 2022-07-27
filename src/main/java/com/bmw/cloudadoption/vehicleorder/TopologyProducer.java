package com.bmw.cloudadoption.vehicleorder;

import com.bmw.cloudadoption.vehicleorder.entity.VehicleOrder;

import io.quarkus.kafka.client.serialization.JsonbSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.inject.Produces;

public class TopologyProducer {

    public static final String VEHICLE_ORDER_STORE_NAME = "vehicle-order-store";

    @ConfigProperty(name = "KAFKA_VEHICLE_ORDER_TOPIC")
    String vehicleOrderTopic;

    /**
     *
     * @return Topology of this application
     */
    @Produces
    public Topology buildTopology() {

        StreamsBuilder builder = new StreamsBuilder();

        KeyValueBytesStoreSupplier vehicleOrderStoreSupplier = Stores.persistentKeyValueStore(VEHICLE_ORDER_STORE_NAME);
        builder.table(vehicleOrderTopic,
                Consumed.with(Serdes.String(), new JsonbSerde<>(VehicleOrder.class)),
                Materialized.as(vehicleOrderStoreSupplier)
        );

        return builder.build();
    }

}
