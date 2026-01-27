# Database Setup Instructions

## Database Tables

Your Spring Boot application will automatically create the necessary tables when you run it (due to `spring.jpa.hibernate.ddl-auto=update`). The following tables will be created:

### 1. Staff Table
```sql
CREATE TABLE staff (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    branch VARCHAR(255) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL
);
```

### 2. Orders Table
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    branch VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    items TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

## Initial Data Setup

### Managing Staff Accounts

The easiest way to manage staff accounts and their PINs is through the `staff-config.json` file in the project root.

**Example `staff-config.json`:**
```json
[
  {
    "name": "Kenridge Manager",
    "branch": "Kenridge",
    "pin": "1234",
    "branchCode": "KENRIDGE_1234567"
  },
  {
    "name": "Uitzicht Manager",
    "branch": "Uitzicht",
    "pin": "5678",
    "branchCode": "UITZICHT_1234567"
  }
]
```

To update a PIN or add a new staff member:
1. Edit `staff-config.json`.
2. Restart the Spring Boot application.
3. The application will automatically synchronize the database with the file content.

Alternatively, you can use the `/api/staff/create` endpoint:

**For Kenridge Branch (PIN: 1234):**
```bash
curl -X POST http://192.168.1.31:8081/api/staff/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Kenridge Staff",
    "branch": "Kenridge",
    "pin": "1234"
  }'
```

**For Uitzicht Branch (PIN: 5678):**
```bash
curl -X POST http://192.168.1.31:8081/api/staff/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Uitzicht Staff",
    "branch": "Uitzicht",
    "pin": "5678"
  }'
```

### Creating Test Orders

You can create test orders using the `/api/orders` endpoint:

```bash
# Kenridge Order
curl -X POST http://192.168.1.31:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "branch": "Kenridge",
    "customerName": "Alice",
    "type": "Pickup",
    "items": "[\"Pizza\",\"Soda\"]"
  }'

# Uitzicht Order
curl -X POST http://192.168.1.31:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "branch": "Uitzicht",
    "customerName": "Bob",
    "type": "Delivery",
    "items": "[\"Burger\",\"Fries\"]"
  }'
```

## Security Notes

- **IMPORTANT:** Staff PINs are hashed using BCrypt before storage. Never store plain text PINs in the database.
- The `/api/staff/create` endpoint is for initial setup. In production, you should secure this endpoint or disable it after creating staff accounts.
- PINs are validated by comparing the entered PIN against the stored BCrypt hash using secure comparison.

## Accessing the POS Frontend

1. Start your Spring Boot application: `./mvnw spring-boot:run`
2. Open browser to: `http://192.168.1.31:8081/orders`
3. Login with 16-character Code:
   - `KENRIDGE_1234567` for Kenridge branch
   - `UITZICHT_1234567` for Uitzicht branch

## Database Configuration

Your database is already configured in `src/main/resources/application.properties`:
- **Database:** PostgreSQL (Neon)
- **Port:** 8081
- **Auto-create tables:** Enabled (`spring.jpa.hibernate.ddl-auto=update`)

The application will automatically create the `staff` and `orders` tables on first run.
