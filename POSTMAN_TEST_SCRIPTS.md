# Postman Test Scripts - Reservation Services (Expected Status Pattern)

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Collection Post-response Script](#2-collection-post-response-script)
3. [Cách dùng Pre-request Script cho Negative Test](#3-cách-dùng-pre-request-script-cho-negative-test)
4. [ReservationService — 17 API (Port 8086)](#4-reservationservice--17-api-port-8086)
5. [ReservationAdminService — 18 API (Port 8087)](#5-reservationadminservice--18-api-port-8087)
6. [Negative Test Examples](#6-negative-test-examples)
7. [Environment Variables](#7-environment-variables)

---

## 1. Tổng quan

| Collection | Service | Port | Base URL |
|---|---|---|---|
| `ReservationService` | ReservationService | 8086 | `{{baseUrl8086}}` |
| `ReservationAdminService` | ReservationAdminService | 8087 | `{{baseUrl8087}}` |

**Nguyên tắc hoạt động:**

- **Positive test**: Không cần script gì thêm — Collection script mặc định đợi `2xx`.
- **Negative test**: Đặt `expected_status` + `bug_case_name` trong **Pre-request Script**.
- **Jira auto-log**: Khi status thực tế != expected → log bug lên Jira.

---

## 2. Collection Post-response Script

> **Đặt tại**: Collection → `Scripts` → `Post-response`

```javascript
/**
 * COLLECTION POST-RESPONSE SCRIPT
 * - Kiểm tra status code khớp expected_status
 * - Tự động log Jira khi FAIL
 * - Chống spam duplicate bug
 */

// ── 1. Lấy expected status ──────────────────────────────────────
let expectedStatus = pm.variables.get("expected_status");
let expectedList = expectedStatus
    ? expectedStatus.toString().split(",").map(s => s.trim())
    : ["200", "201", "202", "204"];

// ── 2. Lấy tên bug case (negative test) ─────────────────────────
let bugCaseName = pm.variables.get("bug_case_name") || "";

const currentStatus = pm.response.code.toString();
const requestName  = pm.info.requestName.trim().replace(/\s+/g, " ");

// Debug
console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
console.log(`🔍 Request  : ${requestName}${bugCaseName ? " → " + bugCaseName : ""}`);
console.log(`🔍 Expected : [${expectedList.join("/")}] | Actual: ${currentStatus}`);

// ── 3. pm.test hiển thị trong Postman ──────────────────────────
pm.test(
    `Status phải là [${expectedList.join("/")}] (Thực tế: ${currentStatus})`,
    function () {
        pm.expect(currentStatus).to.be.oneOf(expectedList);
    }
);

// ── 4. Log Jira khi FAIL ───────────────────────────────────────
const displayName = bugCaseName
    ? `${requestName}: ${bugCaseName}`
    : requestName;

const cacheKey  = "JIRA_DUP_" + displayName.replace(/\s+/g, "_");
const isFailed  = !expectedList.includes(currentStatus);

if (isFailed) {
    const summaryText =
        `[Auto-Bug] Lỗi API: ${displayName} (Đợi: ${expectedList.join("/")}, Thực tế: ${currentStatus})`;

    // Chống spam cùng 1 bug trong 1 session
    if (pm.collectionVariables.get(cacheKey) === summaryText) {
        console.log("⚠️  Đã log rồi. Bỏ qua.");
    } else {
        const jiraDomain = pm.environment.get("jira_domain");
        const jiraEmail  = pm.environment.get("jira_email");
        const jiraToken  = pm.environment.get("jira_token");

        if (!jiraDomain || !jiraEmail || !jiraToken) {
            console.log("❌ Thiếu biến Jira (jira_domain / jira_email / jira_token).");
        } else {
            console.log("🔍 Check duplicate trên Jira...");
            const auth     = "Basic " + btoa(jiraEmail + ":" + jiraToken);
            let body       = pm.response.text();
            try { body = JSON.stringify(pm.response.json(), null, 2); } catch (e) {}

            // Tìm bug trùng trên Jira
            pm.sendRequest(
                {
                    url: `https://${jiraDomain}/rest/api/3/search`,
                    method: "POST",
                    header: { "Content-Type": "application/json", Authorization: auth },
                    body: {
                        mode: "raw",
                        raw: JSON.stringify({
                            jql: `project = ECOCSS AND summary ~ "${displayName}" ORDER BY created DESC`,
                            maxResults: 20
                        })
                    }
                },
                function (err, res) {
                    if (err) { console.log("❌ Lỗi check Jira:", err); return; }

                    let isDuplicate = false;
                    const data = res.json();
                    if (data && data.issues) {
                        for (const issue of data.issues) {
                            const s = (issue.fields.status.name || "").toLowerCase();
                            const closed = ["done","closed","resolved","đã xong","hoàn thành"].includes(s);
                            if (!closed && issue.fields.summary === summaryText) {
                                isDuplicate = true; break;
                            }
                        }
                    }

                    if (isDuplicate) {
                        console.log("⚠️  Bug đã tồn tại trên Jira.");
                        pm.collectionVariables.set(cacheKey, summaryText);
                    } else {
                        console.log("✅ Chưa trùng → Tạo bug mới...");
                        pm.collectionVariables.set(cacheKey, summaryText);

                        const payload = {
                            fields: {
                                project   : { key: "ECOCSS" },
                                summary    : summaryText,
                                description: {
                                    type   : "doc",
                                    version: 1,
                                    content: [{
                                        type   : "paragraph",
                                        content: [{
                                            type: "text",
                                            text: `Phát hiện lỗi không khớp kỳ vọng.\n\n` +
                                                  `- API    : ${pm.request.url}\n` +
                                                  `- Đợi   : ${expectedList.join("/")}\n` +
                                                  `- Thực tế: ${currentStatus}\n` +
                                                  `- Body   : ${body}`
                                        }]
                                    }]
                                },
                                issuetype : { name: "Bug" }
                            }
                        };

                        pm.sendRequest(
                            {
                                url    : `https://${jiraDomain}/rest/api/3/issue`,
                                method : "POST",
                                header : { "Content-Type": "application/json", Authorization: auth },
                                body   : { mode: "raw", raw: JSON.stringify(payload) }
                            },
                            function (e, r) {
                                if (e) console.log("❌ Tạo Jira thất bại:", e);
                                else   console.log("🎉 Bug tạo thành công:", r.json().key);
                            }
                        );
                    }
                }
            );
        }
    }
} else {
    // PASS → giải phóng cache
    pm.collectionVariables.unset(cacheKey);
    console.log("✅ PASS — Xóa cache.");
}
```

---

## 3. Cách dùng Pre-request Script cho Negative Test

### 3.1 Positive Test (Happy Path)

**Không cần gì thêm.** Script mặc định đợi `2xx`.

### 3.2 Negative Test — Pre-request Script

Mở request → tab **Pre-request Script** → thêm:

```javascript
// Ví dụ: kỳ vọng 400 Bad Request
pm.variables.set("expected_status", "400");
pm.variables.set("bug_case_name", "Thiếu vehicleId");
```

### 3.3 Danh sách mã status thường dùng

| Mã | Ý nghĩa |
|---|---|
| `200` | OK |
| `201` | Created |
| `204` | No Content |
| `400` | Bad Request (lỗi validate) |
| `401` | Unauthorized |
| `403` | Forbidden |
| `404` | Not Found |
| `409` | Conflict (trùng dữ liệu) |
| `500` | Internal Server Error |

---

## 4. ReservationService — 17 API (Port 8086)

> **Pre-request Script**: Để trống (Positive Test — đợi `2xx` mặc định)

### 4.1  `GET GET /api/reservations`

```
GET {{baseUrl8086}}/api/reservations
```

```javascript
// ── Post-response (Tests tab) ──────────────────────────────────
pm.test("Response là JSON array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});

pm.test("Mỗi reservation có reservationId, vehicleId, userId, status", function () {
    const data = pm.response.json();
    if (data.length > 0) {
        pm.expect(data[0]).to.have.property("reservationId");
        pm.expect(data[0]).to.have.property("vehicleId");
        pm.expect(data[0]).to.have.property("status");
    }
});

pm.test("Tất cả status hợp lệ (BOOKED/IN_USE/COMPLETED/CANCELLED)", function () {
    const valid = ["BOOKED","IN_USE","COMPLETED","CANCELLED"];
    pm.response.json().forEach(function (r) {
        pm.expect(valid).to.include(r.status, `Status không hợp lệ: ${r.status}`);
    });
});
```

### 4.2  `GET GET /api/reservations/{id}`

```
GET {{baseUrl8086}}/api/reservations/{{reservationId}}
```

```javascript
pm.test("Response là object", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("object");
});

pm.test("Có đủ field: reservationId, vehicleId, userId, status", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.property("reservationId");
    pm.expect(r).to.have.property("vehicleId");
    pm.expect(r).to.have.property("userId");
    pm.expect(r).to.have.property("status");
});

