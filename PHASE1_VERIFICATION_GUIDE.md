# PHASE 1 VERIFICATION GUIDE
## End-to-End Smoke Test for the Transactional Core

### Prerequisites
- ✅ Docker containers are running and healthy
- ✅ DataInitializer logs show all [STABILITY] messages
- ✅ Application is accessible at `http://localhost:8080`

---

## TEST FLOW 1: Admin Dashboard Verification

### Step 1.1: Admin Login
1. Navigate to `http://localhost:8080`
2. You should be redirected to the login page
3. Log in with:
   - **Username:** `admin`
   - **Password:** `admin123`

**Expected Result:**
- ✅ Login successful (no errors)
- ✅ Dashboard redirects to `/admin/dashboard` (NOT a 404 or "Something went wrong" error)
- ✅ Admin panel displays with statistics cards

**What to Look For in Admin Dashboard:**
- ✅ "System Administration Dashboard" header appears
- ✅ Four stat cards show:
  - Total Users: Should show at least 1 (the admin user)
  - Total Equipment Items: Starting count (0 if no items seeded)
  - Inventory: Same as Equipment count
  - System Status: Active (✓)
- ✅ Users table displays with columns: ID, Username, Email, Roles, Status
- ✅ Equipment Inventory table is present (may be empty or populated)

**Docker Console Check:**
```
[STABILITY] Admin accessing dashboard
[STABILITY] Loaded X users for admin dashboard
[STABILITY] Loaded Y items for admin dashboard
[STABILITY] Admin dashboard initialized successfully
```

### Step 1.2: Inspect the Database Schema
1. Open a new terminal while containers are running
2. Connect to PostgreSQL:
   ```bash
   docker exec -it equipment-hub-db psql -U user_name -d research_hub
   ```
3. Check that the `requests` table exists:
   ```sql
   \dt
   ```

**Expected Result:**
You should see these tables:
```
               List of relations
 Schema |        Name        | Type  | Owner
--------+--------------------+-------+----------
 public | requests           | table | user_name
 public | items              | table | user_name
 public | categories         | table | user_name
 public | roles              | table | user_name
 public | user_roles         | table | user_name
 public | users              | table | user_name
```

4. Verify the `requests` table structure:
   ```sql
   \d requests
   ```

**Expected Schema:**
```
            Table "public.requests"
   Column   |           Type           | Modifiers
------------+--------------------------+-----------
 id         | bigint                   | NOT NULL
 borrower_id| bigint                   | NOT NULL
 item_id    | bigint                   | NOT NULL
 start_date | date                     | NOT NULL
 end_date   | date                     | NOT NULL
 message    | character varying(500)   |
 status     | character varying(255)   | NOT NULL
 created_at | timestamp without time zone | NOT NULL
```

5. Exit PostgreSQL:
   ```sql
   \q
   ```

---

## TEST FLOW 2: Provider Registration and Item Creation

### Step 2.1: Register a Provider Account
1. Log out from the admin account at `http://localhost:8080/auth/logout`
2. Click "Register" or navigate to `http://localhost:8080/auth/register`
3. Register a new provider account:
   - **Username:** `azrof_provider`
   - **Email:** `azrof.provider@kuet.edu`
   - **Password:** `Provider@2025`
   - **Confirm Password:** `Provider@2025`
   - **Role:** Select **"PROVIDER"**
4. Click "Register"

**Expected Result:**
- ✅ Registration successful
- ✅ Automatically logged in (or redirected to login)
- ✅ Dashboard redirects to `/provider/dashboard`

### Step 2.2: Add Equipment Item
1. On the provider dashboard, click **"+ List Item"** (or navigate to item creation page)
2. Fill in the form:
   - **Title:** `Microscope`
   - **Description:** `High-powered research microscope for laboratory use`
   - **Category:** Select **"Electronics"** (seeded by DataInitializer)
   - **Condition:** Select **"NEW"**
3. Click **"Save"** or **"List Equipment"**

**Expected Result:**
- ✅ Item created successfully
- ✅ Item appears in provider's inventory list
- ✅ Status shows as "AVAILABLE"

**Docker Console Check:**
```
[STABILITY] Item created successfully by provider
```

### Step 2.3: Admin Verification of Inventory
1. Log out from provider account at `/auth/logout`
2. Log back in as admin:
   - **Username:** `admin`
   - **Password:** `admin123`
3. Navigate to `/admin/dashboard`
4. Scroll to "Equipment Inventory" section

**Expected Result:**
- ✅ "Microscope" appears in the inventory table
- ✅ Owner shows as `azrof_provider`
- ✅ Category shows as "Electronics"
- ✅ Condition shows as "NEW"
- ✅ Status shows as "AVAILABLE"

---

## TEST FLOW 3: Borrower Browsing Equipment

### Step 3.1: Register a Borrower Account
1. Log out from admin account at `/auth/logout`
2. Navigate to `http://localhost:8080/auth/register`
3. Register a borrower account:
   - **Username:** `azrof_borrower`
   - **Email:** `azrof.borrower@kuet.edu`
   - **Password:** `Borrower@2025`
   - **Confirm Password:** `Borrower@2025`
   - **Role:** Select **"BORROWER"**
4. Click "Register"

**Expected Result:**
- ✅ Registration successful
- ✅ Dashboard redirects to `/browse` (Equipment browsing page)

### Step 3.2: Verify Equipment is Visible
1. On the `/browse` page, you should see the equipment listing
2. Look for the "Microscope" card

