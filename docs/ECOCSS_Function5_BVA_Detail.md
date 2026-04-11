# ECOCSS — Function5 BVA Test Cases Detail

> Áp dụng **Boundary Value Analysis (BVA)** theo slide **Chương 4** cho từng test case trong `ECOCSS_Test_Case_Registry_Function5.tsv`.
>
> - File `.md` này: xem **giải thích** và **bảng tổng hợp** per TC.
> - File **TSV**: dán trực tiếp vào Google Sheet Function5.
> - Cột `BVA Tag`: `BVA-STD` (Standard 4n+1) / `BVA-ROB` (Robustness 6n+1) / `BVA-WC` (Worst-Case 5^n)

---

## Cách dùng nhanh

1. Mở `docs/sheets/ECOCSS_Function5_BVA_Detail.tsv`
2. **Ctrl+A → Ctrl+C** toàn bộ nội dung
3. Google Sheet tab **Function5** → chọn **A2** → **Ctrl+V**
4. (Dòng 1 giữ làm header nếu cần; hoặc dán từ A1 rồi tách header riêng)

---

## Cấu trúc BVA mỗi TC

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|

- **Valid partition**: giá trị hợp lệ → HTTP 200/201
- **Invalid partition**: giá trị không hợp lệ → HTTP 400/422
- **TC nào không có input** (getAllReservations, getManageReservations…) → không áp dụng BVA

---

## TC-001: getAllReservations

**GET** `/api/reservations` · ReservationService · Không có input → Không áp dụng BVA.

---

## TC-002: getReservationById

**GET** `/api/reservations/{id}` · ReservationService

### Input & Boundaries

| Variable | Type | Min Valid | Max Tested |
|----------|------|-----------|------------|
| `id` (path) | Integer | 1 | 2³¹−1 |

### BVA Table — Standard

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-001-STD-01 | id | Invalid (< min) | `0` | **404** hoặc **400** | BVA-STD |
| BVA-001-STD-02 | id | **Valid min** | `1` | **200**, object có id=1 | BVA-STD |
| BVA-001-STD-03 | id | Valid (min+1) | `2` | **200**, object có id=2 | BVA-STD |
| BVA-001-STD-04 | id | Invalid (> max) | `999999` hoặc `-1` | **404** | BVA-STD |

---

## TC-003: createReservation

**POST** `/api/reservations` · ReservationService

### Input & Boundaries

| Variable | Type | Min Valid | Valid Range |
|----------|------|-----------|-------------|
| `vehicleId` | Integer | 1 | ≥ 1 |
| `userId` | Integer | 1 | ≥ 1 |
| `startDatetime` | ISO DateTime | — | Khác null, format hợp lệ |
| `endDatetime` | ISO DateTime | — | > startDatetime |
| `purpose` | String | 0 | Non-null |
| `status` | Enum | — | BOOKED / IN_USE / COMPLETED / CANCELLED |

### BVA Table — Standard (startDatetime vs endDatetime)

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-003-STD-01 | endDatetime | Invalid (end = start) | `09:00:00 – 09:00:00` | **400** / **422** | BVA-STD |
| BVA-003-STD-02 | endDatetime | Invalid (end < start) | `09:00:00 – 08:00:00` | **400** / **422** | BVA-STD |
| BVA-003-STD-03 | endDatetime | Valid min (end = start+1min) | `09:00:00 – 09:01:00` | **201**, status=BOOKED | BVA-STD |
| BVA-003-STD-04 | vehicleId | Invalid (< min) | `vehicleId = 0` | **400** / **422** | BVA-STD |
| BVA-003-STD-05 | vehicleId | Valid min | `vehicleId = 1` | **201** | BVA-STD |
| BVA-003-STD-06 | userId | Invalid (< min) | `userId = 0` | **400** / **422** | BVA-STD |
| BVA-003-STD-07 | userId | Valid min | `userId = 1` | **201** | BVA-STD |

