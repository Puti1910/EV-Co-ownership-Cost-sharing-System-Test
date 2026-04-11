# 📋 Hướng dẫn Test API với Postman

## 🚀 Cách Import Collection vào Postman

1. Mở **Postman**
2. Click **Import** (góc trên bên trái)
3. Chọn tab **File**
4. Upload file: `EV_Reservation_System.postman_collection.json`
5. ✅ Xong!

---

## 🔐 Admin Login (Quan trọng!)

### Endpoint: `POST http://localhost:8087/api/admin/auth/login`

**Lưu ý về lỗi 404:**
- ❌ **SAI**: `//api/admin/auth/login` (có double slash)
- ✅ **ĐÚNG**: `http://localhost:8087/api/admin/auth/login`

### Request Body:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Response (Thành công):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin"
}
```

### ⚠️ Nếu gặp lỗi 401 Unauthorized:
- Username/password không đúng hoặc user chưa tồn tại trong database
- Cần kiểm tra database `co_ownership_booking_admin` trong bảng `users`
- Hoặc tạo user mới trong database

---

## 📝 Cấu trúc Collection

### 0. ADMIN AUTH (Port 8087)
- **0.1 Admin Login** - Đăng nhập để lấy JWT token

### 1. RESERVATION SERVICE (Port 8086)
- **1.1** Get All Reservations
- **1.2** Get Reservations By Vehicle ID
- **1.3** Get User Vehicle Reservations
- **1.4** Get User Vehicles
- **1.5** Get Vehicle Group Info
- **1.6** Check Availability
- **1.7** Create Reservation
- **1.8** Update Reservation
- **1.9** Update Reservation Status
- **1.10** Get Reservation By ID
- **1.11** Delete Reservation

### 2. RESERVATION ADMIN SERVICE (Port 8087)
- **2.1** Admin: Get All Reservations
- **2.2** Admin: Get Reservation By ID
- **2.3** Admin: Update Reservation
- **2.4** Admin: Delete Reservation
- **2.5** Admin: Sync Reservation

### 3. RESERVATION ADMIN MANAGE (Port 8087)
- **3.1** Manage: Get All Reservations
- **3.2** Manage: Create Reservation
- **3.3** Manage: Update Reservation
- **3.4** Manage: Delete Reservation

---

## 🔧 Sử dụng JWT Token

Sau khi login thành công, bạn sẽ nhận được JWT token. Để sử dụng token cho các API khác:

1. Copy token từ response
2. Thêm vào **Headers** của request:
   - Key: `Authorization`
   - Value: `Bearer <your-token>`

Hoặc trong Postman:
- Vào tab **Authorization**
- Chọn type: **Bearer Token**
- Paste token vào

---

## 📊 Trạng thái Reservation

- `BOOKED` - Đã đặt
- `IN_USE` - Đang sử dụng
- `COMPLETED` - Hoàn thành
- `CANCELLED` - Đã hủy

---

## 🐛 Troubleshooting

### Lỗi 404 Not Found
- ✅ Kiểm tra URL đúng: `http://localhost:8087/api/admin/auth/login`
- ❌ Không có double slash: `//api/...`
- ✅ Kiểm tra service đang chạy: `docker ps`

### Lỗi 401 Unauthorized
- Username/password không đúng
- User chưa tồn tại trong database
- Cần tạo user trong database trước

### Lỗi Connection Refused
- Service chưa khởi động
- Chạy: `docker-compose up -d`
- Kiểm tra: `docker ps` để xem containers đang chạy

---

## 📌 Quick Test Flow

1. **Login Admin**: `POST /api/admin/auth/login`
2. **Lấy token** từ response
3. **Test các API** với token trong header
4. **Tạo reservation**: `POST /api/reservations`
5. **Xem danh sách**: `GET /api/reservations`
6. **Cập nhật status**: `PUT /api/reservations/{id}/status?status=IN_USE`

---

## 🔗 URLs

- **Reservation Service**: http://localhost:8086
- **Reservation Admin Service**: http://localhost:8087
- **UI Service**: http://localhost:8080
- **API Gateway**: http://localhost:8084