**Expected Result:**
- ✅ **BEFORE Fix:** "No equipment available" message disappeared
- ✅ **AFTER Fix:** "Microscope" card is visible with:
  - ✅ Title: "Microscope"
  - ✅ Owner: "azrof_provider"
  - ✅ Category: "Electronics"
  - ✅ Condition: "NEW"
  - ✅ Status: "AVAILABLE"
  - ✅ "View Details" or "Request" button appears

**Docker Console Check:**
```
[STABILITY] Borrower accessing browse equipment page
[STABILITY] Loaded available equipment for borrower
```

### Step 3.3: View Equipment Details (Optional)
1. Click on the Microscope card or "View Details" button
2. The item detail page should display

**Expected Result:**
- ✅ Full item details are displayed
- ✅ Owner information is shown
- ✅ "Request Equipment" or "Borrow" button is available

---

## TEST FLOW 4: Request Entity Functionality (Foundation for Phase 2)

### Step 4.1: Verify RequestRepository Methods
The Request entity is now in the database schema. In Phase 2, you will:
1. Create a RequestService that uses these repository methods:
   - `findByBorrower(User)` — Fetch all requests by a specific borrower
   - `findByItemOwner(User)` — Fetch all requests for a provider's items

### Step 4.2: Database Insertion (Manual Verification)
When a borrower submits a request (in Phase 2), a new record will be created:
```sql
INSERT INTO requests (borrower_id, item_id, start_date, end_date, message, status, created_at)
VALUES (2, 1, '2025-03-20', '2025-03-25', 'Need microscope for lab', 'PENDING', NOW());
```

---

## TROUBLESHOOTING

### Issue: "Something went wrong" on admin dashboard
**Solution:**
1. Check AdminController logs in Docker console
2. Verify UserService.findAllUsers() method exists
3. Restart containers: `docker compose restart app`

### Issue: `/admin/dashboard` returns 404
**Solution:**
1. Verify AdminController.java was created in `src/main/java/com/kuet/hub/controller/`
2. Verify `@RequestMapping("/admin")` annotation is present
3. Verify `@PreAuthorize("hasRole('ADMIN')")` annotation is present
4. Rebuild the application: `docker compose down -v && docker compose up --build`

### Issue: Admin logs in but redirects to `/dashboard` instead of `/admin/dashboard`
**Solution:**
1. Check SecurityConfig.java `defaultSuccessUrl` — should be `/dashboard` (which then routes to `/admin/dashboard`)
2. Verify the DashboardController redirects correctly based on roles

### Issue: Requests table not found in database
**Solution:**
1. Clean Database: `docker compose down -v`
2. Rebuild: `docker compose up --build`
3. Wait for DataInitializer to complete
4. Verify with: `docker exec -it equipment-hub-db psql -U user_name -d research_hub -c "\dt requests"`

### Issue: Categories not seeded
**Solution:**
1. Check DataInitializer logs for [STABILITY] messages
2. If categories are missing, manually seed them:
   ```sql
   INSERT INTO categories (name, description) VALUES 
   ('Electronics', 'Default Electronics category'),
   ('Lab Equipment', 'Default Lab Equipment category'),
   ('Mechanical Tools', 'Default Mechanical Tools category'),
   ('Textbooks', 'Default Textbooks category');
   ```

---

## VERIFICATION CHECKLIST

- [ ] Admin can log in without "Something went wrong" error
- [ ] Admin dashboard displays users and items
- [ ] `requests` table exists in PostgreSQL
- [ ] Provider can register and add equipment
- [ ] Equipment appears in admin dashboard
- [ ] Borrower can register and browse equipment
- [ ] Equipment card displays for borrower (no "No equipment available" message)
- [ ] SecurityConfig protects all `/admin/**` paths
- [ ] Docker logs show [STABILITY] messages
- [ ] No 404 or 500 errors on admin/dashboard

---

## NEXT STEPS FOR PHASE 2

Once all tests pass, you're ready to implement:
1. **RequestService** — Business logic for requesting equipment
2. **RequestController** — Endpoints for handling requests (/requests/create, /requests/approve, etc.)
3. **Request DTOs** — Data transfer objects for request forms
4. **Email Notifications** — Notify providers and borrowers of request status changes
5. **Request History UI** — Allow borrowers to see their requests and providers to see incoming requests

---

## TECHNICAL SUMMARY

### What Was Implemented

#### 1. Request Entity (`com.kuet.hub.entity.Request`)
- **Fields:** id, startDate, endDate, message, status, createdAt
- **Relationships:**
  - `@ManyToOne` with User (borrower)
  - `@ManyToOne` with Item (equipment being requested)
- **Status Enum:** PENDING, APPROVED, REJECTED, COMPLETED

#### 2. RequestRepository (`com.kuet.hub.repository.RequestRepository`)
- Extends `JpaRepository<Request, Long>`
- Query Method 1: `findByBorrower(User)` — Borrower's request history
- Query Method 2: `findByItemOwner(User)` — Provider's incoming requests

#### 3. AdminController (`com.kuet.hub.controller.AdminController`)
- @RequestMapping("/admin")
- @PreAuthorize("hasRole('ADMIN')")
- Dashboard endpoint: GET /admin/dashboard
- Loads: allUsers, allItems
- Returns: admin/dashboard view

#### 4. Admin Dashboard Template (`admin/dashboard.html`)
- Statistics cards (user count, item count, etc.)
- Users table with role badges
- Equipment inventory table with status badges
- Responsive Bootstrap 5 design

### Database Schema (requests table)
```sql
CREATE TABLE requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL REFERENCES users(id),
    item_id BIGINT NOT NULL REFERENCES items(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    message VARCHAR(500),
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL
);
```

---

**Author:** Senior Backend Architect  
**Date:** Phase 1 Implementation  
**Status:** ✅ Ready for Testing
