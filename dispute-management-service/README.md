# Dispute Management Service

Service quản lý tranh chấp cho hệ thống đồng sở hữu xe điện.

## Cấu hình

- **Port**: 8090
- **Database**: Dispute_Management_DB (MySQL)
- **Database Port**: 3314 (Docker) hoặc 3306 (Local)

## Chạy Service

### 1. Chạy Local (Development)

**Yêu cầu:**
- Java 21
- Maven
- MySQL đang chạy

**Bước 1: Tạo Database**
```sql
source dispute_database_setup.sql
```

**Bước 2: Cập nhật application.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/Dispute_Management_DB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
```

**Bước 3: Chạy Service**
```bash
mvn spring-boot:run
```

### 2. Chạy với Docker

```bash
# Build và chạy tất cả services
docker-compose up -d dispute-mysql dispute-management-service

# Xem logs
docker logs -f dispute-management-service
```

## API Endpoints

### Health Check
```bash
GET http://localhost:8090/api/disputes/health
```

### Disputes
```bash
# Lấy tất cả tranh chấp
GET http://localhost:8090/api/disputes

# Lấy tranh chấp theo ID
GET http://localhost:8090/api/disputes/{id}

# Tạo tranh chấp mới
POST http://localhost:8090/api/disputes
Content-Type: application/json

{
  "groupId": 1,
  "createdBy": 1,
  "title": "Tranh chấp về sử dụng xe",
  "description": "Mô tả chi tiết...",
  "category": "RESERVATION",
  "priority": "HIGH"
}

# Cập nhật tranh chấp
PUT http://localhost:8090/api/disputes/{id}

# Giao tranh chấp cho staff
PUT http://localhost:8090/api/disputes/{id}/assign
Content-Type: application/json

{
  "staffId": 1
}
```

### Comments
```bash
# Lấy bình luận
GET http://localhost:8090/api/disputes/{id}/comments?includeInternal=false

# Thêm bình luận
POST http://localhost:8090/api/disputes/{id}/comments
Content-Type: application/json

{
  "userId": 1,
  "userRole": "ADMIN",
  "content": "Bình luận...",
  "isInternal": false
}
```

### Resolution
```bash
# Lấy giải pháp
GET http://localhost:8090/api/disputes/{id}/resolution

# Tạo giải pháp
POST http://localhost:8090/api/disputes/{id}/resolution
Content-Type: application/json

{
  "resolvedBy": 1,
  "resolutionType": "ACCEPTED",
  "resolutionDetails": "Chi tiết giải pháp...",
  "actionTaken": "Hành động đã thực hiện",
  "compensationAmount": 0
}
```

### Statistics
```bash
GET http://localhost:8090/api/disputes/statistics
```

## Qua API Gateway

Tất cả requests nên đi qua API Gateway (port 8084):

```bash
# Health check qua Gateway
GET http://localhost:8084/api/disputes/health

# Lấy tất cả tranh chấp
GET http://localhost:8084/api/disputes
```

## Kiểm tra Service

### 1. Health Check
```bash
curl http://localhost:8090/api/disputes/health
```

### 2. Test qua API Gateway
```bash
curl http://localhost:8084/api/disputes/health
```

### 3. Xem logs
```bash
# Docker
docker logs dispute-management-service

# Local
# Xem console output
```

## Troubleshooting

### Lỗi kết nối Database
- Kiểm tra MySQL đang chạy
- Kiểm tra username/password trong application.properties
- Kiểm tra database đã được tạo chưa

### Port đã được sử dụng
- Kiểm tra port 8090 có đang được dùng không
- Đổi port trong application.properties nếu cần

### Lỗi compile
- Đảm bảo Java 21 đã được cài đặt
- Chạy `mvn clean install` để rebuild

## Cấu trúc Database

- `Dispute` - Tranh chấp chính
- `DisputeComment` - Bình luận
- `DisputeResolution` - Giải pháp
- `DisputeHistory` - Lịch sử thay đổi
- `DisputeAttachment` - File đính kèm

Xem chi tiết trong `dispute_database_setup.sql`

