# Project Overview

This project is divided into two main sections: the backend and the frontend. The backend, built with Java and Spring Boot, should be developed first as the frontend relies on its RESTful API endpoints.

## [Backend](back/README.md)

The backend provides the following core features:

- **Customer Creation:** An endpoint to create new customers (requires authorization).
- **Customer Information Retrieval:** An endpoint to retrieve customer details.
- **Loan Creation:** An endpoint to create new loan records.
- **Loan Information Retrieval:** An endpoint to retrieve loan details.
- **OpenAPI Specification:** The OpenAPI specification for the backend is located in the `back` folder.

## [Frontend](front/README.md)

The frontend consumes the RESTful API endpoints provided by the backend. Please refer to the `front/README.md` for more details on the frontend setup and usage.

> Development of the test must be done in a branch named `feature/[candidate's first name]_[candidate's last name]` (if there are conflicts, add the second last name), for example: `feature/juan_perez`. Instead of making a pull request to the master branch, the candidate must provide access to their repository where the take-home test is hosted using a token.

## Docker Setup

The entire application can be run using Docker Compose for easy deployment and testing.

### Prerequisites

- Docker (version 20.10 or higher)
- Docker Compose (version 2.0 or higher)

### Running the Application

1. **Build and start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Access the services:**
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080/v1
   - PostgreSQL: localhost:5432

3. **Stop the services:**
   ```bash
   docker-compose down
   ```

   To also remove the database volume:
   ```bash
   docker-compose down -v
   ```

### Docker Configuration Details

#### Services

1. **PostgreSQL Database**
   - User: `challenge_user`
   - Password: `challenge_pass`
   - Database: `challenge_db`
   - Port: 5432

2. **Backend (Spring Boot)**
   - Port: 8080
   - Context path: `/v1`
   - Includes health checks for reliability

3. **Frontend (Angular + Nginx)**
   - Port: 4200
   - Proxies API calls to the backend
   - Optimized production build

### Development Tips

- To run only specific services:
  ```bash
  # Backend and database only
  docker-compose up postgres backend
  
  # Frontend only (requires backend running)
  docker-compose up frontend
  ```

- To rebuild after code changes:
  ```bash
  docker-compose up --build
  ```

- To view logs:
  ```bash
  docker-compose logs -f [service_name]
  ```
