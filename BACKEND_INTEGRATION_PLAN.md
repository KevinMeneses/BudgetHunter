# COMPREHENSIVE BACKEND API INTEGRATION PLAN

## CURRENT STATUS SUMMARY (Updated: 2025-10-25)

### 📢 RECENT UPDATES (2025-10-25)
**RESTful API Migration**: The backend API has been refactored to follow RESTful conventions. All client endpoints have been updated accordingly:

**Budget Endpoints:**
- Create Budget: `POST /api/budgets/create_budget` → `POST /api/budgets`
- Get Budgets: `GET /api/budgets/get_budgets` → `GET /api/budgets`

**Budget Entry Endpoints (to be implemented in Phase 5):**
- Create Entry: `PUT /api/budgets/put_entry` → `POST /api/budgets/{budgetId}/entries`
- Update Entry: `PUT /api/budgets/put_entry` → `PUT /api/budgets/{budgetId}/entries/{entryId}`
- Get Entries: `GET /api/budgets/get_entries?budgetId={id}` → `GET /api/budgets/{budgetId}/entries`
- Stream Events: `GET /api/budgets/new_entry?budgetId={id}` → `GET /api/budgets/{budgetId}/entries/stream`

**Collaborator Endpoints (to be implemented in Phase 6):**
- Add Collaborator: `POST /api/budgets/add_collaborator` → `POST /api/budgets/{budgetId}/collaborators`
- Get Collaborators: `GET /api/budgets/get_collaborators?budgetId={id}` → `GET /api/budgets/{budgetId}/collaborators`

**Request Body Changes:**
- `CreateBudgetEntryRequest`: Removed `budgetId` field (now in URL path)
- `UpdateBudgetEntryRequest`: Removed `id` and `budgetId` fields (now in URL path)
- `AddCollaboratorRequest`: Still contains `budgetId` in body per backend spec

**Legacy Endpoints:** All legacy endpoints with verbs in URLs are still available but deprecated.

### ✅ COMPLETED PHASES
- **Phase 1: Foundation & Infrastructure** - 100% Complete (5/5 tasks)
- **Phase 2: Authentication System** - 100% Complete (9/9 tasks)
- **Phase 3: Database Migration** - 100% Complete (3/3 tasks)
- **Phase 4: Budget Sync Implementation** - 100% Complete (6/6 tasks, 1 skipped optional)

### 🔄 CURRENT STATE
The app now has **fully functional budget synchronization** with:
- ✅ User authentication (sign-up, sign-in, token storage)
- ✅ Auto-sync after sign-in (budgets appear automatically)
- ✅ Budget CRUD API service with **RESTful endpoints** (POST/GET `/api/budgets`)
- ✅ Budget sync manager (push/pull/full sync)
- ✅ Automatic background sync on budget create/update
- ✅ Pull-to-refresh manual sync UI
- ✅ Database schema with sync fields (server_id, is_synced, timestamps)
- ✅ Working end-to-end budget sync (local ↔ server)
- ✅ **UPDATED 2025-10-25**: Migrated to RESTful API conventions following backend refactoring

### ⚠️ IMPORTANT ARCHITECTURAL DECISIONS MADE
1. **Use Cases Skipped**: ViewModels call repositories directly (matches existing app pattern)
2. **Separate ViewModels**: SignInViewModel and SignUpViewModel instead of single AuthViewModel
3. **Authentication Required**: App now requires sign-in (not optional as originally planned)
4. **Sign-up Flow**: Sign-up returns user info only; user must sign-in separately to get tokens
5. **Hardcoded Base URL**: Currently "http://10.0.2.2:8080" for Android emulator
6. **RESTful API Migration**: Updated to use RESTful conventions (POST `/api/budgets` instead of `/api/budgets/create_budget`, GET `/api/budgets` instead of `/api/budgets/get_budgets`)
7. **Request Body Cleanup**: Removed redundant fields from request bodies - budgetId now passed in URL path for entries, not in body

### ⏭️ NEXT IMMEDIATE STEPS (Priority Order)
1. **Phase 5: Budget Entry Sync** - Implement entry synchronization (13 hours) **← RECOMMENDED NEXT**
2. **Phase 9: Error Handling & Offline Support** - Critical for production (10 hours)
3. **Phase 6: Collaborator Management** - Budget sharing (8 hours)

### 📊 PROGRESS METRICS
- **Total Phases**: 11
- **Completed Phases**: 4 (36%)
- **In Progress**: None - ready for Phase 5!
- **Total Tasks**: ~72 (added Task 2.8 and 2.9)
- **Completed Tasks**: ~26 (36%)
- **Estimated Remaining Time**: ~75 hours (~1.9 weeks)

### 🚨 CRITICAL GAPS & RISKS
1. ~~**No Database Schema Changes Yet**~~ ✅ - Budget/BudgetEntry tables now have sync fields
2. ~~**No Budget API Services**~~ ✅ - BudgetApiService working with correct endpoints
3. ~~**No Budget Sync Logic**~~ ✅ - Budget sync working end-to-end
4. ~~**User Data Isolation**~~ ✅ - Sign out now clears all local data (Task 2.8 complete)
5. **No Budget Entry Sync** - Entries don't sync to server yet (Phase 5)
6. **Hardcoded Backend URL** - Not configurable per environment (Task 10.7)
7. **No Offline Support** - Network errors not handled gracefully (Phase 9)
8. **No Real-time Updates** - SSE not implemented (Phase 7)
9. **No Collaborator Management** - Can't share budgets yet (Phase 6)
10. **Token Refresh Not Automatic** - Auth plugin refresh logic incomplete (Task 9.5)

---

## OVERVIEW
Migrate BudgetHunter from a local-only Android app using SqlDelight to a full-stack application with backend API integration, JWT authentication with token rotation, real-time SSE updates, and offline-first capabilities. The migration preserves existing functionality while adding multi-user collaboration and cloud sync.

---

## RISK ASSESSMENT

### HIGH-RISK AREAS
1. **Data Migration**: Existing local budgets/entries must be preserved and migrated to the backend
2. **Authentication Flow**: Adding auth could break existing navigation and user experience
3. **Database Schema Changes**: Need to track server IDs alongside local IDs without breaking existing queries
4. **Real-time Updates**: SSE integration could cause UI update conflicts with local changes
5. **Offline Support**: Must maintain app functionality when backend is unavailable

### MITIGATION STRATEGIES
1. Use feature flags to gradually enable backend features
2. Implement dual-mode operation (local-only vs. synced)
3. Add migration layer to gradually transition data without breaking existing code
4. Use repository pattern abstraction to swap implementations safely
5. Comprehensive rollback strategy for each phase

---

## PHASE 1: FOUNDATION & INFRASTRUCTURE (LOW RISK)

### Task 1.1: Add Network Dependencies ✅ COMPLETED
**Effort**: 0.5 hours
**Risk**: Very Low
**Description**: Add required Ktor dependencies for REST API and SSE support

**Deliverable**:
- Update `build.gradle.kts` with:
  - `ktor-client-auth` for JWT handling
  - `ktor-client-sse` for Server-Sent Events (already have Ktor client)
  - `androidx.security:security-crypto` for encrypted token storage

**Validation**:
- ✅ Gradle sync succeeds
- ✅ Build completes without errors
- ✅ Existing app runs without issues

**Rollback**: Remove added dependencies

**Completion Notes**: Added `ktor-client-auth`, `ktor-client-sse` to commonMain dependencies, and `androidx-security-crypto` to androidMain dependencies. Build verified successfully.

---

### Task 1.2: Create Network Models (API DTOs) ✅ COMPLETED
**Effort**: 2 hours
**Risk**: Very Low
**Description**: Create data transfer objects matching backend API contracts

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/network/models/` with:
```kotlin
// Auth models
@Serializable
data class SignUpRequest(val email: String, val name: String, val password: String)

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(
    val authToken: String,
    val refreshToken: String,
    val email: String,
    val name: String
)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

// Budget models
@Serializable
data class CreateBudgetRequest(val name: String, val amount: Double)

@Serializable
data class BudgetResponse(val id: Long, val name: String, val amount: Double)

// Collaborator models
@Serializable
data class AddCollaboratorRequest(val budgetId: Long, val email: String)

@Serializable
data class CollaboratorResponse(
    val budgetId: Long,
    val budgetName: String,
    val collaboratorEmail: String,
    val collaboratorName: String
)

@Serializable
data class UserInfo(val email: String, val name: String)

// Budget Entry models
@Serializable
data class CreateBudgetEntryRequest(
    val amount: Double,
    val description: String,
    val category: String,
    val type: String
    // Note: budgetId passed in URL path: POST /api/budgets/{budgetId}/entries
)

@Serializable
data class UpdateBudgetEntryRequest(
    val amount: Double,
    val description: String,
    val category: String,
    val type: String
    // Note: budgetId and id passed in URL path: PUT /api/budgets/{budgetId}/entries/{id}
)

@Serializable
data class BudgetEntryResponse(
    val id: Long,
    val budgetId: Long,
    val amount: Double,
    val description: String,
    val category: String,
    val type: String,
    val createdByEmail: String,
    val updatedByEmail: String?,
    val creationDate: String,
    val modificationDate: String
)