pm.test("ID khớp với URL", function () {
    const r    = pm.response.json();
    const path = pm.request.url.path;
    const id   = path[path.length - 1];
    pm.expect(r.reservationId.toString()).to.equal(id);
});

// Lưu vào environment để chain test
if (pm.response.code === 200) {
    const r = pm.response.json();
    pm.environment.set("reservationId", r.reservationId);
    pm.environment.set("vehicleId", r.vehicleId);
    pm.environment.set("userId", r.userId);
}
```

### 4.3  `POST POST /api/reservations`

```
POST {{baseUrl8086}}/api/reservations
Content-Type: application/json

Body:
{
  "vehicleId": {{vehicleId}},
  "userId": {{userId}},
  "startDatetime": "2026-03-28T09:00:00",
  "endDatetime": "2026-03-28T17:00:00",
  "purpose": "Postman test",
  "status": "BOOKED"
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response có reservationId", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.property("reservationId");
    pm.expect(r.reservationId).to.be.a("number");
});

pm.test("Status mặc định là BOOKED", function () {
    const r = pm.response.json();
    pm.expect(r.status).to.equal("BOOKED");
});

pm.test("startDatetime < endDatetime", function () {
    const r     = pm.response.json();
    const start = new Date(r.startDatetime);
    const end   = new Date(r.endDatetime);
    pm.expect(end.getTime()).to.be.above(start.getTime());
});