### BVA Table — Robustness (thêm giá trị ngoài biên)

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-003-ROB-01 | vehicleId | Invalid (< min) | `vehicleId = -1` | **400** / **422** | BVA-ROB |
| BVA-003-ROB-02 | userId | Invalid (< min) | `userId = -1` | **400** / **422** | BVA-ROB |
| BVA-003-ROB-03 | status | Invalid | `status = "INVALID"` | **400** / **422** | BVA-ROB |
| BVA-003-ROB-04 | startDatetime | Invalid format | `"not-a-datetime"` | **400** / **422** | BVA-ROB |

---

## TC-004: updateReservation

**PUT** `/api/reservations/{id}` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `id` (path) | Integer | ≥ 1 |
| `startDatetime` | ISO DateTime | end > start |
| `endDatetime` | ISO DateTime | end > start |
| `status` | Enum | BOOKED / IN_USE / COMPLETED / CANCELLED |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-004-STD-01 | id | Invalid (< min) | `id = 0` | **404** | BVA-STD |
| BVA-004-STD-02 | id | Valid min | `id = 1` | **200**, updated | BVA-STD |
| BVA-004-STD-03 | endDatetime | Invalid (end ≤ start) | `start=09:00, end=09:00` | **400** | BVA-STD |
| BVA-004-STD-04 | endDatetime | Invalid (end < start) | `start=17:00, end=09:00` | **400** | BVA-STD |
| BVA-004-STD-05 | endDatetime | Valid min | `end = start + 1min` | **200** | BVA-STD |
| BVA-004-ROB-01 | status | Invalid enum | `status = "UNKNOWN"` | **400** | BVA-ROB |

---

## TC-005: updateReservationStatus

**PUT** `/api/reservations/{id}/status?status=…` · ReservationService

### Input & Boundaries

| Variable | Type | Valid Values | Invalid Values |
|----------|------|-------------|---------------|
| `id` (path) | Integer | ≥ 1 | 0, -1 |
| `status` (query) | Enum | BOOKED, IN_USE, COMPLETED, CANCELLED | `INVALID`, `booked` (lowercase) |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-005-STD-01 | id | Invalid | `id = 0` | **404** | BVA-STD |
| BVA-005-STD-02 | id | Valid min | `id = 1` | **200** | BVA-STD |
| BVA-005-STD-03 | status | Invalid enum | `status = INVALID` | **400** | BVA-STD |
| BVA-005-STD-04 | status | Valid (uppercase) | `status = BOOKED` | **200** | BVA-STD |
| BVA-005-STD-05 | status | Valid | `status = IN_USE` | **200** | BVA-STD |
| BVA-005-STD-06 | status | Valid | `status = COMPLETED` | **200** | BVA-STD |
| BVA-005-STD-07 | status | Valid | `status = CANCELLED` | **200** | BVA-STD |
| BVA-005-ROB-01 | status | Case-sensitive | `status = booked` (lowercase) | **400** | BVA-ROB |
| BVA-005-ROB-02 | status | Empty | `status = ""` | **400** | BVA-ROB |

---

## TC-006: deleteReservation

**DELETE** `/api/reservations/{id}` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `id` (path) | Integer | ≥ 1 |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-006-STD-01 | id | Invalid | `id = 0` | **404** | BVA-STD |
| BVA-006-STD-02 | id | Invalid | `id = -1` | **404** | BVA-STD |
| BVA-006-STD-03 | id | Valid min | `id = 1` | **204** (No Content) | BVA-STD |
| BVA-006-STD-04 | id | Valid | `id = 2` | **204** | BVA-STD |

---

## TC-007: getVehicleReservations

**GET** `/api/vehicles/{vehicleId}/reservations` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `vehicleId` (path) | Integer | ≥ 1 |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-007-STD-01 | vehicleId | Invalid | `vehicleId = 0` | **404** | BVA-STD |
| BVA-007-STD-02 | vehicleId | Invalid | `vehicleId = -1` | **404** | BVA-STD |
| BVA-007-STD-03 | vehicleId | Valid min | `vehicleId = 1` | **200**, array | BVA-STD |
| BVA-007-STD-04 | vehicleId | Valid | `vehicleId = 2` | **200**, array | BVA-STD |

---

## TC-008: getVehicleGroupInfo