// SSE models
@Serializable
data class BudgetEntryEvent(
    val budgetEntry: BudgetEntryResponse,
    val userInfo: UserInfo
)
```

**Validation**:
- ✅ All models compile without errors
- ✅ Serialization annotations are correct
- ✅ Models match API documentation exactly

**Rollback**: Delete new models package

**Dependencies**: None

**Completion Notes**: Created 5 model files:
- `AuthModels.kt` - SignUpRequest, SignInRequest, AuthResponse, RefreshTokenRequest
- `BudgetModels.kt` - CreateBudgetRequest, BudgetResponse
- `CollaboratorModels.kt` - AddCollaboratorRequest, CollaboratorResponse, UserInfo
- `BudgetEntryModels.kt` - CreateBudgetEntryRequest, UpdateBudgetEntryRequest, BudgetEntryResponse
- `SseModels.kt` - BudgetEntryEvent

Note: SSE functionality is included in `ktor-client-core` for version 2.3.12, no separate dependency needed.

---

### Task 1.3: Create Secure Token Storage ✅ COMPLETED
**Effort**: 2 hours
**Risk**: Low
**Description**: Implement encrypted storage for JWT tokens using DataStore (already in dependencies)

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/auth/data/TokenStorage.kt`:
```kotlin
interface TokenStorage {
    suspend fun saveAuthToken(token: String)
    suspend fun getAuthToken(): String?
    suspend fun saveRefreshToken(token: String)
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
}
```

And platform implementations using DataStore Preferences (already available in project)

**Validation**:
- ✅ Can save and retrieve tokens
- ✅ Tokens persist across app restarts
- ✅ clearTokens() removes all stored data
- ✅ No tokens visible in plain text storage

**Rollback**: Delete TokenStorage files

**Dependencies**: Task 1.1

**Completion Notes**: Created single multiplatform implementation:
- `TokenStorage.kt` (commonMain) - Uses DataStore Preferences for secure storage

DataStore is multiplatform, so no platform-specific implementations needed. Works on both Android and iOS.

---

