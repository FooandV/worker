# Order Worker Service

Este proyecto implementa un sistema de procesamiento de pedidos utilizando Apache Kafka, Redis, MongoDB, y APIs desarrolladas en Go.

## Descripción

Este proyecto es un sistema de procesamiento de pedidos basado en microservicios, diseñado para consumir mensajes de pedidos desde Kafka, enriquecerlos con datos adicionales utilizando APIs externas (desarrolladas en Go) y almacenarlos en una base de datos MongoDB. El sistema también utiliza Redis para gestionar el caching de datos y la concurrencia de pedidos

## Caracteristicas principales

- Consumo de Mensajes de Kafka: El sistema escucha mensajes de un tópico de Kafka y procesa cada pedido de manera reactiva.

- Enriquecimiento de Datos: Los datos de clientes y productos son enriquecidos llamando a APIs externas. Estos datos son cacheados en Redis para optimizar el rendimiento.

- Validación de Pedidos: El sistema valida que el cliente esté activo y que los productos existan antes de almacenar el pedido.

- Almacenamiento en MongoDB: Los pedidos procesados se almacenan en una base de datos MongoDB.

- Manejo de Errores: Se utilizan reintentos exponenciales y circuit breakers para manejar fallos en las APIs externas.

- Locks distribuidos: Para evitar que un mismo pedido sea procesado varias veces simultáneamente, se implementan locks distribuidos utilizando Redis.

## Tecnologías

El sistema utiliza los siguientes componentes:
- **Java 21**
- **Spring Boot 3.x**
- **Spring WebFlux** (Programación reactiva)
- **Kafka** (Mensajería distribuida): Para la comunicación y recepción de mensajes sobre nuevos pedidos.
- **Redis** (Caching y Locks distribuidos): Para gestionar bloqueos distribuidos y reintentos.
- **MongoDB** (Base de datos NoSQL): Para almacenar los pedidos enriquecidos.
- **Resilience4j** (Manejo de reintentos y circuit breakers)
- **Go** (APIs externas para enriquecer datos de clientes y productos)
- **Docker** (para correr instancias de Kafka, Redis y MongoDB)


## Requisitos

Antes de ejecutar el proyecto, asegúrate de tener los siguientes requisitos instalados:

- Java 21+
- Maven
- Docker (para levantar instancias de Kafka, Redis y MongoDB)
- Node.js (opcional si quieres correr las APIs de Go de manera local)

## Requisitos
worker/
│
├── src/main/java/com/foo/worker/
│   ├── config/          # Configuraciones de MongoDB, Redis, etc.
│   ├── consumer/        # Kafka Consumer para procesar mensajes
│   ├── models/          # Modelos de datos
│   ├── repository/      # Repositorios de MongoDB
│   ├── service/         # Lógica de negocios (enriquecimiento de datos, locks, etc.)
│   └── WorkerApplication.java  # Punto de entrada de la aplicación
│
├── src/test/java/com/foo/worker/  # Pruebas unitarias
├── Dockerfile         # Configuración de Docker (si aplica)
├── docker-compose.yml # Definición de servicios Docker (Kafka, Redis, MongoDB)
├── pom.xml            # Archivo de configuración de Maven
└── README.md          # Documentación del proyecto


