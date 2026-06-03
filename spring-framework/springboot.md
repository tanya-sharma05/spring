# Spring Framework & Spring Boot 

## 1. What is the Spring Framework?

Spring is an open-source, lightweight Java framework that makes enterprise application development simpler, more modular, and easier to test.

At its core, Spring provides an **IoC Container** — a runtime that manages the creation, configuration, and lifecycle of Java objects (called *beans*). Instead of your code creating its own dependencies, Spring creates and wires them for you.

### Benefits of Spring

| Benefit | Description |
|---|---|
| **Loose Coupling** | Objects don't create their own dependencies — they are injected, making code easier to swap and test |
| **Testability** | Dependencies can be mocked or replaced easily in unit tests |
| **Modularity** | You only pull in what you need (Spring MVC, Spring Data, Spring Security, etc.) |
| **Boilerplate Reduction** | Spring handles repetitive tasks like transaction management, DB connections, etc. |
| **Convention over Configuration** | Spring Boot (on top of Spring) makes setup near-zero |

---

## 2. Inversion of Control (IoC)

**IoC** is the principle where the *framework* controls the flow of the program and the creation of objects — instead of your own code doing it.

Without IoC, you write:
```java
// You are in control — you create the object
Dev dev = new Dev();
dev.build();
```

With IoC (Spring takes control):
```java
// Spring creates and manages the Dev object for you
ApplicationContext context = SpringApplication.run(MyApp.class, args);
Dev dev = context.getBean(Dev.class);
dev.build();
```

The **IoC Container** lives inside the JVM. When Spring starts, it scans your project, finds classes annotated with `@Component` (and similar), creates objects for them, and stores them in the container. This container is accessible via the `ApplicationContext`.

### Spring creates objects automatically — but only for annotated classes

If a class is **not** annotated (e.g., no `@Component`), Spring will not create a bean for it. 

---

## 3. Spring vs Spring Boot

### Spring Framework (plain)
- You manually configure the container (via XML or Java config)
- You add only the dependencies you need
- You set up everything yourself: `ApplicationContext`, XML beans, etc.

### Spring Boot
- Built **on top of** Spring Framework
- Uses **Convention over Configuration** — sensible defaults are pre-configured
- Auto-configures beans, embedded servers (Tomcat), data sources, etc.
- You just annotate and run — no XML needed

### Key Differences

| Feature | Spring (plain) | Spring Boot |
|---|---|---|
| Configuration | XML or Java-based, manual | Auto-configured |
| Server | External setup needed | Embedded Tomcat/Jetty built in |
| Startup | `ClassPathXmlApplicationContext` | `SpringApplication.run(...)` |
| Dependency setup | Manual | Starter POMs handle it |
| Boilerplate | More | Much less |

### When to use what?
- Use **Spring Boot** for modern apps — REST APIs, microservices, web apps.
- Use **plain Spring** when you need very fine-grained control, or are learning how things work under the hood.

---

## 4. Dependency Injection (DI) — With Spring Boot

**Dependency Injection** is the *design pattern* that implements IoC. Instead of a class creating its own dependencies, they are *injected* into it from outside.

### Without DI (tightly coupled)
```java
public class Controller {
    private Service service; // declared but never injected

    public void handleRequest() {
        service.doSomething(); // NullPointerException!
    }
}
```
The `Controller` never creates the `Service` — you'd have to manually do `service = new Service()` somewhere, which tightly couples them.

### Three Types of Injection

#### A) Constructor Injection
```java
public class Controller {
    private Service service;

    public Controller(Service service) { // injected via constructor
        this.service = service;
    }

    public void handleRequest() {
        service.doSomething();
    }
}
```

#### B) Setter Injection
```java
public class Controller {
    private Service service;

    public void setService(Service service) { // injected via setter
        this.service = service;
    }

    public void handleRequest() {
        service.doSomething();
    }
}
```

#### C) Field Injection (using `@Autowired`)
```java
public class Controller {
    @Autowired
    private Service service; // Spring injects this directly

    public void handleRequest() {
        service.doSomething();
    }
}
```

Field injection is the most concise style and very common in Spring Boot.

---

## 5. `SpringApplication.run()` — The Container Starts Here

```java
@SpringBootApplication
public class MyAppApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MyAppApplication.class, args);
        Dev dev = context.getBean(Dev.class);
        dev.build();
    }
}
```

When `SpringApplication.run(...)` is called:
1. Spring Boot starts up
2. It scans all classes in the package for `@Component`, `@Service`, `@Repository`, `@Controller`, etc.
3. It creates beans for each annotated class and stores them in the **IoC Container** (`ApplicationContext`)
4. You can then retrieve any bean using `context.getBean(Dev.class)`

`@SpringBootApplication` is a convenience annotation that combines:
- `@Configuration` — this class is a config source
- `@EnableAutoConfiguration` — enable Spring Boot's auto-configuration
- `@ComponentScan` — scan this package for annotated components

---

## 6. `@Autowired` — Automatic Dependency Wiring

`@Autowired` tells Spring: *"Please find the right bean from your container and inject it here."*

### Example — Full Spring Boot DI with `@Autowired`

**`Dev.java`** — depends on `Laptop`
```java
package com.telusko;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Dev {

    @Autowired
    private Laptop laptop; // Spring will inject a Laptop bean here

    public void build() {
        laptop.compile();
        System.out.println("working on Awesome Project");
    }
}
```

**`Laptop.java`** — the dependency
```java
package com.telusko;

import org.springframework.stereotype.Component;

@Component
public class Laptop {

    public void compile() {
        System.out.println("Compiling with 404 bugs");
    }
}
```

**`MyAppApplication.java`** — entry point
```java
package com.telusko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class MyAppApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MyAppApplication.class, args);
        Dev dev = context.getBean(Dev.class);
        dev.build();
    }
}
```

**Output:**
```
Compiling with 404 bugs
working on Awesome Project
```

### What happens step by step:
1. `SpringApplication.run(...)` boots Spring and creates the IoC container
2. Spring scans and finds `Dev` and `Laptop` (both `@Component`)
3. Spring creates a `Laptop` bean
4. Spring creates a `Dev` bean, sees `@Autowired` on `laptop`, and injects the `Laptop` bean
5. `context.getBean(Dev.class)` returns the fully wired `Dev` object
6. `dev.build()` runs — `laptop` is not null because Spring injected it

### What if two beans implement the same interface?

Use `@Qualifier` to specify which bean to inject:

```java
public interface Computer {
    void compile();
}

@Component
public class Laptop implements Computer {
    public void compile() { 
        System.out.println("Compiling with 404 bugs"); 
    }
}

@Component
public class Desktop implements Computer {
    public void compile() { 
        System.out.println("Compiling with 404 bugs but faster"); 
    }
}

@Component
public class Dev {
    @Autowired
    @Qualifier("laptop") // tells Spring: inject the Laptop bean specifically
    private Computer comp;

    public void build() {
        comp.compile();
    }
}
```

Or use `@Primary` on the preferred implementation so Spring picks it by default when there's ambiguity.

---

## 7. Typical Web App Architecture

In a real Spring Boot web app, the layers are:

- **Controller** — handles HTTP requests
- **Service** — contains business logic
- **Repository** — handles database access

Spring Boot annotations for these layers: `@Controller` / `@RestController`, `@Service`, `@Repository` — all of which are specializations of `@Component`.

---

## Maven Dependency (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

Spring Boot's starter POMs pull in everything you need automatically.