**GET** `/api/vehicles/{vehicleId}/group` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `vehicleId` (path) | Integer | ≥ 1 |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-008-STD-01 | vehicleId | Invalid | `vehicleId = 0` | **404** | BVA-STD |
| BVA-008-STD-02 | vehicleId | Valid min | `vehicleId = 1` | **200**, object | BVA-STD |
| BVA-008-STD-03 | vehicleId | Valid | `vehicleId = 999999` | **200** hoặc **404** | BVA-STD |

---

## TC-009: getUserVehicles

**GET** `/api/users/{userId}/vehicles` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `userId` (path) | Integer | ≥ 1 |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-009-STD-01 | userId | Invalid | `userId = 0` | **404** | BVA-STD |
| BVA-009-STD-02 | userId | Valid min | `userId = 1` | **200**, array | BVA-STD |
| BVA-009-STD-03 | userId | Invalid | `userId = -1` | **404** | BVA-STD |

---

## TC-010: getUserVehicleReservations

**GET** `/api/users/{userId}/vehicles/{vehicleId}/reservations` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `userId` (path) | Integer | ≥ 1 |
| `vehicleId` (path) | Integer | ≥ 1 |

### BVA Table — Worst-Case (2 biến cùng biên)

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-010-STD-01 | userId | Invalid | `userId = 0` | **404** | BVA-STD |
| BVA-010-STD-02 | userId, vehicleId | Valid min | `userId=1, vehicleId=1` | **200** | BVA-STD |
| BVA-010-STD-03 | userId, vehicleId | Valid | `userId=2, vehicleId=2` | **200** | BVA-STD |
| BVA-010-STD-04 | vehicleId | Invalid | `vehicleId = 0` | **404** | BVA-STD |
| BVA-010-ROB-01 | userId | Invalid | `userId = -1` | **404** | BVA-ROB |
| BVA-010-ROB-02 | vehicleId | Invalid | `vehicleId = -1` | **404** | BVA-ROB |

---

## TC-011: checkAvailability ⭐ (BVA quan trọng nhất)

**GET** `/api/availability?vehicleId=&start=&end=` · ReservationService

### Input & Boundaries

| Variable | Type | Min Valid | Boundary |
|----------|------|-----------|---------|
| `vehicleId` (query) | Integer | 1 | ≥ 1 |
| `start` (query) | ISO DateTime | — | ≠ null, valid format |
| `end` (query) | ISO DateTime | — | > start |

### BVA Table — Standard (3 biến → 4n+1 = 13 TCs)

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-011-STD-01 | vehicleId | Invalid (< min) | `vehicleId = 0` | **400** | BVA-STD |
| BVA-011-STD-02 | vehicleId | Valid min | `vehicleId = 1` | **200** | BVA-STD |
| BVA-011-STD-03 | end | Invalid (end = start) | `start=T09:00, end=T09:00` | **400** | BVA-STD |
| BVA-011-STD-04 | end | Invalid (end < start) | `start=T09:00, end=T08:00` | **400** | BVA-STD |
| BVA-011-STD-05 | end | Valid (end = start+1min) | `end = start+1min` | **200**, boolean | BVA-STD |
| BVA-011-STD-06 | start | Invalid format | `start = "invalid"` | **400** | BVA-STD |
| BVA-011-STD-07 | end | Invalid format | `end = "invalid"` | **400** | BVA-STD |

### BVA Table — Robustness

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-011-ROB-01 | vehicleId | Invalid (< min) | `vehicleId = -1` | **400** | BVA-ROB |
| BVA-011-ROB-02 | start | Invalid | `start = null` | **400** | BVA-ROB |
| BVA-011-ROB-03 | end | Invalid | `end = null` | **400** | BVA-ROB |
| BVA-011-ROB-04 | vehicleId | Invalid | `vehicleId = ""` (empty) | **400** | BVA-ROB |

### BVA Table — Worst-Case (3 biến cùng lúc → 5³ = 125 combos, chọn 5 TCs đại diện)

| BVA Test ID | Variables | Input Value | Expected Result | BVA Tag |
|-------------|-----------|-------------|----------------|---------|
| BVA-011-WC-01 | all at min-1 | `vehicleId=0, end=start` | **400** | BVA-WC |
| BVA-011-WC-02 | all at valid min | `vehicleId=1, end=start+1min` | **200** | BVA-WC |
| BVA-011-WC-03 | vehicleId valid, end invalid | `vehicleId=1, end=start` | **400** | BVA-WC |
| BVA-011-WC-04 | vehicleId invalid, end valid | `vehicleId=0, end=start+1min` | **400** | BVA-WC |

