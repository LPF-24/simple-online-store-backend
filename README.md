# 🛒 Online Store Backend

This is the backend part of a Java-based online store with role-based authorization, JWT-based session handling, and Redis-backed refresh token rotation.

The application follows a clean layered architecture with clear separation of concerns: controllers handle HTTP requests, services encapsulate business logic, and repositories manage data access.

RESTful APIs are documented using OpenAPI and can be explored interactively via Swagger UI.

## 🟢 Project Status
✅ **Completed and fully functional.**  
The application can be run via Docker Compose and includes working authentication, order management, and admin features.

## 🚀 Technologies Used

- Java 17
- Spring Boot 3
- Spring Security (JWT-based auth)
- Spring Data JPA + Hibernate
- PostgreSQL
- Redis
- Docker & Docker Compose
- OpenAPI / Swagger
- JUnit 5
- Mockito
- Spring Boot Test, MockMvc

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
docker-compose up -d --build
```

- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 👥 Sample Accounts

| Role  | Username    | Password   |
|-------|-------------|------------|
| Admin | `admin`     | `ChangeMe_123!` |
| User  | `user` | `user123!` |

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
"productIds": [1, 2],
"addressId": 1
}
```

## 📚 API Documentation
Full interactive documentation is available at:

👉 **[Swagger UI](http://localhost:8080/swagger-ui/index.html)**

You can explore all endpoints, request/response schemas, and example payloads there.

## 👤 Author

**LPF-24** — aspiring Java backend developer building real-world projects with Spring, REST APIs, and Docker.

GitHub: [@LPF-24](https://github.com/LPF-24)

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.