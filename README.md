# Vehicle Order Service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Prerequites
- Java 11
- Maven
- Docker
- Kafka (See 'Running kafka locally' section)

## Data flow
![Data flow](vehicle_orders_data_flow.png)

## Running kafka locally

You need Docker and docker compose installed on your machine. 

From the [local_kafka](local_kafka) folder run: 
```
docker-compose up -d
```

This will bring up kafka running in docker. Use the AKHQ front end at http://localhost:8070 to create the topic bmw.cloudadoption.VehicleMockOrder.v1 with a partion and replication value of 1

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

