# ATSCIRCLE Backend

This is the backend for the ATSCIRCLE Applicant Tracking System. It manages jobs, candidates, interviews, invoices, and sales — built with Spring Boot and MongoDB.

---

## Requirements

Before you start, make sure you have **Java 17** and **Git** installed on your machine.

---

## Setup

### Clone the project

```bash
git clone https://github.com/your-org/ats_backend.git
cd ats_backend
```

### Configure the application

Open `src/main/resources/application.properties` and fill in your credentials:

```properties
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/ATSCIRCLE
jwt.secret=your-secret-key
spring.mail.username=your-email@gmail.com
spring.mail.password=your-gmail-app-password
aws.s3.bucket=your-bucket-name
aws.s3.access-key=YOUR_ACCESS_KEY
aws.s3.secret-key=YOUR_SECRET_KEY
google.client.id=YOUR_GOOGLE_CLIENT_ID
google.client.secret=YOUR_GOOGLE_CLIENT_SECRET
microsoft.client.id=YOUR_MICROSOFT_CLIENT_ID
microsoft.client.secret=YOUR_MICROSOFT_CLIENT_SECRET
microsoft.tenant.id=YOUR_TENANT_ID
```

---

## Run the Application

The simplest way to start the app locally:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Once started, the app runs at **http://localhost:8080**

---

## Build and Run as JAR

```bash
# Build the JAR file
./mvnw clean package -DskipTests

# Run the JAR
java -jar target/ATSCIRCLE-0.0.1-SNAPSHOT.jar
```

---

## Run with Docker

```bash
# Build the image
docker build -t ats-backend .

# Run the container
docker run -d --name backend -p 8080:8080 ats-backend
```

---

## Git Commands

```bash
# Clone the repository
git clone https://github.com/your-org/ats_backend.git

# Check current status
git status

# Pull latest changes
git pull origin main

# Create a new branch
git checkout -b your-branch-name

# Stage your changes
git add .

# Commit your changes
git commit -m "your commit message"

# Push your branch
git push origin your-branch-name

# Switch to main branch
git checkout main

# Merge a branch into main
git merge your-branch-name
```

---

## Docker Commands

```bash
# Build image
docker build -t ats-backend .

# Run container
docker run -d --name backend -p 8080:8080 ats-backend

# View running containers
docker ps

# View logs
docker logs -f backend

# Stop container
docker stop backend

# Remove container
docker rm backend
```
