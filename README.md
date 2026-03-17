# MAC Address Blacklist Remover
===============================

#### WLC (Wireless LAN Controller) MAC Address Blacklist Removal Tool

### A Spring Boot application for removing MAC addresses from Aruba Wireless Controller blacklists via SSH

This application provides automated removal of MAC addresses from wireless controller blacklists using SSH connections. It includes comprehensive logging of all removal operations and maintains synchronization between the database and controller hardware.

-------------------------------------------------------------
- Stack: [JDK 21](http://jdk.java.net/21/), Spring Boot 3.x, Spring Security, Spring Data JPA, Lombok, H2/PostgreSQL Database, Liquibase, Caffeine Cache, SpringDoc OpenApi 2.x, Mapstruct, SSH Client (SSHJ), Jakarta EE, Logback
- Run: `mvn spring-boot:run` in root directory.
-----------------------------------------------------

## Features
- **SSH-Based MAC Address Removal**: Connect to Aruba WLCs via SSH to remove MAC addresses from blacklists
- **Multi-Controller Support**: Supports multiple WLC instances with persistent SSH connections
- **Connection Management**: Automatic SSH connection pooling with health checks and reconnection
- **Privileged Mode Support**: Automatically enters enable mode for administrative commands
- **Comprehensive Logging**: All removal operations are logged with timestamps and user information
- **Database Synchronization**: Maintains sync between local database and WLC hardware
- **User Authentication**: Role-based access control with Spring Security
- **Caching**: Performance optimization with Caffeine cache
- **REST API**: Full REST API with OpenAPI documentation
- **Frontend Interface**: Modern web interface for MAC address management

## SSH Configuration
The application uses persistent SSH connections with the following features:

### Connection Management
- **Persistent Connections**: Maintains long-lived SSH connections to avoid reconnection overhead
- **Enabled Shell Sessions**: Automatically enters privileged mode for administrative access
- **Health Monitoring**: Regular health checks with automatic reconnection on failure
- **Paging Disabled**: Prevents "Press any key to continue" prompts during command execution

### SSH Configuration (application.yaml)
- WLC_USERNAME=your_wlc_username
- WLC_PASSWORD=your_ssh_password
- WLC_ENABLE_PASSWORD=your_enable_password
- WLC1_SSH_HOST=<your_wlc_ip>
- WLC1_SSH_PORT=22
- WLC2_SSH_HOST=<your_wlc_ip>
- WLC2_SSH_PORT=22
- WLC_CONNECTION_TIMEOUT=30
- WLC_COMMAND_TIMEOUT=15
- WLC_HEALTH_CHECK_INTERVAL=30
- DB_URL=jdbc:postgresql://your_host:5432/your_database
- DB_USERNAME=your_db_user
- DB_PASSWORD=your_db_password

## API Documentation
[REST API documentation](http://localhost:8080/)  

## Default Credentials for API
- User: user@gmail.com / password 
- Admin: admin@gmail.com / admin 
- Guest: guest@gmail.com / guest

## Configuration
1. **SSH Settings**: Configure WLC SSH connection parameters in `application.yaml`
2. **Database**: Set up H2 (development) or PostgreSQL (production) connection
3. **Security**: Configure user credentials and roles
4. **Logging**: Set appropriate log levels for SSH operations

## Deployment with Docker

The full stack (frontend + backend + database) is deployed via a single `docker-compose.yml` in this repository. Docker images are automatically built and pushed to Docker Hub on every push to `main`.

### Prerequisites
- Docker and Docker Compose installed

### Steps

**1. Clone this repository:**
```bash
git clone https://github.com/aalexeen/blacklistremover-backend.git
cd blacklistremover-backend
```

**2. Create `.env` from the template and fill in the values:**
```bash
cp .env.example .env
nano .env
```

**3. Start all services:**
```bash
docker compose up -d
```

Docker will automatically pull the following images:
- `aalexeen/blacklistremover-backend:latest`
- `aalexeen/blacklistremover-frontend:latest`
- `postgres:16-alpine`

**4. Open in browser:** `http://localhost`

### Update after code changes
```bash
docker compose pull
docker compose up -d
```

## Local Development
1. Start the application with `mvn spring-boot:run`
2. Access the web interface at http://localhost:8080
3. Login with provided credentials
4. Use the frontend dashboard or REST API to remove MAC addresses from blacklists
5. Monitor SSH connection status and logs for operation details

## SSH Command Execution
The application executes the following types of commands on WLC devices:
- `show ap blacklist-clients` - Query current blacklist entries
- `stm remove-blacklist-client <MAC-Address>` - Remove specific MAC address
- `show version` - Health check and connection verification

## Logging
All MAC address removal operations are automatically logged including:
- Removed MAC addresses with timestamps
- SSH connection status and health
- User who performed the operation
- Success/failure status with error details
- Controller information and command execution logs
- Database synchronization status
