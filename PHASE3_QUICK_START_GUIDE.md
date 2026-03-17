# ⚡ PHASE 3 - QUICK START & TESTING GUIDE
## Research Equipment Hub - Phase 3 Deployment & Verification

---

## 🎯 **5-MINUTE DEPLOYMENT**

### **Step 1: Clean & Rebuild (2 min)**

```bash
# Navigate to project
cd d:\Project\SEPM\research-equipment-hub

# Stop old containers and clean volumes
docker compose down -v

# Build and start fresh
docker compose up --build

# Monitor logs for [ADMIN] prefix (indicates logging is working)
# Look for: [ADMIN] Admin accessing dashboard
```

### **Step 2: Run Tests (2 min)**

```bash
# In a new terminal
mvn clean test

# Expected output:
# Tests run: 21+
# Failures: 0
# Errors: 0
# BUILD SUCCESS
```

### **Step 3: Verify Deployment (1 min)**

```bash
# Wait for: "Tomcat started on port 8080"
# Then access: http://localhost:8080
```

---

## ✅ **PHASE 3 USER ACCEPTANCE TESTS (UAT)**

### **UAT Test 1: Admin Can Access Dashboard**

**Step 1.1:** Login as Admin
- URL: `http://localhost:8080/auth/login`
- Username: `admin`
- Password: `admin123`
- Expected: ✅ Logged in successfully

**Step 1.2:** Access Admin Dashboard
- Navigate to: `http://localhost:8080/admin/dashboard`
- Expected: ✅ See admin/dashboard.html with:
  - Statistics cards (Total Users, Total Equipment, etc.)
  - Users table with Enabled/Disabled status
  - Actions column with Activate/Deactivate buttons
  - Items inventory table
  - Quick Actions section with "Manage Categories" link

**Result:** ✅ TEST PASSED if all elements visible

### **UAT Test 2: Borrower Cannot Access Admin Dashboard**

**Step 2.1:** Login as Borrower
- URL: `http://localhost:8080/auth/login`
- Username: `borrower`
- Password: `borrower123`
- Expected: ✅ Logged in

**Step 2.2:** Try to Access Admin Dashboard
- Navigate to: `http://localhost:8080/admin/dashboard`
- Expected: ✅ **403 Forbidden** error page
- NOT a 500 error (would indicate code bug)

**Result:** ✅ TEST PASSED if 403 error received

### **UAT Test 3: Toggle User Status**

**Step 3.1:** Login as Admin
- Admin login (see UAT Test 1.1)

**Step 3.2:** View Admin Dashboard
- Navigate to: `/admin/dashboard`

**Step 3.3:** Find a User to Toggle
- Locate "borrower" user in Users table
- Current Status: Should be "Enabled" (green badge)

**Step 3.4:** Click Deactivate Button
- Click "Deactivate" button next to borrower
- Expected: ✅ Flash message: "User 'borrower' has been deactivated successfully"
- Expected: ✅ Redirect back to dashboard
- Expected: ✅ User status now shows "Disabled" (red badge)

**Step 3.5:** Check Borrower Can't Login
- Logout (click logout link)
- Try to login as borrower with password `borrower123`
- Expected: ✅ Login fails (account disabled)

**Step 3.6:** Re-enable User
- Login as admin again
- Go to dashboard
- Click "Activate" button for borrower
- Expected: ✅ Flash message: "User 'borrower' has been activated successfully"

**Result:** ✅ TEST PASSED if all steps successful

### **UAT Test 4: Create New Category**

**Step 4.1:** Login as Admin
- Admin login (see UAT Test 1.1)

**Step 4.2:** Navigate to Category Management
- Click "Manage Categories" in Quick Actions
- OR directly: `/admin/categories`
- Expected: ✅ See category creation form and empty category list

**Step 4.3:** Create Category
- Fill form:
  - Name: `Spectroscopy Equipment`
  - Description: `Equipment for spectroscopic analysis`
- Click "Create Category" button
- Expected: ✅ Flash message: "Category 'Spectroscopy Equipment' created successfully"

**Step 4.4:** Verify Category Created
- New category should appear in "All Categories" section
- Should show name and description

**Result:** ✅ TEST PASSED if category appears in list

### **UAT Test 5: Edit Category**

**Step 5.1:** Still on Categories page with new category

