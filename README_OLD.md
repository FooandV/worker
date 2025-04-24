# 🛒 Order Worker Service

Este proyecto es una aplicación de procesamiento de pedidos que consume mensajes desde un tópico de Kafka, enriquece los pedidos utilizando APIs externas (desarrolladas en Go), almacena los pedidos en MongoDB, y gestiona la resiliencia mediante Resilience4j y Reactor.

## Descripción del Proyecto

El sistema está diseñado para procesar pedidos recibidos a través de Kafka. Cada pedido se enriquece con información adicional de un cliente y de productos mediante llamadas a APIs externas desarrolladas en Go. Los pedidos procesados se almacenan en una base de datos MongoDB. En caso de errores, el sistema implementa reintentos exponenciales y circuit breakers para garantizar la resiliencia

## Caracteristicas principales

- Consumo de Mensajes de Kafka: El sistema escucha mensajes de un tópico de Kafka y procesa cada pedido de manera reactiva.

- Enriquecimiento de Datos: Los datos de clientes y productos son enriquecidos llamando a APIs externas. Estos datos son cacheados en Redis para optimizar el rendimiento.

- Validación de Pedidos: El sistema valida que el cliente esté activo y que los productos existan antes de almacenar el pedido.

- Almacenamiento en MongoDB: Los pedidos procesados se almacenan en una base de datos MongoDB.

- Manejo de Errores: Se utilizan reintentos exponenciales y circuit breakers para manejar fallos en las APIs externas.

- Locks distribuidos: Para evitar que un mismo pedido sea procesado varias veces simultáneamente, se implementan locks distribuidos utilizando Redis.

## Tecnologías Utilizadas

El sistema utiliza los siguientes componentes:
- **Java 21**
- **Spring Boot 3.x**
- **Spring WebFlux** (Programación reactiva)
- **Kafka** (Mensajería distribuida): Para la comunicación y recepción de mensajes sobre nuevos pedidos.
- **Redis** (Caching y Locks distribuidos): Para gestionar bloqueos distribuidos y reintentos.
- **MongoDB** (Base de datos NoSQL): Para almacenar los pedidos enriquecidos.
- **Resilience4j** (Manejo de reintentos y circuit breakers)
- **Go** (APIs externas para enriquecer datos de clientes y productos)
- **Docker** (para correr instancias de Kafka, Redis y MongoDB)Pendiente, aun no se ha dockerizado
  
- **JUnit 5 y Mockito** (para pruebas unitarias)


## Requisitos Previos

Antes de ejecutar el proyecto, asegúrate de tener los siguientes requisitos instalados:

- Java 17 o superior
- Maven
- Docker (para levantar instancias de Kafka, Redis y MongoDB)
- Node.js (opcional si quieres correr las APIs de Go de manera local)

## API de Enriquecimiento en Go
Este proyecto incluye dos APIs de prueba desarrolladas en Go para enriquecer los pedidos recibidos desde Kafka. Es necesario tener Go instalado en tu sistema para ejecutar estas APIs

## Instalar dependencias
Si no tienes las dependencias instaladas, puedes hacer lo siguiente:

- Para MongoDB y Redis:
  

sudo apt install mongodb redis

- Para GO:
Si aún no tienes Go instalado, puedes seguir las instrucciones de instalación desde Go Programming Language

## Configuración del Proyecto

- Clona el repositorio desde GitHub:
  
git clone https://github.com/FooandV/worker.git

cd worker

- Configuración de Propiedades
Configura las propiedades en el archivo:
 src/main/resources/application.properties según tu entorno local.
Por ejemplo:

# Configuración de Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=order_group

# Configuración de MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/pedidosDB

# Configuración de Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

Asegúrate de que Kafka, MongoDB, Redis y GO estén ejecutándose en tu máquina antes de ejecutar el proyecto

## Ejecución del Proyecto
Sigue estos pasos para ejecutar la aplicación:

1- Inicia los servicios de Kafka, MongoDB, Redis y GO.

* Para MongoDB:
sudo service mongod start

* Para Redis:
sudo service redis-server start

* Para Kafka, asegúrate de que el servidor Kafka esté activo y que el tópico "orders" esté creado:
  
kafka-topics.sh --create --topic orders --bootstrap-server localhost:9092

2- Ejecutar la Aplicación. 
Desde el directorio raíz del proyecto, usa Maven o cualquier otro IDE para ejecutar el proyecto:

mvn spring-boot:run

O si prefieres, puedes ejecutar la clase principal:

java -jar target/order-worker-0.0.1-SNAPSHOT.jar

3- Navega al directorio donde se encuentran las APIs (.\go-api).
Ejecuta el siguiente comando para iniciar el servidor de GO:

go run main.go

Esto iniciará las APIs en http://localhost:8081

Las APIs son las siguientes:

* API de Producto: GET /product
* API de Cliente: GET /customer

## Ejemplo JSON para pruebas lanzadas desde KAFKA

{
  "orderId": "order-100",
  "customerId": "Freyder-111",
  "products": [
    {
      "productId": "product-100",
      "name": "Iphone",
      "price": 2000
    }
  ]
}

El flujo completo incluirá la recepción de este mensaje en el tópico de Kafka, su procesamiento por la aplicación Java, y el enriquecimiento de los datos del cliente y producto utilizando las APIs de Go, para luego la información ser almacenada en Mongo en Redis segun sea el caso

## Pruebas Unitarias
Las pruebas unitarias están implementadas con JUnit 5 y Mockito. Para ejecutarlas:

mvn test

Se han cubierto los siguientes escenarios de prueba:

* Procesamiento exitoso de pedidos.
* Fallos por cliente inactivo.
* Fallos por productos no encontrados.
* Gestión de locks en Redis.

## Optimización y Escalabilidad
- Índices en MongoDB
El sistema utiliza índices en los campos orderId y customerId para optimizar las consultas a MongoDB. Estos índices se crean automáticamente al iniciar la aplicación.
se ejecuta el siguiente comando para validar los indices en Mongo:

db.orders.getIndexes()

- Caché en Redis
Para reducir la carga de las APIs externas, los datos de cliente y producto se almacenan en Redis, lo que permite un acceso más rápido en caso de repetición de pedidos

- Caché y Resiliencia
Se utiliza Resilience4j para manejar los reintentos automáticos y circuit breakers. El sistema está diseñado para ser resiliente a fallos en las APIs externas, asegurando reintentos exponenciales en caso de errores temporales.
El caché de Redis almacena información de clientes y productos para reducir las llamadas a las APIs y optimizar el rendimiento
![image](https://github.com/user-attachments/assets/a9836240-87b8-4667-8648-24d87a4638f9)
