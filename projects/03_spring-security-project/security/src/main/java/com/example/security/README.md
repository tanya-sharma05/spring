# Spring Security — Custom Authentication Flow

Here, we're creating custom Spring Security authentication. Normally Spring Security has a default username/password. But here we are saying:

> Spring, don't use your default users. I have my own **User** table in database. When someone logs in, go to my database, find the user, check the password and then allow/deny access.

---

## Authentication Flow

```
            CLIENT (Browser/Postman)
                    |
                    | username + password
                    ↓
            Spring Security Filter Chain
                (SecurityConfig)
                    |
                    ↓
               AuthenticationProvider
            (DaoAuthenticationProvider)
                    |
                    ↓
               UserDetailsService
             (MyUserDetailsService)
                    |
                    ↓
                UserRepository
                    |
                    ↓
              Database User Table
                    |
                    ↓
                 User Entity
                    |
                    ↓
                UserPrincipal
    (convert our User into Spring Security User)
                    |
                    ↓
                Authentication Success / Fail
```

---

## Component Breakdown

Think of Spring Security as a **security guard**. Each component plays a specific role:

| Component | Role | Analogy |
|---|---|---|
| `SecurityConfig` | Defines access rules and wires components together | **Rules of the building** |
| `AuthenticationProvider` (`DaoAuthenticationProvider`) | Validates credentials using the UserDetailsService | **How to check identity** |
| `UserDetailsService` (`MyUserDetailsService`) | Fetches user data from the database | **Person who finds the employee record** |
| `UserRepository` | Interfaces with the database | **Database access** |
| `User` | The raw entity stored in the database | **Database record** |
| `UserPrincipal` | Wraps `User` into a format Spring Security understands | **ID card format the security guard understands** |
 
---

# Spring IoC & @Autowired — How Beans Get Injected

### Step 1 — You annotate a class

```java
@Service
public class MyUserDetailsService implements UserDetailsService { }
```

Spring sees `@Service` on startup and does this internally:

```java
MyUserDetailsService obj = new MyUserDetailsService(); // Spring does this, not you
```

This stored object is called a **Bean**.

---

### Step 2 — You request it with `@Autowired`

```java
@Autowired
private UserDetailsService userDetailsService;
```

Spring checks its container: *"Do I have anything of type `UserDetailsService`?"*

It finds `MyUserDetailsService` because:

```
             UserDetailsService (Interface)
                        ↑
   MyUserDetailsService (your class — implements the interface)
```

So Spring injects it. You never call `new`.

---

### Step 3 — Same thing happens for `UserRepository`

```java
@Repository
public interface UserRepository extends JpaRepository<User, Integer> { }
```

Spring Data JPA sees this and internally creates a concrete implementation with all methods (`save`, `findAll`, `findByUserName`, etc.) and stores it as a Bean.

---

## Spring Container at Runtime

```
SPRING CONTAINER
─────────────────────────────────
  MyUserDetailsService  (@Service)
  UserRepository        (@Repository)
  SecurityConfig        (@Configuration)
─────────────────────────────────
```

Injection chain:

```
UserRepository  ──injected into──▶  MyUserDetailsService
MyUserDetailsService  ──injected into──▶  SecurityConfig
```

---

## Why Use the Interface, Not the Class?

```java
// ✅ Preferred
@Autowired
private UserDetailsService userDetailsService;

// ⚠️ Works, but tightly coupled
@Autowired
private MyUserDetailsService userDetailsService;
```

Using the interface means you can swap implementations without touching `SecurityConfig`:

```
Today:   UserDetailsService ← MyUserDetailsService   (DB login)
Tomorrow: UserDetailsService ← GoogleUserDetailsService (Google login)
```

`SecurityConfig` doesn't need to change at all.

---

## `UserDetails` — Same Pattern

Spring Security only accepts user data in `UserDetails` format. Your `User` entity doesn't qualify directly, so you wrap it:

```java
public class UserPrincipal implements UserDetails { }
```

Then in `MyUserDetailsService`:

```java
@Override
public UserDetails loadUserByUsername(String username) {
    User user = userRepo.findByUserName(username);
    return new UserPrincipal(user); // wrapped in UserDetails format
}
```

---

## Full Startup Sequence

```
App Starts
    │
    ▼
Spring scans all classes
    │
    ├── @Service found     → creates MyUserDetailsService bean
    ├── @Repository found  → creates UserRepository bean
    └── @Configuration found → creates SecurityConfig bean
    │
    ▼
@Autowired runs
    │
    ├── UserRepository      ──▶ injected into MyUserDetailsService
    └── MyUserDetailsService ──▶ injected into SecurityConfig
    │
    ▼
Application Ready ✅
```

**You declare. Spring creates. This is Inversion of Control (IoC).**