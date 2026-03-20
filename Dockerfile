FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
# Biên dịch code thành file .jar (bỏ qua chạy thửtest để build nhanh)
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Chỉ lấy file .jar từ builder (GĐ1)
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
# Lệnh "bật công tắc" để chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]