# FR-01 ~ FR-09 Postman 테스트 가이드

`Academic-FR01-FR09.postman_collection.json`으로 학생/반/계정(FR-01), 출석(FR-02), 숙제(FR-03),
테스트(FR-04), 월말모의고사(FR-05), 대시보드(FR-06), 학부모/학생 조회 화면(FR-07), 알림 배지(FR-08),
공지사항(FR-09) API를 처음부터 끝까지 수동으로 확인할 수 있습니다.

이 컬렉션은 실제로 Docker Postgres + `bootRun`으로 띄운 서버에 대해 **newman으로 76개 요청 전부
통과를 확인**했으며(2026-09-02 기준), 같은 DB에 대해 반복 재실행해도 전부 통과하는 것까지
확인했습니다(유형 카테고리 같은 마스터 데이터의 get-or-create 처리 포함).

API 명세서와 코드를 대조해 빠져 있던 4가지(`POST /users/{id}/reset-password`, `PATCH
/parent-links/{id}`, 학부모 부/모 구분(`relationType`), 학생 등록 시 본인 로그인 계정 자동 발급)를
2026-09-01에 추가로 구현하면서 이 컬렉션에도 반영했습니다 — FR-01 폴더의 `[User] 비밀번호 초기화`,
`[ParentStudent] 부모 구분 수정` 요청과, 학생 등록/학부모-자녀 연결 요청 바디의 `account`,
`relationType` 필드가 그것입니다.

2026-09-02(오전)에는 `GET /dashboard/teacher` 응답 구조를 명세서와 완전히 일치시켰습니다. 기존에는
"담당 반 미체크 항목 개수"(`classes[].attendanceUncheckedCount` 등) 방식이었는데, 명세서 예시는
`myClasses`/`allClassesSummary` 배열에 반별 `todayAttendanceRate`/`homeworkDoneRate`(0~1 스케일
비율)를 담는 방식이라 이쪽으로 교체했습니다.

2026-09-02(오후)에는 **실제 인증(JWT + Refresh Token, RBAC + 소유권 체크)을 구현**하면서 컬렉션
전체를 로그인 기반으로 다시 짰습니다. 자세한 내용은 아래 "0. 인증" 및 "5. 알려진 제약 사항"을
참고하세요 — 특히 **admin 계정을 미리 DB에 심어둬야** 컬렉션이 처음부터 돌아갑니다(2번 항목).

## 1. 사전 준비: 서버 띄우기

이 프로젝트는 `application.yml`에서 실제 PostgreSQL을 바라보도록 되어 있고(H2가 아님), JWT 기반
인증(Access/Refresh Token)과 역할·소유권 기반 접근 제어가 구현되어 있어 로그인 없이는 대부분의
API를 호출할 수 없습니다(로그인/토큰재발급/비밀번호 재설정 요청 4개만 예외).

### 1) Docker로 PostgreSQL 띄우기

```bash
docker run -d --name academic-postgres \
  -e POSTGRES_DB=academic \
  -e POSTGRES_USER=ujs \
  -e POSTGRES_PASSWORD=academic1234 \
  -p 5432:5432 \
  postgres:16
```

Docker Desktop이 꺼져 있으면 먼저 실행하세요. (Windows: `C:\Program Files\Docker\Docker\Docker Desktop.exe`)

이미 컨테이너를 만들어 둔 적이 있다면 재실행 시:

```bash
docker start academic-postgres
```

### 2) 서버 실행

```bash
DB_PASSWORD=academic1234 ./gradlew bootRun
```

- `http://localhost:8080`에서 뜹니다.
- devtools가 포함돼 있어 코드를 고치고 `./gradlew compileJava`만 실행해도 서버가 자동 재시작됩니다
  (서버를 매번 껐다 켤 필요 없음).
- 살아있는지 확인: `curl http://localhost:8080/v1/dashboard/admin` → 토큰이 없어서
  `{"error":{"code":"UNAUTHENTICATED",...}}`가 나오면 정상(= 인증이 걸려 있다는 뜻). 서버 자체가
  죽었으면 `curl: (7) Failed to connect`가 뜹니다.

### 3) admin 계정 시드 (필수 — 최초 1회)

`POST /v1/users`는 `role`이 `teacher`/`parent`만 허용합니다(admin은 API로 생성 불가, FR-01 기존
정책). 그런데 이제 인증이 걸려 있어서 컬렉션의 첫 요청(`[Auth] 관리자 로그인`)부터 admin 계정이
있어야 통과합니다. 아래 명령으로 DB에 직접 심어두세요(비밀번호는 `Admin1234!`로 미리 bcrypt
해시를 만들어 뒀습니다 — 컬렉션의 `adminLoginId`/`adminPassword` 변수와 값이 일치해야 합니다):

