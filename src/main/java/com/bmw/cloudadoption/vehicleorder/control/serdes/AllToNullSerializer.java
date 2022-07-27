package com.bmw.cloudadoption.vehicleorder.control.serdes;

import org.apache.kafka.common.serialization.Serializer;

/**
 * This serializer converts every value into a tombstone record.
 * <p>
 * Reactive messaging does not allow to send messages with payload == null, therefore we use this serializer.
 * The {@code Message} can be supplied with any value and when the underlying Kafka producer writes the message
 * to the broker, this serializer is being used - any value is ignored and a tombstone is written.
 *
 * @param <T> type that should be serialized. Irrelevant since we always produce tombstone records
 */
public class AllToNullSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize(String s, T t) {
        // Since Kafka is interpreting every value as byte-stream, using getBytes here should be fine
        return "null".getBytes(); //NOSONAR
    }
}
