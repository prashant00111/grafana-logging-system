<#
.SYNOPSIS
    Builds all microservices and launches the complete Docker Compose stack.
#>

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Building Microservices & Observability Stack (ELK, Prometheus, Grafana)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# Check if Docker is running
try {
    docker info > $null 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Docker does not appear to be running. Please start Docker Desktop and re-run this script."
    }
} catch {
    Write-Warning "Docker command not found in PATH."
}

# Step 1: Maven Build
Write-Host "`n[1/3] Packaging JAR files with Maven..." -ForegroundColor Yellow
if ($env:JAVA_HOME -eq $null -or !(Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    if (Test-Path "C:\Program Files\Java\jdk-17") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
        Write-Host "Using JDK 17 at $env:JAVA_HOME" -ForegroundColor DarkGray
    }
}

mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed! Checking wrapper..." -ForegroundColor Red
}

# Step 2: Docker Compose Build & Run
Write-Host "`n[2/3] Starting Docker Compose containers..." -ForegroundColor Yellow
docker compose up --build -d

# Step 3: Display Endpoints
Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " Stack is Starting! Access the services at:" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Eureka Discovery:   http://localhost:8761" -ForegroundColor White
Write-Host " API Gateway:        http://localhost:8080" -ForegroundColor White
Write-Host " Customer Service:   http://localhost:8081/swagger-ui.html" -ForegroundColor White
Write-Host " Product Service:    http://localhost:8082/swagger-ui.html" -ForegroundColor White
Write-Host " Order Service:      http://localhost:8083/swagger-ui.html" -ForegroundColor White
Write-Host " Payment Service:    http://localhost:8084/swagger-ui.html" -ForegroundColor White
Write-Host " Prometheus Metrics: http://localhost:9090" -ForegroundColor White
Write-Host " Grafana Dashboards: http://localhost:3000 (admin / admin)" -ForegroundColor White
Write-Host " Kibana (ELK Logs):  http://localhost:5601" -ForegroundColor White
Write-Host " Zipkin Tracing:     http://localhost:9411" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