### Task 1.4: Create Base API Client with JWT Interceptor ✅ COMPLETED
**Effort**: 3 hours
**Risk**: Low
**Description**: Create Ktor HTTP client configured with JWT auth and automatic token refresh

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/network/ApiClient.kt`:
```kotlin
class ApiClient(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val json: Json
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenStorage.getAuthToken()
                    token?.let { BearerTokens(it, it) }
                }

                refreshTokens {
                    val refreshToken = tokenStorage.getRefreshToken()
                    // Token refresh logic (handled separately)
                }
            }
        }

        defaultRequest {
            url(baseUrl)
        }
    }

    suspend fun get(path: String): HttpResponse = client.get(path)
    suspend fun post(path: String, body: Any): HttpResponse = client.post(path) { setBody(body) }
    suspend fun put(path: String, body: Any): HttpResponse = client.put(path) { setBody(body) }
}
```

**Validation**:
- ✅ Client initializes without errors
- ✅ Can make unauthenticated requests
- ✅ Auth header not added without token
- ✅ Existing app functionality unchanged

**Rollback**: Delete ApiClient.kt

**Dependencies**: Task 1.2, Task 1.3

**Completion Notes**: Created `HttpClientFactory.kt` in `commons/data/network/`:
- Factory function `createHttpClient()` configures and returns HttpClient
- Configured ContentNegotiation plugin for JSON serialization
- Added Auth plugin with Bearer token support
- Loads tokens from TokenStorage automatically
- Placeholder for token refresh (will be implemented in AuthRepository)
- Base URL configuration via parameter
- Proper JSON serialization (ignoreUnknownKeys, isLenient)
- HttpClient will be provided via Koin DI, no wrapper class needed

---

## PHASE 2: AUTHENTICATION SYSTEM (MEDIUM RISK) ✅ COMPLETED

### Task 2.1: Create Authentication Repository ✅ COMPLETED
**Effort**: 3 hours
**Risk**: Medium
**Description**: Implement auth repository handling sign in, sign up, token refresh with rotation

**Completion Notes**: Created `AuthRepository.kt` in `auth/data/`:
- ✅ `signUp()` - Creates new user, returns SignUpResponse (no tokens, user must sign in separately)
- ✅ `signIn()` - Authenticates user, stores authToken and refreshToken on success
- ✅ `refreshToken()` - Rotates refresh token and updates stored tokens
- ✅ `signOut()` - Clears all stored tokens
- ✅ `isAuthenticated()` - Checks if auth token exists
- ✅ All methods use Result<T> for error handling
- ✅ Automatic token storage on successful auth
- ✅ Token rotation properly implemented
- ✅ Uses HttpClient and TokenStorage via DI

**Note**: Sign up API only returns user info (email, name), not tokens. User must sign in after registration to get tokens.

---

### Task 2.2: Create Authentication Use Cases ⚠️ SKIPPED
**Status**: SKIPPED - Not needed for current implementation
**Reason**: The app follows a simplified architecture where ViewModels call the repository directly. Use cases would add an unnecessary abstraction layer at this stage. This follows the existing pattern used in other features (BudgetListViewModel, BudgetDetailViewModel, etc.).

**Future Consideration**: If business logic complexity increases or the app requires multi-platform (iOS) with shared business logic, use cases can be added later.

---

### Task 2.3: Create Sign In Screen UI ✅ COMPLETED
**Effort**: 3 hours
**Risk**: Medium

**Completion Notes**: Created complete SignInScreen implementation:
- ✅ Email input field with proper keyboard type and validation
- ✅ Password input field with visibility toggle (show/hide password)
- ✅ Sign in button with loading state and proper enablement
- ✅ Navigation to Sign Up screen
- ✅ Error message display using dismissible Card with error styling
- ✅ Follows MVI pattern with SignInState and SignInEvent
- ✅ Loading overlay when request is in progress
- ✅ Proper keyboard actions (Next/Done)
- ✅ Back navigation support when navigating from SignUp
- ✅ Automatic navigation to BudgetList on successful sign in
- ✅ Material3 design, matches app theme

---

### Task 2.4: Create Sign Up Screen UI ✅ COMPLETED
**Effort**: 3 hours
**Risk**: Medium

**Completion Notes**: Created complete SignUpScreen implementation:
- ✅ Name input field with proper validation
- ✅ Email input field with email keyboard type
- ✅ Password input field with visibility toggle
- ✅ Confirm password field with validation
- ✅ Sign up button with loading state
- ✅ Navigation back to Sign In
- ✅ Error message display using dismissible Card
- ✅ Follows MVI pattern with SignUpState and SignUpEvent
- ✅ Password mismatch validation
- ✅ Loading overlay during sign up
- ✅ Success message with automatic redirect to SignIn after successful registration
- ✅ Material3 design, matches app theme

---

### Task 2.5: Create Auth ViewModels ✅ COMPLETED
**Effort**: 2 hours
**Risk**: Low

**Completion Notes**: Created TWO separate ViewModels (architectural decision):
- ✅ **SignInViewModel**: Manages sign-in state and events
  - Email/password input handling
  - Loading state management
  - Error handling with dismissible errors
  - Success navigation via isSignedIn state
  - Direct repository calls (no use cases)

- ✅ **SignUpViewModel**: Manages sign-up state and events
  - Name/email/password/confirmPassword input handling
  - Password match validation
  - Loading state management
  - Error and success message handling
  - Automatic sign-in after successful registration
  - Direct repository calls (no use cases)

**Architecture Note**: Separate ViewModels per screen is cleaner and more maintainable than a single AuthViewModel for multiple screens.

---

### Task 2.6: Update Navigation to Include Auth Flow ✅ COMPLETED
**Effort**: 2 hours
**Risk**: Medium

**Completion Notes**: Navigation fully integrated:
- ✅ Added SignInScreen route to NavHost
- ✅ Added SignUpScreen route to NavHost
- ✅ **SplashScreen checks authentication status and routes accordingly**:
  - If authenticated → BudgetListScreen
  - If not authenticated → SignInScreen
- ✅ Sign in/sign up navigation works bidirectionally
- ✅ Proper back stack management (clear splash from stack after navigation)
- ✅ SignInScreen shows back button only when navigating from SignUp
- ✅ SignUpScreen navigates back to SignIn on back press
- ✅ Successful auth navigates to BudgetListScreen

**Important**: Authentication is now **required** - the app routes to SignIn on first launch if not authenticated. This differs from the plan's gradual approach but matches the current implementation.

---

### Task 2.7: Create Auth Koin Module ✅ COMPLETED
**Effort**: 1 hour
**Risk**: Low

**Completion Notes**: Created complete AuthModule:
- ✅ `TokenStorage` - Single instance using DataStore
- ✅ `HttpClient` - Named instance ("AuthHttpClient") with base URL "http://10.0.2.2:8080"
- ✅ `AuthRepository` - Single instance with HttpClient, TokenStorage, IO dispatcher
- ✅ `SignInViewModel` - Factory (new instance per screen)
- ✅ `SignUpViewModel` - Factory (new instance per screen)
- ✅ Added to `KoinInitializer.kt`
- ✅ All dependencies resolve correctly
- ✅ No circular dependencies
- ✅ Existing modules still work

**Note**: Base URL is hardcoded as Android emulator localhost (10.0.2.2). This should be made configurable (Task 10.7).

---

### Task 2.8: Clear Local Data on Sign Out ✅ COMPLETED
**Effort**: 1.5 hours
**Risk**: Medium
**Description**: Clear all local database data when user signs out to prevent data leaking between users

**Deliverable**:
1. Add `clearAllData()` method to `BudgetRepository` and `BudgetEntryRepository`
2. Create SQL queries in `Budget.sq` and `BudgetEntry.sq`:
   ```sql
   deleteAll:
   DELETE FROM budget;

   deleteAll:
   DELETE FROM budget_entry;
   ```
3. Update `AuthRepository.signOut()` to clear all local data before clearing tokens:
   ```kotlin
   suspend fun signOut() {
       // Clear all local data first
       budgetRepository.clearAllData()
       budgetEntryRepository.clearAllData()

       // Then clear tokens
       tokenStorage.clearTokens()
   }
   ```

**Why This Is Critical**:
- Prevents data leaking between different users on the same device
- Ensures clean state when switching accounts
- Security best practice for multi-user apps
- Simple and reliable implementation

**Alternative Approaches Considered**:
- User-scoped storage (too complex, risk of data leaks if filtering is missed)
- Clear on sign-in (wrong user's data briefly visible)
- Option 1 (this approach) is the safest and cleanest

**Validation**:
- ✅ Sign out clears all budgets from local database
- ✅ Sign out clears all budget entries from local database
- ✅ Sign in with different user shows empty state initially
- ✅ Sync after sign-in downloads new user's data from server
- ✅ No data from previous user remains accessible

**Rollback**: Revert changes to AuthRepository and remove deleteAll queries

**Dependencies**: Task 2.1 (AuthRepository), Phase 3 database schema

**Completion Notes**:
- ✅ Added `deleteAll` SQL query to Budget.sq (line 36-37)
- ✅ Added `deleteAll` SQL query to BudgetEntry.sq (line 44-45)
- ✅ Added `clearAllData()` method to BudgetLocalDataSource (line 81)
- ✅ Added `clearAllData()` method to BudgetEntryLocalDataSource (line 98)
- ✅ Added `clearAllData()` method to BudgetRepository (lines 67-73)
- ✅ Added `clearAllData()` method to BudgetEntryRepository (lines 23-29)
- ✅ Updated `AuthRepository` constructor to accept lazy references to repositories to avoid circular dependency (lines 21-22)
- ✅ Updated `AuthRepository.signOut()` to clear all local data before clearing tokens (lines 84-91)
- ✅ Updated `AuthModule` to inject repositories lazily using `inject()` (lines 35-36)
- ✅ Build completed successfully

**Implementation Details**:
- Used Koin's `inject()` for lazy dependency injection to resolve circular dependency
- `budgetRepository` and `budgetEntryRepository` are now `Lazy<T>` types
- Data clearing happens in correct order: budgets → entries → tokens
- Foreign key CASCADE ensures entries are deleted when budgets are deleted

---

### Task 2.9: Auto-Sync After Sign In ✅ COMPLETED
**Effort**: 0.5 hours
**Risk**: Low
**Description**: Automatically sync user's budgets from server after successful sign-in

**Why This Is Important**:
- Currently, after sign-in, user sees empty budget list
- User must manually pull-to-refresh to see their budgets
- Poor UX - user expects to see their data immediately after signing in

**Deliverable**:
1. Update `SignInViewModel` to inject `BudgetRepository`
2. After successful sign-in, trigger `budgetRepository.sync()` in background
3. User sees their budgets automatically after sign-in completes

**Implementation**:
```kotlin
private fun signIn() {
    // ... existing validation ...

    viewModelScope.launch {
        authRepository.signIn(email, password).fold(
            onSuccess = {
                // Trigger background sync to fetch user's budgets
                launch { budgetRepository.sync() }

                _uiState.update { it.copy(isLoading = false, isSignedIn = true) }
            },
            onFailure = { /* ... */ }
        )
    }
}
```

**Validation**:
- ✅ After sign-in, budgets automatically appear (no manual refresh needed)
- ✅ Sync happens in background (doesn't block navigation)
- ✅ Sign-in still completes even if sync fails
- ✅ Works for both new users (empty list) and existing users (fetches their budgets)

**Rollback**: Revert SignInViewModel changes

**Dependencies**: Task 4.3 (BudgetRepository.sync() method exists)

**Completion Notes**:
- ✅ Updated `SignInViewModel` constructor to inject `BudgetRepository` (line 20)
- ✅ Added background sync launch after successful sign-in (lines 60-63)
- ✅ Updated `AuthModule` to provide `BudgetRepository` to `SignInViewModel` (line 44)
- ✅ Build completed successfully
- ✅ Sync happens in background coroutine, doesn't block sign-in completion
- ✅ Sign-in navigation happens immediately, budgets appear after sync completes

---

## PHASE 3: DATABASE MIGRATION FOR SYNC SUPPORT (HIGH RISK) ✅ COMPLETED

### 📋 PHASE 3 OVERVIEW
**Status**: COMPLETED
**Critical Priority**: HIGH - This phase is a prerequisite for all sync functionality

**Current Database State**:
- ✅ Budget table: Has id, amount, name, date (no sync fields)
- ✅ BudgetEntry table: Has id, budget_id, amount, description, type, date, invoice, category (no sync fields)
- ✅ Domain models (Budget, BudgetEntry): No sync-related properties

**What Needs to Change**:
Both tables and domain models need to track:
1. Server-side IDs (to map local entries to backend)
2. Sync status (to know what needs uploading)
3. Collaboration metadata (who created/updated entries)
4. Sync timestamps (to handle conflicts)

**Migration Strategy**:
- Use SqlDelight migrations to add new columns with sensible defaults
- All existing columns preserved (no data loss)
- New columns nullable or with defaults to maintain backward compatibility
- Update domain models with optional fields
- Update mappers to handle new fields
- Update repositories to use new fields (but don't implement sync logic yet)

---

### Task 3.1: Update Budget Schema to Include Server ID and Sync Status ✅ COMPLETED
**Effort**: 2 hours
**Risk**: High
**Description**: Add columns to track server-side IDs and sync status WITHOUT breaking existing code

**Completion Notes**:
- ✅ Created migration file `1.sqm` with ALTER TABLE statements for `server_id`, `is_synced`, `last_synced_at`
- ✅ Updated `Budget.sq` schema with new columns and default values
- ✅ Added `selectUnsynced` query to fetch unsynced budgets
- ✅ Added `markAsSynced` query to update sync status
- ✅ Added `selectByServerId` query to find budgets by server ID
- ✅ Updated Budget domain model with `serverId: Long?`, `isSynced: Boolean`, `lastSyncedAt: String?`
- ✅ Updated `mapSelectAllToBudget` mapper to handle new fields
- ✅ Updated BudgetLocalDataSource create/update methods to handle sync fields
- ✅ Migration runs successfully on existing databases
- ✅ App loads budget list correctly with migrated data
- ✅ Can create new budgets with sync fields

**Migration Strategy**: SqlDelight automatically applies migration on database open, adding columns with proper defaults (server_id = null, is_synced = 0, last_synced_at = null)

**Dependencies**: None

---

### Task 3.2: Update BudgetEntry Schema for Sync Support ✅ COMPLETED
**Effort**: 2 hours
**Risk**: High
**Description**: Add sync tracking to budget entries, including creator information

**Completion Notes**:
- ✅ Updated `BudgetEntry.sq` schema with `server_id`, `is_synced`, `created_by_email`, `updated_by_email`, `creation_date`, `modification_date`
- ✅ Updated migration file `1.sqm` with ALTER TABLE statements for all new columns
- ✅ Added `selectUnsynced`, `selectUnsyncedByBudgetId`, `markAsSynced`, `selectByServerId` queries
- ✅ Updated insert/update queries to include all new fields
- ✅ Updated BudgetEntry domain model with sync-related properties
- ✅ Updated `Budget_entry.toDomain()` mapper to handle new fields
- ✅ Updated BudgetEntryLocalDataSource create/update methods
- ✅ Build completed successfully
- ✅ Migration will run automatically on database open

**Dependencies**: Task 3.1

**Deliverable**: Update `/composeApp/src/commonMain/sqldelight/com/meneses/budgethunter/db/BudgetEntry.sq`:
```sql
CREATE TABLE budget_entry (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  budget_id INTEGER NOT NULL,
  amount REAL NOT NULL,
  description TEXT NOT NULL,
  type TEXT AS Type NOT NULL,
  date TEXT NOT NULL,
  invoice TEXT,
  category TEXT AS Category NOT NULL,
  server_id INTEGER,  -- NEW: Backend entry ID
  is_synced INTEGER NOT NULL DEFAULT 0,  -- NEW: Sync status
  created_by_email TEXT,  -- NEW: Creator email from backend
  updated_by_email TEXT,  -- NEW: Last updater email
  creation_date TEXT,  -- NEW: Server creation timestamp
  modification_date TEXT,  -- NEW: Server modification timestamp
  FOREIGN KEY (budget_id) REFERENCES budget(id) ON DELETE CASCADE
);