```bash
docker exec academic-postgres psql -U ujs -d academic -c \
  "INSERT INTO users (name, role, login_id, password_hash, active, created_at) VALUES ('원장','ADMIN','admin1','\$2a\$10\$9EDC5fefV91xBZWZQgPiueaL.vZLdBcuUMHoO7O0DIrHDGinXhxNm',true, now()) ON CONFLICT (login_id) DO UPDATE SET password_hash = EXCLUDED.password_hash, active = true;"
```

이미 `admin1`이 다른 비밀번호로 존재해도 `ON CONFLICT`로 비밀번호가 위 값으로 덮어써지니 걱정하지
않아도 됩니다. 다른 비밀번호를 쓰고 싶다면 직접 bcrypt 해시를 만들어 컬렉션 변수 `adminPassword`와
함께 바꾸면 됩니다(`BCryptPasswordEncoder`로 인코딩한 값이면 됩니다).

## 2. Postman에 컬렉션 임포트

1. Postman 실행 → **Import** → `postman/Academic-FR01-FR09.postman_collection.json` 선택
2. 컬렉션이 `학원 학생관리 시스템 - FR-01~FR-09` 이름으로 들어옵니다.
3. 컬렉션 변수(`baseUrl` 등)는 파일에 이미 채워져 있어 별도 Postman Environment를 만들 필요가 없습니다.
   - 서버 주소가 다르면 컬렉션 선택 → **Variables** 탭에서 `baseUrl` 값만 바꾸면 됩니다.

## 3. 실행 방법

### 전체 순서대로 한 번에 (권장)

컬렉션 우클릭 → **Run collection** (Collection Runner) → 전체 선택 후 Run.
폴더 순서(`0. 인증` → FR-01 → FR-02 → ... → FR-08 → FR-09 → `0. 정리(삭제) 예시`)대로 실행되며, 각
단계에서 만든 `teacherId`, `classId`, `studentId`, `adminToken`/`teacherToken`/`parentToken`/
`studentToken` 등을 뒤 단계가 자동으로 이어받습니다(컬렉션 변수에 저장/재사용). `0. 인증` 폴더는
admin 로그인 하나뿐이고, 나머지 3개 로그인(`[Auth] 선생님/학부모/학생 로그인`)은 각 계정이
만들어지는 시점(FR-01 폴더 중간)에 바로 이어서 실행되도록 배치되어 있습니다.

### 요청 하나씩 눈으로 확인하고 싶을 때

폴더를 펼쳐서 위에서부터 순서대로 **Send**를 누르면 됩니다. 순서를 건너뛰면 뒤 요청이 실패할 수
있습니다 (예: `classId`가 없는 상태에서 학생 등록을 호출하면 404).

### 재실행(reset 없이 여러 번 돌리기)

`[User] 선생님/학부모 계정 생성` 요청의 `loginId`는 `teacher_{{$timestamp}}`처럼 매 실행마다 자동으로
달라지게 되어 있어, DB를 초기화하지 않고 컬렉션을 여러 번 다시 실행해도 로그인 아이디 중복
(`409 DUPLICATE_LOGIN_ID`) 없이 새 데이터가 계속 쌓입니다.

`[TypeCategory] 유형 카테고리 생성`(FR-05)은 반대로 학원 전체가 공유하는 마스터 데이터라 이름을
매번 다르게 만들지 않습니다. 대신 테스트 스크립트가 "이미 존재하면(422) 목록에서 같은 이름을 찾아
그 id를 재사용"하도록 짜여 있어(get-or-create), 몇 번을 재실행해도 안전합니다.

DB를 완전히 비우고 처음부터 하고 싶다면:

```bash
docker rm -f academic-postgres
# 위 "1) Docker로 PostgreSQL 띄우기" 명령을 다시 실행 (ddl-auto: update라 테이블은 서버가 자동 생성)
```

DB를 비우면 admin 계정도 함께 사라지므로, 다시 실행하기 전에 **"1.3) admin 계정 시드"를 한 번 더
실행**해야 컬렉션의 첫 요청(`[Auth] 관리자 로그인`)이 통과합니다.

## 4. 컬렉션 구조