// Lưu ID để dùng cho test tiếp theo
if (pm.response.code === 200 || pm.response.code === 201) {
    const r = pm.response.json();
    pm.environment.set("reservationId", r.reservationId);
}
```

### 4.4  `PUT PUT /api/reservations/{id}`

```
PUT {{baseUrl8086}}/api/reservations/{{reservationId}}
Content-Type: application/json

Body:
{
  "startDatetime": "2026-03-28T10:00:00",
  "endDatetime": "2026-03-28T18:00:00",
  "purpose": "Updated",
  "status": "BOOKED"
}
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là object", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("object");
});

pm.test("Có field reservationId (ID không đổi)", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.property("reservationId");
});

// Verify field đã được cập nhật khớp body
pm.test("Field đã cập nhật đúng theo body", function () {
    const r          = pm.response.json();
    const reqBody    = JSON.parse(pm.request.body.raw);
    if (reqBody.startDatetime)
        pm.expect(r.startDatetime).to.equal(reqBody.startDatetime);
    if (reqBody.endDatetime)
        pm.expect(r.endDatetime).to.equal(reqBody.endDatetime);
    if (reqBody.status)
        pm.expect(r.status).to.equal(reqBody.status);
});
```

### 4.5  `PUT PUT /api/reservations/{id}/status`

```
PUT {{baseUrl8086}}/api/reservations/{{reservationId}}/status?status=IN_USE
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Status trong response khớp query param", function () {
    const r        = pm.response.json();
    const newStat  = pm.request.url.query.get("status");
    pm.expect(r.status).to.equal(newStat);
});

pm.test("Status hợp lệ", function () {
    const valid = ["BOOKED","IN_USE","COMPLETED","CANCELLED"];
    pm.expect(valid).to.include(pm.response.json().status);
});

pm.environment.set("lastStatus", pm.response.json().status);
```

### 4.6  `DELETE DELETE /api/reservations/{id}`

```
DELETE {{baseUrl8086}}/api/reservations/{{reservationId}}
```

```javascript
pm.test("Status 200 hoặc 204", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 204]);
});

