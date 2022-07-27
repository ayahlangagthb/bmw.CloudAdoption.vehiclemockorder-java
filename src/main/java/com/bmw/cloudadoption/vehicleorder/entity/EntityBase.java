package com.bmw.cloudadoption.vehicleorder.entity;

/**
 * To generify handling of specific entities within the databackend, this
 * interface is being used to ensure required methods are present in every
 * entity type.
 */
public interface EntityBase {

    String UNDERSCORE = "_";

    String getKey();
}
