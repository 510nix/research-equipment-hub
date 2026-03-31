# PHASE 2 QUICK START & DEPLOYMENT
## Get Up and Running in 5 Minutes

---

## 🚀 **DEPLOYMENT STEPS (Copy & Paste Ready)**

### **Step 1: Close IDE & Reset Docker**

```bash
# Close VS Code first!
# Then in terminal:

docker compose down -v && docker compose up --build

# Wait for output showing [STABILITY] logs
# DONE when you see: "[STABILITY] DataInitializer completed successfully!"
```

### **Step 2: Access Application**

```
URL: http://localhost:8080
```

---

## ✅ **USER ACCEPTANCE TEST (5 Minutes)**

### **TEST 1: Admin Dashboard (Verify Phase 1 Still Works)**

1. Navigate to http://localhost:8080
2. Login: `admin` / `admin123`
3. Should see Admin Dashboard
   - ✅ Statistics cards
   - ✅ Users table
   - ✅ Equipment inventory table
4. **Result:** PASS ✅ or FAIL ❌

---

### **TEST 2: Browse & Search (Borrower Equipment Discovery)**

**Setup:** Register a PROVIDER first
1. Logout from admin
2. Register new account:
   - Username: `provider_test`
   - Email: `provider@test.com`
   - Password: `Provider@123`
   - **Role: PROVIDER**
3. Create an item:
   - Click "List Item" or go to /provider
   - Title: `Microscope`
   - Description: `Lab microscope`
   - Category: `Electronics`
   - Condition: `NEW`
4. Click "Save"

**Now Test Borrower Browsing:**
1. Logout
2. Register BORROWER:
   - Username: `borrower_test`
   - Email: `borrower@test.com`
   - Password: `Borrower@123`
   - **Role: BORROWER**
3. Should auto-redirect to `/browse`
4. **Verify:**
   - ✅ "Microscope" card visible
   - ✅ Title, category "Electronics", condition "NEW"
   - ✅ Two buttons: "👁️ View Details" + "✉️ Request This Item"
5. Test search:
   - Search "Microscope"
   - ✅ Results show "Microscope"
   - Search "Power" (non-existent)
   - ✅ Shows "No equipment available"
6. **Result:** PASS ✅ or FAIL ❌

---

### **TEST 3: Request Equipment (Core Workflow)**

**As Borrower (from TEST 2):**

1. On /browse, click "✉️ Request This Item" button on Microscope
2. Should open http://localhost:8080/requests/new/{itemId}
3. **Verify Form:**
   - ✅ "Request Equipment" title
   - ✅ Start Date date picker
   - ✅ End Date date picker
   - ✅ Message textarea
   - ✅ Submit button + Cancel button
4. **Fill Form:**
   - Start Date: (Select 5 days from today)
   - End Date: (Select 10 days from today)
   - Message: "I need this for my research project"
5. Click "Submit Request"
6. **Should redirect to /requests/my-requests**
7. **Verify My Requests Page:**
   - ✅ Success message: "Equipment request submitted successfully!"
   - ✅ Request appears in table
   - ✅ Status badge shows "PENDING" (yellow)
   - ✅ Request date range displayed
   - ✅ Your message visible
8. **Result:** PASS ✅ or FAIL ❌

---

### **TEST 4: Date Validation (Error Handling)**

**As Borrower:**

1. Go to `/requests/new/{itemId}` again
2. **Try Invalid Dates:**
   - Start Date: 10 days from today
   - End Date: 5 days from today (BEFORE start date)
3. Click Submit
4. **Should Show Error:** "End date cannot be before start date"
5. **Result:** PASS ✅ or FAIL ❌

---

### **TEST 5: My Requests Dashboard (Borrower)**

**As Borrower:**

1. Navigate to http://localhost:8080/my-requests
   - Or click on "My Equipment Requests" or similar link
2. **Verify Dashboard:**
   - ✅ Page title: "📋 My Equipment Requests"
   - ✅ Statistics cards show: Total=1, Pending=1, Approved=0, Rejected=0
   - ✅ Request card displays:
     - Equipment title: "Microscope"
     - Provider: "provider_test"
     - Status badge: "PENDING" (yellow)
     - Start/End dates
     - Your message
     - "View Equipment" button (should link to /browse/{itemId})
     - "Mark as Returned" button (disabled/hidden since not APPROVED)
3. **Result:** PASS ✅ or FAIL ❌

---

### **TEST 6: Security - Unauthorized Access**

**Test Path Protection:**

