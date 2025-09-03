# IntelliJ HTTP Files for API Equivalence Testing

This directory contains IntelliJ IDEA HTTP request files for testing the API equivalence project.

## Files Overview

### 1. `api-v1.http`
Tests the **API V1** service running on **localhost:8081**

**Available Endpoints:**
- `GET /api/v1/health` - Health check
- `GET /api/v1/users` - Get all users
- `GET /api/v1/users/{id}` - Get user by ID
- `GET /api/v1/products` - Get all products
- `GET /api/v1/products/{id}` - Get product by ID

**Data Models:**
- **UserV1**: `id`, `name`, `email`, `age`
- **ProductV1**: `id`, `name`, `price`, `category`

### 2. `api-v2.http`
Tests the **API V2** service running on **localhost:8082**

**Available Endpoints:**
- `GET /api/v2/health` - Health check
- `GET /api/v2/users` - Get all users
- `GET /api/v2/users/{id}` - Get user by ID
- `GET /api/v2/products` - Get all products
- `GET /api/v2/products/{id}` - Get product by ID

**Data Models:**
- **UserV2**: `id`, `name`, `email`, `age`
- **ProductV2**: `id`, `name`, `price`, `category`

### 3. `comparison.http`
Tests the **API Comparison Service** running on **localhost:8080**

**Available Endpoints:**
- `GET /compare/status` - Service status and configuration
- `GET /compare/health` - Compare health endpoints
- `GET /compare/users` - Compare all users
- `GET /compare/users/{id}` - Compare specific user
- `GET /compare/products` - Compare all products
- `GET /compare/products/{id}` - Compare specific product
- `GET /compare/all` - Compare all endpoints at once
- `GET /compare/summary` - Get comparison statistics

## How to Use

1. **Start all services:**
   ```bash
   # Start API V1 (port 8081)
   ./gradlew :api-v1:bootRun
   
   # Start API V2 (port 8082)
   ./gradlew :api-v2:bootRun
   
   # Start Comparison Service (port 8080)
   ./gradlew :check-app:bootRun
   ```

2. **Open HTTP files in IntelliJ IDEA:**
   - Open any `.http` file in IntelliJ IDEA
   - Click the green arrow next to any request to execute it
   - View responses in the HTTP Client tab

3. **Testing Strategy:**
   - Test individual APIs first (`api-v1.http`, `api-v2.http`)
   - Then test comparisons (`comparison.http`)
   - Use `/compare/summary` to get overall comparison statistics

## Key Differences Between V1 and V2

| Aspect | V1 | V2 |
|--------|----|----|
| User model | `id`, `name`, `email`, `age` | `id`, `name`, `email`, `age` |
| Product model | `id`, `name`, `price`, `category` | `id`, `name`, `price`, `category` |
| Health response version | `"version": "v1"` | `"version": "v2"` |
| Data structure | Identical | Identical |

## Expected Comparison Results

The comparison service will detect differences in:
- Health endpoint version field: `"version": "v1"` vs `"version": "v2"`
- All other endpoints (users, products) will show identical responses
- User and product data structures are identical between V1 and V2

This makes it perfect for testing scenarios where APIs have minimal differences and demonstrating how the comparison service can identify even small variations between API versions.