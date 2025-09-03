# API Comparison Project

이 프로젝트는 v1과 v2 API 응답을 비교하는 예제입니다. Spring Boot, Kotlin, Gradle을 사용하여 구현되었습니다.

## 프로젝트 구조

```
api-comparison/
├── api-v1/           # V1 API 서비스
├── api-v2/           # V2 API 서비스  
├── check-app/        # API 비교 서비스
├── http/             # HTTP 테스트 파일들
├── build.gradle.kts  # 루트 빌드 설정
├── settings.gradle.kts
├── test-apps.sh      # 테스트 스크립트
└── README.md
```

## 애플리케이션 설명

### 1. api-v1 (포트: 8081)
- V1 API 엔드포인트를 제공하는 API 서비스
- 사용자(Users)와 제품(Products) 데이터 제공
- 기본적인 데이터 구조 사용

### 2. api-v2 (포트: 8082)  
- V2 API 엔드포인트를 제공하는 API 서비스
- V1과 동일한 데이터 구조 사용
- 헬스 체크 응답의 버전 필드만 다름 ("v2")

### 3. check-app (포트: 8080)
- V1과 V2 API를 호출하여 응답을 비교하는 서비스
- 차이점을 분석하고 상세한 비교 결과 제공
- WebClient를 사용한 비동기 처리

## 기술 스택

- **Spring Boot**: 3.2.0
- **Kotlin**: 1.9.20
- **Gradle**: Kotlin DSL
- **Spring WebFlux**: 비동기 HTTP 클라이언트
- **Jackson**: JSON 처리

## 실행 방법

### 1. 자동 테스트 스크립트 사용 (권장)

```bash
./test-apps.sh
```

이 스크립트는:
- 세 개의 애플리케이션을 모두 시작
- 각 서비스가 준비될 때까지 대기
- 모든 API 엔드포인트 테스트
- 비교 결과 출력

### 2. 수동 실행

각 터미널에서 개별적으로 실행:

```bash
# Terminal 1: api-v1 실행
cd api-v1
../gradlew bootRun

# Terminal 2: api-v2 실행  
cd api-v2
../gradlew bootRun

# Terminal 3: check-app 실행
cd check-app
../gradlew bootRun
```

## API 엔드포인트

### API V1 (http://localhost:8081)
- `GET /api/v1/health` - 헬스 체크
- `GET /api/v1/users` - 사용자 목록
- `GET /api/v1/users/{id}` - 특정 사용자
- `GET /api/v1/products` - 제품 목록
- `GET /api/v1/products/{id}` - 특정 제품

### API V2 (http://localhost:8082)
- `GET /api/v2/health` - 헬스 체크 (버전 필드만 V1과 다름)
- `GET /api/v2/users` - 사용자 목록 (V1과 동일한 구조)
- `GET /api/v2/users/{id}` - 특정 사용자 (V1과 동일한 구조)
- `GET /api/v2/products` - 제품 목록 (V1과 동일한 구조)
- `GET /api/v2/products/{id}` - 특정 제품 (V1과 동일한 구조)

### Check App (http://localhost:8080)
- `GET /compare/status` - 비교 서비스 상태
- `GET /compare/health` - 헬스 엔드포인트 비교
- `GET /compare/users` - 사용자 목록 비교
- `GET /compare/users/{id}` - 특정 사용자 비교
- `GET /compare/products` - 제품 목록 비교
- `GET /compare/products/{id}` - 특정 제품 비교
- `GET /compare/all` - 모든 엔드포인트 비교
- `GET /compare/summary` - 비교 요약 정보

## 비교 결과 예시

### 사용자/제품 엔드포인트 (동일한 응답)
```json
{
  "endpoint": "users",
  "isIdentical": true,
  "v1Response": "[{\"id\":1,\"name\":\"John Doe\",\"email\":\"john@example.com\",\"age\":30}]",
  "v2Response": "[{\"id\":1,\"name\":\"John Doe\",\"email\":\"john@example.com\",\"age\":30}]",
  "differences": [],
  "timestamp": 1699123456789
}
```

### 헬스 체크 엔드포인트 (버전 필드만 다름)
```json
{
  "endpoint": "health",
  "isIdentical": false,
  "v1Response": "{\"status\":\"UP\",\"version\":\"v1\",\"timestamp\":\"1699123456789\"}",
  "v2Response": "{\"status\":\"UP\",\"version\":\"v2\",\"timestamp\":\"1699123456789\"}",
  "differences": [
    "Field 'version': v1='v1', v2='v2'"
  ],
  "timestamp": 1699123456789
}
```

## 주요 차이점

### 사용자 데이터 구조
**V1과 V2 모두 동일:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com", 
  "age": 30
}
```

### 제품 데이터 구조
**V1과 V2 모두 동일:**
```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99,
  "category": "Electronics"
}
```

### 헬스 체크 응답
**V1:**
```json
{
  "status": "UP",
  "version": "v1",
  "timestamp": "1699123456789"
}
```

**V2:**
```json
{
  "status": "UP",
  "version": "v2",
  "timestamp": "1699123456789"
}
```

## 테스트 예시

```bash
# 사용자 비교
curl http://localhost:8080/compare/users

# 특정 사용자 비교
curl http://localhost:8080/compare/users/1

# 제품 비교
curl http://localhost:8080/compare/products

# 전체 요약
curl http://localhost:8080/compare/summary
```

## HTTP 테스트 파일

`http/` 디렉토리에는 IntelliJ IDEA에서 사용할 수 있는 HTTP 요청 파일들이 포함되어 있습니다:

- `api-v1.http` - API V1 서비스 테스트 요청들
- `api-v2.http` - API V2 서비스 테스트 요청들  
- `comparison.http` - API 비교 서비스 테스트 요청들

### 사용 방법
1. IntelliJ IDEA에서 `.http` 파일을 열기
2. 각 요청 옆의 녹색 화살표를 클릭하여 실행
3. HTTP Client 탭에서 응답 확인

자세한 내용은 `http/README.md`를 참조하세요.

## 로그 파일

테스트 스크립트 실행 시 다음 로그 파일이 생성됩니다:
- `api-v1.log` - API V1 애플리케이션 로그
- `api-v2.log` - API V2 애플리케이션 로그  
- `check-app.log` - Check App 애플리케이션 로그

## 종료 방법

테스트 스크립트가 출력하는 PID를 사용하여 프로세스 종료:
```bash
kill [PID1] [PID2] [PID3]
```

또는 각 터미널에서 `Ctrl+C`로 개별 종료