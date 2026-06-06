# Spring Boot — JWT Authentication

## Project Structure

```
com.example.security/
│
├── SecurityApplication.java          ← Entry point (@SpringBootApplication)
│
├── model/
│   ├── User.java                     ← JPA entity mapped to the users table
│   └── UserPrincipal.java            ← Adapter: wraps User into UserDetails
│
├── repository/
│   └── UserRepository.java           ← Spring Data JPA repository
│
├── service/
│   ├── MyUserDetailsService.java     ← Loads user from DB for Spring Security
│   ├── UserService.java              ← Register + Login business logic
│   └── JWTService.java               ← Token generation, parsing, validation
│
├── config/
│   ├── SecurityConfig.java           ← Rules: who can access what, session policy
│   └── JWTFilter.java                ← Intercepts every request, validates token
│
└── controller/
    └── UserController.java           ← POST /register and POST /login endpoints
```

---

## The Full Request Lifecycle

### 1. Registration — `POST /register`

```
Client sends  →  { "id": 1, "username": "test", "password": "secret123" }
                              ↓
                     UserController.register()
                              ↓
                     UserService.register()
                              ↓
              BCryptPasswordEncoder encodes "secret123"
                              ↓
              User saved to DB with hashed password
                              ↓
                     Returns saved User object
```

The plain-text password **never** touches the database. BCrypt with strength 10 applies 2¹⁰ = 1024 hashing rounds.

---

### 2. Login / Token Generation — `POST /login`

```
Client sends  →  { "username": "test", "password": "secret123" }
                              ↓
                     UserController.login()
                              ↓
                     UserService.verify()
                              ↓
               AuthenticationManager.authenticate()
            creates UsernamePasswordAuthenticationToken
                              ↓
           DaoAuthenticationProvider (from SecurityConfig)
                              ↓
           MyUserDetailsService.loadUserByUsername("test")
                              ↓
           UserRepository.findByUsername("test")  ← DB query
                              ↓
              Returns UserPrincipal (Spring Security format)
                              ↓
              BCrypt compares raw password with stored hash
                              ↓
              authentication.isAuthenticated() == true
                              ↓
              JWTService.generateToken("test")
                              ↓
              Client receives signed JWT string
```

---

### 3. Accessing a Protected Endpoint

```
Client sends request with header:
          Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
                              ↓
                         JWTFilter runs
                    (before controller is hit)
                              ↓
              Extract token from "Bearer " prefix
                              ↓
              jwtService.extractUserName(token)  → "test"
                              ↓
              SecurityContext has no existing auth?  → yes
                              ↓
              loadUserByUsername("test")  ← DB lookup
                              ↓
              jwtService.validateToken(token, userDetails)
                 checks: username matches + not expired
                              ↓
              Set authentication in SecurityContextHolder
                              ↓
              filterChain continues → Controller reached ✅
```

If the token is missing, expired, or tampered with → 403 Forbidden, controller never reached.

---

## File-by-File Deep Dive

---

### `model/UserPrincipal.java` — The Bridge Class

Spring Security does not know what a `User` is. It only works with `UserDetails`. `UserPrincipal` is the **adapter** that bridges the two.

**Why is this class needed?**

```
Our world:        Spring Security's world:
──────────        ───────────────────────
User.java    →    UserDetails (interface)
```

Spring Security calls `getPassword()`, `getUsername()`, `getAuthorities()` — methods that don't exist on our plain `User` entity. `UserPrincipal` implements all of them by delegating to the inner `User` object.

---

### `service/MyUserDetailsService.java` — Loading Users for Security

```java
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User not found");
        return new UserPrincipal(user);
    }
}
```

**This is the class Spring Security calls automatically during authentication.**

Spring Security's `DaoAuthenticationProvider` calls `loadUserByUsername()` with the username from the login request. This method:
1. Queries the DB for a `User` with that username
2. Throws if not found
3. Wraps the result in a `UserPrincipal` so Spring Security can work with it

You never call `loadUserByUsername()` yourself — Spring Security calls it internally during `authManager.authenticate()`.

---

### `service/JWTService.java` — Token Engine

This is the core of the JWT mechanism. It has four responsibilities:

#### 1. Secret Key Generation (constructor)

```java
public JWTService() {
    KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
    SecretKey sk = keyGen.generateKey();
    secretKey = Base64.getEncoder().encodeToString(sk.getEncoded());
}
```

A random 256-bit HMAC-SHA256 key is generated **once when the app starts** and stored as a Base64 string. Every JWT issued during that session is signed with this key.

> ⚠️ **Learning note:** Because the key is regenerated on every app restart, all previously issued tokens become invalid after a restart. In production, the secret key is stored in environment variables or a secrets manager (e.g., AWS Secrets Manager) so it survives restarts.

#### 2. Token Generation

```java
public String generateToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    return Jwts.builder()
            .claims().add(claims)
            .subject(username)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 30))
            .and()
            .signWith(getKey())
            .compact();
}
```