---

## TC-012: listReservationCheckpoints

**GET** `/api/reservations/{id}/checkpoints` · ReservationService

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-012-STD-01 | reservationId | Invalid | `reservationId = 0` | **404** | BVA-STD |
| BVA-012-STD-02 | reservationId | Valid min | `reservationId = 1` | **200**, array | BVA-STD |
| BVA-012-STD-03 | reservationId | Valid | `reservationId = 999999` | **404** | BVA-STD |

---

## TC-013: issueReservationCheckpoint

**POST** `/api/reservations/{id}/checkpoints` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `reservationId` (path) | Integer | ≥ 1 |
| `type` | Enum | PICKUP, DROPOFF |
| `issuedBy` | String | Non-null, min 1 char |
| `notes` | String | Nullable (không bắt buộc) |
| `validMinutes` | Integer | ≥ 1, reasonable max 1440 (24h) |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-013-STD-01 | reservationId | Invalid | `reservationId = 0` | **404** | BVA-STD |
| BVA-013-STD-02 | reservationId | Valid min | `reservationId = 1` | **201** | BVA-STD |
| BVA-013-STD-03 | validMinutes | Invalid (< min) | `validMinutes = 0` | **400** | BVA-STD |
| BVA-013-STD-04 | validMinutes | Valid min | `validMinutes = 1` | **201** | BVA-STD |
| BVA-013-STD-05 | validMinutes | Valid max | `validMinutes = 1440` (24h) | **201** | BVA-STD |
| BVA-013-STD-06 | validMinutes | Invalid (> max) | `validMinutes = 1441` | **400** hoặc **201** | BVA-STD |
| BVA-013-STD-07 | type | Invalid enum | `type = "INVALID"` | **400** | BVA-STD |
| BVA-013-STD-08 | type | Valid | `type = "PICKUP"` / `"DROPOFF"` | **201** | BVA-STD |
| BVA-013-ROB-01 | validMinutes | Invalid (< 0) | `validMinutes = -1` | **400** | BVA-ROB |
| BVA-013-ROB-02 | issuedBy | Invalid empty | `issuedBy = ""` | **400** | BVA-ROB |

---

## TC-014: scanCheckpoint ⭐

**POST** `/api/reservations/checkpoints/scan` · ReservationService

### Input & Boundaries

| Variable | Type | Min | Max | Boundary |
|----------|------|-----|-----|---------|
| `token` | String | non-empty | — | Không rỗng |
| `latitude` | Double | -90 | 90 | Ranh giới GPS |
| `longitude` | Double | -180 | 180 | Ranh giới GPS |

### BVA Table — Standard (3 biến → 4×3+1 = 13 TCs)

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-014-STD-01 | token | Invalid (empty) | `""` | **400** | BVA-STD |
| BVA-014-STD-02 | token | Valid | `"valid-token-123"` | **200/201** | BVA-STD |
| BVA-014-STD-03 | latitude | Invalid (< min) | `latitude = -91` | **400** | BVA-STD |
| BVA-014-STD-04 | latitude | Valid min | `latitude = -90` | **200/201** | BVA-STD |
| BVA-014-STD-05 | latitude | Valid (min+1) | `latitude = -89` | **200/201** | BVA-STD |
| BVA-014-STD-06 | latitude | Valid (max-1) | `latitude = 89` | **200/201** | BVA-STD |
| BVA-014-STD-07 | latitude | Valid max | `latitude = 90` | **200/201** | BVA-STD |
| BVA-014-STD-08 | latitude | Invalid (> max) | `latitude = 91` | **400** | BVA-STD |
| BVA-014-STD-09 | longitude | Invalid (< min) | `longitude = -181` | **400** | BVA-STD |
| BVA-014-STD-10 | longitude | Valid min | `longitude = -180` | **200/201** | BVA-STD |
| BVA-014-STD-11 | longitude | Valid max | `longitude = 180` | **200/201** | BVA-STD |
| BVA-014-STD-12 | longitude | Invalid (> max) | `longitude = 181` | **400** | BVA-STD |
| BVA-014-STD-13 | latitude | Valid mid | `latitude = 0` | **200/201** | BVA-STD |

