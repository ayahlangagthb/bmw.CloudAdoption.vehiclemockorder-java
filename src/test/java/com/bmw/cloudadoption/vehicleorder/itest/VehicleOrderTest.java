package com.bmw.cloudadoption.vehicleorder.itest;

import com.bmw.cloudadoption.vehicleorder.base.KafkaResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(KafkaResource.class)
public class VehicleOrderTest {

  @Test
  public void postAndGetEntry() {

  }

  @Test
  public void postAndGetAll() throws InterruptedException {

  }

  @Test
  public void postPutGetAndDelete() {

  }

}