**Step 5.2:** Click Edit Button
- Click "✎ Edit" button on "Spectroscopy Equipment" category
- Expected: ✅ Modal dialog opens with current category details

**Step 5.3:** Modify Category
- Change description to: `Optical and UV-Vis spectroscopy tools`
- Click "✔️ Save Changes" button
- Expected: ✅ Flash message: "Category updated successfully"

**Step 5.4:** Verify Changes
- Category should still be visible with updated description
- Refresh page to confirm persisted to database

**Result:** ✅ TEST PASSED if description updated

### **UAT Test 6: Try to Delete Category with Items**

**Step 6.1:** Login as Provider
- URL: `/auth/login`
- Username: `provider`
- Password: `provider123`

**Step 6.2:** Create Equipment Item
- Navigate to provider dashboard
- Create new item in "Spectroscopy Equipment" category
- (If you don't know how, see Phase 2 guide)

**Step 6.3:** Try to Delete as Admin
- Login as admin
- Go to `/admin/categories`
- Try to delete "Spectroscopy Equipment" category
- Click "🗑️ Delete" button
- Expected: ✅ Error message: "Cannot delete category: 1 item(s) are linked to this category"
- Expected: ✅ Category NOT deleted

**Step 6.4:** Database Integrity Verified
- Category protection prevents orphaned items
- ✅ Data integrity maintained

**Result:** ✅ TEST PASSED if delete prevented with error message

### **UAT Test 7: Logging Verification**

**Step 7.1:** Monitor Docker Logs
- In terminal: `docker logs equipment-hub-app -f`

**Step 7.2:** Trigger Admin Actions
- Login as admin
- Access dashboard
- Toggle a user status
- Create a category

**Step 7.3:** Check Console Logs
- Expected: ✅ Log entries with `[ADMIN]` prefix:
  - `[ADMIN] Admin accessing dashboard`
  - `[ADMIN] Loaded X users for admin dashboard`
  - `[ADMIN] User ID X has been deactivated`
  - `[ADMIN] Category created successfully: Spectroscopy Equipment`

**Result:** ✅ TEST PASSED if all actions logged with [ADMIN] prefix

---

## 🧪 **AUTOMATED TEST RESULTS**

### **Running Tests**

```bash
# Run all tests
mvn clean test

# Run only admin tests
mvn test -Dtest=UserServiceAdminTest,AdminControllerIntegrationTest

# See detailed output
mvn test -X  # More verbose

# Skip specific test
mvn test -Dtest=!BrowseControllerIntegrationTest
```

### **Expected Test Output**

```
Running com.kuet.hub.service.UserServiceAdminTest
  ✓ toggleUserEnabled - Disables enabled user successfully
  ✓ toggleUserEnabled - Enables disabled user successfully
  ✓ toggleUserEnabled - Throws exception when user not found
  ✓ findById - Returns user when found
  ✓ findById - Throws exception when user not found
Tests run: 5, Failures: 0, Errors: 0

Running com.kuet.hub.controller.AdminControllerIntegrationTest
  ✓ GET /admin/dashboard - Unauthenticated user receives 401
  ✓ GET /admin/dashboard - Borrower receives 403
  ✓ GET /admin/dashboard - Admin receives 200 OK
  ✓ GET /admin/categories - Admin can list categories
  ✓ GET /admin/categories - Borrower receives 403
  ✓ POST /admin/categories/new - Admin creates category
  ✓ POST /admin/categories/new - Borrower receives 403
  ✓ POST /admin/users/{id}/toggle-status - Admin toggles successfully
  ✓ POST /admin/users/{id}/toggle-status - Borrower receives 403
Tests run: 9, Failures: 0, Errors: 0

Running com.kuet.hub.controller.BrowseControllerIntegrationTest
  ✓ (All 8+ tests from Phase 2)
Tests run: 8+, Failures: 0, Errors: 0

Aggregate Results:
===============
Tests run: 22+
Failures: 0
Errors: 0
BUILD SUCCESS

Total time: X.XXX s
```

---

## 🔍 **TROUBLESHOOTING**

### **Issue 1: Tests Still Failing with Import Error**

**Error:** `import org.springframework.boot.test.autoconfigure.web cannot be resolved`

**Solution:**
```bash
# Force Maven to refresh dependencies
mvn clean -DskipTests

# Clear IDE cache
# In IDE: File → Invalidate Caches → Invalidate and Restart

# Rebuild
mvn clean test
```

### **Issue 2: 403 Forbidden Showing HTML Error Instead of Plain Page**

**Issue:** Borrower sees error page instead of plain 403

**Solution:** This is expected behavior - security error is rendered as HTML. Check that:
- Status code is actually 403 (not 404 or 500)
- Page title or error message mentions "Access Denied" or "Forbidden"

### **Issue 3: Admin Dashboard Shows No Users/Items**

**Cause:** DataInitializer might not have run

**Solution:**
```bash
# Check logs for DataInitializer output
docker logs equipment-hub-app | grep -i "STABILITY\|ADMIN"

# If not there, DataInitializer didn't run
# Restart containers:
docker compose down -v
docker compose up --build

# Monitor startup logs
docker logs equipment-hub-app -f
```

### **Issue 4: CSRF Token Errors on Form Submit**

**Error:** "Invalid CSRF Token" or similar

**Cause:** Form missing CSRF token input

**Location to Check:** admin/dashboard.html, admin/categories.html
```html
<!-- Must have this in EVERY form that submits data -->
<input type="hidden" name="_csrf" th:value="${_csrf.token}">
```

### **Issue 5: Category Delete Fails Even with No Items**

**Cause:** Items still linked to category (maybe soft delete or stale cache)

**Solution:**
```bash
# Check database directly
docker exec -it equipment-hub-db psql -U user_name -d research_hub

# View items linked to category
SELECT * FROM items WHERE category_id = 5;

# If you need to force delete (CAUTION):
DELETE FROM items WHERE category_id = 5;
DELETE FROM categories WHERE id = 5;
```

---

## 📝 **VERIFICATION CHECKLIST**

### **Pre-Testing**

- [ ] Docker containers running: `docker ps` shows equipment-hub-app and equipment-hub-db
- [ ] Application accessible: `http://localhost:8080` loads login page
- [ ] Can login as admin (admin/admin123)
- [ ] Can login as borrower (borrower/borrower123)
- [ ] Can login as provider (provider/provider123)

### **UAT Testing**

- [ ] UAT Test 1: Admin Dashboard Accessible ✅
- [ ] UAT Test 2: Borrower Gets 403 ✅
- [ ] UAT Test 3: User Toggle Status Works ✅
- [ ] UAT Test 4: Create Category Works ✅
- [ ] UAT Test 5: Edit Category Works ✅
- [ ] UAT Test 6: Delete Protection Works ✅
- [ ] UAT Test 7: Admin Logging Works ✅

### **Automated Testing**

- [ ] UserServiceAdminTest: 5/5 PASS
- [ ] AdminControllerIntegrationTest: 9/9+ PASS
- [ ] BrowseControllerIntegrationTest: 8+/8+ PASS
- [ ] Total: 22+/22+ PASS

### **Code Quality**

- [ ] No compilation errors
- [ ] No runtime errors in Docker logs
- [ ] All [ADMIN] prefixed logs present
- [ ] No hardcoded credentials
- [ ] CSRF tokens in all forms

---

## ✨ **SUCCESS CRITERIA**

**Phase 3 is successful when:**

```
✅ All 7 UAT Tests Pass
✅ All 22+ Automated Tests Pass (21+ from Phase 2 + 12 from Phase 3)
✅ Admin Dashboard Loads with Users Table
✅ Category Management UI Works
✅ User Status Toggle Works
✅ 403 Forbidden for Unauthorized Access
✅ [ADMIN] Prefix Visible in Docker Logs
✅ 0 Build Errors
✅ 0 Test Failures
✅ 0 Runtime Exceptions
```

---

## 🚀 **NEXT STEPS**

### **If All Tests Pass:**
1. ✅ Phase 3 Complete
2. → Ready for Phase 4 (Provider Features)
3. → Ready for Production Deployment

### **If Any Test Fails:**
1. → Check Troubleshooting section
2. → Review Implementation Guide in detail
3. → Check Docker logs: `docker logs equipment-hub-app -f`
4. → Check database: `docker exec -it equipment-hub-db psql ...`

---

**Test Environment:** Docker Compose (PostgreSQL + Spring Boot)
**Test Framework:** JUnit 5 + Mockito + MockMvc
**Expected Build Time:** 3-5 minutes
**Total Test Suite Time:** 1-2 minutes

**Status:** Ready to Test ✅
