# 금융상품 평가 프로젝트

Spring Boot와 React 기반 금융상품 평가 애플리케이션입니다.

## 구성

- `backend/demo`: Spring Boot API
- `frontend`: React + Vite 프론트엔드
- `docs`: 기획/개발 문서

## 로컬 실행

백엔드:

```powershell
cd backend/demo
$env:DB_URL="jdbc:postgresql://localhost:5432/finance_eval"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
.\gradlew.bat bootRun
```

프론트엔드:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

## 배포 설정

Render 백엔드:

- 설정 파일: `backend/demo/render.yaml`
- 필수 환경변수:
- `DB_URL=jdbc:postgresql://dpg-d7l2luhkh4rs73fgglig-a:5432/evaluation_rd15?sslmode=require`
- `DB_USERNAME=evaluation_rd15_user`
- `DB_PASSWORD=Render 대시보드에만 직접 입력`
- `CORS_ALLOWED_ORIGINS=https://ai-asset-evaluation-prototype.vercel.app`

Vercel 프론트엔드:

- 설정 파일: `frontend/vercel.json`
- 필수 환경변수:
- `VITE_API_BASE_URL=https://ai-asset-evaluation-prototype.onrender.com`

## API

- `POST /api/evaluations/stock`
- `POST /api/evaluations/bond`
- `POST /api/evaluations/project`
- `POST /api/evaluations/save`
- `GET /api/evaluations/recent`
- `GET /api/evaluations/health`
