# Spring REST API 

## What is a REST API?

REST (Representational State Transfer) is an architectural style for building web services. A REST API exposes resources (data) over HTTP using standard methods like GET, POST, PUT, DELETE. Each resource is identified by a URL (endpoint), and data is typically exchanged in JSON format.

---

## Project Setup

### Maven Dependency (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

This single dependency pulls in Spring MVC, an embedded Tomcat server, and Jackson (for JSON serialization/deserialization).

---

## Core Annotations

### `@RestController`

Marks a class as a REST controller. It is a combination of `@Controller` + `@ResponseBody`, meaning every method in this class automatically serializes return values to JSON.

```java
@RestController
public class StudentController {
    // all methods here return JSON by default
}
```

### `@Controller`

Marks a class as a Spring MVC controller. Methods return view names (for server-side rendering with Thymeleaf, JSP, etc.). Used in traditional web apps, not REST APIs.

```java
@Controller
public class PageController {
    public String home() {
        return "home"; // returns a view name
    }
}
```

> **Difference:** Use `@RestController` for REST APIs. Use `@Controller` when returning HTML views.

### `@RequestMapping`

Maps HTTP requests to a class or method. Can be placed at the **class level** (base path) and **method level** (specific path + method).

```java
@RestController
@RequestMapping("/api/students") // base path for all methods in this class
public class StudentController {

    @RequestMapping(method = RequestMethod.GET)
    public List<Student> getAll() {}
}
```

---

## HTTP Method Annotations

These are shorthand/composed annotations for `@RequestMapping` with a specific HTTP method. Always prefer these over `@RequestMapping` for clarity.

### `@GetMapping` — Read / Retrieve

Used to **fetch** data. Maps HTTP GET requests. Should not modify any data.

```java
@GetMapping("/students")
public List<Student> getAllStudents() {
    return studentService.findAll();
}

@GetMapping("/students/{id}")
public Student getById(@PathVariable Long id) {
    return studentService.findById(id);
}
```

| Aspect | Detail |
|---|---|
| HTTP Method | GET |
| Purpose | Retrieve a resource or list of resources |
| Request Body | Not used |
| Safe? | Yes (no side effects) |
| Idempotent? | Yes |

---

### `@PostMapping` — Create

Used to **create** a new resource. Maps HTTP POST requests. The new data is sent in the request body.

```java
@PostMapping("/students")
public ResponseEntity<Student> createStudent(@RequestBody Student student) {
    Student saved = studentService.save(student);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

| Aspect | Detail |
|---|---|
| HTTP Method | POST |
| Purpose | Create a new resource |
| Request Body | Required (contains new data) |
| Safe? | No |
| Idempotent? | No (calling twice creates two records) |

---

### `@PutMapping` — Update (Full)

Used to **fully update** an existing resource. Replaces the entire resource with the data provided in the request body.

```java
@PutMapping("/students/{id}")
public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
    return studentService.update(id, student);
}
```

| Aspect | Detail |
|---|---|
| HTTP Method | PUT |
| Purpose | Fully replace an existing resource |
| Request Body | Required (full updated data) |
| Safe? | No |
| Idempotent? | Yes (same request = same result) |

---

### `@PatchMapping` — Update (Partial)

Used to **partially update** a resource. Only the fields provided are updated; others remain unchanged.

```java
@PatchMapping("/students/{id}")
public Student partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
    return studentService.partialUpdate(id, updates);
}
```

| Aspect | Detail |
|---|---|
| HTTP Method | PATCH |
| Purpose | Partially update an existing resource |
| Request Body | Required (only fields to update) |
| Safe? | No |
| Idempotent? | Generally yes |

> **PUT vs PATCH:** Use PUT when replacing the whole object. Use PATCH when updating only a few fields (e.g., just the email or just the phone number).

---

### `@DeleteMapping` — Delete

Used to **delete** a resource. Maps HTTP DELETE requests.

```java
@DeleteMapping("/students/{id}")
public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
    studentService.delete(id);
    return ResponseEntity.noContent().build();
}
```

| Aspect | Detail |
|---|---|
| HTTP Method | DELETE |
| Purpose | Delete a resource |
| Request Body | Not used |
| Safe? | No |
| Idempotent? | Yes (deleting twice = resource still gone) |

---

## Request Handling Annotations

### `@PathVariable`

Extracts a value from the **URL path**. Used when the variable is part of the URL itself.

```java
// URL: GET /students/42
@GetMapping("/students/{id}")
public Student getStudent(@PathVariable Long id) {}

// You can also rename if the variable name differs
@GetMapping("/students/{studentId}")
public Student getStudent(@PathVariable("studentId") Long id) {}
```

---

### `@RequestParam`

Extracts a value from the **query string** (the `?key=value` part of a URL).

```java
// URL: GET /students?name=Alice&page=1
@GetMapping("/students")
public List<Student> search(
    @RequestParam String name,
    @RequestParam(defaultValue = "0") int page
) {}
```

> **PathVariable vs RequestParam:**
> - `@PathVariable` → part of the URL path: `/students/42`
> - `@RequestParam` → query parameter: `/students?id=42`

---

### `@RequestBody`

Binds the **HTTP request body** (JSON) to a Java object. Spring uses Jackson to deserialize JSON → Java object automatically.

```java
// Client sends: POST /students
// Body: { "name": "Alice", "rollNo": "21BT001" }

@PostMapping("/students")
public Student create(@RequestBody Student student) {
    // student.getName() == "Alice"
    return studentService.save(student);
}
```

---

### `@RequestHeader`

Extracts a value from an **HTTP request header**.

```java
@GetMapping("/profile")
public String getProfile(@RequestHeader("Authorization") String token) {
    // use the token for auth validation
    return "profile data";
}
```

---

## Response Handling

### `ResponseEntity<T>`

Gives full control over the HTTP response — status code, headers, and body. Preferred over returning plain objects when you need to customize the response.

```java
// Return 201 Created with body
return ResponseEntity.status(HttpStatus.CREATED).body(savedStudent);

// Return 200 OK with body
return ResponseEntity.ok(student);

// Return 204 No Content (for deletes)
return ResponseEntity.noContent().build();

// Return 404 Not Found
return ResponseEntity.notFound().build();
```

### Common HTTP Status Codes

| Code | Constant | Meaning |
|---|---|---|
| 200 | `HttpStatus.OK` | Request succeeded |
| 201 | `HttpStatus.CREATED` | Resource successfully created |
| 204 | `HttpStatus.NO_CONTENT` | Success, no body to return |
| 400 | `HttpStatus.BAD_REQUEST` | Invalid request from client |
| 404 | `HttpStatus.NOT_FOUND` | Resource not found |
| 500 | `HttpStatus.INTERNAL_SERVER_ERROR` | Server-side error |

---

## Exception Handling

### `@ExceptionHandler`

Handles exceptions within a single controller.

```java
@ExceptionHandler(StudentNotFoundException.class)
public ResponseEntity<String> handleNotFound(StudentNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
}
```

### `@ControllerAdvice` / `@RestControllerAdvice`

Handles exceptions **globally** across all controllers. Best practice for production apps.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<String> handleNotFound(StudentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Something went wrong: " + ex.getMessage());
    }
}
```