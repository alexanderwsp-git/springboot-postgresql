# API springboot + postgresSQL + maven + docker.

_El objetivo es crear un api en springboot con maven, que se conecte a una base de datos postgresSQL, publique un endpoint que pueda consumir una entidad, contenerizar el api, crear un stack (docker-compose) para el api y la db._

> Requisitos:
Tener instalado y configurado java11, maven 3.6+, docker 19+, docker-compose 1.29+, curl.
OS Ubuntu 18+

- Setup contenedor postgresSQL.
- Crear api springboot con maven.
- Crear los endpoints.
- Crear dockerfile para el api usando usando dockerfile-maven-plugin de Spotify.
- Crear docker-compose.

#### Setup contenedor postgresSQL
En este paso levantaremos un contenedor postgreSQL, fijarse en las variables de entorno `-e`, los valores de estas serán usados más adelante al levantar nuestra API.
```docker
docker run --rm -it --name db-postresql -e POSTGRES_PASSWORD=docker -e POSTGRES_USER=docker -e POSTGRES_DB=docker postgres:alpine
```
#### Clonamos el proyecto
El proyecto es un API simple con dos paths las cuales estan definidas en el controlador, fijarse en el archivo de `application.yaml` ahi se encuentran las configuraciones para el API.
```git
git@github.com:alexanderwsp-git/springboot-postgresql.git
```
_Revisamos el archivo application.yaml, y exportamos las variables de entorno que estan configuradas en el mismo._
```cmd
export POSTGRES_SERVICES=<localhost or POSTGRESQL-IP> && \
export POSTGRES_PORT=5432 && \
export POSTGRES_DB=docker && \
export POSTGRES_USER=docker && \
export POSTGRES_PASSWORD=docker && \
export LOG_LEVEL=debug && \
export CONTEXT_PATH=/v1/api && \
export API_PORT=1080 && \
export DDL_AUTO=create && \
export LOG_FILE=/var/tmp/sbpgdocker.log
```
_Importante la variable `DDL_AUTO`, su valor se encuentra en `create` cuando se levanta el api por primera vez, luego debe ser cambiada a `update`_
##### _Levantamos el API_
```cmd
./mvnw spring-boot:run
```
_Comprobamos que nuestro servicio esta UP_
```cmd
curl -i localhost:1080/v1/api/actuator/health/
```
_Para crear un nuevo registro usar_
```cmd
curl -X POST -H "Content-Type: application/json" -i localhost:1080/v1/api/employee/ -d '{"name":"Other", "age":"31"}'
```
_Para obtener todos los registros_
```cmd
curl -X GET -i localhost:1080/v1/api/employee/
```
#### El siguiente punto crearemos la imagen usando dockerfile-maven-plugin de Spotify
Para lo cual debemos tener creado un archivo Dockerfile de la siguiente manera
```Dockerfile
FROM adoptopenjdk/openjdk11:alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
VOLUME /tmp
ARG JAR_FILE
ADD ${JAR_FILE} /app/app.jar
EXPOSE 1080 #EL PUERTO DEBE SER EL MISMO QUE ESTE CONFIGURADO EN LA VARIABLE API_PORT
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
```
Lo siguiente es tener en nuestro pom la siguiente configuración.
```pom
<plugin>
  <groupId>com.spotify</groupId>
  <artifactId>dockerfile-maven-plugin</artifactId>
  <version>1.4.13</version>
  <executions>
    <execution>
      <id>default</id>
      <goals>
        <goal>build</goal>
        <goal>push</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <repository>${project.artifactId}</repository>
    <tag>${project.version}</tag>
    <buildArgs>
      <JAR_FILE>target/${project.build.finalName}.jar</JAR_FILE>
    </buildArgs>
  </configuration>
</plugin>
```
Creamos la imagen con el comando
```cmd
mvn clean package
```
Verificamos que la imagen este creada.
```cmd
docker images
```
Para probar que nuestro contendor funciona lo corremos de la siguiente manera, recordar que no debemos tener en este punto corriendo nuestra API. Con "CTRL + C" paramos nuestra API.
```docker
docker run -P -it --rm --name springbootpostgresdocker \
 -e POSTGRES_SERVICES=172.17.0.2 \
 -e POSTGRES_PORT=5432 \
 -e POSTGRES_DB=docker \
 -e POSTGRES_USER=docker \
 -e POSTGRES_PASSWORD=docker \
 -e LOG_LEVEL=debug \
 -e CONTEXT_PATH=/v1/api \
 -e API_PORT=1080 \
 -e DDL_AUTO=update \
 -e LOG_FILE=/var/tmp/sbpgdocker.log \
 springbootpostgresdocker:0.0.1-SNAPSHOT
 ```
 En este punto estamos levantando el contenedor con el comando docker run, además de pasarle como variable de entorno la conexión hacia el postgres y la configuración del API.
_Probamos nuestro API_
```cmd
curl -X GET -i <IP-CONTAINER-API>:1080/v1/api/employee/
```
_Paramos nuestra API con "CTRL + C" y eliminanos el contenedor, buscandolo con "docker ps" y luego con el ID hacemos un "docker rm -f ID_CONTAINER"
#### Por último crearemos el archivo docker-compose
>> docker-compose version 1.29.1
```
version: '3.8'
networks:
  backend:
    name: app
    driver: bridge
volumes:
  postgres_data:
    driver: local
services:
  app:
    image: springbootpostgresdocker:0.0.1-SNAPSHOT
    container_name: web-app
    ports:
      - "1080:1080"
    environment:
      POSTGRES_SERVICES: db
      POSTGRES_PORT: 5432
      POSTGRES_USER: docker
      POSTGRES_PASSWORD: docker
      POSTGRES_DB: docker
      PGDATA: /var/lib/postgresql/data/pgdata
      LOG_LEVEL: debug
      CONTEXT_PATH: /v1/api
      API_PORT: 1080
      DDL_AUTO: create
      LOG_FILE: /var/tmp/sbpgdocker.log
    networks:
      - backend
    depends_on:
      - db
    healthcheck:
      test: curl -f http://localhost:1080/demo/profile || exit 1
      interval: 1m
      timeout: 10s
      retries: 2
  db:
    image: postgres:12
    container_name: postgres-db
    restart: always
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - backend
    environment:
      POSTGRES_USER: docker
      POSTGRES_PASSWORD: docker
      POSTGRES_DB: docker
      PGDATA: /var/lib/postgresql/data/pgdata
    healthcheck:
      test: pg_isready -U postgres
      interval: 1m
      timeout: 10s
      retries: 2
```
Y ahora podemos probar nuestra api con los curl escritos al inicio.
