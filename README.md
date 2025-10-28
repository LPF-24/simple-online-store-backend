# 🛒 Simple Online Store Backend

This is the backend part of a Java-based online store with role-based authorization, JWT-based session handling, and Redis-backed refresh token rotation.

The application follows a clean layered architecture with clear separation of concerns: controllers handle HTTP requests, services encapsulate business logic, and repositories manage data access.

RESTful APIs are documented using OpenAPI and can be explored interactively via Swagger UI.

## 🚀 Technologies Used

- Java 17
- Spring Boot 3
- Spring Security (JWT-based auth)
- Spring Data JPA + Hibernate
- PostgreSQL
- Redis
- Docker & Docker Compose
- OpenAPI / Swagger UI

## 📦 Features

- 👥 Registration & login (JWT)
- 🛒 Order creation, update, cancelation
- 📦 Product and Pickup point management (admin)
- 🔁 Refresh token rotation via Redis
- 🧾 OpenAPI documentation via Swagger UI
- 👤 Role-based access (User/Admin)
- 🧩 Input validation with custom annotations

## 🐳 Run with Docker

```bash
docker-compose up --build
```

- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## 👥 Sample Accounts

| Role  | Username    | Password   |
|-------|-------------|------------|
| Admin | `test2`     | `Test234!` |
| User  | `jorge_doe` | `Zegh576!` |

## 🔐 JWT Authentication

1. `POST /auth/login` returns Access + Refresh tokens
2. Access token is passed via `Authorization: Bearer ...`
3. Refresh token is stored in HttpOnly cookie
4. `/auth/refresh` returns new access token
5. `/auth/logout` clears refresh token and invalidates session

## 📬 Sample Requests

### 🔐 Login

```http
POST /auth/login
Content-Type: application/json

{
  "username": "jack",
  "password": "jack123"
}
```

### 🛒 Create Order

```http
POST /orders/create-order
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "productsIds": [1, 2],
  "pickupLocation": {
    "city": "New York",
    "street": "Main St",
    "houseNumber": "10A"
  }
}
```

## 📚 Documentation

Interactive API available at:
```
http://localhost:8080/swagger-ui.html
```

## 🚧 Project Status: In Development

This project is currently under active development. Some features may be incomplete, temporarily disabled, or not fully functional yet.

The repository is intended to demonstrate the architecture and implementation approach. Functionality such as authentication, database integration, and user management is still being finalized.

If you encounter issues during startup or usage, this is expected at this stage of development.

## 👤 Author

**LPF-24** — aspiring Java backend developer building real-world projects with Spring, REST APIs, and Docker.

GitHub: [@LPF-24](https://github.com/LPF-24)

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.