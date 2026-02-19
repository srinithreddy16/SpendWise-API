# Testing Guide for SpendWise API UI

This guide helps you test all SpendWise API endpoints and edge cases through the UI running at http://localhost:5173.

## Prerequisites

- **API** running at http://localhost:8080 (Docker or Maven)
- **UI** running at http://localhost:5173
- Browser with developer tools (Network tab) for inspecting requests

---

## 1. Authentication Testing

### Happy Path

1. **Register new user**
   - Fill form: email (e.g., `test@example.com`), password (min 8 chars), name
   - Submit → should show success, display `accessToken` and `refreshToken`
   - Tokens should be stored (check localStorage or UI state)

2. **Login**
   - Use registered email and password
   - Submit → should return tokens, UI should use accessToken automatically

3. **Refresh token**
   - Use the `refreshToken` from login/register
   - Call refresh endpoint → should return new `accessToken` and `refreshToken`

4. **Logout**
   - Click logout → should clear stored tokens
   - Protected endpoints should no longer work

### Edge Cases

- **Duplicate email registration**
  - Try registering with an email that already exists
  - Expected: Error "Email already in use"

- **Invalid login credentials**
  - Wrong password → "Invalid email or password"
  - Non-existent email → "Invalid email or password"

- **Invalid refresh token**
  - Use expired or invalid refreshToken
  - Expected: "Invalid or expired refresh token"

- **Access without token**
  - Logout, then try accessing any protected endpoint (e.g., GET /categories)
  - Expected: 401 Unauthorized, redirect to login

---

## 2. User Profile Testing

### Happy Path

- **GET /users/me**
  - After login, navigate to "Me" section
  - Should display: user id, email, name

### Edge Cases

- **Without authentication**
  - Logout, then try GET /users/me
  - Expected: 401 Unauthorized

---

## 3. Categories Testing

### Happy Path

1. **Create category**
   - Name: "Food"
   - Submit → should appear in categories list

2. **List categories**
   - Should show all categories for current user

3. **Get category by ID**
   - Click on a category → should show details

4. **Update category**
   - Change name (e.g., "Food" → "Groceries")
   - Submit → name should update in list

5. **Delete category**
   - Delete a category not used by expenses/budgets
   - Should disappear from list

### Edge Cases

- **Duplicate category name**
  - Create "Food", then try creating another "Food" (same user)
  - Expected: "A category with this name already exists"

- **Update to duplicate name**
  - Have "Food" and "Transport"
  - Update "Transport" to "Food"
  - Expected: "A category with this name already exists"

- **Delete category used by expenses**
  - Create category "Food"
  - Create expense with category "Food"
  - Try deleting "Food"
  - Expected: "Cannot delete category: it is used by expenses"

- **Delete category used by budgets**
  - Create category "Transport"
  - Create budget with categoryIds=["Transport"]
  - Try deleting "Transport"
  - Expected: "Cannot delete category: it is used by budgets"

- **Access another user's category**
  - User A creates category, User B tries to access it by ID
  - Expected: 401/403 Unauthorized

---

## 4. Expenses Testing

### Happy Path

1. **Create expense**
   - Category: select from dropdown (from GET /categories)
   - Amount: positive number (e.g., 50.00)
   - Description: optional text
   - Expense Date: YYYY-MM-DD format (e.g., 2025-02-15)
   - Submit → should appear in expenses list

2. **List expenses**
   - Default: page 0, size 10
   - Should show paginated results

3. **Filter expenses**
   - By category: select categoryId
   - By date range: fromDate, toDate
   - By amount: minAmount, maxAmount
   - Apply filters → list should update

4. **Pagination**
   - Change page number
   - Change page size
   - Results should paginate correctly

5. **Sorting**
   - Sort by expenseDate (desc/asc)
   - Sort by amount (desc/asc)
   - Results should be sorted

6. **Get expense by ID**
   - Click on expense → should show full details

7. **Update expense**
   - Change amount, category, date, or description
   - Submit → should update in list