1. Logout (click Logout)
2. Try to access protected pages:

   **Attempt 1:** Go to http://localhost:8080/requests/my-requests
   - ✅ Should redirect to login (401) or show login page
   
   **Attempt 2:** Go to http://localhost:8080/browse
   - ✅ Should redirect to login

3. **Result:** PASS ✅ or FAIL ❌

---

## 🧪 **AUTOMATED TEST VERIFICATION**

Run the test suite to verify all components:

```bash
# Navigate to project directory
cd d:\Project\SEPM\research-equipment-hub

# Run ALL tests
mvn test

# Expected output:
# [INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS
```

**Individual Tests:**

```bash
# Just RequestService tests
mvn test -Dtest=RequestServiceTest

# Just BrowseController integration tests
mvn test -Dtest=BrowseControllerIntegrationTest

# Specific test method
mvn test -Dtest=RequestServiceTest#testCreateRequest_ValidData_SuccessfullySaved
```

---

## 📋 **FINAL CHECKLIST**

### **Code Implemented**
- [ ] RequestDto created
- [ ] RequestService created with 10 methods
- [ ] RequestController created with 7 endpoints
- [ ] browse.html updated with request buttons
- [ ] my-requests.html template created
- [ ] request-form.html template created
- [ ] RequestService unit tests (13 tests)
- [ ] BrowseController integration tests (8 tests)
- [ ] SecurityConfig updated

### **Deployment Complete**
- [ ] Docker containers rebuild and run without errors
- [ ] [STABILITY] logs appear in Docker output
- [ ] No 404/500 errors on admin dashboard

### **User Acceptance Tests PASS**
- [ ] TEST 1: Admin Dashboard works ✅
- [ ] TEST 2: Browse & Search works ✅
- [ ] TEST 3: Request Equipment workflow complete ✅
- [ ] TEST 4: Date validation works ✅
- [ ] TEST 5: My Requests dashboard displays correctly ✅
- [ ] TEST 6: Security prevents unauthorized access ✅

### **Automated Tests PASS**
- [ ] `mvn test` shows 18 tests with 0 failures

---

## 🎯 **SUCCESS CRITERIA**

✅ **Phase 2 is successful when:**

1. Borrower can browse equipment with search
2. Borrower can request equipment with date validation
3. Borrower can see all requests on My Requests dashboard
4. Status badges display correct colors for PENDING/APPROVED/REJECTED
5. Unauthorized users cannot access /browse or /requests/**
6. All 18+ tests pass without errors
7. No 404/500 errors in application logs
8. Docker logs show [REQUEST] and [BROWSE] prefixed messages

---

## 🐛 **Quick Troubleshooting**

| Issue | Solution |
|-------|----------|
| 404 on /requests/new | Verify RequestController exists in `src/main/java/.../controller/` |
| "No equipment available" | Verify provider created an item (status = AVAILABLE) |
| Date validation not working | Verify RequestService.validateDates() method exists |
| Tests fail | Run `mvn clean test` to rebuild |
| 403 Forbidden on /requests | Verify SecurityConfig has `/requests/**` → authenticated() |
| Page styling broken | Clear browser cache (Ctrl+Shift+Del) |

---

## 📊 **Expected Test Output**

```
Test Run Summary
───────────────────────────────────────────
RequestService Tests:          13 PASSED ✅
BrowseController Tests:         8 PASSED ✅
───────────────────────────────────────────
Total: 21 PASSED, 0 FAILED
Build: SUCCESS ✅
Execution Time: ~3-5 seconds
```

---

## 🚀 **Next Steps (Phase 3)**

Once Phase 2 is fully verified:

1. **Provider Request Management**
   - Create /provider/requests endpoint
   - Show incoming requests in table
   - Add approve/reject buttons

2. **Email Notifications**
   - Send email when request approved/rejected
   - Send email to providers of new requests

3. **Enhanced Features**
   - Request cancellation
   - Rental history
   - Equipment ratings

---

## 📞 **Support**

**Docker logs not showing [REQUEST] messages?**
```bash
docker logs equipment-hub-app -f  # Follow logs
```

**Database schema verification:**
```bash
docker exec -it equipment-hub-db psql -U user_name -d research_hub -c "\d requests"
```

**Clear cache & restart:**
```bash
docker compose down -v
docker compose up --build
```

---

**Status:** ✅ PHASE 2 READY FOR TESTING  
**Estimated Test Time:** 15 minutes  
**Expected Test Pass Rate:** 100%
