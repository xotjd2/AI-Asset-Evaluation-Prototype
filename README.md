# 금융상품 및 프로젝트 평가 프로토타입

Spring Boot와 React 기반의 금융상품 및 프로젝트 평가 웹 프로토타입입니다. 채권 가치평가, 프로젝트 사업성 평가, VaR 중심 리스크 요약, 최근 평가 이력 저장 기능을 제공합니다.

## 구조

- `backend/demo`: Spring Boot API, MySQL 저장, 평가 계산 로직
- `frontend`: React + Vite 대시보드
- `docs`: 제출용 기획서와 개발문서 초안

## 주요 기능

- 채권 평가: 현재가치, Duration, Convexity, VaR 95%, 내부 리스크 등급
- 프로젝트 평가: NPV, IRR, 회수기간, VaR 95%, 내부 리스크 등급
- 평가 이력 저장: MySQL `evaluation_records` 테이블
- 최근 평가 조회: 대시보드 하단 카드 목록

## 백엔드 실행

1. MySQL 실행
2. 데이터베이스 계정 확인
3. `backend/demo/src/main/resources/application.properties` 기본값 사용 또는 환경변수 설정

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/finance_eval?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="1234"
.\gradlew.bat bootRun
```

실행 위치는 `backend/demo`입니다.

## 프론트 실행

```powershell
npm.cmd install
npm.cmd run dev
```

실행 위치는 `frontend`입니다.

## API

- `POST /api/evaluations/bond`
- `POST /api/evaluations/project`
- `GET /api/evaluations/recent`
- `GET /api/evaluations/health`
