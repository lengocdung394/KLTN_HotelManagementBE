# SEN VIỆT HOTEL - SPRING BOOT BACKEND GUIDELINES
<!-- Quy chuẩn lập trình Tối giản & Kỷ luật cao (Karpathy + Ponytail Standard) -->

This file defines the strict behavioral rules and coding guidelines for AI agents working on the Sen Việt Spring Boot Backend repository.

---

## 1. COMMUNICATION & LANGUAGE (GIAO TIẾP VỚI LẬP TRÌNH VIÊN)
- **Developer Communication**: Always reply to the developer in Vietnamese (Tiếng Việt) clearly, honestly, and concisely.
- **End of Response**: End every reply with 1-2 lines summarizing: (1) exact files/lines modified, (2) what was skipped or untouched, (3) any edge-case risks.

---

## 2. ANDREJ KARPATHY 4 GOLDEN RULES (4 NGUYÊN TẮC VÀNG KARPATHY)

### Rule 1: Think Before Coding (Suy nghĩ trước khi gõ phím)
- Never assume database schema or business rules. Check PostgreSQL DDL and entities first.
- Present architectural tradeoffs (e.g., transactional boundaries, locking, cascade effects) before implementing complex mutations.
- If a simpler approach exists, propose it before coding.

### Rule 2: Simplicity First (Tối giản là trên hết)
- Minimum code that solves the problem. Nothing speculative.
- Do NOT create speculative interfaces, unnecessary DTOs, or complex design patterns for simple CRUD operations.
- Avoid adding new Gradle dependencies if Spring Boot starters or Java standard library already provide the functionality.

### Rule 3: Surgical Changes (Can thiệp chuẩn xác như phẫu thuật)
- Touch ONLY the exact service, repository, or controller necessary.
- Do NOT touch existing working transactions, entity relations, or unrelated endpoints.
- Match existing layered architecture: `Entity` -> `Repository` -> `Service` / `ServiceImpl` -> `Controller`.
- Clean up unused imports and variables introduced by your edits.

### Rule 4: Goal-Driven Execution (Làm việc theo mục tiêu kiểm chứng được)
- Always verify API endpoints via real HTTP / cURL calls against the running server (Port 8081).
- Ensure Gradle builds compile cleanly without broken contracts or broken unit tests.

---

## 3. PONYTAIL SENIOR DEV PRINCIPLES (TƯ DUY SENIOR LƯỜI)
- *"The best code is the code never written."* (Dòng code tốt nhất là dòng code không bao giờ phải viết).
- Deletion beats addition.
- Decision hierarchy before writing new code:
  1. Does this endpoint/service method need to exist? Skip unrequested options.
  2. Already in this codebase? Reuse existing repositories, helper mappers, and utility classes.
  3. Standard Java 17/21 feature? Use native stream, optional, and collection methods.
  4. Installed Spring Boot starter? Use Spring Security, Spring Data JPA, Netty-SocketIO.

---

## 4. PROJECT ARCHITECTURE & RUNTIME
- **Tech Stack**: Spring Boot 3, Java 21, Gradle, Spring Data JPA, PostgreSQL, Netty Socket.IO, PayOS SDK.
- **Server Ports**:
  - REST API: `http://localhost:8081` (`gradlew.bat bootRun`)
  - WebSocket Server: `http://localhost:8085` (Netty Socket.IO for real-time live chat)
- **Key Modules**:
  - `com.senviet.hotelmanagement.service`: Core business logic (Bookings, Rooms, Payment, LiveChat).
  - `com.senviet.hotelmanagement.controller`: REST endpoints matching frontend contracts.
  - `com.senviet.hotelmanagement.socket`: Socket event handlers for customer & staff chat synchronization.
