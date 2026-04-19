# ECOCSS — Test Case List & Function5 (Reservation + Reservation Admin)

Nguồn API: `postman/Reservation-Admin-Service.postman_collection.json` (folder **ReservationService**, **ReservationAdminService**).

---

## Cách copy nhanh vào Google Sheet (khuyến nghị)

1. Mở file **TSV** bằng Notepad / VS Code (đã dùng ký tự **Tab** làm phân cột).
2. **Ctrl+A** → **Ctrl+C** toàn bộ nội dung file.
3. Trong Google Sheet: chọn ô **A1** → **Ctrl+V**.
4. Hoặc trong Google Sheet: **File → Import → Upload** → chọn file `.tsv` → Separator **Tab**.

| File TSV | Dán vào tab Sheet |
|----------|-------------------|
| `docs/sheets/ECOCSS_Test_Case_List.tsv` | **Test Case List** (2 cột: Trường / Giá trị) |
| `docs/sheets/ECOCSS_Test_Case_Registry_Function5.tsv` | **Test_Case_Registry** — **5 cột giống mẫu của bạn** (`# No`, Requirement Name, Function Name, Sheet Name, Description) |
| `docs/sheets/ECOCSS_Function5_Detail.tsv` | **Function5** — chi tiết 35 dòng (TC_ID, Service, Method, Path, Port, …) |

Sau khi dán **Registry**: cột **Sheet Name** → chuột phải → **Insert link** → chọn range tab **Function5** (giống link Function1 trong mẫu).

---

## Tiêu đề tab "Test Case List"

Merge hàng 1, căn giữa, gõ: **Test Case List**  
Dữ liệu bảng bắt đầu từ dòng 2 (hoặc dán TSV từ A1 nếu không cần dòng tiêu đề merge — bạn tự chèn tiêu đề phía trên).

---

## Cấu trúc cột Test_Case_Registry (khớp ảnh mẫu)

| # No | Requirement Name | Function Name | Sheet Name | Description |
|------|------------------|---------------|------------|-------------|

- **Function Name**: camelCase, không dấu backtick khi dán từ TSV.
- **Sheet Name**: toàn bộ `Function5` (sau đó gắn hyperlink nội bộ).

---

## Function5 (chi tiết)

File `ECOCSS_Function5_Detail.tsv` gồm các cột:

`TC_ID` | `Service` | `# No` | `Function Name` | `Requirement Name` | `Method` | `Path` | `Port` | `Precondition` | `Test Steps` | `Expected Result` | `Priority`

Bạn có thể ẩn cột `Method`/`Path`/`Port` nếu Sheet quá rộng — vẫn giữ để trace với Postman.

---

## Tham chiếu nhanh (đọc thêm)

- Script Postman: `POSTMAN_TEST_SCRIPTS.md`
- Collection: `postman/Reservation-Admin-Service.postman_collection.json`

---

*Tệp mô tả: `docs/ECOCSS_Function5_Reservation_TestCase.md`*  
*TSV: `docs/sheets/*.tsv`*
