#!/bin/bash

echo "=== API Comparison Test Script ==="
echo "This script demonstrates the v1, v2 API comparison functionality"
echo

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "Port $port is already in use"
        return 1
    fi
    return 0
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for $service_name to start..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "$service_name is ready!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts - $service_name not ready yet..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "ERROR: $service_name failed to start within expected time"
    return 1
}

# Function to test API endpoints
test_endpoint() {
    local url=$1
    local description=$2
    echo
    echo "=== Testing: $description ==="
    echo "URL: $url"
    echo "Response:"
    curl -s "$url" | jq . 2>/dev/null || curl -s "$url"
    echo
}

echo "Step 1: Checking if ports are available..."
check_port 8081 || exit 1
check_port 8082 || exit 1
check_port 8080 || exit 1

echo "Step 2: Starting api-v1 application (port 8081)..."
cd api-v1
../gradlew bootRun > ../api-v1.log 2>&1 &
API_V1_PID=$!
cd ..

echo "Step 3: Starting api-v2 application (port 8082)..."
cd api-v2
../gradlew bootRun > ../api-v2.log 2>&1 &
API_V2_PID=$!
cd ..

echo "Step 4: Starting check-app application (port 8080)..."
cd check-app
../gradlew bootRun > ../check-app.log 2>&1 &
CHECK_APP_PID=$!
cd ..

# Wait for all services to start
wait_for_service "http://localhost:8081/api/v1/health" "api-v1" || exit 1
wait_for_service "http://localhost:8082/api/v2/health" "api-v2" || exit 1
wait_for_service "http://localhost:8080/compare/status" "check-app" || exit 1

echo
echo "=== All applications are running! ==="
echo "api-v1: http://localhost:8081"
echo "api-v2: http://localhost:8082" 
echo "check-app: http://localhost:8080"
echo

# Test v1 endpoints
test_endpoint "http://localhost:8081/api/v1/health" "API V1 Health"
test_endpoint "http://localhost:8081/api/v1/users" "API V1 Users"
test_endpoint "http://localhost:8081/api/v1/products" "API V1 Products"

# Test v2 endpoints
test_endpoint "http://localhost:8082/api/v2/health" "API V2 Health"
test_endpoint "http://localhost:8082/api/v2/users" "API V2 Users"
test_endpoint "http://localhost:8082/api/v2/products" "API V2 Products"

# Test comparison endpoints
test_endpoint "http://localhost:8080/compare/status" "Comparison Service Status"
test_endpoint "http://localhost:8080/compare/health" "Health Comparison"
test_endpoint "http://localhost:8080/compare/users" "Users Comparison"
test_endpoint "http://localhost:8080/compare/products" "Products Comparison"
test_endpoint "http://localhost:8080/compare/summary" "Comparison Summary"

echo
echo "=== Test completed! ==="
echo "To stop all applications, run:"
echo "kill $API_V1_PID $API_V2_PID $CHECK_APP_PID"
echo
echo "Log files:"
echo "- api-v1.log"
echo "- api-v2.log" 
echo "- check-app.log"
echo
echo "You can also test individual endpoints manually:"
echo "curl http://localhost:8080/compare/users/1"
echo "curl http://localhost:8080/compare/products/1"