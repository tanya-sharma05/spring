# Spring Framework Without Spring Boot

## 1. Setup — Maven Dependency

Without Spring Boot, you add only the Spring context dependency to `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>6.1.6</version>
    </dependency>

    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>3.8.1</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

This pulls in: `spring-core`, `spring-beans`, `spring-context`, `spring-expression`, `spring-aop`, and `spring-jcl`.

---

## 2. Creating Beans — XML Configuration

In plain Spring, you define your beans in an XML file (traditionally named `spring.xml`) placed under `src/main/resources/`.

### `spring.xml`
```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="dev" class="com.telusko.Dev" />

</beans>
```

- `id` — the name used to look up this bean from the container
- `class` — the fully-qualified class name

### `Dev.java`
```java
package com.telusko;

public class Dev {

    public Dev() {
        System.out.println("Dev Constructor");
    }

    public void build() {
        System.out.println("working on Awesome Project");
    }
}
```

### `App.java` — Starting the Container
```java
package com.telusko;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {
    public static void main(String[] args) {
        // Load the XML config — this boots the IoC container
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");

        // Retrieve the bean by type
        Dev obj = context.getBean(Dev.class);
        obj.build();
    }
}
```

**Output:**
```
Dev Constructor
working on Awesome Project
```

> **Note:** Unlike Spring Boot which uses `SpringApplication.run(...)`, plain Spring uses `ClassPathXmlApplicationContext("spring.xml")` to boot the container. If you pass a wrong/missing filename, you get `BeanFactory not initialized` or `IllegalStateException`.

---

## 3. Bean Scope — Spring Creates Beans Eagerly

By default, Spring creates **all beans defined in XML when the container starts** — not when you call `getBean()`. This is called *eager initialization* (singleton scope).

You can verify this: if you define two beans of the same class, both constructors fire at startup:

```xml
<bean id="dev"  class="com.telusko.Dev" />
<bean id="dev1" class="com.telusko.Dev" />
```

Output at startup:
```
Dev Constructor
Dev Constructor
```

Both objects are created immediately, even before `getBean()` is called.

---

## 4. Injecting Primitive Values — Setter Injection via XML

To inject a value into a field, the class needs a **setter method**, and you use the `<property>` tag in XML.

### `Dev.java`
```java
package com.telusko;

public class Dev {
    private int age;

    public Dev() {
        System.out.println("Dev Constructor");
    }

    public int getAge() { 
        return age; 
    }
    public void setAge(int age) { 
        this.age = age; 
    }

    public void build() {
        System.out.println("working on Awesome Project");
    }
}
```

### `spring.xml`
```xml
<bean id="dev" class="com.telusko.Dev">
    <property name="age" value="12" />
</bean>
```

### `App.java`
```java
ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
Dev obj = (Dev) context.getBean("dev");
System.out.println(obj.getAge());  // prints: 12
```

`<property name="age" value="12" />` calls `setAge(12)` on the bean automatically.

---

## 5. Injecting Primitive Values — Constructor Injection via XML

If you want Spring to call a **parameterized constructor**, use `<constructor-arg>`:

### `Dev.java`
```java
public class Dev {
    private int age;

    public Dev() {
        System.out.println("Dev Constructor");
    }

    public Dev(int age) {
        this.age = age;
        System.out.println("Dev 1 constructor");
    }

    public int getAge() { return age; }
}
```

### `spring.xml`
```xml
<bean id="dev" class="com.telusko.Dev">
    <constructor-arg value="14" />
</bean>
```

Spring calls `new Dev(14)` — the parameterized constructor.

---

## 6. Injecting Object Dependencies via XML

To inject one bean into another (i.e., Dependency Injection of object type), use the `ref` attribute.

### `Laptop.java`
```java
package com.telusko;

public class Laptop {

    public Laptop() {
        System.out.println("Laptop Constructor");
    }

    public void compile() {
        System.out.println("Compiling");
    }
}
```

### `Dev.java` — with a `Laptop` dependency
```java
package com.telusko;

public class Dev {
    private Laptop laptop;
    private int age;

    public Dev() { 
        System.out.println("Dev Constructor"); 
    }

    public Dev(Laptop laptop) {
        this.laptop = laptop;
        System.out.println("Dev 1 constructor");
    }

    public void setLaptop(Laptop laptop) { 
        this.laptop = laptop; 
    }
    public Laptop getLaptop() { 
        return laptop; 
    }

    public void build() {
        System.out.println("working on Awesome Project");
        laptop.compile();
    }
}
```

### Setter Injection of an Object — `spring.xml`
```xml
<bean id="dev"  class="com.telusko.Dev">
    <property name="laptop" ref="lap1" /> 
</bean>

<bean id="lap1" class="com.telusko.Laptop" />
```

### Constructor Injection of an Object — `spring.xml`
```xml
<bean id="dev" class="com.telusko.Dev">
    <constructor-arg ref="lap1" />   
</bean>

<bean id="lap1" class="com.telusko.Laptop" />
```

**Output:**
```
Laptop Constructor
Dev 1 constructor
working on Awesome Project
Compiling
```

---

## 7. Retrieving Beans — By Type vs By Name

```java
// By type (Spring figures out which bean matches Dev.class)
Dev obj = context.getBean(Dev.class);

// By name/id (string must match the 'id' in XML)
Dev obj = (Dev) context.getBean("dev");
```

If multiple beans of the same type exist and you try to get by type, Spring throws an error. In that case, get by name instead.
