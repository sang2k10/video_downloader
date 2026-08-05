# PROJECT RULES

## Language

- Luôn trả lời bằng tiếng Việt.
- Giải thích ngắn gọn trước khi sửa code.
- Không tự ý thay đổi kiến trúc nếu chưa được yêu cầu.

----------------------------------------------------

## Planning

Luôn chia dự án thành nhiều phần.

Ví dụ

Part 1
Part 2
Part 3

Không thực hiện Part tiếp theo nếu Part trước còn lỗi.

----------------------------------------------------

## Progress

Luôn hiển thị

=========================
TIẾN TRÌNH
=========================

Project:
Current Part:
Current Task:
Next Task:
Estimated Remaining:

----------------------------------------------------

## Checkpoint

Sau mỗi task thành công

Sinh

checkpoint.md

bao gồm

- Thời gian
- Phần đã hoàn thành
- File đã sửa
- Lý do sửa
- Các bug đã fix
- Bug còn tồn tại
- Việc tiếp theo

----------------------------------------------------

## Conversation Memory

Sau mỗi checkpoint

Tóm tắt toàn bộ cuộc trò chuyện

Bao gồm

- Quyết định kiến trúc
- Thư viện đã chọn
- API đang dùng
- Các vấn đề đã giải quyết
- TODO

----------------------------------------------------

## Debug

Sau mỗi lần code

Bắt buộc

1. Build

2. Phân tích log

3. Chỉ rõ nguyên nhân

4. Đề xuất cách sửa

Không đoán lỗi.

----------------------------------------------------

## File Safety

Không được

- xóa file ngoài project
- sửa Registry
- sửa PATH
- sửa HOME
- sửa Desktop
- sửa Documents
- sửa Downloads
- sửa AppData
- sửa Program Files
- sửa System32

Chỉ được thao tác trong thư mục project trừ khi người dùng yêu cầu rõ ràng.

----------------------------------------------------

## Commands

Không chạy

rm -rf

del /f /q

format

diskpart

takeown

icacls

shutdown

reboot

hoặc các lệnh có thể gây mất dữ liệu.

----------------------------------------------------

## Before editing

Luôn giải thích

- Vì sao sửa
- Sẽ sửa file nào
- Có ảnh hưởng gì

----------------------------------------------------

## After editing

Luôn báo

- File đã sửa
- Dòng chính thay đổi
- Cách test
- Kết quả mong đợi

----------------------------------------------------

## Finish Task

Khi hoàn thành

Luôn in

=========================
DONE
=========================

✔ Đã hoàn thành

✔ File thay đổi

✔ Cách test

✔ Commit message gợi ý

✔ Việc tiếp theo