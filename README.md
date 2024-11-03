# 메일 서버

SMTP와 IMAP 프로토콜을 구현한 간단한 메일 서버입니다.

## 요구사항

- Java 21 이상
- Gradle 8.5 이상

## 빌드 방법

```bash
./gradlew clean build
# 빌드 후 실행(빌드파일이 있는 위치에서)
java -jar mail-1.0.jar
```

## 서버 정보

- SMTP 서버: 포트 25
- IMAP 서버: 포트 143

## 기능

- SMTP 서버
  - 로컬 도메인(yeop.site)으로 오는 메일 저장
  - 외부 도메인으로 가는 메일 릴레이
- IMAP 서버
  - 기본적인 IMAP 명령어 지원 (LOGIN, LIST, SELECT, FETCH)
  - 메일박스 조회 및 메일 내용 확인

## 메일 저장 위치

메일은 `mailbox/` 디렉토리 아래에 사용자별로 저장됩니다.
예: `mailbox/user_at_yeop.site/`
