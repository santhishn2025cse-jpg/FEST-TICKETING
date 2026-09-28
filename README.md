# 🎫 FestPass — College Fest Ticketing & QR Check-in System

FestPass is a Spring Boot 3 & Java 21 backend application integrated with an interactive Web UI for college fest event management, digital ticket issuance with visual QR codes, and gate check-in validation enforcing strict business rules.

---

## ✨ Features

1. **Fest Event Management**: Create events with capacity limits, ticket pricing, dates, and venue locations.
2. **Digital QR Ticket Issuance**: Issue tickets with unique QR code tokens and generated Base64 PNG images.
3. **Gate Check-in Validation**: Single-use QR check-in validation. Duplicate entry attempts are rejected immediately with descriptive error messages.
4. **Capacity Limit Enforcement**: Ticket issuance stops automatically once an event reaches max capacity.
5. **Real-time Headcount & Dashboard**: Monitor real-time headcount vs total capacity and occupancy percentages.
6. **OpenAPI / Swagger UI Docs**: Interactive API documentation at `/swagger-ui.html`.

---

## 🛠️ Tech Stack

- **Java 21** & **Spring Boot 3.2.5**
- **Spring Data JPA** & **H2 Database**
- **ZXing Engine** (QR Code Generation)
- **Spring Validation & Global Exception Handling**
- **Swagger / OpenAPI 3**
- **Vanilla CSS & HTML5 Web Dashboard**

---

## 🚀 Getting Started

### Prerequisites
- Java 21 LTS
- Apache Maven 3.x

### Running the Application

```bash
# Clone the repository
git clone https://github.com/santhishn2025cse-jpg/FEST-TICKETING.git
cd FEST-TICKETING

# Build and run tests
mvn clean test

# Launch application
mvn spring-boot:run
```

Once launched, access:
- **Web App**: [http://localhost:8085](http://localhost:8085)
- **Swagger API**: [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- **H2 Console**: [http://localhost:8085/h2-console](http://localhost:8085/h2-console) (JDBC URL: `jdbc:h2:mem:festpassdb`)

---

## 🧪 Running Tests

```bash
mvn clean test
```

The test suite in `FestPassApplicationTests` verifies:
- Event creation & attendee registration
- Ticket purchase & QR code generation
- Capacity limit enforcement rule
- Single-use QR validation rule & duplicate check-in rejection
- Real-time event headcount calculation

---

## 📜 License

MIT License