pm.test("Không có error trong response", function () {
    if (pm.response.code === 200) {
        pm.response.to.be.json;
        pm.expect(pm.response.json()).to.not.have.property("error");
    }
});
```

### 4.7  `GET GET /api/vehicles/{vehicleId}/reservations`

```
GET {{baseUrl8086}}/api/vehicles/{{vehicleId}}/reservations
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});

pm.test("Tất cả reservation thuộc vehicleId đúng", function () {
    const data     = pm.response.json();
    const vehicleId = pm.request.url.path[3];
    data.forEach(function (r) {
        pm.expect(r.vehicleId.toString()).to.equal(vehicleId);
    });
});

pm.test("Sắp xếp theo startDatetime tăng dần", function () {
    const data = pm.response.json();
    if (data.length > 1) {
        for (let i = 1; i < data.length; i++) {
            const prev = new Date(data[i-1].startDatetime);
            const curr = new Date(data[i].startDatetime);
            pm.expect(curr.getTime()).to.be.at.least(prev.getTime(),
                `Chưa sắp xếp đúng tại index ${i}`);
        }
    }
});
```

### 4.8  `GET GET /api/vehicles/{vehicleId}/group`

```
GET {{baseUrl8086}}/api/vehicles/{{vehicleId}}/group
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response có groupId, groupName, members", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.property("groupId");
    pm.expect(r).to.have.property("groupName");
    pm.expect(r).to.have.property("members");
});

pm.test("Members là array", function () {
    pm.expect(pm.response.json().members).to.be.an("array");
});
```

### 4.9  `GET GET /api/users/{userId}/vehicles`

```
GET {{baseUrl8086}}/api/users/{{userId}}/vehicles
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});

pm.test("Mỗi item có vehicleId, groupId", function () {
    const data = pm.response.json();
    if (data.length > 0) {
        pm.expect(data[0]).to.have.property("vehicleId");
        pm.expect(data[0]).to.have.property("groupId");
    }
});
```

### 4.10  `GET GET /api/users/{userId}/vehicles/{vehicleId}/reservations`

```
GET {{baseUrl8086}}/api/users/{{userId}}/vehicles/{{vehicleId}}/reservations
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});
```

### 4.11  `GET GET /api/availability`

```
GET {{baseUrl8086}}/api/availability?vehicleId={{vehicleId}}&start=2026-03-28T09:00:00&end=2026-03-28T17:00:00
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là boolean", function () {
    const r = pm.response.json();
    pm.expect(r).to.be.a("boolean");
});

pm.test("Giá trị là true hoặc false (không null)", function () {
    const r = pm.response.json();
    pm.expect([true, false]).to.include(r);
});
```

### 4.12  `GET GET /api/reservations/{reservationId}/checkpoints`

```
GET {{baseUrl8086}}/api/reservations/{{reservationId}}/checkpoints
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});
```

### 4.13  `POST POST /api/reservations/{reservationId}/checkpoints`

```
POST {{baseUrl8086}}/api/reservations/{{reservationId}}/checkpoints
Content-Type: application/json

Body:
{
  "type": "PICKUP",
  "issuedBy": "admin",
  "notes": "Postman test",
  "validMinutes": 60
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response có checkpointId hoặc token", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.any.keys("checkpointId", "token", "id");
});

if (pm.response.code === 200 || pm.response.code === 201) {
    const r = pm.response.json();
    pm.environment.set("checkpointId", r.checkpointId || r.id);
}
```

### 4.14  `POST POST /api/reservations/checkpoints/scan`

```
POST {{baseUrl8086}}/api/reservations/checkpoints/scan
Content-Type: application/json

Body:
{
  "token": "checkpoint-token-from-issue",
  "latitude": 10.762622,
  "longitude": 106.660172
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response có trường liên quan đến checkpoint", function () {
    pm.response.to.be.json;
    const r = pm.response.json();
    pm.expect(r).to.be.an("object");
});
```

### 4.15  `POST POST /api/reservations/checkpoints/{checkpointId}/sign`

```
POST {{baseUrl8086}}/api/reservations/checkpoints/{{checkpointId}}/sign
Content-Type: application/json

