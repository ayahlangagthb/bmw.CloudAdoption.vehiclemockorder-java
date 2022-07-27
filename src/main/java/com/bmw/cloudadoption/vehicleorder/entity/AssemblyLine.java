package com.bmw.cloudadoption.vehicleorder.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection //Since we do not use this directly, it has to be registered for native mode
@AllArgsConstructor
@NoArgsConstructor
public class AssemblyLine {

    private String plantId;
    private String logisticLevel;
    private String areaCode;
}