8. **Delete expense**
   - Delete an expense
   - Should disappear from list (soft delete)

### Edge Cases - Budget Validation

**Test budget enforcement:**

1. Create category "Food"
2. Create budget: amount=500, year=2025, month=2, categoryIds=["Food"]
3. Create expense: category="Food", amount=300, date=2025-02-15 → **should succeed**
4. Create expense: category="Food", amount=250, date=2025-02-20 → **should fail** (300+250=550 > 500)
   - Expected: "Expense exceeds remaining monthly budget"
5. Update first expense: change amount to 100 → **should succeed** (100+250=350 < 500)
6. Create expense: category="Food", amount=250, date=2025-02-20 → **should now succeed**

**Other budget edge cases:**

- **Update expense amount to exceed budget**
  - Have budget=500, existing expense=300
  - Update expense amount to 300 → total=600 > 500
  - Expected: "Expense exceeds remaining monthly budget"

- **Update expense date to month with insufficient budget**
  - Expense in Feb with budget=500, already spent=400
  - Update expense date to Feb (same month) but amount increases total
  - Expected: "Expense exceeds remaining monthly budget"

- **Update expense category to one with lower budget**
  - Expense in category "Food" (budget=500)
  - Update category to "Transport" (budget=200, already spent=150)
  - If new total exceeds Transport budget → "Expense exceeds remaining monthly budget"

- **Create expense when no budget exists**
  - Create expense without creating a budget first
  - Expected: **should succeed** (no limit when no budget)

### Edge Cases - Validation

- **Invalid date range**
  - fromDate: 2025-02-20, toDate: 2025-02-15 (fromDate > toDate)
  - Expected: "fromDate must be before or equal to toDate"

- **Invalid amount range**
  - minAmount: 100, maxAmount: 50 (minAmount > maxAmount)
  - Expected: "minAmount must be less than or equal to maxAmount"

- **Invalid sort field**
  - Sort by "invalidField"
  - Expected: "Invalid sort field"

- **Invalid sort direction**
  - Sort direction: "invalid"
  - Expected: "Invalid sort direction"

- **Access another user's expense**
  - User A creates expense, User B tries to access it by ID
  - Expected: 401/403 Unauthorized

---

## 5. Budgets Testing

### Happy Path

1. **Create budget**
   - Amount: positive number (e.g., 500)
   - Year: valid year (e.g., 2025)
   - Month: 1-12 (e.g., 2 for February)
   - Category IDs: select one or more categories (multi-select)
   - Submit → should appear in budgets list

2. **List budgets**
   - Enter year and month (e.g., year=2025, month=2)
   - Should show budgets for that period

3. **Get budget by ID**
   - Click on budget → should show:
     - Amount
     - Year, month
     - Category IDs
     - **totalSpent** (calculated from expenses)
     - **remainingBudget** (amount - totalSpent)

4. **Update budget**
   - Change amount, year, month, or categoryIds
   - Submit → should update in list

5. **Delete budget**
   - Delete a budget
   - Should disappear from list (soft delete)

### Edge Cases

- **Duplicate budget**
  - Create budget: year=2025, month=2
  - Try creating another budget: year=2025, month=2 (same user)
  - Expected: "A budget already exists for this user, year and month"

- **Update budget to duplicate year/month**
  - Have budget for Feb 2025
  - Update another budget to Feb 2025
  - Expected: "A budget already exists for this user, year and month"

- **Invalid month**
  - Month < 1 (e.g., 0) or > 12 (e.g., 13)
  - Expected: "Month must be between 1 and 12"

- **Access another user's budget**
  - User A creates budget, User B tries to access it by ID
  - Expected: 401/403 Unauthorized

---

## 6. Integration Scenarios

### Budget Enforcement Flow

**Complete workflow:**

1. Create category "Food"
2. Create budget: amount=500, year=2025, month=2, categoryIds=["Food"]
3. Create expense: category="Food", amount=300, date=2025-02-15 → succeeds
4. Create expense: category="Food", amount=250, date=2025-02-20 → fails (300+250=550 > 500)
5. Update first expense: change amount to 100 → succeeds (100+250=350 < 500)
6. Create expense: category="Food", amount=250, date=2025-02-20 → now succeeds