-- Keep existing queries unchanged
selectAllByBudgetId:
SELECT * FROM budget_entry
WHERE budget_id = :budgetId
ORDER BY id DESC;

-- NEW queries
selectUnsynced:
SELECT * FROM budget_entry WHERE is_synced = 0;

markAsSynced:
UPDATE budget_entry SET is_synced = 1, server_id = ?,
  created_by_email = ?, updated_by_email = ?,
  creation_date = ?, modification_date = ?
WHERE id = ?;
```

Update BudgetEntry domain model accordingly.

**Validation**:
- Database migration runs successfully
- Existing entries preserved with defaults
- Budget detail screen loads entries
- Can create/edit entries
- No UI breakage

**Rollback Strategy**: Same as Task 3.1

**Dependencies**: Task 3.1

---

### Task 3.3: Create Sync Status Indicator UI Component ✅ COMPLETED
**Effort**: 1.5 hours
**Risk**: Low
**Description**: Create reusable component to show sync status (synced/pending/error)

**Completion Notes**:
- ✅ Created `SyncStatusIndicator.kt` composable component
- ✅ Shows CloudDone icon (green) when synced
- ✅ Shows CloudQueue icon (gray) when pending sync
- ✅ Accepts `isSynced` boolean parameter
- ✅ Customizable via Modifier parameter
- ✅ Proper content descriptions for accessibility
- ✅ Build completed successfully

**Dependencies**: Task 3.1, Task 3.2

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/commons/ui/SyncStatusIndicator.kt`:
```kotlin
@Composable
fun SyncStatusIndicator(
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    // Small icon: cloud check (synced), cloud upload (pending), cloud error (failed)
    Icon(
        imageVector = when {
            isSynced -> Icons.Default.CloudDone
            else -> Icons.Default.CloudQueue
        },
        contentDescription = "Sync status",
        modifier = modifier.size(16.dp),
        tint = when {
            isSynced -> Color.Green
            else -> Color.Gray
        }
    )
}
```

**Validation**:
- Component renders correctly
- Icons display based on state
- Doesn't break existing UI

**Rollback**: Delete component file

**Dependencies**: Task 3.1, Task 3.2

---

## PHASE 4: BUDGET SYNC IMPLEMENTATION (MEDIUM-HIGH RISK) ✅ 100% COMPLETE

### 📋 PHASE 4 OVERVIEW
**Status**: 6/6 tasks complete (Task 4.4 optional, skipped)
**Dependencies**: Phase 3 ✅ COMPLETED
**Description**: Implement budget synchronization with backend API

**What This Phase Delivers**:
- ✅ BudgetApiService for CRUD operations (with correct endpoints)
- ✅ BudgetSyncManager for push/pull/full sync
- ✅ Integration with BudgetRepository (automatic + manual sync)
- ✅ Pull-to-refresh UI in budget list
- ✅ Sync status indicators on budget items
- ⚠️ Background sync worker (optional, skipped for MVP)

**Current Status**: Budget sync is **fully complete and functional**. End-to-end budget synchronization with visual indicators working.

---

### Task 4.1: Create Budget API Service ✅ COMPLETED
**Effort**: 3 hours
**Risk**: Medium
**Description**: Implement API calls for budget CRUD operations

**Completion Notes**:
- ✅ Created `BudgetApiService.kt` in `budgetList/data/network/`
- ✅ Implemented `createBudget()` - POST /api/budgets (RESTful endpoint)
- ✅ Implemented `getBudgets()` - GET /api/budgets (RESTful endpoint)
- ✅ Implemented `getBudgetById()` - Fetches all budgets and filters by ID (backend doesn't have dedicated endpoint)
- ✅ All methods return `Result<T>` for error handling
- ✅ Uses HttpClient for API calls
- ✅ Runs on IO dispatcher for non-blocking operations
- ✅ Proper content type and JSON serialization
- ✅ Comprehensive error logging added
- ✅ **Updated to RESTful conventions** (2025-10-25): Changed from `/api/budgets/create_budget` to `/api/budgets` and from `/api/budgets/get_budgets` to `/api/budgets`
- ✅ Build completed successfully

**Migration Note**: Backend API was refactored to follow RESTful conventions. Legacy endpoints (with verbs) still available but deprecated.

**Dependencies**: Task 1.4, Task 2.1

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/budgetList/data/network/BudgetApiService.kt`:
```kotlin
class BudgetApiService(private val apiClient: ApiClient) {
    suspend fun createBudget(request: CreateBudgetRequest): Result<BudgetResponse>
    suspend fun getBudgets(): Result<List<BudgetResponse>>
    // Future: update, delete if needed
}
```

**Validation**:
- Can create budget on backend
- Can fetch budgets from backend
- Errors are properly wrapped in Result
- No impact on existing app

**Rollback**: Delete BudgetApiService

**Dependencies**: Task 1.4, Task 2.1 (for auth tokens)

---

### Task 4.2: Create Budget Sync Manager ✅ COMPLETED
**Effort**: 4 hours
**Risk**: High
**Description**: Orchestrate syncing local budgets with backend

**Completion Notes**:
- ✅ Created `BudgetSyncManager.kt` in `budgetList/data/sync/`
- ✅ Implemented `syncPendingBudgets()` - pushes unsynced local budgets to server
- ✅ Implemented `pullBudgetsFromServer()` - fetches server budgets and merges with local
- ✅ Implemented `performFullSync()` - bidirectional sync (push then pull)
- ✅ Authentication check before sync operations
- ✅ Updates local budgets with server IDs after successful creation
- ✅ Handles merge by server_id to avoid duplicates
- ✅ Uses BudgetQueries for direct database access (markAsSynced, selectByServerId)
- ✅ All operations wrapped in Result<T> for error handling
- ✅ Continues syncing other budgets even if one fails
- ✅ Build completed successfully

**Dependencies**: Task 3.1, Task 4.1

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/budgetList/data/sync/BudgetSyncManager.kt`:
```kotlin
class BudgetSyncManager(
    private val localDataSource: BudgetLocalDataSource,
    private val budgetApiService: BudgetApiService,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    // Push local unsynced budgets to server
    suspend fun syncPendingBudgets(): Result<Unit>

    // Pull budgets from server and merge with local
    suspend fun pullBudgetsFromServer(): Result<Unit>

    // Full bidirectional sync
    suspend fun performFullSync(): Result<Unit>
}
```

**Implementation Strategy**:
1. Check if user is authenticated (skip if not)
2. Push: Get unsynced local budgets, create on server, update local with server IDs
3. Pull: Fetch server budgets, merge by server_id, update existing or insert new
4. Conflict resolution: Server wins for now (future: last-write-wins with timestamp)

**Validation**:
- Unsynced local budget gets synced to server
- Server budget gets pulled to local
- No duplicates created
- Sync can run multiple times safely (idempotent)
- Sync failure doesn't corrupt local data

**Rollback**: Delete BudgetSyncManager

**Dependencies**: Task 3.1, Task 4.1

---

### Task 4.3: Integrate Sync into BudgetRepository ✅ COMPLETED
**Effort**: 2 hours
**Risk**: Medium
**Description**: Update BudgetRepository to trigger sync on create/update when authenticated

**Completion Notes**:
- ✅ Updated `BudgetRepository` constructor to accept `BudgetSyncManager`, `AuthRepository`, and `CoroutineScope`
- ✅ Modified `create()` to trigger background sync after creating budget
- ✅ Modified `update()` to trigger background sync after updating budget
- ✅ Added `sync()` method for manual full sync
- ✅ Sync only triggers when user is authenticated
- ✅ Background sync uses scope.launch to avoid blocking operations
- ✅ Updated `BudgetListModule` with proper dependency injection
- ✅ Added `BudgetApiService` singleton with AuthHttpClient
- ✅ Added `BudgetSyncManager` singleton with all dependencies
- ✅ Updated `BudgetRepository` singleton with new parameters
- ✅ Build completed successfully

**Dependencies**: Task 4.1, Task 4.2

**Deliverable**: Update `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/budgetList/data/BudgetRepository.kt`:
```kotlin
class BudgetRepository(
    private val localDataSource: BudgetLocalDataSource,
    private val budgetSyncManager: BudgetSyncManager,  // NEW
    private val authRepository: AuthRepository,  // NEW
    private val ioDispatcher: CoroutineDispatcher
) {
    val budgets: Flow<List<Budget>>
        get() = localDataSource.budgets

    suspend fun create(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.create(budget)
        // Optionally sync immediately if authenticated
        if (authRepository.isAuthenticated()) {
            budgetSyncManager.syncPendingBudgets()
        }
    }

    suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.update(budget)
        if (authRepository.isAuthenticated()) {
            budgetSyncManager.syncPendingBudgets()
        }
    }

    // NEW: Manual sync trigger
    suspend fun sync(): Result<Unit> = budgetSyncManager.performFullSync()
}
```

**Validation**:
- Creating budget while authenticated syncs to server
- Creating budget while not authenticated stays local
- Existing create/update behavior preserved for unauthenticated users
- No errors thrown on sync failure (graceful degradation)

**Rollback**: Revert BudgetRepository changes

**Dependencies**: Task 4.2

---

### Task 4.4: Add Background Sync Worker ⚠️ SKIPPED (Optional for MVP)
**Effort**: 3 hours
**Risk**: Medium
**Description**: Create periodic background sync (Android WorkManager, iOS Background Tasks)

**Status**: SKIPPED - Manual pull-to-refresh is sufficient for MVP. Can be implemented later if needed.

**Reasoning**:
- Manual sync via pull-to-refresh provides adequate functionality
- Automatic sync on budget create/update already implemented
- Background workers add complexity and battery drain concerns
- Can be added in future iteration if needed

**Dependencies**: Task 4.3

---

### Task 4.5: Add Pull-to-Refresh for Budget List ✅ COMPLETED
**Effort**: 1.5 hours
**Risk**: Low
**Description**: Add swipe-to-refresh gesture to manually sync budgets

**Completion Notes**:
- ✅ Added `isSyncing: Boolean` to BudgetListState
- ✅ Added `SyncBudgets` event to BudgetListEvent
- ✅ Implemented `syncBudgets()` in BudgetListViewModel that calls `budgetRepository.sync()`
- ✅ Added `PullToRefreshBox` to BudgetListContent with Material3
- ✅ Pull-to-refresh triggers full bidirectional sync
- ✅ Loading indicator displays during sync
- ✅ Works when online (syncs) and offline (shows cached data)

**Implementation**:
- BudgetListState.kt:17 - `isSyncing` field added
- BudgetListEvent.kt:20 - `SyncBudgets` event added
- BudgetListViewModel.kt:78-82 - `syncBudgets()` implementation
- BudgetListContent.kt:62-68 - PullToRefreshBox UI

**Dependencies**: Task 4.3

---

### Task 4.6: Update Budget List UI to Show Sync Status ✅ COMPLETED
**Effort**: 1 hour
**Risk**: Low
**Description**: Add sync status indicator to budget list items

**Completion Notes**:
- ✅ Added `SyncStatusIndicator` import to BudgetListContent.kt
- ✅ Integrated sync status indicator into BudgetItem UI between budget info and menu button
- ✅ Indicator shows green CloudDone icon for synced budgets
- ✅ Indicator shows gray CloudQueue icon for unsynced budgets
- ✅ Icon sized at 20dp with 8dp right padding
- ✅ Build completed successfully
- ✅ UI is clean and not cluttered

**Implementation**:
- BudgetListContent.kt:45 - SyncStatusIndicator import added
- BudgetListContent.kt:165-170 - SyncStatusIndicator integrated into BudgetItem Row layout

**Visual Result**:
```
[Budget Info]  [🌥️ Sync Icon]  [⋮ Menu]
```

**Validation**:
- ✅ Synced budgets show green cloud check icon
- ✅ Pending (unsynced) budgets show gray cloud queue icon
- ✅ Icon appears between budget info and menu button
- ✅ UI remains clean and not cluttered

**Dependencies**: Task 3.3 ✅, Task 4.3 ✅

---

## PHASE 5: BUDGET ENTRY SYNC IMPLEMENTATION (MEDIUM-HIGH RISK) ⏳ NOT STARTED

### 📋 PHASE 5 OVERVIEW
**Status**: NOT STARTED
**Dependencies**: Phase 3 and Phase 4 must be completed first
**Description**: Implement budget entry synchronization with creator/updater tracking

---

### Task 5.1: Create Budget Entry API Service ⏳ NOT STARTED
**Effort**: 3 hours
**Risk**: Medium
**Description**: Implement API calls for budget entry operations using RESTful conventions

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/budgetEntry/data/network/BudgetEntryApiService.kt`:
```kotlin
class BudgetEntryApiService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) {
    // POST /api/budgets/{budgetId}/entries
    suspend fun createEntry(budgetId: Long, request: CreateBudgetEntryRequest): Result<BudgetEntryResponse>

