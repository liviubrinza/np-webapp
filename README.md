# Notary Public Web Application

A Spring Boot web application for a notary public office: a multilingual public-facing
site (Romanian/English/Hungarian) and a protected admin panel for managing appointments
and documents.

## Tech Stack

- Java 25, Spring Boot 4.1, Maven
- Thymeleaf, Bootstrap 5
- Spring Data JPA, Flyway migrations
- H2 (local dev) / PostgreSQL (docker/prod)
- Spring Security (admin panel)

## Running Locally

```
mvn spring-boot:run
```

This starts the app on the `h2` profile (default), backed by a local file-based H2
database at `data/np-webapp`. The app is available at `http://localhost:8080`.

## Accessing the H2 Console

With the app running locally (`h2` profile), the H2 web console is available at:

```
http://localhost:8080/h2-console
```

Connect using:

| Field       | Value                                          |
|-------------|-------------------------------------------------|
| JDBC URL    | `jdbc:h2:file:./data/np-webapp;AUTO_SERVER=TRUE` |
| User Name   | `sa`                                             |
| Password    | *(leave blank)*                                  |

`AUTO_SERVER=TRUE` lets you connect with the console (or any other client, e.g. a
JetBrains/VS Code database tool) even while the app itself is running — no need to stop
the app first.

The H2 console is only enabled on the `h2` profile; it is not available when running
against PostgreSQL (`docker` profile).

## Outbound Network Access

The admin Statistics > Traffic page resolves each visitor IP's approximate (city-level)
location via a background call to `http://ip-api.com` (`GeoLocationService`). This is the
app's only outbound HTTP dependency. In a production environment, the firewall/network
policy must allow outgoing traffic from the app to `ip-api.com` on port 80 (plain HTTP -
the free tier doesn't offer HTTPS), or these lookups will simply time out and locations
will stay unresolved (shown as `—` in the table) - nothing else is affected.