**Verify:**
- Budget shows totalSpent=350, remainingBudget=150
- All expenses visible in list
- Budget prevents overspending

### Category Dependency Flow

**Test deletion constraints:**

1. Create category "Transport"
2. Create expense with category "Transport" → succeeds
3. Create budget with categoryIds=["Transport"] → succeeds
4. Try to delete "Transport" → fails (used by expense and budget)
   - Expected: "Cannot delete category: it is used by expenses" or "it is used by budgets"
5. Delete expense → succeeds
6. Try to delete "Transport" → fails (still used by budget)
   - Expected: "Cannot delete category: it is used by budgets"
7. Delete budget → succeeds
8. Delete "Transport" → now succeeds

### Multi-User Isolation

**Test security and data isolation:**

1. Register User A, login → get tokenA
2. Register User B, login → get tokenB
3. User A creates category "Food"
4. User B tries to access User A's category by ID → 401/403
5. User B creates own category "Food" → succeeds (names unique per user)
6. User A's expenses/budgets not visible to User B
7. User B cannot modify User A's data

---

## 7. UI-Specific Checks

### Network Inspection

- Open browser DevTools → Network tab
- Perform actions in UI
- Verify:
  - All requests include `Authorization: Bearer <token>` header (except /auth/*)
  - Request URLs are correct (base URL + endpoint)
  - Request bodies are properly formatted JSON
  - Response status codes match expectations (200, 201, 400, 401, 403, 404)

### Error Handling

- **401 responses**
  - Should clear stored token
  - Should redirect to login page
  - Should show "Session expired" or similar message

- **Error messages**
  - Should display API error messages clearly
  - Should not show raw stack traces or technical details

### Loading States

- Forms should show loading indicator during API calls
- Lists should show loading state while fetching
- Buttons should be disabled during submission

### Form Validation

- Required fields should be marked and validated
- Date format should be YYYY-MM-DD
- Amount should accept positive numbers
- Email format should be validated
- Password should meet minimum length (8 chars)

---

## Quick Test Checklist

Use this checklist to verify all major features:

- [ ] Register new user
- [ ] Login with credentials
- [ ] Refresh token
- [ ] Logout
- [ ] View current user profile
- [ ] Create category
- [ ] List categories
- [ ] Update category
- [ ] Delete unused category
- [ ] Try deleting category used by expense (should fail)
- [ ] Create expense
- [ ] List expenses with filters
- [ ] List expenses with pagination
- [ ] List expenses with sorting
- [ ] Update expense
- [ ] Delete expense
- [ ] Create budget
- [ ] List budgets by year/month
- [ ] View budget with totalSpent and remainingBudget
- [ ] Update budget
- [ ] Delete budget
- [ ] Test budget enforcement (create expense that exceeds budget)
- [ ] Test multi-user isolation (two users, verify data separation)

---

## Tips

- **Start fresh**: Clear browser data or use incognito mode for clean testing
- **Check console**: Look for JavaScript errors or warnings
- **Network tab**: Inspect all API requests and responses
- **Test edge cases**: Don't just test happy paths; try invalid inputs, duplicates, and boundary conditions
- **Budget math**: Keep track of budget amounts and expenses to verify calculations manually

---

## Troubleshooting

**CORS errors:**
- Ensure API CORS config allows http://localhost:5173
- Check API is running at http://localhost:8080

**401 errors:**
- Verify token is stored and sent in Authorization header
- Check token hasn't expired (access tokens expire in 15 minutes)
- Try refreshing token or logging in again

**Budget validation not working:**
- Ensure budget and expense are in the same month
- Ensure expense category matches budget categoryIds
- Check budget was created before the expense

**Category deletion fails:**
- Check if category is used by any expenses (even soft-deleted ones may block)
- Check if category is used by any budgets
