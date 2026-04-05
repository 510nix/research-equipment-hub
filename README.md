# 🔬 Research Equipment Hub

> A web-based platform for managing and sharing research laboratory equipment across university departments at **KUET (Khulna University of Engineering and Technology)**.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1.1-green?logo=thymeleaf)](https://www.thymeleaf.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Live Demo](https://img.shields.io/badge/Live_Demo-Render-informational?style=flat&logo=render)](https://uni-research-equipment-hub.onrender.com/)

---

## 📖 Overview

Research Equipment Hub solves a real problem faced by research universities: **expensive laboratory equipment sits idle in one lab while another lab needs it**. This platform provides a centralized, self-service system where:

- **Faculty (Providers)** list their research equipment and manage lending requests
- **Researchers & Students (Borrowers)** browse, search, and request equipment
- **Admins** manage users, categories, and monitor system health

Built with a classic Spring Boot MVC stack and PostgreSQL, the system implements sophisticated patterns including **double-booking prevention**, **dependency lock enforcement**, and a **request lifecycle state machine**.

---

## 🚀 Live Demo

The application is deployed and publicly accessible at:
**[https://uni-research-equipment-hub.onrender.com/](https://uni-research-equipment-hub.onrender.com/)**

> **Note:** Since this is hosted on a free instance, the server may take 30-60 seconds to "wake up" during the first visit.

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 Role-Based Access | Three distinct roles: Admin, Provider, Borrower — each with scoped permissions |
| 🔎 Equipment Browse & Search | Browse all available equipment; keyword search by title |
| 📅 Borrow Request Lifecycle | PENDING → APPROVED / REJECTED → COMPLETED with atomic state transitions |
| 🚫 Double-Booking Prevention | Approving a request auto-rejects all competing requests for the same item |
| 🔒 Dependency Lock Pattern | Prevents deactivating users or deleting categories with active transactions |
| 👁️ Soft-hide on Deactivation | Deactivated provider equipment disappears from browse without deletion |
| 🛡️ Spring Security | BCrypt password hashing, CSRF protection, session management |
| 📊 Admin Dashboard | System-wide statistics, user management, category CRUD |
| 🐳 Docker Support | `docker-compose.yml` for one-command full-stack startup |

---

## 🏗️ Architecture

The application follows a **3-tier layered MVC architecture**:

```
┌────────────────────────────────────────────────────────┐
│             Browser (Admin / Provider / Borrower)       │
└───────────────────────┬────────────────────────────────┘
                        │ HTTP
┌───────────────────────▼────────────────────────────────┐
│  Tier 1 · Presentation Layer                            │
│  Thymeleaf Views  │  Spring MVC Controllers (×6)        │
└───────────────────────┬────────────────────────────────┘
                        │
┌───────────────────────▼────────────────────────────────┐
│  Tier 2 · Business Logic Layer                          │
│  Spring Security  │  UserService  │  ItemService        │
│                   │  RequestService                     │
└───────────────────────┬────────────────────────────────┘
                        │
┌───────────────────────▼────────────────────────────────┐
│  Tier 3 · Data Access Layer                             │
│  JPA Repositories (×5)  │  Hibernate ORM  │  JDBC      │
└───────────────────────┬────────────────────────────────┘
                        │
              ┌─────────▼──────────┐
              │   PostgreSQL 15    │
              │ users · roles ·    │
              │ items · requests · │
              │ categories         │
              └────────────────────┘
```

---

## 🗂️ Project Structure

```
research-equipment-hub/
├── src/main/java/com/kuet/hub/
│   ├── ResearchEquipmentHubApplication.java   # Entry point
│   ├── config/
│   │   ├── SecurityConfig.java                # Spring Security config
│   │   └── DataInitializer.java               # Seeds roles & categories
│   ├── controller/
│   │   ├── AuthController.java                # Login, register
│   │   ├── DashboardController.java           # Role-based routing
│   │   ├── BrowseController.java              # Equipment browsing
│   │   ├── ItemController.java                # Provider CRUD
│   │   ├── RequestController.java             # Borrow lifecycle
│   │   └── AdminController.java               # Admin panel
│   ├── service/
│   │   ├── UserService.java                   # Registration, dependency locks
│   │   ├── ItemService.java                   # Equipment CRUD, ownership
│   │   ├── RequestService.java                # Approval, double-booking prevention
│   │   └── CustomUserDetailsService.java      # Spring Security integration
│   ├── repository/                            # Spring Data JPA interfaces (×5)
│   ├── entity/                                # JPA entities: User, Role, Item, Category, Request
│   ├── dto/                                   # Input DTOs: RegistrationDto, ItemDto, RequestDto
│   └── exception/
│       └── GlobalExceptionHandler.java        # Centralized error handling
├── src/main/resources/
│   ├── application.yaml                       # App configuration
│   ├── templates/                             # Thymeleaf HTML templates
│   └── static/                               # CSS, JS, images
├── src/test/                                  # JUnit 5 + H2 tests
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 LTS |
| Framework | Spring Boot | 3.4.3 |
| Web Layer | Spring MVC | 6.x |
| Templates | Thymeleaf | 3.1.1 |
| Security | Spring Security + BCrypt | 6.x |
| ORM | Hibernate / JPA | 6.x |
| Database | PostgreSQL | 15 |
| Build Tool | Maven | 3.x |
| Validation | Jakarta Bean Validation | 3.x |
| Utilities | Lombok | Latest |
| Testing | JUnit 5 + H2 + Spring Security Test | Latest |
| Containers | Docker + Docker Compose | Latest |

---

## ⚡ Getting Started

### Prerequisites

- Java 21 JDK
- Maven 3.x
- PostgreSQL 15 (or Docker)

### Option A — Local Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/kuet-cse/research-equipment-hub.git
   cd research-equipment-hub
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE research_hub;
   CREATE USER hub_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE research_hub TO hub_user;
   ```

3. **Configure credentials** in `src/main/resources/application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/research_hub
       username: hub_user
       password: your_password
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

5. **Open in browser**
   ```
   http://localhost:8080
   ```

   The `DataInitializer` will automatically create the three role records (`ROLE_ADMIN`, `ROLE_PROVIDER`, `ROLE_BORROWER`) and seed default equipment categories on first startup.

---

### Option B — Docker Compose (Recommended)

```bash
git clone https://github.com/kuet-cse/research-equipment-hub.git
cd research-equipment-hub
docker compose up --build
```

This starts both the Spring Boot application and a PostgreSQL 15 container with a persistent named volume. Access at `http://localhost:8080`.

---

## 🔐 Default Roles

Register an account and select your role during sign-up:

| Role | Registration | Capabilities |
|---|---|---|
| `ROLE_BORROWER` | Default selection | Browse, search, request equipment, track requests |
| `ROLE_PROVIDER` | Select "Provider" | All above + list and manage own equipment, approve/reject requests |
| `ROLE_ADMIN` | Manually assigned | Full system access — user management, categories, dashboards |

---

## 🔄 Request Lifecycle

```
Borrower submits request
         │
         ▼
      [PENDING]
      /        \
 Provider     Provider
 Approves     Rejects
     │             │
     ▼             ▼
 [APPROVED]    [REJECTED]
 (item → BORROWED)
 (other requests auto-rejected)
     │
 Borrower returns
     │
     ▼
 [COMPLETED]
 (item → AVAILABLE)
```

---

## 🗄️ Database Schema

Five core tables with full referential integrity:

```
users ──────┬──── user_roles ──── roles
            │
            ├──── items ──────── categories
            │         │
            └──── requests ──────┘
```

Key constraints:
- `items.owner_id → users.id` (ON DELETE CASCADE)
- `items.category_id → categories.id` (ON DELETE RESTRICT)
- `requests.borrower_id → users.id` (ON DELETE CASCADE)
- `requests.item_id → items.id` (ON DELETE CASCADE)

---

## 🧪 Running Tests

```bash
# Run all tests (uses in-memory H2 database)
mvn test

# Run with coverage report
mvn verify
```

---

## 📋 API Endpoints Summary

| Path | Method | Role | Description |
|---|---|---|---|
| `/auth/login` | GET/POST | Public | Login page and processing |
| `/auth/register` | GET/POST | Public | Registration form and processing |
| `/browse` | GET | Borrower | Browse available equipment |
| `/browse/search` | GET | Borrower | Search by keyword |
| `/browse/{id}` | GET | Borrower | Equipment detail view |
| `/requests/new/{itemId}` | GET | Borrower | Borrow request form |
| `/requests/submit` | POST | Borrower | Submit borrow request |
| `/my-requests` | GET | Borrower | View own requests |
| `/requests/{id}/complete` | POST | Borrower | Mark equipment returned |
| `/provider/dashboard` | GET | Provider | Provider overview |
| `/provider/items/new` | GET/POST | Provider | Create equipment |
| `/provider/items/{id}/edit` | GET/POST | Provider | Edit equipment |
| `/provider/items/{id}/delete` | POST | Provider | Delete equipment |
| `/provider/requests` | GET | Provider | View incoming requests |
| `/requests/{id}/approve` | POST | Provider | Approve request |
| `/requests/{id}/reject` | POST | Provider | Reject request |
| `/admin/dashboard` | GET | Admin | System dashboard |
| `/admin/users/{id}/toggle-status` | POST | Admin | Enable/disable user |
| `/admin/categories` | GET | Admin | Category list |
| `/admin/categories/new` | POST | Admin | Create category |
| `/admin/categories/{id}/edit` | POST | Admin | Update category |
| `/admin/categories/{id}/delete` | POST | Admin | Delete category |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

Please follow the existing code style and include unit tests for new service-layer logic.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

Developed by the **H.M. AZROF & ANIKA NAWER**.

Department of Computer Science and Engineering  
Khulna University of Engineering and Technology (KUET)  
Khulna, Bangladesh

---

<p align="center">Built with ☕ Java, 🍃 Spring Boot, and 🐘 PostgreSQL</p>