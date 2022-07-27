package com.bmw.cloudadoption.vehicleorder.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
@AllArgsConstructor
@NoArgsConstructor
public class OrderPerPlant {

    private String plantId;

    private LocalDate plannedOrderStartDate;

    private LocalDate plannedOrderEndDate;

    private AssemblyLine assemblyLine;
}
