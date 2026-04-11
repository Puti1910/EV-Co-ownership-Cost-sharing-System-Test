# PHÂN TÍCH GIÁ TRỊ BIÊN (BOUNDARY VALUE ANALYSIS)

## Chương 4 – Các Kỹ Thuật Thiết Kế Test

---

## Nội dung

1. Xác định điều kiện Test và thiết kế Test cases
2. Phân loại các kỹ thuật thiết kế Test
3. **Kỹ thuật kiểm thử hộp đen**
   - Equivalence Partitioning
   - **Boundary Value Analysis**
   - State Transition Testing
   - Decision Table Testing
4. Kỹ thuật kiểm thử hộp trắng
5. Kỹ thuật kiểm thử dựa trên kinh nghiệm
6. Chọn lựa kỹ thuật kiểm thử

---

## Điều kiện Test và thiết kế Test cases

**3 hoạt động quan trọng:**

- Phân tích Test: Xác định điều kiện Test
- Thiết kế Test: Thiết kế Test cases
- Thực thi Test: Xây dựng Test Procedures / Test Scripts

**Test case gồm các thành phần:**

- Test case ID
- Test Summary / Description
- Pre-condition
- Test steps
- Inputs (test data)
- Expected result
- Pass / Fail

---

## Phân hoạch lớp tương đương (EP)

Phân chia tập hợp các điều kiện test (dữ liệu đầu vào) thành những vùng / lớp tương đương nhau.

**Lớp tương đương:** 2 tests thuộc cùng lớp → kết quả mong đợi giống nhau → nhiều tests cùng lớp là **dư thừa**.

```
    1              100          101        0
  ────┬──────────────┼─────────────┼────────┤
 invalid │    valid       │  invalid │ invalid
```

---

## Phân tích giá trị biên (BVA)

**Biên (boundary)** là điểm chuyển từ lớp tương đương này sang lớp tương đương khác.

**Tại sao test biên?**

- Lỗi thường xảy ra **gần ranh giới** giữa các vùng hợp lệ và không hợp lệ.
- BVA tập trung vào giá trị tại và lân cận biên.

```
    1              100          101        0
  ────┬──────────────┼─────────────┼────────┤
 invalid │    valid       │  invalid │ invalid
              ↑
           Boundary
```

---

## Standard BVA

Với **n biến**, số lượng Test Cases:

**4n + 1**

| Biến | Valid partition | Invalid partitions |
|-------|----------------|-------------------|
| 1 biến | min → max | < min ; > max |
| n biến | 4n giá trị biên | Thêm 1 giá trị trung tâm |

**Tập trung vào:** giá trị biên của mỗi biến.

---

## Ví dụ: Bài toán kiểm tra tam giác

*(Slide minh họa – đặc tả tam giác tùy giá trị 3 cạnh a, b, c)*

---

## Ví dụ: Tìm ngày kế tiếp

*(Slide minh họa – nhập ngày tháng, xuất ngày tiếp theo)*

---

## Standard BVA – Sơ đồ

```
         min-1 | min | min+1 | ... | max-1 | max | max+1
  ─────────────┼─────┼───────┼─────┼───────┼─────┼───────────
   Invalid  ───┤Valid├──────Valid──────┤Valid ├─ Invalid
               ↑biên dưới          ↑biên trên
```

---

## Robustness BVA

Mở rộng từ Standard BVA, tập trung vào giá trị **không hợp lệ** (xử lý ngoại lệ).

Số lượng Test Cases: **6n + 1**

| Thành phần | Số lượng |
|-----------|----------|
| min - 1 | n (dưới biên dưới) |
| min | n |
| min + 1 | n |
| max - 1 | n |
| max | n |
| max + 1 | n (trên biên trên) |
| Giá trị trung tâm | 1 |
| **Tổng** | **6n + 1** |

---

## Worst-Case Testing

Standard BVA chỉ test biên của **1 biến** tại một thời điểm.

Thực tế, nhiều biến có thể đồng thời ở giá trị biên → cần test tổ hợp.

Số lượng Test Cases: **5^n**

> Mỗi biến nhận 5 giá trị biên cùng lúc: min−1, min, min+1, max−1, max, max+1

---

## Robust Worst-Case Testing

Kết hợp Robustness + Worst-Case.

Số lượng Test Cases: **7^n**

| Giá trị mỗi biến |
|------------------|
| min − 1 |
| min |
| min + 1 |
| **midpoint** |
| max − 1 |
| max |
| max + 1 |

---

## Ví dụ: Loan Application

| Trường | Ràng buộc |
|--------|-----------|
| Customer Name | 2–64 ký tự |
| Account Number | 6 chữ số, chữ số đầu khác 0 |
| Loan Amount | £500 – £9000 |
| Term of Loan | 1–30 năm |
| Monthly Repayment | ≥ £10 |

---

## Ví dụ: Customer Name (Standard BVA)

**Số lượng ký tự:** 2 – 64 chars

```
  1      2        64       65
 ───┬──────┼──────────┼───────┬───
Invalid│ Valid │  Valid  │Invalid│
       ↑min  ↑min+1  ↑max-1 ↑max+1
```

