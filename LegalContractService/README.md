# Legal Contract Service

Service quản lý hợp đồng pháp lý điện tử cho hệ thống chia sẻ xe điện.

## ⚠️ QUAN TRỌNG: Setup Database Trước Khi Chạy

### Cách 1: Chạy Script Setup (Khuyến nghị)

**Windows PowerShell:**
```powershell
cd LegalContractService
.\setup-database.ps1
```

**Windows CMD:**
```cmd
cd LegalContractService
setup-database.bat
```

**Linux/Mac:**
```bash
cd LegalContractService
mysql -u root -h localhost -P 3306 < ../legal_contract_database_setup.sql
```

### Cách 2: Tạo Database Thủ Công

1. Kết nối MySQL:
```bash
mysql -u root -p
```

2. Tạo database:
```sql
CREATE DATABASE IF NOT EXISTS legal_contract CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE legal_contract;
```

3. Chạy script setup:
```sql
SOURCE ../legal_contract_database_setup.sql;
```

Hoặc từ command line:
```bash
mysql -u root -p < ../legal_contract_database_setup.sql
```

### Cách 3: Dùng Docker (Tự động)

Nếu chạy với Docker Compose, database sẽ tự động được tạo:
```bash
docker-compose up -d legal-mysql legal-contract-service
```

## Khởi Động Service

Sau khi database đã được tạo:

```bash
mvn spring-boot:run
```

Hoặc:
```bash
mvn clean package
java -jar target/LegalContractService-0.0.1-SNAPSHOT.jar
```

## Cấu Hình

File cấu hình: `src/main/resources/application.properties`

- **Port:** 8089
- **Database:** legal_contract
- **Username:** root (mặc định)
- **Password:** (trống mặc định, sửa nếu cần)

## API Endpoints

- `GET /api/legalcontracts/all` - Lấy tất cả hợp đồng
- `GET /api/legalcontracts/{id}` - Lấy hợp đồng theo ID
- `GET /api/legalcontracts/group/{groupId}` - Lấy hợp đồng theo nhóm
- `GET /api/legalcontracts/status/{status}` - Lấy hợp đồng theo trạng thái
- `POST /api/legalcontracts/create` - Tạo hợp đồng mới
- `PUT /api/legalcontracts/update/{id}` - Cập nhật hợp đồng
- `PUT /api/legalcontracts/sign/{id}` - Ký hợp đồng
- `PUT /api/legalcontracts/archive/{id}` - Lưu trữ hợp đồng
- `DELETE /api/legalcontracts/{id}` - Xóa hợp đồng
- `GET /api/legalcontracts/{id}/history` - Lấy lịch sử hợp đồng
- `GET /api/legalcontracts/health` - Health check

## Giao Diện Admin

Truy cập: http://localhost:8080/admin/legal-contracts

## Xử Lý Lỗi

### Lỗi: "Unknown database 'legal_contract'"
**Giải pháp:** Chạy script setup database (xem phần Setup Database ở trên)

### Lỗi: "Access denied for user"
**Giải pháp:** Kiểm tra và sửa username/password trong `application.properties`

### Lỗi: "Connection refused"
**Giải pháp:** 
- Kiểm tra MySQL đã chạy: `mysql -u root -p`
- Kiểm tra port (mặc định 3306)
- Nếu dùng Docker: `docker ps` để kiểm tra container

## Cấu Trúc Database

- **LegalContract**: Bảng hợp đồng chính
- **ContractHistory**: Lịch sử thay đổi hợp đồng  
- **ContractSignatures**: Chữ ký hợp đồng
- **CheckInOutLog**: Log check-in/check-out

