# Spring Backend Concepts

## 1. JDBC — Steps to Connect to a Database

JDBC (Java Database Connectivity) is the standard Java API to interact with relational databases. Every database operation follows these **7 steps** in order:

| Step | What Happens |
|------|--------------|
| **1. Load Driver** | Registers the database-specific driver (e.g., MySQL, PostgreSQL) with the JVM |
| **2. Define Connection URL** | Specifies the database location, e.g., `jdbc:mysql://localhost:3306/mydb` |
| **3. Establish Connection** | `DriverManager.getConnection(url, user, password)` — opens a physical connection to DB |
| **4. Create Statement Object** | `conn.createStatement()` or `conn.prepareStatement(sql)` — creates the query executor |
| **5. Execute Query Using Statement** | Runs the SQL — `executeQuery()` for SELECT, `executeUpdate()` for INSERT/UPDATE/DELETE |
| **6. Process Result** | Iterates over the `ResultSet` to read returned data row by row |
| **7. Close Connection** | Releases the DB connection — **always do this** (ideally in a `finally` block or try-with-resources) |

### Example (JDBC with PreparedStatement)
```java
Connection conn = DriverManager.getConnection(url, user, password);
PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?");
ps.setInt(1, 101);
ResultSet rs = ps.executeQuery();
while (rs.next()) {
    System.out.println(rs.getString("productName"));
}
conn.close();
```

> ⚠️ **Problem with raw JDBC:** Opening and closing a connection for every request is expensive. This is where **Connection Pooling** comes in.

---

## 2. Connection Pooling

### The Problem
Every time you call `DriverManager.getConnection()`, a new physical connection to the database is created. This is:
- **Slow** (takes time to establish TCP handshake, auth, etc.)
- **Resource-heavy** (each connection uses memory on both app and DB side)
- **Not scalable** for many users hitting the app simultaneously

### The Solution: Connection Pooling

```
Application ←──────→ Connection Pool ←──────→ Database
               borrow/return         single set of
                connections          connections
```

A **Connection Pool** is a cache of pre-opened database connections. Instead of creating a new connection each time:
- The pool maintains a set of **ready-to-use connections**
- Your app **borrows** a connection from the pool when needed
- After the query, the connection is **returned** (not closed) to the pool for reuse

### Popular Connection Pool Libraries
| Library | Used With |
|---------|-----------|
| **HikariCP** | Spring Boot default (auto-configured) |
| **Apache DBCP** | General Java apps |
| **C3P0** | Older projects |

### In Spring Boot (application.properties)
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
```

> Spring Boot auto-configures HikariCP when you add `spring-boot-starter-data-jpa` — no manual setup needed!

---

## 3. ORM, Hibernate & JPA

### The Problem with JDBC
With JDBC, you must manually:
- Write SQL for every query
- Map each `ResultSet` column to a Java object field
- Handle all CRUD boilerplate

For a `Product` table with 10 columns, you'd write repetitive SQL + mapping code constantly.

### What is ORM?

**ORM = Object-Relational Mapping**

It bridges the gap between **Java Objects** and **Database Tables** automatically.

```
Java Object              →    Database Table
─────────────────────────────────────────────
Product (class)          →    PRODUCT (table)
  prodId (field)         →    prod_id (column)
  prodName (field)       →    prod_name (column)
  price (field)          →    price (column)

product object:
  prodId = 101           →    Row: 101 | iPhone | 50000
  prodName = "iPhone"
  price = 50000
```

### Popular ORM Tools
| ORM Tool | Notes |
|----------|-------|
| **Hibernate** | Most popular Java ORM, powerful and widely used |
| **EclipseLink** | JPA reference implementation |
| **MyBatis** | Semi-ORM — you still write SQL but mapping is automatic |

### JPA vs Hibernate vs Spring Data JPA

```
Spring Data JPA  (highest abstraction — magic repositories)
      ↓
   JPA API       (standard specification / interface)
      ↓
  Hibernate      (actual implementation doing the work)
      ↓
   JDBC           (talks to the actual database)
```

| Layer | What it is |
|-------|-----------|
| **JPA** | A *specification* (set of rules/interfaces) — not actual code that runs |
| **Hibernate** | An *implementation* of JPA — the actual engine that generates SQL |
| **Spring Data JPA** | A Spring wrapper over JPA/Hibernate — gives you `JpaRepository` with zero SQL |

### Example — Spring Data JPA
```java
// Entity
@Entity
public class Product {
    @Id
    private int prodId;
    private String prodName;
    private double price;
}

// Repository — no SQL needed!
public interface ProductRepository extends JpaRepository<Product, Integer> {
    // findById, save, findAll, delete — all built-in!
}
```

> ORM lets you think in **Java objects**, not SQL tables. Hibernate handles the translation.

---

## 4. Servlet Container & Front Controller

### What is a Servlet Container?
A **Servlet Container** (like **Apache Tomcat**) is the runtime environment that:
- Receives HTTP requests from clients (browsers, Postman, etc.)
- Routes them to the correct servlet
- Sends back the HTTP response

```
Client (Browser/App)
        |
        | HTTP Request
        ↓
  ┌─────────────────────────────┐
  │      Servlet Container      │  ← Tomcat
  │                             │
  │  ┌──────────────────────┐   │
  │  │  Front Controller    │   │  ← DispatcherServlet (Spring MVC)
  │  │  (DispatcherServlet) │   │
  │  └──────────┬───────────┘   │
  │             │               │
  │    ┌────────┼────────┐      │
  │    ↓        ↓        ↓      │
  │  home/   add/    balance/   │  ← Your @RestController methods
  │                             │
  └─────────────────────────────┘
        |
        | HTTP Response
        ↓
       Client