| Phân vùng | Giá trị test |
|-----------|-------------|
| Invalid (< 2) | 1 char |
| Valid | 2, 3, 63, 64 chars |
| Invalid (> 64) | 65 chars |
| Giá trị trung tâm | 33 chars |

---

## Ví dụ: Customer Name (Bảng Conditions – Valid Partitions)

| Conditions | Valid Partitions | Invalid Partitions |
|-----------|-----------------|-------------------|
| **Customer name** | 2 – 64 chars | < 2 chars ; > 64 chars ; invalid chars |
| | **V1, V2** | **X1, X2, X3** |

| Boundary (Standard) | B1 | B2 | B3 | B4 | B5 |
|---------------------|----|----|----|----|----|
| 2 chars | ✓ | | | | |
| 3 chars | | ✓ | | | |
| 63 chars | | | ✓ | | |
| 64 chars | | | | ✓ | |
| 33 chars (mid) | | | | | ✓ |

---

## Ví dụ: Account Number (Standard BVA)

**Số chữ số:** 6 digits. **Chữ số đầu tiên:** khác 0 (non-zero).

```
  4         6          7
 ───┬────────┼───────────┬───
Invalid│ Valid  │ Invalid  │
```

| Phân vùng | Giá trị test |
|-----------|-------------|
| < 6 digits | 5 digits |
| 6 digits (1st ≠ 0) | 100000, 100001, 500000, 999998, 999999 |
| > 6 digits | 7 digits |
| 1st digit = 0 | (invalid) |

---

## Ví dụ: Loan Amount (Standard BVA)

**Khoảng:** £500 – £9000

```
  499     500         9000     9001
 ────┬─────────┬───────────┬───────┬───
Invalid│ Valid   │   Valid   │Invalid│
```

| Phân vùng | Giá trị test |
|-----------|-------------|
| < 500 | 499 |
| Valid range | 500, 5001, 4750, 8999, 9000 |
| > 9000 | 9001 |
| Non-numeric | (invalid) |

---

## Thiết kế Test Cases – Bảng đầy đủ

| Test Case | Input | Expected Output | Tags Covered |
|-----------|-------|----------------|-------------|
| 1 | Name: John Smith, Acc: 123456, Loan: 2500, Term: 3 years | Repayment: 79.86, Rate: 10%, Total: 2874.96 | V1, V2, V3, V4, V5, … |
| 2 | Name: AB, Acc: 100000, Loan: 500, Term: 1 year | Repayment: 44.80, Rate: 7.5%, Total: 537.60 | B1, B6, B11, … |

---

## Bảng điều kiện Test đầy đủ (Loan Application)

| Condition | Valid Partitions Tag | Invalid Partitions Tag | Boundaries Tag |
|-----------|---------------------|----------------------|---------------|
| **Customer name** (2–64 chars) | V1, V2 | X1, X2, X3 | B1–B5 |
| **Account number** (6 digits, 1st ≠ 0) | V3, V4, V5 | X4, X5, X6, X7 | B6–B10 |
| **Loan amount** (£500–£9000) | V6, V7 | X8, X9, X10 | B11–B15 |

---

## So sánh các phương pháp BVA

| Phương pháp | Số TCs (n biến) | Tập trung |
|-------------|----------------|-----------|
| Standard BVA | **4n + 1** | Giá trị biên |
| Robustness BVA | **6n + 1** | Biên + giá trị ngoài biên |
| Worst-Case | **5^n** | Tất cả biên cùng lúc |
| Robust Worst-Case | **7^n** | Biên + ngoài biên + tất cả cùng lúc |

---

## Bài tập 1

Một tài khoản tiết kiệm trong ngân hàng nhận lãi suất khác nhau theo số dư:

- Số dư **$1 – $100** → lãi suất **3%**
- Số dư **> $100 – < $1000** → lãi suất **5%**
- Số dư **≥ $1000** → lãi suất **7%**

Sử dụng **EP** và **BVA** để thiết kế các test cases tối thiểu.

---

## Bài tập 2

Xây dựng chương trình: Nhập vào **1 tháng** → xuất **Quý** tương ứng.

Thiết kế test cases bằng:

- **EP** (Phân hoạch lớp tương đương)
- **BVA** (Phân tích giá trị biên)

---

## Tóm tắt

1. **BVA** kiểm thử tại và gần **ranh giới** giữa các vùng hợp lệ / không hợp lệ.
2. Kết hợp với **EP** để xác định các vùng trước, sau đó chọn giá trị biên.
3. **Standard BVA: 4n + 1** – phương pháp cơ bản.
4. **Robustness BVA: 6n + 1** – thêm kiểm thử ngoại lệ.
5. **Worst-Case: 5^n** – tất cả biên đồng thời.
6. **Robust Worst-Case: 7^n** – kết hợp cả hai.

---

## Kết luận

> *"Lỗi thường tập trung tại biên. BVA là kỹ thuật đơn giản nhưng hiệu quả cao trong kiểm thử hộp đen."*

**Khi chọn kỹ thuật kiểm thử, cần xem xét:**

- Loại hệ thống
- Tiêu chuẩn quy định
- Yêu cầu khách hàng
- Mức độ rủi ro
- Thời gian và ngân sách
- Kiến thức testers