Body:
{
  "signerName": "Nguyen Van A",
  "signerIdNumber": "001234567890",
  "signatureData": "base64-or-text-placeholder"
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response là object", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("object");
});
```

### 4.16  `GET GET /api/fairness/vehicles/{vehicleId}`

```
GET {{baseUrl8086}}/api/fairness/vehicles/{{vehicleId}}?rangeDays=30
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là object", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("object");
});

pm.test("Có trường fairness/summary", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.any.keys("fairness", "summary", "totalUsageHours");
});
```

### 4.17  `POST POST /api/fairness/vehicles/{vehicleId}/suggest`

```
POST {{baseUrl8086}}/api/fairness/vehicles/{{vehicleId}}/suggest
Content-Type: application/json

Body:
{
  "userId": {{userId}},
  "desiredStart": "2026-03-28T09:00:00",
  "durationHours": 2.0
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response là object", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("object");
});
```

---

## 5. ReservationAdminService — 18 API (Port 8087)

### 5.1  `POST POST /api/admin/auth/login`

```
POST {{baseUrl8087}}/api/admin/auth/login
Content-Type: application/json

Body:
{
  "username": "your_admin_username",
  "password": "your_password"
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response có token hoặc accessToken", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.any.keys("token", "accessToken", "jwt");
});

// Lưu token để dùng cho các request tiếp theo
if (pm.response.code === 200 || pm.response.code === 201) {
    const r = pm.response.json();
    pm.environment.set("adminToken", r.token || r.accessToken);
}
```

### 5.2  `GET GET /api/admin/reservations`

```
GET {{baseUrl8087}}/api/admin/reservations
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});

pm.test("Mỗi reservation có reservationId, vehicleName, status", function () {
    const data = pm.response.json();
    if (data.length > 0) {
        pm.expect(data[0]).to.have.property("reservationId");
        pm.expect(data[0]).to.have.property("status");
    }
});

pm.test("Status hợp lệ", function () {
    const valid = ["BOOKED","IN_USE","COMPLETED","CANCELLED"];
    pm.response.json().forEach(function (r) {
        pm.expect(valid).to.include(r.status);
    });
});
```

### 5.3  `GET GET /api/admin/reservations/{id}`

```
GET {{baseUrl8087}}/api/admin/reservations/{{reservationId}}
```

```javascript
pm.test("Status 200 hoặc 404", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 404]);
});

if (pm.response.code === 200) {
    pm.test("Response có đủ field admin", function () {
        const r = pm.response.json();
        pm.expect(r).to.have.property("reservationId");
        pm.expect(r).to.have.property("status");
    });
}

if (pm.response.code === 404) {
    pm.test("404 có message lỗi", function () {
        pm.response.to.be.json;
        const r = pm.response.json();
        pm.expect(r).to.have.property("message");
    });
}
```

### 5.4  `PUT PUT /api/admin/reservations/{id}`

```
PUT {{baseUrl8087}}/api/admin/reservations/{{reservationId}}
Content-Type: application/json

Body:
{
  "vehicleId": 1,
  "userId": 1,
  "startDatetime": "2026-03-28T09:00:00",
  "endDatetime": "2026-03-28T17:00:00",
  "purpose": "Admin update",
  "status": "BOOKED"
}
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response không có error", function () {
    if (pm.response.code === 200) {
        pm.response.to.be.json;
        pm.expect(pm.response.json()).to.not.have.property("error");
    }
});
```

### 5.5  `DELETE DELETE /api/admin/reservations/{id}`

```
DELETE {{baseUrl8087}}/api/admin/reservations/{{reservationId}}
```

```javascript
pm.test("Status 200 hoặc 204", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 204]);
});
```

### 5.6  `POST POST /api/admin/reservations/sync`

```
POST {{baseUrl8087}}/api/admin/reservations/sync
Content-Type: application/json

Body:
{
  "reservationId": 1,
  "vehicleId": 1,
  "userId": 1,
  "startDatetime": "2026-03-28T09:00:00",
  "endDatetime": "2026-03-28T17:00:00",
  "purpose": "Sync test",
  "status": "BOOKED"
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response có message hoặc id", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.any.keys("message", "id", "reservationId");
});
```

### 5.7  `GET GET /api/admin/reservations/manage`

```
GET {{baseUrl8087}}/api/admin/reservations/manage
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});
```

### 5.8  `POST POST /api/admin/reservations/manage`

```
POST {{baseUrl8087}}/api/admin/reservations/manage
Content-Type: application/json

