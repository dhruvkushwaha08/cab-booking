# CabGo -- Cab Booking Application

## Project Links

-   **GitHub repository:**
    https://github.com/dhruvkushwaha08/cab-booking
-   **Live frontend:** https://cab-booking-i6jk.onrender.com
-   **Backend API:** https://cabgo-backend-jcoz.onrender.com

**CabGo** is a full-stack cab-booking web application that lets
customers choose pickup and destination locations on a map and request
rides. It includes separate customer and driver workflows, ride-status
management, and ride history.

> The frontend and backend are deployed on Render. Free-tier services
> may take some time to wake up after a period of inactivity.

## 1. Introduction

CabGo is designed to demonstrate the core workflow of an online
ride-hailing platform. Customers can register and sign in, select pickup
and destination points, choose a ride category, and request a ride.
Drivers can register, manage their availability, and handle ride
requests. The application uses a backend API and a PostgreSQL database
to manage user and ride data.

## 2. Research

The project is based on the common workflow used by ride-hailing
services:

-   **Customer flow:** register or sign in, choose locations, select a
    ride type, request a ride, and review ride history.
-   **Driver flow:** register as a driver, manage online/offline
    availability, accept a ride, and update its status as the trip
    progresses.
-   **System flow:** the frontend sends HTTP requests to the Spring Boot
    backend. The backend applies authentication and business rules and
    stores application data in PostgreSQL.
-   **Deployment:** the frontend and backend are hosted separately on
    Render, with environment variables used for deployment
    configuration.

The project focuses on implementing and demonstrating these essential
workflows rather than reproducing every feature of a commercial
ride-hailing platform.

## 3. Technology Stack and Why It Was Chosen

  -----------------------------------------------------------------------
  Technology              Purpose                 Reason for choosing it
  ----------------------- ----------------------- -----------------------
  React.js                Frontend user interface Supports reusable
                                                  components and
                                                  interactive pages.

  Tailwind CSS            Frontend styling        Makes it easier to
                                                  build a consistent,
                                                  responsive interface.

  Java                    Backend programming     Provides a strong
                          language                object-oriented
                                                  foundation for
                                                  application logic.

  Spring Boot             Backend REST API        Simplifies API
                                                  development and
                                                  integration with
                                                  security and
                                                  persistence.

  Spring Security and JWT Authentication and      Supports authenticated
                          endpoint protection     requests and role-based
                                                  access patterns.

  PostgreSQL              Relational database     Stores structured user
                                                  and ride information
                                                  reliably.

  Spring Data JPA /       Database persistence    Maps Java entities to
  Hibernate               and ORM                 database tables and
                                                  reduces routine SQL
                                                  code.

  Leaflet and             Map display and         Provides an interactive
  OpenStreetMap           location selection      map and map data for
                                                  choosing ride
                                                  locations.

  Maven                   Java build tool         Manages backend
                                                  dependencies and builds
                                                  the application.

  Git and GitHub          Version control and     Tracks code changes and
                          source hosting          provides a central
                                                  repository.

  Render                  Deployment platform     Hosts the live frontend
                                                  and backend services.
  -----------------------------------------------------------------------

## 4. Libraries and Tools

The main frameworks, libraries, and tools used in the project include:

-   **React.js** -- building the frontend interface.
-   **Vite** -- frontend development server and production build
    tooling.
-   **Tailwind CSS** -- styling and responsive layout.
-   **Leaflet** -- interactive maps and map markers.
-   **Spring Boot Web** -- REST endpoints and HTTP request handling.
-   **Spring Security** -- securing API endpoints.
-   **JWT support** -- token-based authentication.
-   **Spring Data JPA and Hibernate** -- persistence and ORM.
-   **PostgreSQL JDBC driver** -- connecting the backend to PostgreSQL.
-   **Maven** -- dependency management and backend builds.
-   **VS Code** -- development environment.
-   **Thunder Client** -- testing API requests during development.
-   **Docker** -- local containerized development and testing.
-   **Render** -- deployment of the live application.

*Note: confirm these names against the current `pom.xml` and
`frontend/package.json` before submitting the report, and remove any
dependency that is not present in the project.*

## 5. Project Functionality

### Customer features

-   Customer registration and login.
-   Interactive map with pickup and destination markers.
-   Ride-category selection.
-   Ride request and ride-status display.
-   Ride history.
-   Downloadable text receipts for completed rides.

### Driver features

-   Driver registration with required driver and vehicle details.
-   Driver login.
-   Online/offline availability.
-   Accepting available ride requests.
-   Updating a ride's status through the trip workflow.

### Security and data management

-   Shared user system with customer and driver roles.
-   JWT-based authentication for protected API requests.
-   PostgreSQL persistence for application data.
-   CORS configuration for the deployed frontend and local development.

### Screenshots

Add screenshots of the running application to a `screenshots` folder in
the repository, then update the filenames below to match your images.

#### Customer home page and map

![CabGo customer home page](screenshots/customer-home.png)

#### Customer sign-in

![CabGo sign-in page](screenshots/sign-in.png)

#### Ride request and ride details

![CabGo ride request](screenshots/ride-request.png)

#### Driver dashboard

![CabGo driver dashboard](screenshots/driver-dashboard.png)

#### Ride history and receipt

![CabGo ride history](screenshots/ride-history.png)

## 6. Use Cases

1.  **Customer registration:** a new customer creates an account and
    signs in.
2.  **Location selection:** the customer chooses pickup and destination
    points on the map.
3.  **Ride booking:** the customer chooses a ride category and submits a
    ride request.
4.  **Driver availability:** a driver signs in and switches their
    availability online or offline.
5.  **Ride acceptance:** a driver accepts an available request.
6.  **Trip progress:** the ride moves through its supported statuses
    until completion.
7.  **Ride history:** the customer reviews past rides and can download a
    text receipt for a completed ride.

## 7. Conclusion

CabGo demonstrates the development and deployment of a full-stack
ride-hailing application using React.js, Java Spring Boot, and
PostgreSQL. It brings together a map-based interface, user
authentication, customer and driver workflows, ride management, and ride
history. Deploying the frontend and backend on Render also provides
practical experience with environment configuration and hosting a web
application.

## Running the Project Locally

Refer to the repository's configuration files for the current setup. In
general, local development requires Java and Maven for the backend,
Node.js and npm for the frontend, and PostgreSQL for the database.
Configure secrets and database credentials through local environment
configuration; **do not commit passwords, JWT secrets, or `.env`
files**.

## Future Improvements

-   Improve address search and display human-readable pickup and
    destination names.
-   Further test and refine real-time ride updates.
-   Add automated tests and database migrations.
-   Improve error handling, validation, and accessibility.
-   Add more detailed fare and ride summaries as the application
    evolves.
