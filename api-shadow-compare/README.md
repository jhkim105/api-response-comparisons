# Shadow Compare Sample

Spring Boot 3.5.5 + Kotlin 2.0.20 기반의 **Shadow Deployment + Differential Testing** 예제 프로젝트입니다.  
기존 API(v1)를 리팩토링한 API(v2)와 동치성을 검증하기 위해, 운영 트래픽을 그대로 v1이 처리하면서 **같은 요청을 v2에 shadow 호출**하여 결과를 비교하고 로그로 남깁니다.

---

## 주요 특징

- **Shadow Deployment (a.k.a. Dark Launch, Shadow Traffic)**  
  - 사용자 응답은 v1이 정상적으로 반환  
  - 동일 입력을 v2에 비동기 shadow 호출 → 결과만 비교 및 로깅  

- **Differential Testing (Back-to-Back Test / Dual Run)**  
  - v1, v2 결과를 JSON 단위로 비교  
  - 동일하면 `equal=true`, 다르면 JSON Patch 형식의 diff 출력  

- **운영 안전성**  
  - 비동기 실행 (Coroutine + SupervisorJob)  
  - 샘플링 비율 지정 가능 (`sample-rate`)  
  - 타임아웃 적용 (`timeout-ms`)  
  - v2 호출 오류/지연은 v1 사용자 응답에 영향 없음  

---

## 프로젝트 구조

```
shadow-compare-sample/
 ├── build.gradle.kts
 ├── settings.gradle.kts
 ├── src/main/kotlin/com/example/shadow/
 │    ├── ShadowCompareApplication.kt   # Spring Boot 실행 진입점
 │    ├── FooService.kt                 # 요청/응답 모델 + v1/v2 서비스
 │    ├── FooController.kt              # /api/v1/foo, /api/v2/foo 엔드포인트
 │    └── ApiComparator.kt              # v1/v2 결과 비교 컴포넌트
 └── src/main/resources/application.yml # 설정값
```

---

## 설정

`src/main/resources/application.yml`

```yaml
shadow:
  differential-testing:
    enabled: true     # shadow 비교 활성화 여부
    sample-rate: 1.0  # 트래픽 샘플링 비율 (0.0~1.0)
    timeout-ms: 1000  # v2 호출 타임아웃(ms)
```

---

## 실행 방법

```bash
./gradlew bootRun
```

서버가 기동되면 기본 포트 `8080`에서 다음 엔드포인트가 열립니다:

- `POST /api/v1/foo`  
  - 실제 응답은 v1  
  - 내부적으로 v2를 shadow 호출하고 결과 비교 로그 출력  

- `POST /api/v2/foo`  
  - 리팩토링된 v2 결과 직접 확인  

---

## 요청 예시

```http
POST http://localhost:8080/api/v1/foo
Content-Type: application/json

{
  "id": 123,
  "q": "hello"
}
```

**응답(v1):**
```json
{
  "id": 123,
  "value": "v1:hello",
  "tags": ["legacy"]
}
```

**로그 예시 (equal=false):**
```
WARN  shadow.compare  [API-COMPARE][foo] equal=false, diff=[{"op":"replace","path":"/tags/0","value":"refactored"}]
```

---

## IntelliJ HTTP Client 테스트

`shadow-compare.http` 파일을 활용하면 v1/v2를 연속 호출하고 응답을 자동 비교할 수 있습니다.  
예제는 프로젝트에 포함된 `.http` 스크립트를 참고하세요.

---

## 참고 용어

- **Shadow Deployment / Shadow Traffic**: 운영 트래픽을 새 버전에 미러링하는 배포 전략  
- **Differential Testing (Back-to-Back Test)**: 동일 입력에 대해 두 구현의 결과를 비교하는 테스트 기법  
- **Dual Run**: 두 시스템을 병행 운용하며 결과를 비교 검증하는 방식  

---

## 라이선스

본 예제는 자유롭게 수정/활용하실 수 있습니다.
