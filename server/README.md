# CroudTrip Server

This repo contains the server used by the CroudTrip android application.

## Running locally

To run the server locally copy the `configuration.yml.template` file to `configuration.yml` 
and remove the following section

```
server:
  type: bridge
```

After that is a simple matter of starting the Java `main` method via gradle: `./gradlew run`.
The server will be hosted at `http://localhost:8080`.


## Creating a `war` file

Copy the `configuration.yml.template` file to `src/main/resources/configuration.yml` and
create a war file by running gradle via `./gradlew war`. The created war file can be found
under `build/libs/`.
