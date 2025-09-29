# =================
# 1. Build Stage
# =================
# Gradle과 JDK 21을 포함한 이미지를 빌드 환경으로 사용
FROM gradle:8.5.0-jdk21 AS builder

# 소스 코드를 컨테이너의 /app 디렉토리로 복사
WORKDIR /app
COPY . .

# Gradle을 사용하여 실행 가능한 .war 파일 빌드 (테스트는 생략)
# --no-daemon 옵션은 CI/CD 환경에서 빌드 후 프로세스가 깔끔하게 종료되도록 보장
RUN gradle bootWar -x test --no-daemon


# =================
# 2. Final Stage
# =================
# JRE 21 이미지를 기반으로 최종 실행 환경 구성
FROM eclipse-temurin:21-jre

# 빌드 스테이지에서 생성된 .war 파일을 /app 디렉토리로 복사
WORKDIR /app
COPY --from=builder /app/build/libs/*.war app.war

# 애플리케이션 포트 노출
EXPOSE 6736

# 컨테이너 실행 시 Spring Profile을 받아 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java -jar app.war --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
