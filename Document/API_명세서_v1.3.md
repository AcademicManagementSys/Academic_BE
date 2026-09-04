# API 명세서 v1.3 (구현 기준)

작성일: 2026-09-04 갱신(최초 2026-09-03). `Document/API_명세서_V2.pdf`(v1.2)를 기준으로,
[[api-spec-compliance-audit]] A/B/C/D 감사 전체가 반영된 **실제 구현 코드**를 그대로 문서화한
것입니다. PDF를 대체하는 최신 버전이며, PDF→코드 간 남은 차이는 `Document/스펙_변경_제안.md`에
정리되어 있습니다(그 문서의 제안이 이 문서에는 이미 전부 반영되어 있음).

- 소스 기준: `feature/api-spec-compliance-auth` 브랜치, 커밋 `67e1a8c` 이후 `POST
  /v1/auth/password/reset-request`/`reset`(토큰을 응답에 노출하던 이메일 대체 임시조치) 제거,
  `POST /v1/auth/password/change`(현재 비밀번호 확인 후 변경) 추가
- 컨트롤러 14개, 엔드포인트 54개 전수 조사

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [인증 API](#2-인증-api-v1auth-v1me)
3. [계정 관리 API](#3-계정-관리-api-v1users)
4. [반 관리 API](#4-반-관리-api-v1classes)
5. [선생님 배정 API](#5-선생님-배정-api-v1teacher-assignments)
6. [학부모-자녀 연결 API](#6-학부모-자녀-연결-api)
7. [학생 관리 API](#7-학생-관리-api-v1students)
8. [출석 API](#8-출석-api)
9. [숙제 API](#9-숙제-api)
10. [테스트 API](#10-테스트-api)
11. [월말모의고사 API](#11-월말모의고사-api)
12. [공지사항 API](#12-공지사항-api)
13. [대시보드 API](#13-대시보드-api-v1dashboard)
14. [권한 매트릭스 전체 요약](#14-권한-매트릭스-전체-요약)
15. [에러 코드](#15-에러-코드)
16. [Enum 값 정리](#16-enum-값-정리)

---

## 1. 공통 사항

### 1.1 Base URL / 응답 포맷

모든 API는 `/v1`으로 시작합니다. 성공 응답은 항상 아래 형태로 감싸집니다(`null` 값 필드는
직렬화에서 생략됨 — `@JsonInclude(NON_NULL)`):

```json
{ "data": { ... } }
```

목록 응답은 `data`가 배열, 응답 본문이 없는 API(204 No Content 등)는 `data`가 생략됩니다.

### 1.2 인증 헤더

로그인/토큰재발급/비밀번호 재설정 4개 엔드포인트(§2 참고)를 제외한 **모든 API**는
`Authorization: Bearer {accessToken}` 헤더가 필수입니다. 없거나 무효(만료·서명불일치·형식오류)하면
컨트롤러에 진입하기 전에 필터(`JwtAuthenticationFilter`)가 즉시 `401 UNAUTHENTICATED`를 반환합니다.

토큰 검증 통과 시 서버는 토큰의 `sub`(userId)/`role`/`loginId` claim으로 `AuthenticatedUser`를
구성해 매 요청마다 역할 기반 접근 제어(RBAC) + 소유권(ownership) 체크를 수행합니다. 세부 규칙은
각 API 항목과 [§14 권한 매트릭스](#14-권한-매트릭스-전체-요약)를 참고하세요.

### 1.3 에러 응답 포맷

```json
{
  "error": {
    "code": "FORBIDDEN_SCOPE",
    "message": "담당하지 않은 반의 데이터에는 접근할 수 없습니다.",
    "details": [{ "field": "title", "message": "must not be blank" }]
  }
}
```

`details`는 Bean Validation(`@Valid`) 실패 시에만 채워지고, 그 외에는 생략됩니다. 코드/상태 매핑은
[§15](#15-에러-코드) 참고.

### 1.4 날짜/시간 형식

- 날짜(`LocalDate`): `YYYY-MM-DD`
- 날짜시간(`LocalDateTime`): ISO-8601 (`YYYY-MM-DDTHH:mm:ss`)
- 월(`examMonth` 등): `YYYY-MM`

---

## 2. 인증 API (`/v1/auth`, `/v1/me`)

인증 없이 호출 가능한 것은 이 섹션의 `login`/`refresh` **2개뿐**입니다. Access Token 만료 30분,
Refresh Token 만료 14일(`application.yml` `app.jwt.access-token-ttl-minutes`/`refresh-token-ttl-days`).
Refresh Token은 서버에 SHA-256 해시로 저장되어(`RefreshToken` 엔티티) 재발급 시 로테이션(기존 토큰
폐기 후 신규 발급)되고, `/auth/logout` 호출 시 폐기됩니다. 재사용(이미 폐기된 refresh token으로
재발급 시도)은 401 `UNAUTHENTICATED`로 거부됩니다.

비밀번호를 잊어버린 경우(REQ-AUTH-07) 이메일 인프라가 없어 자가 재설정 토큰 플로우는 두지 않고,
**로그인 상태에서 현재 비밀번호를 아는 경우**의 `password/change`(아래)와 **완전히 잊어버린 경우**의
관리자 강제 초기화(`POST /v1/users/{id}/reset-password`, §3, admin 전용) 두 경로만 지원합니다.

### `POST /v1/auth/login` — 로그인 (인증 불필요)

**Request**
```json
{ "loginId": "teacher1", "password": "Teacher1234!" }
```
| 필드 | 타입 | 필수 |
|---|---|---|
| loginId | string | ✔ |
| password | string | ✔ |

**Response** `200`
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "user": { "id": 3, "name": "김선생", "role": "teacher", "hasMultipleChildren": false }
  }
}
```
`user.hasMultipleChildren`은 role이 `parent`이고 자녀가 2명 이상일 때만 `true` — 프런트엔드는
이 값이 `true`면 홈 대신 자녀 선택 화면(SCR-17)으로 먼저 이동한다.

**에러**: `loginId`/`password` 불일치 또는 `active=false` → `401 UNAUTHENTICATED`.

### `POST /v1/auth/logout` — 로그아웃

인증 필요(본인 확인용, `me`는 사용되지 않고 요청 바디의 refresh token만 폐기됨).

**Request**
```json
{ "refreshToken": "eyJ..." }
```
**Response** `200`, `data` 없음. 존재하지 않거나 이미 폐기된 토큰이어도 조용히 성공 처리.

### `POST /v1/auth/refresh` — 토큰 재발급 (인증 불필요, refresh token 자체가 인증 수단)

**Request**
```json
{ "refreshToken": "eyJ..." }
```
**Response** `200`
```json
{ "data": { "accessToken": "eyJ...", "refreshToken": "eyJ..." } }
```
기존 refresh token은 이 호출로 즉시 폐기(로테이션)되며, 반환된 새 refresh token으로 다음 재발급을
해야 합니다. **에러**: 유효하지 않거나 이미 폐기된 토큰 → `401 UNAUTHENTICATED`.

### `POST /v1/auth/password/change` — 비밀번호 변경 (인증 필요)

로그인 상태에서 본인이 현재 비밀번호를 직접 확인하고 새 비밀번호로 바꾸는 자기 서비스 경로. 비밀번호를
완전히 잊어버려 현재 비밀번호를 모르는 경우는 관리자 강제 초기화(`POST /v1/users/{id}/reset-password`,
§3, admin 전용)를 사용한다.

**Request**
```json
{ "currentPassword": "Teacher1234!", "newPassword": "NewPass123!" }
```
| 필드 | 제약 |
|---|---|
| currentPassword | 필수 |
| newPassword | 8자 이상 |

**Response** `200`, `data` 없음.
**에러**: `currentPassword`가 실제 비밀번호와 불일치 → `422 VALIDATION_ERROR`.

### `GET /v1/me` — 내 정보 조회

인증만 필요(역할 무관). 토큰의 사용자 id로 `UserResponse`(§3) 반환.

---

## 3. 계정 관리 API (`/v1/users`)

**전 엔드포인트 admin 전용** (`FORBIDDEN_ROLE` — teacher/parent/student는 전부 거부).

| Method | Path | 설명 |
|---|---|---|
| GET | `/v1/users?role={role}` | 목록 조회 (role 생략 시 전체) |
| POST | `/v1/users` | 계정 생성 |
| GET | `/v1/users/{id}` | 상세 조회 |
| PATCH | `/v1/users/{id}` | 정보 수정 |
| PATCH | `/v1/users/{id}/status` | 활성/비활성 전환 |
| POST | `/v1/users/{id}/reset-password` | 비밀번호 강제 초기화 |

**`UserCreateRequest`**
```json
{ "role": "teacher", "name": "김선생", "loginId": "teacher1", "password": "Teacher1234!", "phone": "010-1111-2222", "classIds": [1, 2] }
```
`role`은 `admin`을 받으면 `422 VALIDATION_ERROR`(이 API로는 teacher/parent만 생성 가능 — admin은
psql로만 시딩, [[admin-account-creation-blocked]] 참고). `loginId` 중복 시 `409 DUPLICATE_LOGIN_ID`.

**`UserResponse`**
```json
{ "id": 3, "name": "김선생", "role": "teacher", "loginId": "teacher1", "phone": "010-1111-2222", "active": true, "createdAt": "2026-09-01T10:00:00" }
```

**`UserUpdateRequest`**: `{ "name": "...", "phone": "..." }` (둘 다 선택)

**`UserStatusUpdateRequest`**: `{ "active": true }` (필수)

**`POST /{id}/reset-password` Response**
```json
{ "data": { "userId": 3, "loginId": "teacher1", "tempPassword": "Ax7f...", "mustChangePassword": true } }
```
`tempPassword`는 이 응답 1회에 한해 평문 노출.

---

## 4. 반 관리 API (`/v1/classes`)

admin/teacher만 접근 가능. teacher는 담당 반만 조회/열람 가능(`FORBIDDEN_SCOPE`), 생성/수정/삭제는
admin 전용.

| Method | Path | 역할 | 소유권 체크 |
|---|---|---|---|
| GET | `/v1/classes?teacherId={id}` | admin, teacher | teacher는 `teacherId` 무시하고 항상 본인 반만 |
| POST | `/v1/classes` | admin | — |
| GET | `/v1/classes/{id}` | admin, teacher | teacher는 담당 반만 |
| PATCH | `/v1/classes/{id}` | admin | — |
| DELETE | `/v1/classes/{id}` | admin | — |
| GET | `/v1/classes/{id}/students` | admin, teacher | teacher는 담당 반만 |

**`ClassCreateRequest`**: `{ "name": "고1 수학반", "teacherId": 3, "schedule": "월수금 19:00" }`
(`name` 필수, 나머지 선택)

**`ClassResponse`**
```json
{ "id": 1, "name": "고1 수학반", "teacherId": 3, "teacherName": "김선생", "schedule": "월수금 19:00" }
```

**`ClassUpdateRequest`**: `{ "name": "...", "teacherId": 1, "schedule": "..." }` (전부 선택)

`GET /{id}/students` 응답은 `StudentResponse[]`(§7).

---

## 5. 선생님 배정 API (`/v1/teacher-assignments`)

**전 엔드포인트 admin 전용.** 반 단위(`classId`) 또는 학생 단위(`studentId`) 개별 배정을 추가로
등록할 수 있음(반 담당 외에 특정 학생만 추가 배정하는 경우).

| Method | Path |
|---|---|
| GET | `/v1/teacher-assignments?teacherId={id}` |
| POST | `/v1/teacher-assignments` |
| DELETE | `/v1/teacher-assignments/{id}` |

**`TeacherAssignmentCreateRequest`**: `{ "teacherId": 3, "classId": 1, "studentId": null }`
(`teacherId` 필수, `classId`/`studentId` 중 용도에 맞게 선택)

**`TeacherAssignmentResponse`**
```json
{ "id": 10, "teacherId": 3, "teacherName": "김선생", "classId": 1, "className": "고1 수학반", "studentId": null, "studentName": null }
```

---

## 6. 학부모-자녀 연결 API

연결 자체의 생성/수정/해제는 admin 전용, 본인 자녀 목록 조회는 로그인한 학부모 본인의
`/me/children`.

| Method | Path | 역할 |
|---|---|---|
| GET | `/v1/me/children` | parent (본인) |
| POST | `/v1/parent-links` | admin |
| PATCH | `/v1/parent-links/{id}` | admin |
| DELETE | `/v1/parent-links/{id}` | admin |

**`GET /v1/me/children` Response** — `ChildResponse[]`
```json
[{ "studentId": 5, "name": "김학생", "grade": "고1", "className": "고1 수학반", "relationType": "mother" }]
```

**`ParentStudentCreateRequest`**: `{ "parentUserId": 7, "studentId": 5, "relationType": "mother" }`
(전부 필수)

**`ParentStudentResponse`**
```json
{ "id": 2, "parentUserId": 7, "parentName": "김학부모", "studentId": 5, "studentName": "김학생", "relationType": "mother" }
```

**`ParentStudentUpdateRequest`**: `{ "relationType": "father" }` (필수 — `relationType`만 변경 가능)

---

## 7. 학생 관리 API (`/v1/students`)

| Method | Path | 역할 | 소유권 |
|---|---|---|---|
| GET | `/v1/students?classId=&status=&keyword=` | admin, teacher | teacher: `classId` 지정 시 담당 반 아니면 403, 생략 시 담당 반 전체로 자동 필터링 |
| POST | `/v1/students` | admin | — |
| GET | `/v1/students/{id}` | admin, teacher(담당), parent(자녀), student(본인) | `requireCanViewStudent` |
| PATCH | `/v1/students/{id}` | admin | — |
| GET | `/v1/students/{id}/summary?month=` | admin, teacher(담당), parent(자녀), student(본인) | `requireCanViewStudent` — SCR-11(교사용 상세)과 FR-07-01(학부모/학생 홈)을 겸용 |
| GET | `/v1/students/{id}/notifications/badge?since=` | admin, teacher(담당), parent(자녀), student(본인) | `requireCanViewStudent` |

> `GET /students`에서 teacher가 `classId`를 생략하면 admin과 달리 응답이 "본인이 담당하는 반의
> 학생 전체"로 자동 필터링됩니다(전체 학생이 아님).

**`StudentCreateRequest`**
```json
{
  "name": "김학생", "birthDate": "2009-03-15", "school": "OO고", "grade": "고1",
  "phone": "010-3333-4444", "classId": 1, "teacherId": 3, "enrolledAt": "2026-03-01",
  "parent": { "createNew": true, "name": "김학부모", "phone": "010-5555-6666",
              "loginId": "parent1", "password": "Parent1234!", "relationType": "mother" },
  "account": { "loginId": null, "autoGenerateLoginId": true }
}
```
`name` 필수. `parent`는 `createNew=true`면 새 학부모 계정을 함께 생성(`loginId`/`password`/`name`
필요), `false`면 `parentUserId`로 기존 학부모와 연결. `account`를 생략하면 학생 본인 로그인 계정은
발급되지 않는다 — 값을 주면 `loginId` 생략 시 서버가 자동 생성, 임시 비밀번호도 함께 발급.

**`StudentResponse`**
```json
{
  "id": 5, "name": "김학생", "birthDate": "2009-03-15", "school": "OO고", "grade": "고1",
  "phone": "010-3333-4444", "classId": 1, "className": "고1 수학반", "status": "enrolled",
  "enrolledAt": "2026-03-01",
  "account": { "userId": 12, "loginId": "student_5f3a", "tempPassword": "Bz2k..." }
}
```
`account`는 생성 응답에만 채워지고(1회 노출), 이후 조회 응답에서는 `null`(생략).

**`StudentUpdateRequest`**: `{ "name": "...", "birthDate": "...", "school": "...", "grade": "...", "phone": "...", "classId": 1, "status": "paused" }` (전부 선택)

**`GET /{id}/summary` Response** (`StudentSummaryResponse`)
```json
{
  "student": { "id": 5, "name": "김학생", "grade": "고1", "className": "고1 수학반" },
  "attendance": { "presentDays": 18, "totalDays": 20, "lateCount": 1, "absentCount": 1 },
  "homework": [{ "title": "1단원 문제집", "isDone": true, "date": "2026-09-01" }],
  "recentTest": { "sessionDate": "2026-08-30", "scores": { "vocab": 90, "reading": 85, "grammar": 80, "syntax": 88 } },
  "recentMonthlyExam": { "examMonth": "2026-08", "rawScore": 92, "deltaFromPrev": 4 }
}
```

**`GET /{id}/notifications/badge` Response** (`NotificationBadgeResponse`, FR-08 — 명세서에 없는
추가 기능, D 버킷)
```json
{ "since": "2026-09-01T00:00:00", "attendanceCount": 3, "homeworkCount": 5, "testCount": 1, "monthlyExamCount": 0, "totalCount": 9 }
```

---

## 8. 출석 API

| Method | Path | 역할/소유권 |
|---|---|---|
| GET | `/v1/attendance?classId=&date=` | admin 전체, teacher는 담당 반만(`requireCanManageClass`) |
| POST | `/v1/attendance/bulk` | 동일 |
| PATCH | `/v1/attendance/{id}` | 대상 출석 기록이 속한 반 기준으로 동일 체크(개별 학생 배정인 경우 classId가 없으면 admin 전용) |
| GET | `/v1/students/{studentId}/attendance?month=` | admin, teacher(담당), parent(자녀), student(본인) |

**`AttendanceBulkRequest`**
```json
{
  "classId": 1, "date": "2026-09-03",
  "records": [{ "studentId": 5, "status": "present", "note": null }, { "studentId": 6, "status": "absent", "note": "병결" }]
}
```
`classId`/`date`/`records` 필수, `records`는 1건 이상.

**`AttendanceResponse`**
```json
{ "id": 100, "studentId": 5, "studentName": "김학생", "date": "2026-09-03", "status": "present", "note": null }
```
기록이 없는 학생은 `id: null, status: null, note: null`로 내려와 프런트가 "미입력" 상태를 표현할
수 있게 한다(`AttendanceResponse.unchecked`).

**`AttendanceUpdateRequest`**: `{ "status": "late", "note": "..." }` (둘 다 선택)

---

## 9. 숙제 API

| Method | Path | 역할/소유권 |
|---|---|---|
| GET | `/v1/homework-items?classId=&week=` | admin, teacher(담당) |
| POST | `/v1/homework-items` | admin, teacher(담당 반 또는 담당 학생) |
| PATCH | `/v1/homework-items/{id}` | 대상 항목의 반/학생 기준 동일 체크 |
| DELETE | `/v1/homework-items/{id}` | 동일 |
| GET | `/v1/homework-items/{id}/records` | 동일 |
| POST | `/v1/homework-records/bulk` | admin, teacher(담당 반) |
| GET | `/v1/students/{studentId}/homework?from=&to=` | admin, teacher(담당), parent(자녀), student(본인) |

**`HomeworkItemCreateRequest`** — `classId`/`studentId` 중 정확히 하나만 채움(반 전체 숙제 vs
개별 학생 숙제):
```json
{ "classId": 1, "studentId": null, "title": "1단원 문제집", "scope": "p.10~20", "assignedDate": "2026-09-01", "dueDate": "2026-09-05" }
```
`title`/`assignedDate` 필수.

**`HomeworkItemResponse`**
```json
{ "id": 20, "classId": 1, "className": "고1 수학반", "studentId": null, "studentName": null, "title": "1단원 문제집", "scope": "p.10~20", "assignedDate": "2026-09-01", "dueDate": "2026-09-05" }
```

**`HomeworkItemUpdateRequest`**: `{ "title": "...", "scope": "...", "assignedDate": "...", "dueDate": "..." }` (전부 선택)

**`HomeworkRecordBulkRequest`** (학생×숙제항목 매트릭스 일괄 저장):
```json
{
  "classId": 1,
  "items": [{ "homeworkItemId": 20, "records": [{ "studentId": 5, "isDone": true, "score": 90, "comment": null }] }]
}
```

**`HomeworkRecordResponse`**
```json
{ "id": 200, "homeworkItemId": 20, "homeworkItemTitle": "1단원 문제집", "assignedDate": "2026-09-01", "dueDate": "2026-09-05", "studentId": 5, "studentName": "김학생", "isDone": true, "score": 90, "comment": null }
```
미입력 학생은 `id: null, isDone: false`로 내려온다(`unchecked`).

---

## 10. 테스트 API

| Method | Path | 역할/소유권 |
|---|---|---|
| GET | `/v1/test-sessions?classId=` | admin, teacher(담당) |
| POST | `/v1/test-sessions` | 동일 |
| GET | `/v1/test-sessions/{id}/records` | 동일 (세션이 속한 반 기준) |
| POST | `/v1/test-records/bulk` | 동일 |
| GET | `/v1/students/{studentId}/tests?limit=10` | admin, teacher(담당), parent(자녀), student(본인) |

**`TestSessionCreateRequest`**: `{ "classId": 1, "title": "9월 1주 어휘테스트", "testDate": "2026-09-03" }` (전부 필수)

**`TestSessionResponse`**
```json
{ "id": 30, "classId": 1, "className": "고1 수학반", "title": "9월 1주 어휘테스트", "testDate": "2026-09-03" }
```

**`TestRecordBulkRequest`** (학생×영역 4열 매트릭스):
```json
{
  "testSessionId": 30,
  "records": [{ "studentId": 5, "subject": "vocab", "isTaken": true, "score": 18, "maxScore": 20, "comment": null }]
}
```

**`TestRecordResponse`**
```json
{ "id": 300, "testSessionId": 30, "testTitle": "9월 1주 어휘테스트", "testDate": "2026-09-03", "studentId": 5, "studentName": "김학생", "subject": "vocab", "isTaken": true, "score": 18, "maxScore": 20, "comment": null }
```

---

## 11. 월말모의고사 API

`MonthlyExam`(회차) 자체는 반과 무관한 학원 전체 자원 — 목록/생성은 역할 체크만, 성적/피드백은
학생 단위로 소유권을 체크합니다.

| Method | Path | 역할/소유권 |
|---|---|---|
| GET | `/v1/monthly-exams` | admin, teacher |
| POST | `/v1/monthly-exams` | admin, teacher |
| GET | `/v1/monthly-exams/{id}/records?classId=` | admin, teacher(담당 반) |
| POST | `/v1/monthly-exam-records` | admin, teacher(담당 학생) |
| PATCH | `/v1/monthly-exam-records/{id}` | admin, teacher(대상 학생 담당) |
| GET | `/v1/monthly-exam-records/{id}` | admin, teacher(담당), parent(자녀), student(본인) |
| GET | `/v1/students/{studentId}/monthly-exams?limit=5` | admin, teacher(담당), parent(자녀), student(본인) |
| POST | `/v1/monthly-exam-records/{id}/type-feedbacks` | admin, teacher(대상 학생 담당) |
| PATCH | `/v1/type-feedbacks/{id}` | admin, teacher(대상 학생 담당) |
| DELETE | `/v1/type-feedbacks/{id}` | admin, teacher(대상 학생 담당) |
| PUT | `/v1/monthly-exam-records/{id}/score-feedback` | admin, teacher(대상 학생 담당) |
| GET | `/v1/type-categories` | 로그인한 전체 역할 |
| POST | `/v1/type-categories` | admin |

**`MonthlyExamCreateRequest`**: `{ "examName": "9월 모의고사", "examMonth": "2026-09" }`
(`examMonth`는 `YYYY-MM` 정규식 검증)

**`MonthlyExamResponse`**: `{ "id": 1, "examName": "9월 모의고사", "examMonth": "2026-09" }`

**`MonthlyExamRecordCreateRequest`**
```json
{ "monthlyExamId": 1, "studentId": 5, "rawScore": 92, "stdScore": 130, "percentile": 88, "grade": "2" }
```
`monthlyExamId`/`studentId` 필수, 나머지 학원에서 사용하는 지표 선택 입력.

**`MonthlyExamRecordResponse`**
```json
{ "id": 400, "monthlyExamId": 1, "examName": "9월 모의고사", "examMonth": "2026-09", "studentId": 5, "studentName": "김학생", "rawScore": 92, "stdScore": 130, "percentile": 88, "grade": "2" }
```
미기록 학생은 `id: null`로 내려온다(`unrecorded`).

**`MonthlyExamRecordUpdateRequest`**: `{ "rawScore": .., "stdScore": .., "percentile": .., "grade": ".." }` (전부 선택)

**`GET /monthly-exam-records/{id}` Response** (`MonthlyExamRecordDetailResponse`)
```json
{
  "record": { "...MonthlyExamRecordResponse..." },
  "typeFeedbacks": [{ "id": 1, "typeCategoryId": 2, "typeCategory": "독해", "status": "needsWork", "feedbackText": "..." }],
  "scoreFeedback": { "scoreBand": "1등급", "feedbackText": "..." }
}
```

**`GET /students/{studentId}/monthly-exams` Response** — `MonthlyExamTrendResponse[]`:
`[{ "examMonth": "2026-08", "rawScore": 88 }, { "examMonth": "2026-09", "rawScore": 92 }]`
(그래프용 추이 — 최신순 `limit`개)

**`TypeFeedbackCreateRequest`**: `{ "typeCategoryId": 2, "status": "needsWork", "feedbackText": "..." }`
(`typeCategoryId`/`status` 필수). **`TypeFeedbackUpdateRequest`**: `{ "status": .., "feedbackText": .. }` (전부 선택).

**`TypeFeedbackResponse`**: `{ "id": 1, "typeCategoryId": 2, "typeCategory": "독해", "status": "needsWork", "feedbackText": "..." }`

**`ScoreFeedbackUpsertRequest`**: `{ "scoreBand": "1등급", "feedbackText": "..." }` (`scoreBand` 필수)
**`ScoreFeedbackResponse`**: `{ "scoreBand": "1등급", "feedbackText": "..." }`

**`TypeCategoryCreateRequest`**: `{ "name": "독해" }` / **`TypeCategoryResponse`**: `{ "id": 2, "name": "독해" }`

---

## 12. 공지사항 API

| Method | Path | 역할/소유권 |
|---|---|---|
| GET | `/v1/notices?scope=&classId=&limit=` | admin, teacher — 아래 참고 |
| POST | `/v1/notices` | admin, teacher(담당 반, scope=class일 때만) — scope=all은 admin 전용 |
| GET | `/v1/notices/{id}` | scope=class면 그 반과 관련 있는 사용자만(`canViewClassScopedContent`), scope=all이면 admin/teacher 역할만 통과(위 GET 목록과 동일 역할 제한) |
| PATCH | `/v1/notices/{id}` | scope=all: admin만. scope=class: **본인이 작성했고 + 지금도 담당인** teacher만 |
| DELETE | `/v1/notices/{id}` | 동일 |
| PATCH | `/v1/notices/{id}/pin` | 동일 |
| GET | `/v1/me/notices?limit=` | parent(자녀), student(본인) |

> **`GET /v1/notices` 필터링 규칙**: teacher가 `classId`를 지정하면 담당 반이 아닐 시 403. `classId`
> 없이 호출(전체 조회)하면 admin과 달리 teacher는 "scope=all 전체 + 본인이 담당하는 반들"만 걸러서
> 반환한다 — §15 각주의 "필터링이 아닌 거부" 원칙의 유일한 예외(목록 API 특성상 완전 거부보다
> 필터링이 자연스럽다고 판단).
>
> **수정/삭제/고정 규칙은 "본인이 작성한 담당 반 공지만"을 문자 그대로 강제** — 작성자 본인이어도
> 그 사이 반 담당이 바뀌면 더 이상 수정할 수 없다.

**`NoticeCreateRequest`**
```json
{ "scope": "class", "classId": 1, "title": "9월 시간표 안내", "content": "...", "isPinned": false }
```
`scope`/`title`/`content` 필수, `scope=class`면 `classId` 필수. 작성자는 토큰(`me.id()`)에서
가져오므로 요청 바디에 `authorId`를 받지 않는다.

**`NoticeResponse`**
```json
{ "id": 50, "authorId": 3, "authorName": "김선생", "scope": "class", "classId": 1, "className": "고1 수학반", "title": "9월 시간표 안내", "content": "...", "isPinned": false, "createdAt": "2026-09-01T09:00:00" }
```

**`NoticeUpdateRequest`**: `{ "title": "...", "content": "..." }` (둘 다 선택)
**`NoticePinUpdateRequest`**: `{ "isPinned": true }` (필수)

**`GET /v1/me/notices`**: `X-Active-Student-Id` 헤더(선택)로 부모의 활성 자녀를 지정. 생략하면
연결 순서상 **첫 번째 자녀**를 기본값으로 사용, 본인 자녀가 아닌 id를 지정하면
`403 FORBIDDEN_SCOPE`. student 역할은 헤더 무시하고 항상 본인 기준. 응답 형태는 `NoticeResponse[]`
(해당 학생과 관련된 공지: scope=all 전체 + 그 학생이 속한 반의 scope=class 공지).

---

## 13. 대시보드 API (`/v1/dashboard`)

| Method | Path | 역할 |
|---|---|---|
| GET | `/v1/dashboard/admin?date=` | admin |
| GET | `/v1/dashboard/teacher?date=` | teacher (본인 고정 — 쿼리로 다른 teacherId 지정 불가) |
| GET | `/v1/dashboard/classes?month=` | admin, teacher — FR-08, 명세서에 없는 추가 API(D 버킷) |

**`GET /admin` Response** (`AdminDashboardResponse`, `date` 생략 시 오늘)
```json
{
  "date": "2026-09-03", "totalStudentCount": 42, "todayAttendanceRate": 0.93,
  "todayHomeworkUncheckedCount": 5,
  "classes": [{ "classId": 1, "className": "고1 수학반", "teacherId": 3, "teacherName": "김선생", "studentCount": 12, "attendanceRate": 0.92, "homeworkCompletionRate": 0.83 }]
}
```

**`GET /teacher` Response** (`TeacherDashboardResponse`, `date` 생략 시 오늘 — v1.1 형태)
```json
{
  "myClasses": [{ "classId": 1, "className": "고1 수학반", "todayAttendanceRate": 0.92, "homeworkDoneRate": 0.83 }],
  "allClassesSummary": [{ "classId": 1, "className": "고1 수학반", "todayAttendanceRate": 0.92, "homeworkDoneRate": 0.83 }, { "classId": 2, "className": "고2 영어반", "todayAttendanceRate": null, "homeworkDoneRate": null }]
}
```
`myClasses`=로그인한 teacher의 담당 반, `allClassesSummary`=학원 전체 반 — 둘 다 같은 날짜 기준.
그날 출석/숙제 기록이 하나도 없는 반은 rate가 `null`.

**`GET /classes` Response** — `ClassStatisticsResponse[]` (형태는 `AdminDashboardResponse.classes`
원소와 동일, `month` 단위 집계).

---

## 14. 권한 매트릭스 전체 요약

역할 체크 위반은 `403 FORBIDDEN_ROLE`, 통과했지만 범위(담당 반/자녀/본인)를 벗어나면
`403 FORBIDDEN_SCOPE`로 구분됩니다(`AuthorizationService`, `academic.academic.global.security`).
전부 **응답 필터링이 아니라 요청 단계에서 거부**가 원칙이며, 유일한 예외는
[`GET /notices`의 목록 필터링](#12-공지사항-api)입니다.

| 자원 | admin | teacher | parent | student |
|---|---|---|---|---|
| 계정 관리(`/users/*`) | 전체 | ✗ | ✗ | ✗ |
| 반 관리 생성/수정/삭제 | 전체 | ✗ | ✗ | ✗ |
| 반 조회 | 전체 | 담당만 | ✗ | ✗ |
| 선생님 배정 관리 | 전체 | ✗ | ✗ | ✗ |
| 학부모-자녀 연결 관리 | 전체 | ✗ | ✗ | ✗ |
| 내 자녀 목록(`/me/children`) | ✗ | ✗ | 본인 | ✗ |
| 학생 생성/정보수정 | 전체 | ✗ | ✗ | ✗ |
| 학생 목록/개별 조회 | 전체 | 담당만 | ✗ / 자녀만(상세) | ✗ / 본인만(상세) |
| 학생 상세요약·알림배지·최근성적 | 전체 | 담당만 | 자녀만 | 본인만 |
| 출석/숙제/테스트/월말고사 입력(반 단위) | 전체 | 담당 반만 | ✗ | ✗ |
| 출석/숙제/테스트/월말고사 입력(학생 단위) | 전체 | 담당 학생만 | ✗ | ✗ |
| 출석/숙제/테스트/월말고사 학생별 조회 | 전체 | 담당만 | 자녀만 | 본인만 |
| 유형 카테고리 조회 | 전체 | 전체 | 전체 | 전체 |
| 유형 카테고리 생성 | 전체 | ✗ | ✗ | ✗ |
| 공지 scope=all 작성/수정/삭제 | 전체 | ✗ | ✗ | ✗ |
| 공지 scope=class 작성 | 전체 | 담당 반만 | ✗ | ✗ |
| 공지 scope=class 수정/삭제/고정 | 전체 | **본인 작성 + 담당 반**만 | ✗ | ✗ |
| 공지 목록/상세 조회 | 전체 | 전체(필터링됨)/관련 반만 | ✗ | ✗ |
| 내 공지(`/me/notices`) | ✗ | ✗ | 자녀 기준 | 본인 기준 |
| 대시보드(admin) | 전체 | ✗ | ✗ | ✗ |
| 대시보드(teacher, 본인 고정) | ✗ | 본인만 | ✗ | ✗ |
| 대시보드(classes, FR-08) | 전체 | 전체 | ✗ | ✗ |

---

## 15. 에러 코드

| code | HTTP status | 발생 상황 |
|---|---|---|
| `UNAUTHENTICATED` | 401 | 토큰 없음/무효/만료, 로그인 실패, refresh 토큰 재사용/폐기됨 |
| `FORBIDDEN_ROLE` | 403 | 역할 자체가 이 API를 쓸 수 없음 |
| `FORBIDDEN_SCOPE` | 403 | 역할은 맞지만 담당/자녀/본인 범위를 벗어남 |
| `VALIDATION_ERROR` | 422 | `@Valid` 실패, 필수 쿼리 파라미터 누락, 파라미터 타입 불일치, 비즈니스 규칙 위반(예: admin 역할로 계정 생성 시도, 만료된 재설정 토큰) |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `DUPLICATE_LOGIN_ID` | 409 | `loginId` 중복 |
| `INTERNAL_ERROR` | 500 | 그 외 미처리 예외 |

---

## 16. Enum 값 정리

모든 enum은 JSON 직렬화 시 아래의 소문자/카멜케이스 값을 사용합니다(`@JsonProperty`).

| Enum | 값 |
|---|---|
| `Role` | `admin`, `teacher`, `parent`, `student` |
| `AttendanceStatus` | `present`, `late`, `absent`, `earlyLeave`, `makeup` |
| `TestSubject` | `vocab`, `reading`, `grammar`, `syntax` |
| `NoticeScope` | `all`, `class` |
| `RelationType` | `father`, `mother`, `other` |
| `FeedbackStatus` | `strength`, `needsWork` |
| `StudentStatus` | `enrolled`, `paused`, `withdrawn` |

---

## 부록: 남은 갭 (D 버킷 — 명세서에 없지만 유지되는 추가 기능)

- `GET /v1/dashboard/classes`, `GET /v1/students/{id}/notifications/badge` — 명세서에 대응 엔드포인트가 없음(FR-08).
- `/notices`, `/students/{id}/summary`의 `limit` 쿼리 파라미터 — 명세서에 없지만 편의상 추가.
- `GET /students/{id}/summary`·`/notifications/badge`의 parent/student 권한 확장 — SCR-11(교사용) 외
  FR-07-01(학부모/학생 홈)도 겸용하기 위함.

자세한 배경은 `Document/스펙_변경_제안.md`, [[api-spec-compliance-audit]] 참고.