### BVA Table — Robustness

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-014-ROB-01 | latitude | Invalid | `latitude = -90.0001` | **400** | BVA-ROB |
| BVA-014-ROB-02 | latitude | Invalid | `latitude = 90.0001` | **400** | BVA-ROB |
| BVA-014-ROB-03 | longitude | Invalid | `longitude = -180.0001` | **400** | BVA-ROB |
| BVA-014-ROB-04 | longitude | Invalid | `longitude = 180.0001` | **400** | BVA-ROB |
| BVA-014-ROB-05 | token | null | `token = null` | **400** | BVA-ROB |
| BVA-014-ROB-06 | latitude | Invalid type | `"abc"` | **400** | BVA-ROB |

---

## TC-015: signCheckpoint

**POST** `/api/reservations/checkpoints/{id}/sign` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `checkpointId` (path) | Integer | ≥ 1 |
| `signerName` | String | Non-empty, min 1 char |
| `signerIdNumber` | String | Thường 9 hoặc 12 chữ số (VN CCCD) |
| `signatureData` | String | Non-empty |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-015-STD-01 | checkpointId | Invalid | `checkpointId = 0` | **404** | BVA-STD |
| BVA-015-STD-02 | checkpointId | Valid min | `checkpointId = 1` | **201** | BVA-STD |
| BVA-015-STD-03 | signerName | Invalid (empty) | `signerName = ""` | **400** | BVA-STD |
| BVA-015-STD-04 | signerName | Valid min | `signerName = "A"` | **201** | BVA-STD |
| BVA-015-STD-05 | signerIdNumber | Invalid (empty) | `signerIdNumber = ""` | **400** | BVA-STD |
| BVA-015-STD-06 | signerIdNumber | Valid (9 digits) | `"123456789"` | **201** | BVA-STD |
| BVA-015-STD-07 | signerIdNumber | Valid (12 digits) | `"001234567890"` | **201** | BVA-STD |
| BVA-015-STD-08 | signerIdNumber | Invalid (too short) | `"12345"` (< 9) | **400** | BVA-STD |
| BVA-015-STD-09 | signerIdNumber | Invalid (too long) | `13+ chars` | **400** | BVA-STD |
| BVA-015-STD-10 | signatureData | Invalid (empty) | `signatureData = ""` | **400** | BVA-STD |
| BVA-015-STD-11 | signatureData | Valid | `"base64-or-text"` | **201** | BVA-STD |
| BVA-015-ROB-01 | signerIdNumber | Non-numeric | `"ABCDEFGHI"` | **400** | BVA-ROB |
| BVA-015-ROB-02 | signerName | null | `signerName = null` | **400** | BVA-ROB |

---

## TC-016: getFairnessSummary

**GET** `/api/fairness/vehicles/{vehicleId}?rangeDays=30` · ReservationService

### Input & Boundaries

| Variable | Type | Min Valid | Max Reasonable |
|----------|------|-----------|----------------|
| `vehicleId` (path) | Integer | 1 | — |
| `rangeDays` (query) | Integer | 1 | 365 |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-016-STD-01 | vehicleId | Invalid | `vehicleId = 0` | **404** | BVA-STD |
| BVA-016-STD-02 | vehicleId | Valid min | `vehicleId = 1` | **200** | BVA-STD |
| BVA-016-STD-03 | rangeDays | Invalid (< min) | `rangeDays = 0` | **400** | BVA-STD |
| BVA-016-STD-04 | rangeDays | Valid min | `rangeDays = 1` | **200** | BVA-STD |
| BVA-016-STD-05 | rangeDays | Valid | `rangeDays = 30` | **200** | BVA-STD |
| BVA-016-STD-06 | rangeDays | Valid max | `rangeDays = 365` | **200** | BVA-STD |
| BVA-016-STD-07 | rangeDays | Invalid (> max) | `rangeDays = 366` | **400** | BVA-STD |
| BVA-016-ROB-01 | rangeDays | Invalid (< 0) | `rangeDays = -1` | **400** | BVA-ROB |
| BVA-016-ROB-02 | rangeDays | Invalid non-int | `rangeDays = "abc"` | **400** | BVA-ROB |