Body:
{
  "vehicleId": {{vehicleId}},
  "userId": {{userId}},
  "startDatetime": "2026-03-28T09:00:00",
  "endDatetime": "2026-03-28T17:00:00",
  "purpose": "Manage create",
  "status": "BOOKED"
}
```

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});

pm.test("Response có message và id", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.property("message");
    pm.expect(r).to.have.property("id");
});

if (pm.response.code === 200 || pm.response.code === 201) {
    pm.environment.set("adminManageId", pm.response.json().id);
}
```

### 5.9  `PUT PUT /api/admin/reservations/manage/{id}`

```
PUT {{baseUrl8087}}/api/admin/reservations/manage/{{reservationId}}
Content-Type: application/json

Body:
{
  "startDatetime": "2026-03-28T10:00:00",
  "endDatetime": "2026-03-28T18:00:00",
  "status": "BOOKED"
}
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response có message", function () {
    const r = pm.response.json();
    pm.expect(r).to.have.property("message");
});
```

### 5.10  `DELETE DELETE /api/admin/reservations/manage/{id}`

```
DELETE {{baseUrl8087}}/api/admin/reservations/manage/{{reservationId}}
```

```javascript
pm.test("Status 200 hoặc 204", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 204]);
});
```

### 5.11  `GET GET /api/admin/vehicles`

```
GET {{baseUrl8087}}/api/admin/vehicles
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});

pm.test("Mỗi vehicle có vehicleId", function () {
    const data = pm.response.json();
    if (data.length > 0) {
        pm.expect(data[0]).to.have.property("vehicleId");
    }
});

// Lưu vehicleId đầu tiên để test
if (pm.response.code === 200 && pm.response.json().length > 0) {
    pm.environment.set("vehicleId", pm.response.json()[0].vehicleId);
}
```

### 5.12–5.17 Proxy `/api/reservations` (8087)

> Các request bên dưới chạy trên **Admin Service (8087)** nhưng proxy sang `/api/reservations`.
> Dùng chung test script tương tự 8086.

#### 5.12 `GET GET /api/reservations`

```javascript
pm.test("Status 200", function () { pm.response.to.have.status(200); });
pm.test("Response là array", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("array");
});
```

#### 5.13 `GET GET /api/reservations/{id}`

```javascript
pm.test("Status 200 hoặc 404", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 404]);
});
```

#### 5.14 `POST POST /api/reservations`

```javascript
pm.test("Status 200 hoặc 201", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201]);
});
```

#### 5.15 `PUT PUT /api/reservations/{id}`

```javascript
pm.test("Status 200", function () { pm.response.to.have.status(200); });
```

#### 5.16 `PUT PUT /api/reservations/{id}/status`

```javascript
pm.test("Status 200", function () { pm.response.to.have.status(200); });
pm.test("Status trong response khớp query", function () {
    const r = pm.response.json();
    const q = pm.request.url.query.get("status");
    pm.expect(r.status).to.equal(q);
});
```

#### 5.17 `DELETE DELETE /api/reservations/{id}`

```javascript
pm.test("Status 200 hoặc 204", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 204]);
});
```

### 5.18  `GET GET /api/test/user/{userId}`

```
GET {{baseUrl8087}}/api/test/user/{{userId}}
```

```javascript
pm.test("Status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response là object", function () {
    pm.response.to.be.json;
    pm.expect(pm.response.json()).to.be.an("object");
});
```

---

## 6. Negative Test Examples

### 6.1  POST `/api/reservations` — thiếu `vehicleId`

**Pre-request Script** (Pre-request tab):

```javascript
pm.variables.set("expected_status", "400");
pm.variables.set("bug_case_name", "Thiếu vehicleId");
```

**Body gửi đi** (sai):

```json
{
  "userId": 1,
  "startDatetime": "2026-03-28T09:00:00",
  "endDatetime": "2026-03-28T17:00:00",
  "purpose": "Test"
}
```

