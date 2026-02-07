# Spring Boot JWT Authentication & REST API

A secure RESTful API demo built with Spring Boot, featuring user authentication and management. This project demonstrates JWT-based authentication, role-based access control (RBAC), and CRUD operations for users, integrated with PostgreSQL and Spring Security.

## Project Name
The name of this project is **Spring-Boot-Jwt-Auth** (based on the artifact ID `RESTFull-API-Demo` in `pom.xml`, with corrected spelling for standard terminology).

## Features

- **User Authentication**: Register and login endpoints with JWT token generation.
- **Role-Based Access Control**: Supports roles like USER, ADMIN, and MODERATOR. Admins can manage users (activate/deactivate, delete, etc.).
- **User Management**: CRUD operations for users, including partial updates and status management.
- **Validation**: Input validation using Jakarta Bean Validation for requests.
- **Exception Handling**: Centralized global exception handler for authentication errors, validation failures, and resource not found scenarios.
- **CORS Configuration**: Allows requests from specified frontend origins (e.g., localhost:3000, localhost:4200).
- **Security**: JWT filtering, password encryption with BCrypt, and stateless session management.
- **API Responses**: Standardized responses using `ApiResponse` DTO for success/error handling.
- **Database Integration**: Uses PostgreSQL with Spring Data JPA for persistence.

## Tech Stack

- **Java 21**
- **Spring Boot 3.x** (or 4.x preview)
- **Spring Security** for authentication and authorization
- **JWT (JSON Web Tokens)** using `io.jsonwebtoken:jjwt` for secure token management
- **Spring Data JPA** with Hibernate for ORM
- **PostgreSQL** as the database
- **Lombok** for boilerplate reduction (getters/setters, builders, etc.)
- **Maven** for build and dependency management
- **SLF4J** for logging (integrated in exception handling)

## Prerequisites

- Java 21 JDK
- Maven 3.x
- PostgreSQL database (create a database named `your_db_name`)
- Optional: Postman or similar tool for API testing

## Setup and Installation

1. **Clone the Repository**
   ```
   git clone <repository-url>
   cd RESTFull-API-Demo
   ```

2. **Configure Database**  
   Update `src/main/resources/application.properties` (or create one if missing) with your PostgreSQL credentials:
   ```
   spring.datasource.url=jdbc:postgresql://localhost:5432/your_db_name
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.datasource.driver-class-name=org.postgresql.Driver
   
   spring.jpa.hibernate.ddl-auto=update  # Or 'create' for initial setup
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   
   # JWT Expiration (in ms, e.g., 1 hour)
   jwt.expiration=3600000
   ```

3. **Build the Project**
   ```
   mvn clean install
   ```

4. **Run the Application**
   ```
   mvn spring-boot:run
   ```  
   The API will be available at `http://localhost:8080`.

5. **Database Migration**  
   On startup, Spring Boot will automatically create/update tables based on entities (e.g., `users` table).

### Environment Variables

Create environment variables or a `.env` file:

JWT_SECRET=your-secret-key
DB_USERNAME=postgres
DB_PASSWORD=postgres

## API Endpoints

Base URL: `/api/v1`

### Authentication (`/auth`)

| Method | Endpoint       | Description              | Request Body                  | Authentication |
|--------|----------------|--------------------------|-------------------------------|----------------|
| POST   | `/register`    | Register a new user      | `RegisterRequest` JSON        | None           |
| POST   | `/login`       | Login and get JWT token  | `AuthenticationRequest` JSON  | None           |

### Users (`/users`)

| Method | Endpoint              | Description                      | Request Body/Params       | Authentication          |
|--------|-----------------------|----------------------------------|---------------------------|-------------------------|
| GET    | `/me`                 | Get current authenticated user   | None                      | JWT (Any Role)          |
| GET    | `/`                   | Get all users                    | None                      | JWT (ADMIN)             |
| GET    | `/{id}`               | Get user by ID                   | Path: `id`                | JWT (Owner or ADMIN)    |
| POST   | `/`                   | Create a new user                | `UserRequest` JSON        | JWT (ADMIN)             |
| PUT    | `/{id}`               | Full update of user              | `UserRequest` JSON, Path: `id` | JWT (Owner or ADMIN) |
| PATCH  | `/{id}`               | Partial update of user           | `UserRequest` JSON, Path: `id` | JWT (Owner or ADMIN) |
| DELETE | `/{id}`               | Delete user                      | Path: `id`                | JWT (ADMIN)             |
| PATCH  | `/{id}/activate`      | Activate user                    | Path: `id`                | JWT (ADMIN)             |
| PATCH  | `/{id}/deactivate`    | Deactivate user                  | Path: `id`                | JWT (ADMIN)             |
| GET    | `/stats/active-count` | Count active users               | None                      | JWT (ADMIN)             |

### Example Requests

- **Register User** (POST `/api/v1/auth/register`):
  ```json
  {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "securePass123",
    "age": 25,
    "phoneNumber": "+1234567890",
    "role": "USER"
  }
  ```

- **Login** (POST `/api/v1/auth/login`):
  ```json
  {
    "email": "john@example.com",
    "password": "securePass123"
  }
  ```  
  Response includes JWT token for subsequent requests (add to Authorization header: `Bearer <token>`).

## Security Notes

- **JWT Secret**: Loaded from environment variables for security, Dynamic secrets are used only for development.
- **Password Hashing**: Uses BCrypt with strength 12.
- **CORS**: Restricted to specific origins; adjust in `SecurityConfig` for production.
- **Roles**: Prefix "ROLE_" is added automatically in `UserPrincipal`.
- **Exceptions**: Handled with custom `ErrorResponse` for consistent error messages.

## Project Structure

```
src/main/java/com.abdelrahman/
├── config/
│   ├── JwtFilter.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthenticationController.java
│   └── UserController.java
├── dto/
│   ├── ApiResponse.java
│   ├── AuthenticationRequest.java
│   ├── AuthenticationResponse.java
│   ├── RegisterRequest.java
│   ├── UserRequest.java
│   └── UserResponse.java
├── exception/
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── model/
│   ├── User.java
│   └── UserPrincipal.java
├── repository/
│   └── UserRepo.java (assumed based on usage)
├── service/
│   ├── AuthenticationService.java
│   ├── JwtService.java
│   ├── MyUserDetailsService.java
│   └── UserService.java
```

## Testing

- Use Postman collections for endpoint testing.
- Unit/Integration Tests: Dependencies for testing (e.g., `spring-boot-starter-test`, `spring-security-test`) are included in `pom.xml`.

## Future Improvements

- Add refresh token endpoint.
- Implement email verification on registration.
- Add pagination and sorting for user lists.
- Integrate Swagger for API documentation.
- Add rate limiting and additional security headers.
- Deploy to cloud (e.g., Heroku, AWS) with environment-specific configurations.

## License

This project is for educational and demonstration purposes. No specific license is applied—feel free to use and modify.

---
**Developed by Abdelrahman**  
February 07, 2026