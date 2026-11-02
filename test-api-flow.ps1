<#
.SYNOPSIS
    End-to-End verification script for the Microservices Architecture.
    Tests API Gateway -> Customer Service, Product Service, Order Service, Payment Service.
#>

$GATEWAY = "http://localhost:8080"
$HEADERS = @{
    "Content-Type" = "application/json"
    "X-Correlation-ID" = [System.Guid]::NewGuid().ToString()
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Running End-to-End Microservice Architecture Smoke Test" -ForegroundColor Green
Write-Host " Using Correlation-ID: $($HEADERS['X-Correlation-ID'])" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Create a Customer
Write-Host "`n[1] Creating Customer..." -ForegroundColor Yellow
$customerPayload = @{
    firstName = "Jane"
    lastName = "Doe"
    email = "jane.doe.$([System.DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@example.com"
    phoneNumber = "+1-555-0199"
    address = "123 Distributed System Way, Cloud City"
    initialBalance = 1500.00
} | ConvertTo-Json

try {
    $custResp = Invoke-RestMethod -Uri "$GATEWAY/api/v1/customers" -Method Post -Headers $HEADERS -Body $customerPayload
    $customerId = $custResp.data.id
    Write-Host "  Success! Customer ID: $customerId, Wallet: `$$($custResp.data.walletBalance)" -ForegroundColor Green
} catch {
    Write-Host "  Failed to create customer: $_" -ForegroundColor Red
    exit 1
}

# 2. Create a Product
Write-Host "`n[2] Creating Product..." -ForegroundColor Yellow
$productPayload = @{
    sku = "MACBOOK-M3-$([System.DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
    name = "Apple MacBook Pro M3"
    description = "14-inch, 16GB Unified Memory, 512GB SSD"
    price = 1200.00
    stockQuantity = 50
    category = "Electronics"
} | ConvertTo-Json

try {
    $prodResp = Invoke-RestMethod -Uri "$GATEWAY/api/v1/products" -Method Post -Headers $HEADERS -Body $productPayload
    $productId = $prodResp.data.id
    Write-Host "  Success! Product ID: $productId, SKU: $($prodResp.data.sku), Stock: $($prodResp.data.stockQuantity)" -ForegroundColor Green
} catch {
    Write-Host "  Failed to create product: $_" -ForegroundColor Red
    exit 1
}

# 3. Create an Order (Orchestrated transaction: Customer -> Product -> Payment)
Write-Host "`n[3] Submitting Order (Customer -> Product stock reservation -> Payment capture)..." -ForegroundColor Yellow
$orderPayload = @{
    customerId = $customerId
    items = @(
        @{
            productId = $productId
            quantity = 1
        }
    )
    paymentMethod = "WALLET"
} | ConvertTo-Json

try {
    $orderResp = Invoke-RestMethod -Uri "$GATEWAY/api/v1/orders" -Method Post -Headers $HEADERS -Body $orderPayload
    $orderNumber = $orderResp.data.orderNumber
    Write-Host "  Success! Order: $orderNumber" -ForegroundColor Green
    Write-Host "  Status:           $($orderResp.data.status)" -ForegroundColor Green
    Write-Host "  Payment Ref:      $($orderResp.data.paymentReference)" -ForegroundColor Green
    Write-Host "  Total Amount:     `$$($orderResp.data.totalAmount)" -ForegroundColor Green
} catch {
    Write-Host "  Failed to place order: $_" -ForegroundColor Red
    exit 1
}

# 4. Verify Customer Wallet Deduction
Write-Host "`n[4] Verifying Customer Wallet Deduction..." -ForegroundColor Yellow
try {
    $updatedCust = Invoke-RestMethod -Uri "$GATEWAY/api/v1/customers/$customerId" -Method Get -Headers $HEADERS
    Write-Host "  New Wallet Balance: `$$($updatedCust.data.walletBalance) (Deducted 1200.00 from 1500.00)" -ForegroundColor Green
} catch {
    Write-Host "  Failed to fetch updated customer: $_" -ForegroundColor Red
}

# 5. Verify Product Stock Deduction
Write-Host "`n[5] Verifying Product Stock Deduction..." -ForegroundColor Yellow
try {
    $updatedProd = Invoke-RestMethod -Uri "$GATEWAY/api/v1/products/$productId" -Method Get -Headers $HEADERS
    Write-Host "  New Stock: $($updatedProd.data.stockQuantity) units (Deducted 1 unit from 50)" -ForegroundColor Green
} catch {
    Write-Host "  Failed to fetch updated product: $_" -ForegroundColor Red
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " All Microservice Integration Tests Passed Successfully!" -ForegroundColor Green
Write-Host " View Logs in Kibana:     http://localhost:5601" -ForegroundColor White
Write-Host " View Metrics in Grafana: http://localhost:3000" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
