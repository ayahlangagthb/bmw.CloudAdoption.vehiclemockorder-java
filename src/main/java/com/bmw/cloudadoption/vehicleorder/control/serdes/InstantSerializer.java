package com.bmw.cloudadoption.vehicleorder.control.serdes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class InstantSerializer extends JsonSerializer<Instant> {

    private DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;

    @Override
    public void serialize(Instant instant, JsonGenerator jsonGenerator, SerializerProvider sp) throws IOException {
        String str = fmt.format(instant);
        jsonGenerator.writeString(str);
    }
}