```

### Front Controller Pattern
Instead of having each URL mapped to its own servlet, Spring uses a single **DispatcherServlet** as the **Front Controller**:
- All requests come to `DispatcherServlet` first
- It inspects the URL and routes to the correct `@Controller`/`@RestController` method
- This is the core of **Spring MVC**

> In Spring Boot, Tomcat is **embedded** — you don't configure it separately. Just run the app!

---

## 5. CSRF — Cross-Site Request Forgery

### What is CSRF?
**CSRF (Cross-Site Request Forgery)** is a security attack where a malicious website tricks your logged-in browser into making unwanted requests to another site where you're authenticated.

### How the Attack Works
```
1. You log into your bank → browser stores your session cookie
2. You visit a malicious site (without logging out)
3. The malicious site secretly sends a request to your bank
   (e.g., transfer money) using your stored cookie
4. The bank thinks it's YOU making the request → executes it!
```

### Why it Works
Browsers automatically attach cookies to requests. The malicious site exploits this — it doesn't need your password, just your active session.

### CSRF Protection in Spring Security
Spring Security enables CSRF protection **by default** for state-changing operations (POST, PUT, DELETE).

It uses a **CSRF Token** strategy:
- Server sends a unique token with each form/session
- Every state-changing request must include this token
- If the token is missing/wrong → request is rejected

### For REST APIs (Stateless)
Since REST APIs use **JWT** (stateless, no session cookies), CSRF is generally disabled:
```java
http.csrf(csrf -> csrf.disable()); // Safe for stateless JWT-based APIs
```

> CSRF is a concern for **session-based (cookie)** authentication. With JWT in headers, CSRF doesn't apply.

---

## 6. Authentication Provider Flow

### The Flow

```
  Unauthenticated              Authentication              Authenticated
     Object        ──────────→   Provider     ──────────→    Object
  (username +                  (validates                  (user details +
   password)                  credentials)                  authorities)
```

### What Each Part Means

| Component | Role |
|-----------|------|
| **Unauthenticated Object** | An `Authentication` object with just username + password (not yet verified) |
| **Authentication Provider** | Checks the credentials against a `UserDetailsService` + password encoder |
| **Authenticated Object** | Same `Authentication` object, now marked as authenticated, with roles/authorities attached |

### The Full Spring Security Chain
```
HTTP Request
    ↓
Security Filter Chain
    ↓
UsernamePasswordAuthenticationFilter (extracts credentials)
    ↓
AuthenticationManager
    ↓
AuthenticationProvider  ← calls UserDetailsService + BCryptPasswordEncoder
    ↓
SecurityContext (stores authenticated user for the request)
    ↓
Your Controller
```

### Key Classes
```java
// 1. Load user from DB
@Service
public class MyUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) {
        // fetch user from DB, return UserDetails object
    }
}

// 2. Configure auth in SecurityFilterChain
@Bean
public AuthenticationProvider authProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(myUserDetailsService);
    provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
    return provider;
}
```

---

## 7. Encryption

### What is Encryption?
**Encryption** is the process of converting readable data (**Plain Text**) into an unreadable format (**Cipher Text**) using a **key**, so only authorized parties can read it.

```
Plain Text  + 🔑 Key  →  🔒  →  Cipher Text
Cipher Text + 🔑 Key  →  🔓  →  Plain Text (original)
```

### Types of Encryption

| Type | How it Works | Example Use |
|------|-------------|-------------|
| **Symmetric** | Same key for encrypt & decrypt | AES (fast, for data at rest) |
| **Asymmetric** | Public key to encrypt, Private key to decrypt | RSA, HTTPS/TLS |
| **Hashing** | One-way — cannot be reversed | BCrypt for passwords |

### Encryption vs Hashing

| | Encryption | Hashing |
|--|-----------|---------|
| Reversible? | ✅ Yes (with key) | ❌ No |
| Use case | Sensitive data in transit/storage | Passwords |
| Example | AES, RSA | BCrypt, SHA-256 |

> **Passwords should NEVER be encrypted — they should be HASHED.** Encryption can be reversed; hashing cannot. If your DB is breached, encrypted passwords can be decrypted, but hashed ones cannot be reversed.

### BCrypt in Spring Security
```java
// Encoding a password
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12); // strength = 12
String hashed = encoder.encode("myPassword123");
// Result: $2a$12$... (different every time due to salt)

// Verifying
boolean match = encoder.matches("myPassword123", hashed); // true
```

### Encryption in HTTPS (JWT-relevant)
JWT tokens use **signing** (HMAC with a secret key or RSA) to ensure the token hasn't been tampered with:
- **HS256** → Symmetric (same secret key to sign + verify)
- **RS256** → Asymmetric (private key to sign, public key to verify)