    // PUT /api/budgets/{budgetId}/entries/{entryId}
    suspend fun updateEntry(budgetId: Long, entryId: Long, request: UpdateBudgetEntryRequest): Result<BudgetEntryResponse>

    // GET /api/budgets/{budgetId}/entries
    suspend fun getEntries(budgetId: Long): Result<List<BudgetEntryResponse>>
}
```

**RESTful Endpoints**:
- Create: `POST /api/budgets/{budgetId}/entries` (budgetId in path, not body)
- Update: `PUT /api/budgets/{budgetId}/entries/{entryId}` (both IDs in path, not body)
- Get: `GET /api/budgets/{budgetId}/entries`
- Stream (SSE): `GET /api/budgets/{budgetId}/entries/stream` (for real-time updates)

**Note**: Legacy endpoints (PUT `/api/budgets/put_entry`) are deprecated. Use RESTful endpoints only.

**Validation**:
- Can create entry on backend with budgetId in URL path
- Can update entry on backend with both IDs in URL path
- Can fetch entries for a budget
- Request bodies contain only entry data (no IDs)
- Proper error handling

**Rollback**: Delete service file

**Dependencies**: Task 1.4, Task 2.1

---

### Task 5.2: Create Budget Entry Sync Manager
**Effort**: 4 hours
**Risk**: High
**Description**: Sync budget entries bidirectionally with conflict resolution

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/budgetEntry/data/BudgetEntrySyncManager.kt`:
```kotlin
class BudgetEntrySyncManager(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val budgetEntryApiService: BudgetEntryApiService,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun syncPendingEntries(budgetId: Int): Result<Unit>
    suspend fun pullEntriesFromServer(budgetServerId: Long): Result<Unit>
    suspend fun performFullSync(budgetId: Int, budgetServerId: Long): Result<Unit>
}
```

**Sync Logic**:
1. Push unsynced local entries to server
2. Pull server entries and merge by server_id
3. Update modification tracking (created_by_email, updated_by_email, timestamps)

**Validation**:
- Local entry syncs to server
- Server entry appears locally
- No duplicates
- Creator/updater info preserved
- Sync is idempotent

**Rollback**: Delete sync manager

**Dependencies**: Task 3.2, Task 5.1

---

### Task 5.3: Integrate Sync into BudgetEntryRepository
**Effort**: 2 hours
**Risk**: Medium
**Description**: Update repository to sync entries on create/update

**Deliverable**: Update `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/budgetEntry/data/BudgetEntryRepository.kt`:
```kotlin
class BudgetEntryRepository(
    private val localDataSource: BudgetEntryLocalDataSource,
    private val syncManager: BudgetEntrySyncManager,  // NEW
    private val authRepository: AuthRepository,  // NEW
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun create(entry: BudgetEntry, budgetServerId: Long?) = withContext(ioDispatcher) {
        localDataSource.create(entry)
        if (authRepository.isAuthenticated() && budgetServerId != null) {
            syncManager.syncPendingEntries(entry.budgetId)
        }
    }

    suspend fun update(entry: BudgetEntry) = withContext(ioDispatcher) {
        localDataSource.update(entry)
        if (authRepository.isAuthenticated()) {
            syncManager.syncPendingEntries(entry.budgetId)
        }
    }

    suspend fun sync(budgetId: Int, budgetServerId: Long): Result<Unit> =
        syncManager.performFullSync(budgetId, budgetServerId)
}
```

**Validation**:
- Entry creation triggers sync when authenticated
- Works offline (no sync, stays local)
- Existing entry operations preserved

**Rollback**: Revert repository changes

**Dependencies**: Task 5.2

---

### Task 5.4: Update Budget Detail Screen for Entry Sync
**Effort**: 2 hours
**Risk**: Low
**Description**: Add sync status and manual sync for entries

**Deliverable**:
- Add pull-to-refresh to budget detail screen
- Show sync status on each entry
- Show creator/updater info on entries (when synced)

**Validation**:
- Pull-to-refresh syncs entries
- Creator email shown on synced entries
- Unsynced entries clearly marked

**Rollback**: Remove sync UI additions

**Dependencies**: Task 5.3

---

## PHASE 6: COLLABORATOR MANAGEMENT (MEDIUM RISK) ⏳ NOT STARTED

### 📋 PHASE 6 OVERVIEW
**Status**: NOT STARTED
**Dependencies**: Phase 4 and Phase 5 must be completed first
**Description**: Add ability to share budgets and collaborate with other users

---

### Task 6.1: Create Collaborator API Service ⏳ NOT STARTED
**Effort**: 2 hours
**Risk**: Low
**Description**: Implement collaborator endpoints using RESTful conventions

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/collaborator/data/network/CollaboratorApiService.kt`:
```kotlin
class CollaboratorApiService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) {
    // POST /api/budgets/{budgetId}/collaborators
    suspend fun addCollaborator(budgetId: Long, request: AddCollaboratorRequest): Result<CollaboratorResponse>

