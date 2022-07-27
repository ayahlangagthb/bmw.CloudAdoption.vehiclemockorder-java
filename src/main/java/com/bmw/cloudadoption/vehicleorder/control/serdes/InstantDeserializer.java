package com.bmw.cloudadoption.vehicleorder.control.serdes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;


public class InstantDeserializer extends JsonDeserializer<Instant> {
    
    private DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;


    @Override
    public Instant deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        return Instant.from(fmt.parse(jp.getText()));
    }
    
}
