# DocumentService

Stores the files attached to tenders: metadata in its own PostgreSQL database (`document_db`), content in a directory
(`STORAGE_DIR`, a Docker volume). Port 8085. Files get a random name on disk; only pdf, doc, docx, xls, xlsx, png, jpg, jpeg, txt, max 10 MB.

## Endpoints (JWT issued by IdentityService)
| Method | URL | Permission |
|---|---|---|
| POST | /api/files (multipart: `file`, `tenderId`) | DOCUMENT_WRITE |
| GET | /api/files?tenderId= | DOCUMENT_READ |
| GET | /api/files/{id} (metadata) | DOCUMENT_READ |
| GET | /api/files/{id}/download | DOCUMENT_READ |
| DELETE | /api/files/{id} | DOCUMENT_DELETE |

## Communication (Feign)
- TenderFlow: before an upload, `GET /api/tenders/{id}` with the user's token (404 if the tender does not exist / is not visible).
- IdentityService + ReportingService: with the technical account (`SERVICE_ACCOUNT_EMAIL/PASSWORD`, role SERVICE) it publishes
  `FILE_UPLOADED` / `FILE_DELETED` audit events (best effort, never blocks an upload).

## Run
Env vars: `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (same as IdentityService), `SERVICE_ACCOUNT_EMAIL`, `SERVICE_ACCOUNT_PASSWORD`.
Docker: `docker network create platform-net` (once), `copy .env.example .env`, `docker compose up --build`.