> **Kỳ vọng**: Server trả `400`. Nếu trả `200/201` → **Auto-Bug Jira**.

---

### 6.2  POST `/api/reservations` — `startDatetime > endDatetime`

**Pre-request Script**:

```javascript
pm.variables.set("expected_status", "400");
pm.variables.set("bug_case_name", "start > end datetime");
```

**Body gửi đi**:

```json
{
  "vehicleId": 1,
  "userId": 1,
  "startDatetime": "2026-03-28T18:00:00",
  "endDatetime": "2026-03-28T09:00:00",
  "purpose": "Test"
}
```

---

### 6.3  GET `/api/reservations/{id}` — ID không tồn tại

**Pre-request Script**:

```javascript
pm.variables.set("expected_status", "404");
pm.variables.set("bug_case_name", "Reservation ID không tồn tại");
```

**URL**: `GET {{baseUrl8086}}/api/reservations/999999`

---

### 6.4  PUT `/api/reservations/{id}/status` — status không hợp lệ

**Pre-request Script**:

```javascript
pm.variables.set("expected_status", "400");
pm.variables.set("bug_case_name", "Status không hợp lệ");
```

**URL**: `PUT {{baseUrl8086}}/api/reservations/{{reservationId}}/status?status=INVALID_STATUS`

---

### 6.5  POST `/api/admin/auth/login` — sai password

**Pre-request Script**:

```javascript
pm.variables.set("expected_status", "401");
pm.variables.set("bug_case_name", "Sai password");
```

**Body**:

```json
{
  "username": "admin",
  "password": "sai-mat-khau"
}
```

---

### 6.6  POST `/api/reservations` — overlap thời gian

**Pre-request Script**:

```javascript
pm.variables.set("expected_status", "400");
pm.variables.set("bug_case_name", "Overlap thời gian đặt");
```

> Tạo reservation A (09:00–17:00), sau đó tạo reservation B cùng vehicle, cùng khoảng 10:00–14:00 → server phải trả `400`.

---

## 7. Environment Variables

### 7.1 Cần thiết lập

| Tên | Ví dụ giá trị | Mục đích |
|---|---|---|
| `baseUrl8086` | `http://localhost:8086` | ReservationService |
| `baseUrl8087` | `http://localhost:8087` | ReservationAdminService |
| `base_url` | `http://localhost:8082` | user-account-service (nếu dùng) |
| `reservationId` | `1` | ID reservation thực tế |
| `userId` | `1` | ID user thực tế |
| `vehicleId` | `1` | ID vehicle thực tế |
| `checkpointId` | `1` | ID checkpoint thực tế |
| `jira_domain` | `your-domain.atlassian.net` | Jira Cloud |
| `jira_email` | `your@email.com` | Jira email |
| `jira_token` | `your-api-token` | Jira API Token |

### 7.2 Jira API Token

