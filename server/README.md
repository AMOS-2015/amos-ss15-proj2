# CroudTrip Server

This repo contains the server used by the CroudTrip android application.


## PostgreSQL setup

Before running the server make sure to update the username and password of the db connector
which is defined in `configuration.yml` under `database`.

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


## Postman collection

For quick testing [here](https://www.getpostman.com/collections/3466b0d29e60794cce85) is
a postman collection which contains most request that the server is capable of handling.

To run the collection an environment needs to be configured within Postman which sets common
varaibles for interacting with the server (e.g. base url, credentials, etc.).
For localhost it could look like this:

```json
{
	"id": "5ace8ff8-c58b-0b55-52df-98f06deced7b",
	"name": "amos-localhost",
	"values": [
		{
			"key": "base_url",
			"value": "http://localhost:8080",
			"type": "text",
			"name": "base_url",
			"enabled": true
		}
	],
	"timestamp": 1429467864523,
	"synced": false,
	"syncedFilename": ""
}
```