---

## TC-017: suggestFairnessSlot

**POST** `/api/fairness/vehicles/{vehicleId}/suggest` · ReservationService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `vehicleId` (path) | Integer | ≥ 1 |
| `userId` (body) | Integer | ≥ 1 |
| `desiredStart` (body) | ISO DateTime | Valid format |
| `durationHours` (body) | Double | > 0, min 0.5 hoặc 1 |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-017-STD-01 | vehicleId | Invalid | `vehicleId = 0` | **404** | BVA-STD |
| BVA-017-STD-02 | vehicleId | Valid min | `vehicleId = 1` | **201** | BVA-STD |
| BVA-017-STD-03 | userId | Invalid | `userId = 0` | **400** | BVA-STD |
| BVA-017-STD-04 | userId | Valid min | `userId = 1` | **201** | BVA-STD |
| BVA-017-STD-05 | durationHours | Invalid (≤ 0) | `durationHours = 0` | **400** | BVA-STD |
| BVA-017-STD-06 | durationHours | Invalid (≤ 0) | `durationHours = -1` | **400** | BVA-STD |
| BVA-017-STD-07 | durationHours | Valid min | `durationHours = 0.5` | **201** | BVA-STD |
| BVA-017-STD-08 | durationHours | Valid | `durationHours = 2.0` | **201** | BVA-STD |
| BVA-017-STD-09 | desiredStart | Invalid format | `"not-datetime"` | **400** | BVA-STD |
| BVA-017-ROB-01 | durationHours | Invalid decimal | `durationHours = 0.1` | **400** (nếu min=1) | BVA-ROB |

---

## TC-018: adminLogin

**POST** `/api/admin/auth/login` · ReservationAdminService

### Input & Boundaries

| Variable | Type | Boundary |
|----------|------|---------|
| `username` | String | Non-empty, min 1 char |
| `password` | String | Non-empty, min 1 char |

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-018-STD-01 | username | Invalid (empty) | `username = ""` | **400** | BVA-STD |
| BVA-018-STD-02 | username | Valid min | `username = "a"` | **200/401** | BVA-STD |
| BVA-018-STD-03 | username | Valid | `"admin"` | **200/401** | BVA-STD |
| BVA-018-STD-04 | password | Invalid (empty) | `password = ""` | **400** | BVA-STD |
| BVA-018-STD-05 | password | Valid | `"password123"` | **200/401** | BVA-STD |
| BVA-018-ROB-01 | username | null | `username = null` | **400** | BVA-ROB |
| BVA-018-ROB-02 | password | null | `password = null` | **400** | BVA-ROB |
| BVA-018-ROB-03 | body | missing | `{}` (empty body) | **400** | BVA-ROB |

---

## TC-019: getAllAdminReservations

**GET** `/api/admin/reservations` · Không có input → Không áp dụng BVA.

---

## TC-020: getAdminReservationById

**GET** `/api/admin/reservations/{id}` · ReservationAdminService

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-020-STD-01 | id | Invalid | `id = 0` | **404** | BVA-STD |
| BVA-020-STD-02 | id | Valid min | `id = 1` | **200** | BVA-STD |
| BVA-020-STD-03 | id | Valid | `id = 999999` | **404** | BVA-STD |
| BVA-020-ROB-01 | id | Invalid | `id = -1` | **404** | BVA-ROB |

---

## TC-021: updateAdminReservation

**PUT** `/api/admin/reservations/{id}` · ReservationAdminService

> Xem TC-003 và TC-004 cho body fields (vehicleId, userId, start/end, status).

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-021-STD-01 | id | Invalid | `id = 0` | **404** | BVA-STD |
| BVA-021-STD-02 | id | Valid min | `id = 1` | **200** | BVA-STD |
| BVA-021-STD-03 | endDatetime | Invalid | `end = start` | **400** | BVA-STD |
| BVA-021-STD-04 | vehicleId | Invalid | `vehicleId = 0` | **400** | BVA-STD |
| BVA-021-STD-05 | status | Invalid enum | `status = "INVALID"` | **400** | BVA-STD |

