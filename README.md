# Vehicle Order Service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Prerequites
- Java 11
- Maven
- Docker
- Kafka (See 'Running kafka locally' section)

## Running kafka locally

You need Docker and docker compose installed on your machine.

From the [local_kafka](local_kafka) folder run:
```
docker-compose up -d
```

## Connecting to the deployed Kafka cluster

[Download the kafka binary](https://kafka.apache.org/downloads)
Create a properties file in the bin folder called sandbox-config.properties with the following:
```
bootstrap.servers=pkc-lq8gm.westeurope.azure.confluent.cloud:9092
ssl.endpoint.identification.algorithm=https
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="3LVJS32PKIBHQOMX" password="GgGhT2lm+cu1KTQWhwdJ/cOoFLchKIr1dHHaisYfsPK3J3r8+WCCc/VIowvlHCUB";
```

Run the command (in the bin folder) to consume from the topic:
```
.\kafka-console-consumer.bat --bootstrap-server pkc-lq8gm.westeurope.azure.confluent.cloud:9092 --topic bmw.cloudadoption.vehiclemockorder.v1 --consumer.config sandbox-config.properties --property print.key=true
```


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```
Once deployed you can view the swagger page at http://localhost:8080/q/swagger-ui/

## Vehicle-order JSON

```
{
    "orderNumber": "MB25042",
    "vehicleId": "G01",
    "orderPerPlant": [
        {
            "plantId": "034.00",
            "plannedOrderStartDate": "2022-05-11",
            "plannedOrderEndDate": "2022-05-12",
            "assemblyLine": {
                "plantId": "034.00",
                "logisticLevel": "R0",
                "areaCode": "01"
            }
        }
    ]
}
```

## Deployment URL

https://vehicle-order-service-java.ttd.aws.bmw.cloud/


## Technical Documentation for Vehicle Order Service (VOS)

VOS is a self-contained service that provides direct communication with the `bmw.cloudadoption.VehicleMockOrder.v1` Kafka state-store to perform any necessary actions regarding ordering vehicles (new orders, modifications, etc.). In order to do so,

VOS consists of a REST interface, explained in this handbook, from which all CRUD operations can be preformed on the KAFKA state store. Each endpoint in the interface represents a different call related to one of them.

The  VOS does not make use of a relational entity store instead it uses KAFKA streams state store.

Kafka State Store is a feature provided by Kafka Streams, which is a client library for building applications and microservices that process record streams in real-time. The state store allows these applications to store and query data, which is an essential capability for many stream processing use cases. In Kafka Streams, each stream task has an associated state store that can be used to store and query data. This state store is local to the application instance that hosts the stream task, which means that it can be accessed with low latency.

Here is the entire list of available endpoints. All of them are detailed in the REST Interface section of this handbook:

![image.png](https://eraser.imgix.net/workspaces/xJrLTP4HeFsZQMDbAyWP/dbRns97g32foSlxPargWzxBXpHg2/ZWkZqlFluMGFH0umMzJ_9.png?ixlib=js-3.7.0 "image.png")

### Swagger URL

https://vehicle-order-service-java.ttd.aws.bmw.cloud/q/swagger-ui

## Data flow

![Application Overview](/assets/application-overview-export-4-4-2024-8_50_49-AM.png)


### Entity/Document Relationship

1. _VehicleOrder_: This is the main entity representing an order for a vehicle. It contains a list of OrderPerPlant entities, indicating that a single VehicleOrder can be associated with multiple plants where the vehicle is to be manufactured.
2. _OrderPerPlant_: This entity represents an order for a vehicle at a specific plant. It contains an AssemblyLine entity, indicating that each OrderPerPlant is associated with a specific assembly line in the plant where the vehicle is to be assembled.
3. _AssemblyLine_: This entity represents an assembly line in a plant. It doesn't have a direct relationship with other entities, but it's associated with OrderPerPlant, indicating that each assembly line is dedicated to a specific order at a plant.
   ![Entity relationship](/assets/entity-relationship-export-4-4-2024-8_50_50-AM.png)
