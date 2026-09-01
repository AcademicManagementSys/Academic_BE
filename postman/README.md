# FR-01 ~ FR-09 Postman 테스트 가이드

`Academic-FR01-FR09.postman_collection.json`으로 학생/반/계정(FR-01), 출석(FR-02), 숙제(FR-03),
테스트(FR-04), 월말모의고사(FR-05), 대시보드(FR-06), 학부모/학생 조회 화면(FR-07), 알림 배지(FR-08),
공지사항(FR-09) API를 처음부터 끝까지 수동으로 확인할 수 있습니다.

이 컬렉션은 실제로 Docker Postgres + `bootRun`으로 띄운 서버에 대해 **newman으로 69개 요청 전부
통과를 확인**했으며(2026-09-01 기준), 같은 DB에 대해 반복 재실행해도 전부 통과하는 것까지
확인했습니다(유형 카테고리 같은 마스터 데이터의 get-or-create 처리 포함).

## 1. 사전 준비: 서버 띄우기

이 프로젝트는 `application.yml`에서 실제 PostgreSQL을 바라보도록 되어 있고(H2가 아님),
인증/보안(JWT 등)은 아직 구현되어 있지 않아 토큰 없이 바로 API를 호출할 수 있습니다.

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
- 살아있는지 확인: `curl http://localhost:8080/v1/dashboard/admin` → `{"data":{...}}` 형태가 나오면 정상.

## 2. Postman에 컬렉션 임포트

1. Postman 실행 → **Import** → `postman/Academic-FR01-FR09.postman_collection.json` 선택
2. 컬렉션이 `학원 학생관리 시스템 - FR-01~FR-09` 이름으로 들어옵니다.
3. 컬렉션 변수(`baseUrl` 등)는 파일에 이미 채워져 있어 별도 Postman Environment를 만들 필요가 없습니다.
   - 서버 주소가 다르면 컬렉션 선택 → **Variables** 탭에서 `baseUrl` 값만 바꾸면 됩니다.

## 3. 실행 방법

### 전체 순서대로 한 번에 (권장)

컬렉션 우클릭 → **Run collection** (Collection Runner) → 전체 선택 후 Run.
폴더 순서(FR-01 → FR-02 → ... → FR-08 → FR-09 → `0. 정리(삭제) 예시`)대로 실행되며, 앞 단계에서 만든
`teacherId`, `classId`, `studentId` 등을 뒤 단계가 자동으로 이어받습니다(컬렉션 변수에 저장/재사용).

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

## 4. 컬렉션 구조

| 폴더 | 대응 요구사항 | 내용 |
|---|---|---|
| FR-01. 학생/반/계정 관리 | FR-01-01~07 | 선생님/학부모 계정 생성, 반 생성, 학생 등록, 반 배정, 학부모-자녀 연결 |
| FR-02. 출석 관리 | FR-02-01~06 | 반 단위 출석 일괄 저장, 조회, 수정, 학생 월별 조회 |
| FR-03. 숙제 관리 | FR-03-01~07 | 숙제 항목 생성/수정, 학생×항목 매트릭스 일괄 저장, 조회 |
| FR-04. 테스트 관리 | FR-04-01~06 | 테스트 회차 생성, 학생×4영역 매트릭스 일괄 저장, 조회 |
| FR-05. 월말모의고사 관리 | FR-05-01~08 | 회차/유형카테고리 생성, 성적 등록·수정, 유형별/점수대별 피드백 |
| FR-06. 대시보드 | FR-06-01~03 | 관리자 대시보드, 선생님 대시보드(체크리스트+전체 반 통계), 반별 통계 |
| FR-07. 학부모/학생 조회 화면 | FR-07-01 | 학생 홈 요약(`GET /v1/students/{id}/summary`) — 최근 출석/숙제/테스트/월말모의고사를 한 번에 조회. SCR-11(선생님용 학생 상세)에도 같은 API 사용 |
| FR-08. 알림 (선택 기능) | FR-08-01 | 알림 배지(`GET /v1/students/{id}/notifications/badge?since=`) — since 이후 새로 생긴 출석/숙제/테스트/월말모의고사 건수 집계. 서버는 읽음 상태를 저장하지 않고(stateless), 클라이언트가 마지막 확인 시각을 매번 넘기는 방식 |
| FR-09. 공지사항 관리 | FR-09-01~06 | 반별 공지 작성/수정/삭제, 상단 고정(pin) 토글, 목록 필터(scope/classId), 학생에게 노출되는 공지 조회(`GET /v1/students/{id}/notices`). 전체(all) 공지 작성 실패(선생님 시도 시 422) 케이스 포함. 전체 공지 성공 케이스는 관리자 계정이 있어야 하는데 관리자는 API로 못 만들어서 자동 플로우엔 없음 — 아래 "알려진 제약 사항" 참고 |
| 0. 정리(삭제) 예시 | - | DELETE류 API 예시. 맨 마지막에 선택 실행 (반 삭제는 소속 학생이 있어 **422가 정상**입니다 — 삭제 제약 확인용) |

각 요청에는 상태 코드를 검증하는 테스트 스크립트가 붙어 있어, Collection Runner로 돌리면
초록/빨강으로 바로 성공 여부가 보입니다.

## 5. 알려진 제약 사항

- **인증 미구현**: 요구사항 명세서 3장의 RBAC(원장/선생님/학부모/학생 권한 분리)와 API 명세서의
  `/auth/login` 등은 아직 서버에 구현되어 있지 않습니다. 지금은 `teacherId`, `studentId` 같은
  파라미터를 직접 넘겨서 호출합니다. 인증이 추가되면 이 컬렉션도 로그인 요청과
  `Authorization: Bearer {{accessToken}}` 헤더를 추가해야 합니다.
  API 명세서의 `/me/children`, `/me/notices`처럼 "로그인한 사용자" 기준 경로도 같은 이유로
  `/v1/parents/{parentUserId}/children`, `/v1/students/{id}/notices`처럼 id를 직접 명시하는
  경로로 대체했습니다. 같은 이유로 "선생님 본인이 작성한 공지만 수정/삭제 가능" 같은 세부 권한도
  아직 서버에서 강제하지 않습니다(다른 도메인의 수정/삭제 API들과 동일한 수준).
- **FR-09 전체(all) 공지는 API로 못 만든 관리자 계정이 필요**: `POST /v1/users`는 정책상 `role`이
  `teacher`/`parent`만 허용합니다(admin은 API로 생성 불가 — FR-01의 기존 설계). 그래서 전체 공지
  "성공" 케이스는 Postman 컬렉션에 넣지 못했고, 대신 "선생님이 전체 공지를 시도하면 422"라는 실패
  케이스로 그 권한 규칙(FR-09-01/02: 전체 공지는 원장/관리자만)을 확인합니다. 전체 공지 생성까지 직접
  확인하려면 DB에 admin 계정을 하나 심어야 합니다:
  ```bash
  docker exec academic-postgres psql -U ujs -d academic -c \
    "INSERT INTO users (name, role, login_id, password_hash, active, created_at) VALUES ('원장','ADMIN','admin1','\$2a\$dummy',true, now());"
  ```
  그 후 `POST /v1/notices`에 그 계정의 `id`를 `authorId`로, `scope`를 `"all"`로 넣어 호출하면 됩니다.
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
| `GET /v1/students`, `GET /v1/homework-items`가 500이고 로그에 `could not determine data type of parameter` 또는 `bytea` 관련 에러 | 2026-09-01에 발견·수정된 PostgreSQL 전용 버그입니다. 최신 코드인지(`StudentRepository`, `HomeworkItemRepository`) 확인하세요 |