| 폴더 | 대응 요구사항 | 내용 |
|---|---|---|
| 0. 인증 (로그인) | REQ-AUTH-01·06, API 명세서 §3 | admin 로그인(`POST /v1/auth/login`) → `adminToken` 저장. 선생님/학부모/학생 로그인은 각 계정이 생성되는 시점에 맞춰 FR-01 폴더 중간에 배치되어 있습니다(`teacherToken`/`parentToken`/`studentToken`) |
| FR-01. 학생/반/계정 관리 | FR-01-01~07 | 선생님/학부모 계정 생성·비밀번호 초기화, 반 생성, 학생 등록(본인 로그인 계정 자동 발급 포함), 반 배정, 학부모-자녀 연결(부/모 구분 relationType, 수정 포함), 자녀 목록 조회(`GET /v1/me/children`, 학부모 토큰) |
| FR-02. 출석 관리 | FR-02-01~06 | 반 단위 출석 일괄 저장, 조회, 수정(선생님 토큰), 학생 월별 조회(학부모 토큰) |
| FR-03. 숙제 관리 | FR-03-01~07 | 숙제 항목 생성/수정, 학생×항목 매트릭스 일괄 저장, 조회(선생님 토큰), 학생 숙제 이력 조회(학생 토큰) |
| FR-04. 테스트 관리 | FR-04-01~06 | 테스트 회차 생성, 학생×4영역 매트릭스 일괄 저장, 조회(선생님 토큰), 학생 테스트 이력 조회(학생 토큰) |
| FR-05. 월말모의고사 관리 | FR-05-01~08 | 회차/유형카테고리 생성, 성적 등록·수정, 유형별/점수대별 피드백(선생님 토큰), 회차 상세(학부모 토큰), 성적 추이(학생 토큰) |
| FR-06. 대시보드 | FR-06-01~03 | 관리자 대시보드(admin 토큰), 선생님 대시보드(myClasses/allClassesSummary 출석률·숙제완료율, 선생님 토큰), 반별 통계 |
| FR-07. 학부모/학생 조회 화면 | FR-07-01 | 학생 홈 요약(`GET /v1/students/{id}/summary`, 학생 토큰) — 최근 출석/숙제/테스트/월말모의고사를 한 번에 조회. SCR-11(선생님용 학생 상세)에도 같은 API 사용 |
| FR-08. 알림 (선택 기능) | FR-08-01 | 알림 배지(`GET /v1/students/{id}/notifications/badge?since=`, 학생 토큰) — since 이후 새로 생긴 출석/숙제/테스트/월말모의고사 건수 집계. 서버는 읽음 상태를 저장하지 않고(stateless), 클라이언트가 마지막 확인 시각을 매번 넘기는 방식 |
| FR-09. 공지사항 관리 | FR-09-01~06 | 반별 공지 작성/수정/삭제, 상단 고정(pin) 토글(선생님 토큰), 목록 필터(scope/classId), 전체(all) 공지 작성(admin 토큰, 성공 케이스 포함) 및 실패 케이스(선생님 시도 시 422), 학생에게 노출되는 공지 목록 조회(`GET /v1/me/notices`, 학생 토큰) |
| 0. 정리(삭제) 예시 | - | DELETE류 API 예시. 맨 마지막에 선택 실행 (반 삭제는 소속 학생이 있어 **422가 정상**입니다 — 삭제 제약 확인용) |

각 요청에는 상태 코드를 검증하는 테스트 스크립트가 붙어 있어, Collection Runner로 돌리면
초록/빨강으로 바로 성공 여부가 보입니다.

## 5. 알려진 제약 사항

- **인증(JWT + Refresh Token, RBAC + 소유권 체크) 구현 완료 (2026-09-02)**: 요구사항 명세서
  REQ-AUTH-01~06, API 명세서 §3(인증 API)·§15(권한 매트릭스)가 이제 실제로 서버에서 강제됩니다.
  모든 API는 `Authorization: Bearer {{accessToken}}` 헤더가 있어야 하며(로그인/토큰재발급 2개만
  예외), 역할(admin/teacher/parent/student)과 소유권(담당 반·자녀·본인)이 맞지 않으면
  403(`FORBIDDEN_ROLE`/`FORBIDDEN_SCOPE`)이 돌아옵니다. API 명세서의 `/me/children`,
  `/me/notices`도 이제 명세서 그대로 `GET /v1/me/children`, `GET /v1/me/notices`로 구현되어 있고
  (로그인 토큰에서 신원을 가져옴), "선생님 본인이 작성한 담당 반 공지만 수정/삭제 가능" 같은 세부
  권한도 서버에서 강제합니다.
