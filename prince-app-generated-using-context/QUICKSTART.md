# Quick Start Guide - Tax Debt Recovery System

**Context:** ctx_99a057884357

## 🚀 Get Started in 3 Steps

### Step 1: Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

### Step 2: Access the Application

Open your browser and navigate to:

- **Web Interface:** http://localhost:8080
- **API Docs (Swagger):** http://localhost:8080/swagger-ui.html
- **H2 Database Console:** http://localhost:8080/h2-console

### Step 3: Try the Demo

1. **Open the web interface** at http://localhost:8080
2. **Select a citizen** from the dropdown (John Doe or Jane Smith)
3. **View the dashboard** to see:
   - Tax credit balance (initially €0.00)
   - Outstanding debts
4. **Create a prepayment:**
   - Click "Prepayments" tab
   - Select "VAT" or "Advance Payment"
   - Enter amount (e.g., 500.00)
   - Click "Submit Prepayment"
5. **Confirm the prepayment:**
   - Click the "Confirm" button in the prepayment history
   - This simulates payment gateway confirmation
6. **Check updated balance:**
   - Go back to Dashboard
   - See the tax credit balance updated!

## 📊 Sample Data

The system comes pre-loaded with:

### Citizens
- **John Doe** (CIT-001)
  - 2 debts: €1,500 (tax) + €250 (penalty)
- **Jane Smith** (CIT-002)
  - 1 debt: €2,000 (tax)

## 🔧 H2 Console Access

To view the database directly:

1. Go to http://localhost:8080/h2-console
2. Use these settings:
   - **JDBC URL:** `jdbc:h2:mem:taxdebtdb`
   - **Username:** `sa`
   - **Password:** (leave empty)
3. Click "Connect"

## 📡 API Testing with cURL

### Create a Prepayment
```bash
curl -X POST http://localhost:8080/api/v1/prepayments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-$(date +%s)" \
  -d '{
    "citizenId": "550e8400-e29b-41d4-a716-446655440000",
    "prepaymentType": "VAT",
    "amount": 500.00,
    "currency": "EUR",
    "description": "Q1 VAT prepayment"
  }'
```

### Get Tax Credit Balance
```bash
curl http://localhost:8080/api/v1/citizens/550e8400-e29b-41d4-a716-446655440000/tax-credit
```

### List Debts
```bash
curl http://localhost:8080/api/v1/citizens/550e8400-e29b-41d4-a716-446655440000/debts
```

## 🎯 Key Features Demonstrated

✅ **Prepayment Management** - Create and confirm prepayments  
✅ **Tax Credit Tracking** - Real-time balance updates  
✅ **Debt Overview** - View all outstanding debts  
✅ **Idempotency** - Prevent duplicate transactions  
✅ **Optimistic Locking** - Concurrent modification prevention  
✅ **REST API** - Full OpenAPI/Swagger documentation  
✅ **Hexagonal Architecture** - Clean separation of concerns  

## 📚 Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Review the [implementation plans](plans/) for architecture details
- Explore the [domain documentation](docs/) for business rules
- Check the [API documentation](http://localhost:8080/swagger-ui.html) for all endpoints

## 🐛 Troubleshooting

**Port 8080 already in use?**
```bash
# Change port in src/main/resources/application.yml
server:
  port: 8081
```

**Build fails?**
```bash
# Ensure Java 17 is installed
java -version

# Clean and rebuild
mvn clean install -U
```

**Application won't start?**
```bash
# Check logs for errors
# Ensure no other Spring Boot apps are running
```

## 💡 Tips

- Use the **Swagger UI** for interactive API testing
- Check the **H2 Console** to see database changes in real-time
- The **Idempotency-Key** header prevents duplicate prepayments
- Sample data is loaded automatically on startup

---

**Happy Testing! 🎉**

For more information, see [README.md](README.md)