---

## TC-022: deleteAdminReservation

**DELETE** `/api/admin/reservations/{id}` · ReservationAdminService

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-022-STD-01 | id | Invalid | `id = 0` | **404** | BVA-STD |
| BVA-022-STD-02 | id | Valid min | `id = 1` | **204** | BVA-STD |
| BVA-022-ROB-01 | id | Invalid | `id = -1` | **404** | BVA-ROB |

---

## TC-023: syncReservationFromBooking

**POST** `/api/admin/reservations/sync` · ReservationAdminService

> Body fields: reservationId, vehicleId, userId, startDatetime, endDatetime, purpose, status — giống TC-003.

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-023-STD-01 | reservationId | Invalid | `reservationId = 0` | **400** | BVA-STD |
| BVA-023-STD-02 | reservationId | Valid min | `reservationId = 1` | **201** | BVA-STD |
| BVA-023-STD-03 | vehicleId | Invalid | `vehicleId = 0` | **400** | BVA-STD |
| BVA-023-STD-04 | userId | Invalid | `userId = 0` | **400** | BVA-STD |
| BVA-023-STD-05 | endDatetime | Invalid | `end = start` | **400** | BVA-STD |
| BVA-023-STD-06 | endDatetime | Invalid | `end < start` | **400** | BVA-STD |
| BVA-023-STD-07 | status | Invalid enum | `status = "INVALID"` | **400** | BVA-STD |

---

## TC-024: getManageReservations

**GET** `/api/admin/reservations/manage` · Không có input → Không áp dụng BVA.

---

## TC-025: createManageReservation

**POST** `/api/admin/reservations/manage` · ReservationAdminService

> Body fields giống TC-003.

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-025-STD-01 | vehicleId | Invalid | `vehicleId = 0` | **400** | BVA-STD |
| BVA-025-STD-02 | vehicleId | Valid min | `vehicleId = 1` | **201** | BVA-STD |
| BVA-025-STD-03 | userId | Invalid | `userId = 0` | **400** | BVA-STD |
| BVA-025-STD-04 | endDatetime | Invalid | `end = start` | **400** | BVA-STD |
| BVA-025-STD-05 | endDatetime | Invalid | `end < start` | **400** | BVA-STD |
| BVA-025-STD-06 | status | Valid | `status = BOOKED` | **201**, message + id | BVA-STD |

---

## TC-026: updateManageReservation

**PUT** `/api/admin/reservations/manage/{id}` · ReservationAdminService

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-026-STD-01 | id | Invalid | `id = 0` | **404** | BVA-STD |
| BVA-026-STD-02 | id | Valid min | `id = 1` | **200**, message | BVA-STD |
| BVA-026-STD-03 | startDatetime / endDatetime | Invalid | `end = start` | **400** | BVA-STD |

---

## TC-027: deleteManageReservation

**DELETE** `/api/admin/reservations/manage/{id}` · ReservationAdminService

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-027-STD-01 | id | Invalid | `id = 0` | **404** | BVA-STD |
| BVA-027-STD-02 | id | Valid min | `id = 1` | **204** | BVA-STD |
| BVA-027-ROB-01 | id | Invalid | `id = -1` | **404** | BVA-ROB |

---

## TC-028: getAdminVehicles

**GET** `/api/admin/vehicles` · Không có input → Không áp dụng BVA.

---

## TC-029: proxyGetAllReservations

**GET** `/api/reservations` (8087 proxy) · Không có input → Không áp dụng BVA.

---

## TC-030: proxyGetReservationById

**GET** `/api/reservations/{id}` (8087 proxy) · Xem TC-002.

---

## TC-031: proxyCreateReservation

**POST** `/api/reservations` (8087 proxy) · Xem TC-003.

---

## TC-032: proxyUpdateReservation

**PUT** `/api/reservations/{id}` (8087 proxy) · Xem TC-004.

---

## TC-033: proxyUpdateReservationStatus

**PUT** `/api/reservations/{id}/status` (8087 proxy) · Xem TC-005.

---

## TC-034: proxyDeleteReservation

**DELETE** `/api/reservations/{id}` (8087 proxy) · Xem TC-006.

