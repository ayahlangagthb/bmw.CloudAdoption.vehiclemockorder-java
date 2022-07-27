package com.bmw.cloudadoption.vehicleorder.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import javax.json.bind.annotation.JsonbTransient;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection //Since we do not use this directly, it has to be registered for native mode
@AllArgsConstructor
@NoArgsConstructor
public class VehicleOrder implements EntityBase {

    private String orderNumber;
    private String vehicleId;
    private List<OrderPerPlant> orderPerPlant;

    @Override
    @JsonbTransient
    public String getKey() {
        return orderNumber;
    }

}
