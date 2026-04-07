# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║  ENTERPRISE SERVICE HUB — Professional ERP Platform                       ║
# ║  Nova Agency | Full-Stack Multi-Tenant SaaS                               ║
# ╚══════════════════════════════════════════════════════════════════════════════╝

Enterprise Service Hub is a high-performance, multi-tenant ERP platform designed for modern agencies. It leverages a powerful Java backend and a sleek Next.js frontend to provide a seamless enterprise experience.

## 🚀 Technology Stack

- **Backend**: Java 21+ with Spring Boot 4.0.3, Hibernate 7, PostgreSQL 17.
- **Frontend**: React 19, Next.js 15, TypeScript, Lucide Icons.
- **Infrastructure**: Docker, Redis (Caching), GitHub Actions (CI/CD).
- **Communication**: JWT-based Authentication, RESTful APIs.

## 🛠️ Getting Started

### Prerequisites
- **Docker Desktop** (Must be running for database and cache).
- **Java 21+** (for backend development).
- **Node.js 20+** (for frontend development).
- **Maven 3.9+**.

### Development Setup

The easiest way to start the entire environment is using the provided startup script:

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/HamzaElmansouri-Pr/Entreprise_Service_Hub-Spring-boot-Next-js-.git
    cd enterprise-service-hub
    ```
2.  **Start all services**:
    Run the development batch script:
    ```cmd
    start-dev.bat
    ```
    This script will:
    - Verify Docker is running.
    - Start PostgreSQL, pgAdmin, and Redis via Docker Compose.
    - Launch the Spring Boot backend in a new window.
    - Launch the Next.js frontend in a new window.

### Accessing Services
- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080/api](http://localhost:8080/api)
- **Database UI (pgAdmin)**: [http://localhost:5050](http://localhost:5050)
  - User: `admin@esh.com`
  - Pass: `admin`

## 📂 Project Structure

- `/enterprise-service-hub`: Spring Boot Maven project (Core API).
- `/nova-frontend`: Next.js frontend application.
- `docker-compose.yml`: Infrastructure definitions (DB, Cache).
- `start-dev.bat`: Unified development entry point.

## 🛡️ CI/CD

This project uses **GitHub Actions** for automated quality assurance. Every push to `main` triggers:
- Maven build and unit tests.
- Frontend linting and type checking.
- Docker configuration validation.

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.

---
Built with ❤️ by the **Nova Agency** team.