- **비밀번호 재설정(REQ-AUTH-07)은 이메일 없이 두 경로만 지원 (2026-09-04)**: 로그인 상태에서 현재
  비밀번호를 아는 사용자는 `POST /v1/auth/password/change`(본인, 현재 비밀번호 확인 후 변경)를,
  비밀번호를 완전히 잊은 경우는 `POST /v1/users/{id}/reset-password`(admin 전용 강제 초기화, 임시
  비밀번호를 응답에 1회 노출)를 씁니다. 이메일 발송 인프라 없이 토큰을 응답에 노출하던 자가 재설정
  임시조치(`POST /auth/password/reset-request`/`reset`)는 제거했습니다.
- **FR-09 전체(all) 공지도 이제 자동 플로우에 포함**: admin 로그인이 가능해지면서(위 "1.3) admin
  계정 시드" 참고) `[Notice] 전체(all) 공지 작성 (관리자)` 요청으로 성공 케이스까지 컬렉션에 포함
  시켰습니다. "선생님이 전체 공지를 시도하면 422"라는 실패 케이스(FR-09-01/02 권한 규칙)도 그대로
  유지됩니다.
- **FR-08은 "선택" 기능이자 stateless 설계**: 요구사항 명세서에 "(선택) ... 알림 배지로 표시할 수
  있어야 한다"로 명시돼 있고, API 명세서에는 대응 엔드포인트가 아예 없어 직접 설계했습니다. 서버가
  "읽음/안읽음" 상태를 저장하는 대신, 클라이언트가 마지막으로 확인한 시각(`since`)을 매번 파라미터로
  넘기고 서버는 그 이후 생성된 항목 수만 세어 돌려줍니다. DB 스키마 변경이 없어 가장 가벼운 방식이며,
  더 정교한(서버가 읽음 상태를 직접 저장하는) 버전이 필요해지면 별도 마이그레이션이 필요합니다.
- **테스트 실행(testUncheckedCount 등) 확인**: FR-06 대시보드의 `testUncheckedCount`를 보려면
  FR-04 폴더의 테스트 회차 생성까지 실행한 뒤 대시보드를 호출해야 값이 채워집니다(컬렉션 순서상 이미
  그렇게 되어 있음).
- **FR-07은 API 하나뿐**: FR-07-02(탭 구분)·FR-07-03(반응형)은 프론트엔드 화면 요구사항이라 이
  백엔드(Academic_BE) 저장소 범위 밖입니다. FR-07-04(저장 즉시 반영)는 서버에 캐싱이 없어 별도 구현
  없이 이미 충족됩니다. 그래서 컬렉션에는 FR-07-01에 대응하는
  `GET /v1/students/{id}/summary` 하나만 있습니다.

## 6. 트러블슈팅

| 증상 | 원인 / 조치 |
|---|---|
| `curl: (7) Failed to connect` / Postman에서 `ECONNREFUSED` | 서버(`bootRun`)가 안 떠 있음. 1번부터 다시 확인 |
| 서버 로그에 `FATAL: password authentication failed` | `DB_PASSWORD` 환경변수가 Docker 컨테이너 생성 시 준 비밀번호와 다름 |
| `POST /v1/students`가 500 | 한글이 포함된 요청 바디를 curl로 직접 보낼 때 터미널 인코딩이 깨지는 경우가 있음(Postman은 문제 없음). curl로 테스트할 땐 UTF-8로 저장한 JSON 파일을 `--data-binary @file`로 보낼 것 |
| 컬렉션 재실행 시 `DUPLICATE_LOGIN_ID` | `{{$timestamp}}` 치환이 안 된 경우. Postman 최신 버전인지 확인하거나 loginId를 직접 유니크하게 수정 |
| `[Auth] 관리자 로그인`이 401(`UNAUTHENTICATED`) | admin 계정이 DB에 없거나 비밀번호가 다름. "1.3) admin 계정 시드" 명령을 다시 실행 |
| 다른 요청들이 전부 401(`UNAUTHENTICATED`) | 앞선 로그인 요청(`[Auth] ...`)이 실패했거나 건너뛴 것. Collection Runner로 순서대로 실행했는지, 로그인 요청들이 초록(성공)이었는지 확인 |
| 특정 요청만 403(`FORBIDDEN_ROLE`/`FORBIDDEN_SCOPE`) | 그 요청에 걸린 토큰 변수(`{{teacherToken}}` 등)가 실제 필요한 역할·소유권과 맞는지 확인. 예: 담당하지 않은 반(`classId`)에 선생님 토큰으로 접근하면 `FORBIDDEN_SCOPE`가 정상 동작임 |
| `GET /v1/students`, `GET /v1/homework-items`가 500이고 로그에 `could not determine data type of parameter` 또는 `bytea` 관련 에러 | 2026-09-01에 발견·수정된 PostgreSQL 전용 버그입니다. 최신 코드인지(`StudentRepository`, `HomeworkItemRepository`) 확인하세요 |