    // GET /api/budgets/{budgetId}/collaborators
    suspend fun getCollaborators(budgetId: Long): Result<List<UserInfo>>
}
```

**RESTful Endpoints**:
- Add: `POST /api/budgets/{budgetId}/collaborators` (budgetId in path)
- Get: `GET /api/budgets/{budgetId}/collaborators`

**Note**:
- Legacy endpoint (POST `/api/budgets/add_collaborator`) is deprecated
- Legacy endpoint (GET `/api/budgets/get_collaborators?budgetId=1`) is deprecated
- AddCollaboratorRequest still contains budgetId in body per backend spec (backend expects it in both URL and body)

**Validation**:
- Can add collaborator to budget with budgetId in URL path
- Can fetch collaborators list for a budget
- Proper error handling (user not found, etc.)

**Rollback**: Delete service file

**Dependencies**: Task 1.4

---

### Task 6.2: Create Collaborator Repository
**Effort**: 1.5 hours
**Risk**: Low
**Description**: Repository layer for collaborator operations

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/collaborator/data/CollaboratorRepository.kt`:
```kotlin
class CollaboratorRepository(
    private val collaboratorApiService: CollaboratorApiService,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun addCollaborator(budgetServerId: Long, email: String): Result<CollaboratorResponse>
    suspend fun getCollaborators(budgetServerId: Long): Result<List<UserInfo>>
}
```

**Validation**:
- Repository methods work correctly
- Errors propagated properly

**Rollback**: Delete repository

**Dependencies**: Task 6.1

---

### Task 6.3: Create Collaborators Screen
**Effort**: 3 hours
**Risk**: Low
**Description**: New screen to view and add collaborators for a budget

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/collaborator/ui/CollaboratorsScreen.kt`:
- List of current collaborators
- Add collaborator button
- Email input dialog
- Success/error feedback
- Only shown for synced budgets

**Validation**:
- Screen renders correctly
- Can add collaborator by email
- Collaborator list updates
- Error shown for invalid email

**Rollback**: Delete screen file

**Dependencies**: Task 6.2

---

### Task 6.4: Add Navigation to Collaborators Screen
**Effort**: 1 hour
**Risk**: Low
**Description**: Add navigation from budget detail to collaborators screen

**Deliverable**:
- Add "Manage Collaborators" button to budget detail screen (only if budget.isSynced)
- Add route and navigation handling

**Validation**:
- Button only shows for synced budgets
- Navigation works correctly
- Can navigate back

**Rollback**: Remove button and route

**Dependencies**: Task 6.3

---

### Task 6.5: Create Collaborator Koin Module
**Effort**: 0.5 hours
**Risk**: Low
**Description**: Wire up collaborator dependencies

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/di/CollaboratorModule.kt` and add to KoinInitializer

**Validation**:
- Module loads without errors
- Dependencies resolve correctly

**Rollback**: Remove module from KoinInitializer

**Dependencies**: Task 6.1, Task 6.2

---

## PHASE 7: REAL-TIME UPDATES WITH SSE (MEDIUM-HIGH RISK) ⏳ NOT STARTED

### 📋 PHASE 7 OVERVIEW
**Status**: NOT STARTED
**Dependencies**: Phase 5 must be completed first
**Description**: Implement Server-Sent Events for real-time budget entry updates from collaborators

---

### Task 7.1: Create SSE Client ⏳ NOT STARTED
**Effort**: 3 hours
**Risk**: Medium
**Description**: Implement Server-Sent Events client for real-time budget entry updates

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/network/services/SseClient.kt`:
```kotlin
class SseClient(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val json: Json
) {
    fun subscribeToBudgetEntries(budgetServerId: Long): Flow<BudgetEntryEvent>
    fun close()
}
```

**Implementation**:
- Use Ktor SSE plugin
- Parse `event: budget-entry` and `data: {...}` format
- Automatic reconnection on disconnect
- Include auth token in request

**Validation**:
- Can connect to SSE endpoint
- Receives events when entries created on backend
- Reconnects on connection loss
- Properly parses event data

**Rollback**: Delete SseClient

**Dependencies**: Task 1.1, Task 1.4

---

### Task 7.2: Create Real-Time Sync Manager
**Effort**: 3 hours
**Risk**: High
**Description**: Integrate SSE events into local database

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/budgetEntry/data/RealTimeSyncManager.kt`:
```kotlin
class RealTimeSyncManager(
    private val sseClient: SseClient,
    private val localDataSource: BudgetEntryLocalDataSource,
    private val budgetRepository: BudgetRepository
) {
    fun startListening(budgetServerId: Long)
    fun stopListening()

    private fun handleBudgetEntryEvent(event: BudgetEntryEvent) {
        // Insert or update local entry based on server_id
        // Update UI through Flow from repository
    }
}
```

**Validation**:
- Receiving SSE event updates local database
- UI reflects changes automatically (through Flow)
- No duplicate entries created
- Own entries don't create conflicts

**Rollback**: Delete manager

**Dependencies**: Task 7.1, Task 5.2

---

### Task 7.3: Integrate Real-Time Updates into Budget Detail Screen
**Effort**: 2 hours
**Risk**: Medium
**Description**: Start SSE listener when viewing synced budget detail

**Deliverable**: Update BudgetDetailViewModel:
```kotlin
class BudgetDetailViewModel(...) {
    private val realTimeSyncManager: RealTimeSyncManager

    init {
        // Start listening when budget is synced
        viewModelScope.launch {
            budget?.let {
                if (it.isSynced && it.serverId != null) {
                    realTimeSyncManager.startListening(it.serverId)
                }
            }
        }
    }

    override fun onCleared() {
        realTimeSyncManager.stopListening()
        super.onCleared()
    }
}
```

**Validation**:
- Opening synced budget starts SSE connection
- Collaborator creating entry shows in real-time
- Closing screen stops SSE connection
- No memory leaks

**Rollback**: Remove SSE integration from ViewModel

**Dependencies**: Task 7.2

---

### Task 7.4: Add Visual Feedback for Real-Time Updates
**Effort**: 1.5 hours
**Risk**: Low
**Description**: Show notification when new entry arrives from collaborator

**Deliverable**:
- Subtle animation when new entry appears
- Toast/snackbar: "New entry from [collaborator name]"
- Highlight new entries briefly

**Validation**:
- Notification shows for collaborator entries
- Doesn't show for own entries
- Animation is smooth and not distracting

**Rollback**: Remove notification logic

**Dependencies**: Task 7.3

---

## PHASE 8: AUTHENTICATION ENFORCEMENT & MIGRATION (HIGH RISK) ⚠️ PARTIALLY ADDRESSED

### 📋 PHASE 8 OVERVIEW
**Status**: PARTIALLY ADDRESSED
**Note**: Authentication is already required (implemented in Phase 2), but data migration for existing users is not yet implemented.

**What's Already Done**:
- ✅ Authentication is mandatory (SplashScreen routes to SignIn if not authenticated)
- ✅ No "continue offline" option exists

**What Still Needs to Be Done**:
- ❌ Data migration dialog for existing users with local data
- ❌ One-time sync of existing local budgets/entries to server
- ❌ Migration progress UI
- ❌ Migration flag to prevent duplicate migrations

**Recommendation**: This phase can be deprioritized since authentication is already enforced. However, if you have existing users with local data, implement this before releasing the backend-enabled version.

---

### Task 8.1: Create Data Migration Dialog ⏳ NOT STARTED
**Effort**: 3 hours
**Risk**: Medium
**Description**: Prompt existing users to sign up/sign in to sync local data

**Deliverable**: Create dialog shown on app start for unauthenticated users with local data:
- "Sync your data to the cloud"
- Options: Sign Up, Sign In, Continue Offline
- Explain benefits (collaboration, sync across devices)

**Validation**:
- Dialog shows only for users with local data
- All options work correctly
- User can continue offline indefinitely

**Rollback**: Remove dialog

**Dependencies**: Phase 2 complete

---

### Task 8.2: Implement Initial Data Migration Flow
**Effort**: 4 hours
**Risk**: High
**Description**: Upload all local budgets and entries to server after first sign in

**Deliverable**: Create one-time migration process:
```kotlin
class InitialDataMigrationUseCase(
    private val budgetSyncManager: BudgetSyncManager,
    private val budgetEntrySyncManager: BudgetEntrySyncManager,
    private val budgetRepository: BudgetRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend fun performMigration(): Result<Unit> {
        if (preferencesManager.hasPerformedMigration()) return Result.success(Unit)

        // 1. Sync all local budgets
        budgetSyncManager.syncPendingBudgets()

        // 2. For each budget, sync its entries
        val budgets = budgetRepository.getAllCached()
        budgets.forEach { budget ->
            budget.serverId?.let { serverId ->
                budgetEntrySyncManager.performFullSync(budget.id, serverId)
            }
        }

        preferencesManager.setMigrationCompleted()
        return Result.success(Unit)
    }
}
```