The JWT is built with three parts:

| Part | Content |
|---|---|
| Header | Algorithm: HS256 |
| Payload (Claims) | `sub` = username, `iat` = issued-at timestamp, `exp` = expiry timestamp |
| Signature | HMAC-SHA256 signature using the secret key |

The `claims` map is empty here but can carry extra data (roles, user ID, email, etc.) in production.

**Expiry:** `60 * 60 * 30` = 108,000 milliseconds = **108 seconds (~1.8 minutes)**. This is very short — intended for learning. Production tokens typically expire in 15 minutes to 1 hour.

#### 3. Token Parsing

```java
public String extractUserName(String token) {
    return extractClaim(token, Claims::getSubject);
}

private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
}

private Claims extractAllClaims(String token) {
    return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

`extractAllClaims()` does two things at once: it **verifies the signature** (rejects tampered tokens) and **decodes the payload**. `extractClaim` uses a `Function<Claims, T>` so you can extract any field generically — `Claims::getSubject` for username, `Claims::getExpiration` for expiry, etc.

#### 4. Token Validation

```java
public boolean validateToken(String token, UserDetails userDetails) {
    final String userName = extractUserName(token);
    return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
}
```

Two conditions must both be true:
- The username in the token matches the user loaded from the DB
- The token has not expired

---

### `service/UserService.java` — Business Logic

```java
@Service
public class UserService {
    @Autowired private JWTService jwtService;
    @Autowired AuthenticationManager authManager;
    @Autowired private UserRepository userRepo;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public User register(User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        userRepo.save(user);
        return user;
    }

    public String verify(User user) {
        Authentication authentication = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(user.getUsername());
        }
        return "fail";
    }
}
```

**`register()`**: Encodes the password with BCrypt (strength 10) before persisting. The original plain-text password is never stored.

**`verify()`**: Creates a `UsernamePasswordAuthenticationToken` with the raw credentials and hands it to Spring Security's `AuthenticationManager`. Internally, this triggers the full chain: `DaoAuthenticationProvider` → `MyUserDetailsService.loadUserByUsername()` → BCrypt comparison. If it passes, a JWT is generated and returned to the client.

---

### `config/SecurityConfig.java` — The Rules Engine

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(customizer -> customizer.disable())
            .authorizeHttpRequests(request -> request
                .requestMatchers("/register", "/login").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(10));
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**`DaoAuthenticationProvider`**: Tells Spring Security: "When you need to verify credentials, use this `UserDetailsService` to look up the user and this `BCryptPasswordEncoder` to compare the password."

**`AuthenticationManager` bean**: Exposes Spring's internal `AuthenticationManager` so `UserService` can call `authManager.authenticate()` directly.

---

### `config/JWTFilter.java` — The Request Interceptor

```java
@Component
public class JWTFilter extends OncePerRequestFilter {

    @Autowired private JWTService jwtService;
    @Autowired ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtService.extractUserName(token);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = context.getBean(MyUserDetailsService.class)
                                             .loadUserByUsername(username);
            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

`OncePerRequestFilter` guarantees this runs exactly once per HTTP request (not once per servlet dispatch).

**Step-by-step logic:**

```
1. Read the "Authorization" header
2. Does it start with "Bearer "?  → Yes → extract the token (substring after "Bearer ")
3. Parse the username from the token payload
4. Is the username non-null AND no auth is set in SecurityContext yet?
       → load UserDetails from DB
       → validate token (username match + not expired)
       → if valid: create auth object and set it in SecurityContextHolder
5. Always call filterChain.doFilter() to pass the request forward
```

Setting the auth in `SecurityContextHolder` is what tells Spring Security "this request is authenticated." If validation fails, the security context stays empty and Spring Security returns 403.

> **Why `ApplicationContext` instead of `@Autowired MyUserDetailsService`?** Direct `@Autowired` between two `@Component`/`@Bean` classes that both depend on `SecurityConfig` can create a circular dependency. Fetching the bean lazily from `ApplicationContext` breaks that cycle.

--- 

## Key Design Decisions Explained

**Why BCrypt and not SHA-256?**
BCrypt is specifically designed for password hashing. It is intentionally slow (configurable rounds) and includes a built-in salt, making rainbow table and brute-force attacks impractical. SHA-256 is a general-purpose hash — fast, no salt, unsuitable for passwords.

**Why `STATELESS` session management?**
HTTP sessions store login state on the server. In a stateless REST API (especially one meant to scale horizontally), you don't want server-side state. The JWT carries all the proof of identity — the server just validates it mathematically on every request.

**Why is the JWT secret regenerated on every restart?**
This is a learning/development shortcut. In production the secret is fixed (environment variable or secrets manager) so tokens survive restarts and so multiple app instances share the same key.

**Why does `JWTFilter` not extend `SecurityConfig`?**
Separation of concerns. `SecurityConfig` defines rules (who can go where). `JWTFilter` enforces one specific rule (validate the bearer token). Keeping them separate makes each easier to understand and test.