---

## TC-035: getTestUserById

**GET** `/api/test/user/{userId}` · ReservationAdminService

### BVA Table

| BVA Test ID | Variable | Partition | Input Value | Expected Result | BVA Tag |
|-------------|----------|-----------|-------------|----------------|---------|
| BVA-035-STD-01 | userId | Invalid | `userId = 0` | **404** | BVA-STD |
| BVA-035-STD-02 | userId | Valid min | `userId = 1` | **200**, object | BVA-STD |
| BVA-035-STD-03 | userId | Valid | `userId = 999999` | **200** hoặc **404** | BVA-STD |
| BVA-035-ROB-01 | userId | Invalid | `userId = -1` | **404** | BVA-ROB |

---

## Tổng hợp số lượng BVA Test Cases

| TC | Function | # Standard BVA TCs | # Robustness TCs | # Worst-Case TCs | Ghi chú |
|----|----------|:-----------------:|:---------------:|:----------------:|---------|
| 001 | getAllReservations | — | — | — | Không BVA |
| 002 | getReservationById | 4 | — | — | |
| 003 | createReservation | 7 | 4 | — | |
| 004 | updateReservation | 5 | 1 | — | |
| 005 | updateReservationStatus | 7 | 2 | — | |
| 006 | deleteReservation | 4 | — | — | |
| 007 | getVehicleReservations | 4 | — | — | |
| 008 | getVehicleGroupInfo | 3 | — | — | |
| 009 | getUserVehicles | 3 | — | — | |
| 010 | getUserVehicleReservations | 4 | 2 | — | 2 biến |
| 011 | checkAvailability | 7 | 4 | 4 | **3 biến, nhiều BVA nhất** |
| 012 | listReservationCheckpoints | 3 | — | — | |
| 013 | issueReservationCheckpoint | 8 | 2 | — | |
| 014 | scanCheckpoint | 13 | 6 | — | **GPS boundaries** |
| 015 | signCheckpoint | 11 | 2 | — | |
| 016 | getFairnessSummary | 7 | 2 | — | |
| 017 | suggestFairnessSlot | 9 | 1 | — | |
| 018 | adminLogin | 5 | 3 | — | |
| 019 | getAllAdminReservations | — | — | — | Không BVA |
| 020 | getAdminReservationById | 3 | 1 | — | |
| 021 | updateAdminReservation | 5 | — | — | |
| 022 | deleteAdminReservation | 2 | 1 | — | |
| 023 | syncReservationFromBooking | 7 | — | — | |
| 024 | getManageReservations | — | — | — | Không BVA |
| 025 | createManageReservation | 6 | — | — | |
| 026 | updateManageReservation | 3 | — | — | |
| 027 | deleteManageReservation | 2 | 1 | — | |
| 028 | getAdminVehicles | — | — | — | Không BVA |
| 029 | proxyGetAllReservations | — | — | — | Không BVA |
| 030 | proxyGetReservationById | 4 | — | — | = TC-002 |
| 031 | proxyCreateReservation | 7 | 4 | — | = TC-003 |
| 032 | proxyUpdateReservation | 5 | 1 | — | = TC-004 |
| 033 | proxyUpdateReservationStatus | 7 | 2 | — | = TC-005 |
| 034 | proxyDeleteReservation | 4 | — | — | = TC-006 |
| 035 | getTestUserById | 3 | 1 | — | |
| | **TỔNG** | **153** | **35** | **4** | **~192 TCs** |

---

## Ghi chú khi chạy BVA trên Postman

1. Mỗi dòng BVA trong bảng trên → tạo 1 request trong Postman (hoặc 1 dòng trong Google Sheet Function5).
2. Pre-request Script đặt giá trị biến:

```javascript
// Ví dụ: TC-014 BVA latitude = -91
pm.variables.set("expected_status", "400");
pm.variables.set("latitude", "-91");
```

3. So sánh response `status` thực tế với `expected_status`.
4. TC nào không có ràng buộc cụ thể (ví dụ: `max` của durationHours) → tham khảo database schema hoặc tài liệu API để xác định boundary.

---

*Tệp: `docs/ECOCSS_Function5_BVA_Detail.md`*