Call after successful first sign in.

**Validation**:
- All local budgets uploaded to server
- All entries uploaded under correct budgets
- Migration happens only once
- User can track progress
- Failures are recoverable

**Rollback**: Reset migration flag, revert to local-only

**Dependencies**: Task 4.2, Task 5.2, Task 8.1

---

### Task 8.3: Add Migration Progress UI
**Effort**: 2 hours
**Risk**: Low
**Description**: Show progress during data migration

**Deliverable**:
- Full-screen progress dialog during migration
- "Syncing budgets: 3/5"
- "Syncing entries: 12/45"
- Cancel button (keeps data local)

**Validation**:
- Progress updates in real-time
- Cancel works correctly
- Success state dismisses dialog

**Rollback**: Remove progress UI

**Dependencies**: Task 8.2

---

### Task 8.4: Optional: Enforce Authentication for New Users
**Effort**: 1 hour
**Risk**: Medium
**Description**: Make sign up/sign in mandatory for new users (optional feature flag)

**Deliverable**:
- Update splash screen to route to sign in if not authenticated AND no local data
- Existing users can continue using app offline
- New users must create account

**Validation**:
- New installs require sign in
- Existing users unaffected
- Can be toggled with feature flag

**Rollback**: Remove authentication requirement

**Dependencies**: Phase 8 tasks

---

## PHASE 9: ERROR HANDLING & OFFLINE SUPPORT (MEDIUM RISK) ⏳ NOT STARTED

### 📋 PHASE 9 OVERVIEW
**Status**: NOT STARTED
**Critical Priority**: HIGH - Essential for production app
**Description**: Implement robust error handling, offline detection, and retry logic

**Current Issue**: Network errors will cause app crashes or unexpected behavior. This phase is essential before production release.

---

### Task 9.1: Implement Offline Detection ⏳ NOT STARTED
**Effort**: 2 hours
**Risk**: Low
**Description**: Monitor network connectivity and adjust sync behavior

**Deliverable**: Create `/composeApp/src/commonMain/kotlin/com/meneses/budgethunter/commons/network/NetworkMonitor.kt`:
```kotlin
class NetworkMonitor(/* platform-specific */) {
    val isOnline: StateFlow<Boolean>
}
```

Platform implementations for Android (ConnectivityManager) and iOS (NWPathMonitor)

**Validation**:
- Detects online/offline correctly
- Updates reactive state
- No battery drain

**Rollback**: Delete network monitor

**Dependencies**: None

---

### Task 9.2: Add Offline Banner
**Effort**: 1 hour
**Risk**: Low
**Description**: Show banner when offline

**Deliverable**: Add to main screen scaffold:
```kotlin
@Composable
fun OfflineBanner(isOffline: Boolean) {
    AnimatedVisibility(visible = isOffline) {
        Surface(color = Color.Gray) {
            Text("Offline - Changes will sync when online")
        }
    }
}
```

**Validation**:
- Banner shows when offline
- Hides when back online
- Doesn't obstruct UI

**Rollback**: Remove banner

**Dependencies**: Task 9.1

---

### Task 9.3: Implement Sync Retry Logic
**Effort**: 2 hours
**Risk**: Medium
**Description**: Retry failed syncs with exponential backoff

**Deliverable**: Update sync managers to:
- Catch network errors
- Queue failed syncs
- Retry with exponential backoff (1s, 2s, 4s, 8s, max 30s)
- Retry when back online

**Validation**:
- Failed sync retries automatically
- Exponential backoff works
- Stops retrying after max attempts

**Rollback**: Remove retry logic

**Dependencies**: Task 9.1

---

### Task 9.4: Add Comprehensive Error Handling
**Effort**: 3 hours
**Risk**: Medium
**Description**: Handle all API error scenarios gracefully

**Deliverable**: Create error handling for:
- 401 Unauthorized → Trigger token refresh → Retry request
- 403 Forbidden → Show error, user lacks permission
- 404 Not Found → Handle deleted resources
- 500 Server Error → Retry with backoff
- Network timeout → Retry
- Parse errors → Log and show generic error

**Validation**:
- Each error type handled correctly
- User sees appropriate messages
- App doesn't crash
- Data not corrupted

**Rollback**: Revert to simple error handling

**Dependencies**: Task 9.3

---

### Task 9.5: Implement Token Refresh on 401
**Effort**: 2 hours
**Risk**: Medium
**Description**: Automatically refresh expired tokens and retry request

**Deliverable**: Update Ktor Auth plugin in ApiClient:
```kotlin
install(Auth) {
    bearer {
        loadTokens { /* ... */ }

        refreshTokens {
            val refreshToken = tokenStorage.getRefreshToken() ?: return@refreshTokens null

            val response = authRepository.refreshToken()
            response.getOrNull()?.let {
                tokenStorage.saveAuthToken(it.authToken)
                tokenStorage.saveRefreshToken(it.refreshToken)
                BearerTokens(it.authToken, it.refreshToken)
            }
        }
    }
}
```

**Validation**:
- Expired token triggers refresh
- Refresh rotates token correctly
- Original request retries with new token
- Failed refresh logs user out

**Rollback**: Remove auto-refresh logic

**Dependencies**: Task 2.1, Task 9.4

---

## PHASE 10: TESTING & POLISH (LOW-MEDIUM RISK) ⏳ NOT STARTED

### 📋 PHASE 10 OVERVIEW
**Status**: NOT STARTED
**Priority**: MEDIUM - Essential for code quality and maintainability
**Description**: Add comprehensive tests, loading states, and user feedback

---

### Task 10.1: Write Unit Tests for Auth Repository ⏳ NOT STARTED
**Effort**: 3 hours
**Risk**: Low
**Description**: Test all auth flows with mock API

**Deliverable**: Create test file with tests for:
- Successful sign up
- Successful sign in
- Token refresh with rotation
- Sign out clears tokens
- Failed auth scenarios

**Validation**:
- All tests pass
- Mock API used (Ktor MockEngine)
- 80%+ coverage on auth code

**Rollback**: Delete tests (not recommended)

**Dependencies**: Task 2.1

---

### Task 10.2: Write Unit Tests for Sync Managers
**Effort**: 4 hours
**Risk**: Low
**Description**: Test sync logic thoroughly

**Deliverable**: Tests for:
- Push unsynced budgets
- Pull server budgets
- Merge without duplicates
- Conflict resolution
- Entry sync with creator info

**Validation**:
- All tests pass
- Edge cases covered
- 80%+ coverage

**Rollback**: Delete tests

**Dependencies**: Task 4.2, Task 5.2

---

### Task 10.3: Integration Tests for Full Sync Flow
**Effort**: 3 hours
**Risk**: Low
**Description**: End-to-end tests with real local database and mock API

**Deliverable**: Tests for:
- Create local budget → Sync → Verify on server
- Create server budget → Pull → Verify locally
- Full bidirectional sync
- Migration flow

**Validation**:
- Tests run against actual SqlDelight database
- Mock API responds correctly
- Tests are repeatable

**Rollback**: Delete tests

**Dependencies**: Phase 4, Phase 5 complete

---

### Task 10.4: Add Loading States to All Async Operations
**Effort**: 2 hours
**Risk**: Low
**Description**: Ensure all API calls show loading indicators

**Deliverable**: Review and add loading states to:
- Sign in/sign up screens
- Budget list refresh
- Entry creation/update
- Collaborator operations

**Validation**:
- All async operations show loading
- Loading indicators dismiss on completion
- User can't trigger duplicate requests

**Rollback**: Not applicable (polish)

**Dependencies**: Phases 2-7 complete

---

### Task 10.5: Add User Feedback for Sync Operations
**Effort**: 2 hours
**Risk**: Low
**Description**: Toast/snackbar messages for sync success/failure

**Deliverable**:
- "Budget synced successfully"
- "Failed to sync - will retry when online"
- "New entry from [collaborator]"
- "Signed in as [email]"

**Validation**:
- Messages appear at appropriate times
- Not too intrusive
- User understands what happened

**Rollback**: Remove messages

**Dependencies**: Phases 2-7 complete

---

### Task 10.6: Performance Testing with Large Datasets
**Effort**: 2 hours
**Risk**: Low
**Description**: Test app with 100+ budgets and 1000+ entries

**Deliverable**:
- Create test data generator
- Test sync performance
- Test UI scroll performance
- Test SSE with high message volume

**Validation**:
- App remains responsive
- Sync completes in reasonable time
- No memory leaks
- UI doesn't lag

**Rollback**: Not applicable (testing only)

**Dependencies**: Phases 4-7 complete

---

### Task 10.7: Add Configuration for Backend URL
**Effort**: 1 hour
**Risk**: Low
**Description**: Make backend URL configurable (dev/staging/prod)

**Deliverable**: Update `local.properties`:
```properties
GEMINI_API_KEY=...
BACKEND_URL=http://localhost:8080  # or production URL
```

Read in build.gradle.kts and inject as BuildConfig field.

**Validation**:
- Can switch between environments
- Default to production
- Works on both Android and iOS

**Rollback**: Hardcode URL