1. Truy cập [id.atlassian.com/manage-profile/tokens](https://id.atlassian.com/manage-profile/tokens)
2. Tạo token mới
3. Paste vào biến `jira_token` trong Postman Environment

---

## 8. Mẹo sử dụng

1. **Chạy từng service**: Chạy Collection → chọn **ReservationAdminService** hoặc **ReservationService** → Run.
2. **Chạy hết**: Chạy toàn bộ Collection cùng lúc.
3. **Xem kết quả**: Tab **Test Results** (màu xanh = Pass, đỏ = Fail).
4. **Xem Jira**: [your-domain.atlassian.net/browse/ECOCSS](https://your-domain.atlassian.net/browse/ECOCSS)
5. **Reset biến**: Sau mỗi đợt test, xóa cache Jira: `pm.collectionVariables.clear()` trong Console.
6. **Tắt Jira tạm**: Comment hoặc xóa phần `jiraDomain` block trong Collection script.

---

## 9. Bảng tóm tắt tất cả API

### ReservationService (8086)

| # | Tên request | Method | Path | Positive |
|---|---|---|---|---|
| 1 | GET /api/reservations | GET | /api/reservations | 200 |
| 2 | GET /api/reservations/{id} | GET | /api/reservations/{{reservationId}} | 200 |
| 3 | POST /api/reservations | POST | /api/reservations | 200,201 |
| 4 | PUT /api/reservations/{id} | PUT | /api/reservations/{{reservationId}} | 200 |
| 5 | PUT /api/reservations/{id}/status | PUT | /api/reservations/{{reservationId}}/status | 200 |
| 6 | DELETE /api/reservations/{id} | DELETE | /api/reservations/{{reservationId}} | 200,204 |
| 7 | GET /api/vehicles/{vehicleId}/reservations | GET | /api/vehicles/{{vehicleId}}/reservations | 200 |
| 8 | GET /api/vehicles/{vehicleId}/group | GET | /api/vehicles/{{vehicleId}}/group | 200 |
| 9 | GET /api/users/{userId}/vehicles | GET | /api/users/{{userId}}/vehicles | 200 |
| 10 | GET /api/users/{userId}/vehicles/{vehicleId}/reservations | GET | /api/users/{{userId}}/vehicles/{{vehicleId}}/reservations | 200 |
| 11 | GET /api/availability | GET | /api/availability | 200 |
| 12 | GET /api/reservations/{reservationId}/checkpoints | GET | /api/reservations/{{reservationId}}/checkpoints | 200 |
| 13 | POST /api/reservations/{reservationId}/checkpoints | POST | /api/reservations/{{reservationId}}/checkpoints | 200,201 |
| 14 | POST /api/reservations/checkpoints/scan | POST | /api/reservations/checkpoints/scan | 200,201 |
| 15 | POST /api/reservations/checkpoints/{checkpointId}/sign | POST | /api/reservations/checkpoints/{{checkpointId}}/sign | 200,201 |
| 16 | GET /api/fairness/vehicles/{vehicleId} | GET | /api/fairness/vehicles/{{vehicleId}} | 200 |
| 17 | POST /api/fairness/vehicles/{vehicleId}/suggest | POST | /api/fairness/vehicles/{{vehicleId}}/suggest | 200,201 |

### ReservationAdminService (8087)

| # | Tên request | Method | Path | Positive |
|---|---|---|---|---|
| 1 | POST /api/admin/auth/login | POST | /api/admin/auth/login | 200,201 |
| 2 | GET /api/admin/reservations | GET | /api/admin/reservations | 200 |
| 3 | GET /api/admin/reservations/{id} | GET | /api/admin/reservations/{{reservationId}} | 200,404 |
| 4 | PUT /api/admin/reservations/{id} | PUT | /api/admin/reservations/{{reservationId}} | 200 |
| 5 | DELETE /api/admin/reservations/{id} | DELETE | /api/admin/reservations/{{reservationId}} | 200,204 |
| 6 | POST /api/admin/reservations/sync | POST | /api/admin/reservations/sync | 200,201 |
| 7 | GET /api/admin/reservations/manage | GET | /api/admin/reservations/manage | 200 |
| 8 | POST /api/admin/reservations/manage | POST | /api/admin/reservations/manage | 200,201 |
| 9 | PUT /api/admin/reservations/manage/{id} | PUT | /api/admin/reservations/manage/{{reservationId}} | 200 |
| 10 | DELETE /api/admin/reservations/manage/{id} | DELETE | /api/admin/reservations/manage/{{reservationId}} | 200,204 |
| 11 | GET /api/admin/vehicles | GET | /api/admin/vehicles | 200 |
| 12 | GET /api/reservations (proxy) | GET | /api/reservations | 200 |
| 13 | GET /api/reservations/{id} (proxy) | GET | /api/reservations/{{reservationId}} | 200,404 |
| 14 | POST /api/reservations (proxy) | POST | /api/reservations | 200,201 |
| 15 | PUT /api/reservations/{id} (proxy) | PUT | /api/reservations/{{reservationId}} | 200 |
| 16 | PUT /api/reservations/{id}/status (proxy) | PUT | /api/reservations/{{reservationId}}/status | 200 |
| 17 | DELETE /api/reservations/{id} (proxy) | DELETE | /api/reservations/{{reservationId}} | 200,204 |
| 18 | GET /api/test/user/{userId} | GET | /api/test/user/{{userId}} | 200 |
