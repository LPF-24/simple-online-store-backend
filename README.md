# 🛒 Simple Online Store Backend

This is a backend part of a Java-based online store project with role-based authorization, session handling via JWT tokens, Redis-powered refresh tokens, and RESTful APIs documented via OpenAPI.

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

## 🔐 Access

### Sample Users
| Role  | Username | Password   |
|-------|----------|------------|
| Admin | `test2`  | `Test234!` |
| User  | `jack`   | `Test234!`  |

## 🔄 JWT Authentication

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

## 👤 Author

**LPF-24** — aspiring Java Developer  
GitHub: [LPF-24](https://github.com/LPF-24)

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.