**Dependencies**: None

---

## PHASE 11: DOCUMENTATION & DEPLOYMENT (LOW RISK) ⏳ NOT STARTED

### 📋 PHASE 11 OVERVIEW
**Status**: NOT STARTED
**Priority**: LOW - Can be done incrementally alongside feature development
**Description**: Update documentation and prepare for release

---

### Task 11.1: Update User Guide ⏳ NOT STARTED
**Effort**: 2 hours
**Risk**: Low
**Description**: Document new features in app user guide

**Deliverable**: Update user guide with:
- How to sign up/sign in
- How to sync data
- How to add collaborators
- Offline mode explanation

**Validation**:
- User guide renders correctly
- Clear and concise

**Rollback**: Not applicable

**Dependencies**: All features complete

---

### Task 11.2: Create Migration Guide for Existing Users
**Effort**: 1 hour
**Risk**: Low
**Description**: In-app tutorial for migrating local data

**Deliverable**:
- One-time tutorial on first launch after update
- Screenshots/animations
- Step-by-step guide

**Validation**:
- Tutorial is clear
- Shows only once
- Can be skipped

**Rollback**: Remove tutorial

**Dependencies**: Task 8.1

---

### Task 11.3: Add Privacy Policy & Terms
**Effort**: 1 hour
**Risk**: Low
**Description**: Add links to privacy policy and terms of service

**Deliverable**:
- Add settings screen entries
- Link to hosted documents
- Show on sign up

**Validation**:
- Links work
- Documents load

**Rollback**: Remove links

**Dependencies**: None

---

### Task 11.4: Prepare for App Store Submission
**Effort**: 2 hours
**Risk**: Low
**Description**: Update app metadata for new features

**Deliverable**:
- Update app description
- New screenshots with sync features
- Update version number
- Release notes

**Validation**:
- Metadata is accurate
- Screenshots look professional

**Rollback**: Not applicable

**Dependencies**: All features complete

---

## RECOMMENDED ACTION PLAN & PRIORITIES

### 🎯 SHORT-TERM PRIORITIES (Next 2 Weeks)
Focus on getting basic sync functionality working:

**Week 1: Database Schema & Budget Sync**
1. ✅ Task 3.1: Update Budget schema (2 hours)
2. ✅ Task 3.2: Update BudgetEntry schema (2 hours)
3. ✅ Task 3.3: Create sync status indicator UI (1.5 hours)
4. ✅ Task 4.1: Create BudgetApiService (3 hours)
5. ✅ Task 4.2: Create BudgetSyncManager (4 hours)
6. ✅ Task 4.3: Integrate sync into BudgetRepository (2 hours)

**Week 2: Entry Sync & Basic Error Handling**
7. ✅ Task 5.1: Create BudgetEntryApiService (3 hours)
8. ✅ Task 5.2: Create BudgetEntrySyncManager (4 hours)
9. ✅ Task 5.3: Integrate sync into BudgetEntryRepository (2 hours)
10. ✅ Task 9.4: Add comprehensive error handling (3 hours)
11. ✅ Task 9.5: Implement token refresh on 401 (2 hours)

**Total: ~28.5 hours** - Achievable in 2 weeks with ~3 hours/day

### 🎯 MEDIUM-TERM PRIORITIES (Weeks 3-4)
Make the app production-ready:

**Week 3: Offline Support & UI Polish**
12. ✅ Task 4.5: Add pull-to-refresh for budget list (1.5 hours)
13. ✅ Task 4.6: Show sync status in budget list UI (1 hour)
14. ✅ Task 5.4: Update budget detail screen for entry sync (2 hours)
15. ✅ Task 9.1: Implement offline detection (2 hours)
16. ✅ Task 9.2: Add offline banner (1 hour)
17. ✅ Task 9.3: Implement sync retry logic (2 hours)
18. ✅ Task 10.7: Make backend URL configurable (1 hour)

**Week 4: Background Sync & Testing**
19. ✅ Task 4.4: Add background sync worker (3 hours)
20. ✅ Task 10.1: Write unit tests for AuthRepository (3 hours)
21. ✅ Task 10.2: Write unit tests for sync managers (4 hours)
22. ✅ Task 10.4: Add loading states to all async operations (2 hours)
23. ✅ Task 10.5: Add user feedback for sync operations (2 hours)

**Total: ~24.5 hours** - Achievable in 2 weeks with ~2.5 hours/day

### 🎯 LONG-TERM PRIORITIES (Weeks 5+)
Advanced features and polish:

**Collaboration & Real-time (Week 5-6)**
- Phase 6: Collaborator Management (8 hours)
- Phase 7: Real-time Updates with SSE (9.5 hours)

**Migration & Documentation (Week 7-8)**
- Phase 8: Data migration for existing users (if needed) (10 hours)
- Phase 10: Additional testing and polish (remaining tasks) (~10 hours)
- Phase 11: Documentation and deployment (6 hours)

### ⚠️ CRITICAL ITEMS TO ADDRESS IMMEDIATELY
1. ~~**Clear Local Data on Sign Out** (Task 2.8)~~ ✅: COMPLETED - Sign out now clears all budgets and entries
2. **Fix Token Refresh Logic** (Task 9.5): HttpClientFactory has placeholder for token refresh - needs to call AuthRepository.refreshToken()
3. **Make Base URL Configurable** (Task 10.7): Move hardcoded "http://10.0.2.2:8080" to build config or environment variable
4. **Add Basic Error Handling**: Current auth flows don't handle network errors gracefully

### 🚫 CAN BE DEFERRED
1. **Use Cases** (Task 2.2): Not needed for current architecture, can add later if needed
2. **Data Migration Dialog** (Phase 8): Only needed if you have existing users with local data
3. **Collaborator Management** (Phase 6): Core sync must work first
4. **SSE Real-time Updates** (Phase 7): Nice-to-have, but not essential for MVP
5. **Background Sync Worker** (Task 4.4): Manual sync is sufficient initially

---

## SUMMARY & SUCCESS METRICS

### Total Estimated Effort

**Completed:**
- Phase 1: 10.5 hours ✅ (100% complete)
- Phase 2: 16 hours ✅ (skipped 2.5 hours for use cases)

**Remaining:**
- Phase 3: 5.5 hours ⏳
- Phase 4: 15.5 hours ⏳
- Phase 5: 13 hours ⏳
- Phase 6: 8 hours ⏳
- Phase 7: 9.5 hours ⏳
- Phase 8: 10 hours ⚠️ (partially addressed, migration tasks remaining)
- Phase 9: 10 hours ⏳ (critical for production)
- Phase 10: 18 hours ⏳
- Phase 11: 6 hours ⏳

**TOTAL ORIGINAL ESTIMATE**: ~125 hours (~3 weeks for one developer)
**COMPLETED SO FAR**: ~26.5 hours (21%)
**REMAINING WORK**: ~98.5 hours (~2.5 weeks)

**REALISTIC MVP ESTIMATE** (excluding optional features):
- Core sync functionality: ~53 hours (Phases 3, 4, 5)
- Error handling & offline support: ~10 hours (Phase 9)
- Basic testing: ~10 hours (Phase 10 subset)
- Configuration: ~1 hour (Task 10.7)
**MVP TOTAL**: ~74 hours from current state (~2 weeks with focus)

### Success Metrics & Current Status

1. **Authentication** ✅
   - ✅ Sign up and sign in working
   - ✅ Token storage implemented
   - ⚠️ Token refresh placeholder needs completion
   - ✅ Navigation integrated

2. **Data Integrity** ⏳ (Not yet applicable)
   - Target: 100% of local data migrates successfully
   - Status: Database schema changes not yet implemented

3. **Offline Support** ❌ (Not yet implemented)
   - Target: App fully functional without internet
   - Status: Network errors will cause failures

4. **Real-Time Updates** ⏳ (Not started)
   - Target: Collaborator entries appear within 2 seconds
   - Status: SSE not implemented

5. **Sync Performance** ⏳ (Not yet applicable)
   - Target: 100 budgets sync in <5 seconds
   - Status: Sync not implemented

6. **Error Rate** ⏳
   - Target: <1% of API calls fail due to client bugs
   - Status: Basic error handling exists, needs improvement

### Rollback Strategy
Each phase can be rolled back independently by:
1. Feature flag to disable backend features
2. Database migration scripts to revert schema
3. Keep local-only mode fully functional
4. Gradual rollout (10% → 50% → 100% of users)

### Testing Strategy
1. **Unit Tests**: All repositories, use cases, sync managers
2. **Integration Tests**: Full sync flows with mock API
3. **Manual Testing**: Each screen after each phase
4. **Beta Testing**: Release to 10% of users before full rollout
5. **Regression Testing**: Verify existing features after each phase

### Key Principles Applied
- Smallest possible increments (each task 0.5-4 hours)
- Existing functionality preserved at every step
- Database migrations are additive (no data loss)
- Local-only mode always available (offline-first)
- Authentication is optional initially (migration strategy)
- Each phase is independently testable and deployable
- Comprehensive error handling and retry logic
- Real-time updates don't interfere with local operations
