# ☕ Java JDBC Complete Guide

*A single-file handbook that **teaches every core and advanced topic of Java JDBC**—from loading the driver to production-grade performance tuning.*

---

## 📚 Table of Contents

1. [Introduction](#1-introduction)
2. [JDBC Architecture & Driver Types](#2-jdbc-architecture--driver-types)
3. [Environment Setup](#3-environment-setup)
4. [Connecting to a Database](#4-connecting-to-a-database)
5. [CRUD with `Statement`](#5-crud-with-statement)
6. [Parameterized Queries – `PreparedStatement`](#6-parameterized-queries--preparedstatement)
7. [Calling Stored Procedures – `CallableStatement`](#7-calling-stored-procedures--callablestatement)
8. [Working with `ResultSet`](#8-working-with-resultset)
9. [Transactions & Savepoints](#9-transactions--savepoints)
10. [Batch Processing](#10-batch-processing)
11. [BLOB / CLOB Handling](#11-blob--clob-handling)
12. [Database & ResultSet MetaData](#12-database--resultset-metadata)
13. [Connection Pooling](#13-connection-pooling)
14. [Exception Handling & Logging](#14-exception-handling--logging)
15. [Performance Tuning Checklist](#15-performance-tuning-checklist)
16. [Security Best Practices](#16-security-best-practices)
17. [Common Errors & Fixes](#17-common-errors--fixes)
18. [Cheat Sheet](#18-cheat-sheet)
19. [Further Reading](#19-further-reading)
20. [GitHub Sample Project](#20-github-sample-project)
21. [License](#21-license)

---

## 1. Introduction

**JDBC (Java Database Connectivity)** is the standard Java API for executing SQL statements, allowing Java apps to communicate with relational databases (MySQL, PostgreSQL, Oracle, SQL Server, etc.). It is low-level, but mastering it teaches you the foundations that higher-level frameworks (Spring JDBC, JPA/Hibernate, MyBatis) build upon.

---

## 2. JDBC Architecture & Driver Types

```
+--------------+           +-----------------+
|   Java App   |  JDBC API | JDBC Driver     |  DB Protocol
+--------------+ ------->  +-----------------+ ---------->
```

| Driver Type | Description                      | Pros                        | Cons                     |
| ----------- | -------------------------------- | --------------------------- | ------------------------ |
| **Type 1**  | JDBC-ODBC bridge                 | Quick demos                 | Deprecated, Windows-only |
| **Type 2**  | Native API (C/C++ libs)          | Faster than Type 1          | Requires native libs     |
| **Type 3**  | Network middleware               | Thin client, load-balancing | Extra network hop        |
| **Type 4**  | Pure Java (direct wire protocol) | Portable, best performance  | One driver per DB        |

> 99% of modern projects use **Type 4** (e.g., `mysql-connector-j`, `postgresql`, `ojdbc`).

---

## 3. Environment Setup

1. **Install JDK 11 or newer**.
2. **Install a database** (example: MySQL 8.4).
3. **Add driver JAR** to project:

**Gradle:**

```kotlin
dependencies {
    implementation("mysql:mysql-connector-j:8.4.0")
}
```

**Maven:**

```xml
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <version>8.4.0</version>
</dependency>
```

Create a sample schema:

```sql
CREATE DATABASE jdbc_demo;
USE jdbc_demo;

CREATE TABLE users (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE files (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    data LONGBLOB
);
```

---

## 4. Connecting to a Database

```java
String url  = "jdbc:mysql://localhost:3306/jdbc_demo?useSSL=false&serverTimezone=UTC";
String user = "root";
String pass = "secret";

try (Connection conn = DriverManager.getConnection(url, user, pass)) {
    System.out.println("✅ Connected to " + conn.getCatalog());
} catch (SQLException e) {
    e.printStackTrace();
}
```

> From JDBC 4+, the driver auto-registers; no need for `Class.forName()`.

---

## 5. CRUD with `Statement`

### Insert

```java
String sql = "INSERT INTO users (name, email) VALUES ('Alice','alice@mail.com')";
try (Statement st = conn.createStatement()) {
    System.out.println(st.executeUpdate(sql) + " row(s) inserted");
}
```

### Select

```java
try (Statement st = conn.createStatement()) {
    try (ResultSet rs = st.executeQuery("SELECT * FROM users")) {
        while (rs.next()) {
            System.out.printf("%d %s %s%n",
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"));
        }
    }
}
```

> **⚠ Risk:** concatenating SQL strings opens you to **SQL Injection**. Use `PreparedStatement` instead.

---

## 6. Parameterized Queries – `PreparedStatement`

```java
String q = "SELECT * FROM users WHERE email = ?";
try (PreparedStatement ps = conn.prepareStatement(q)) {
    ps.setString(1, "alice@mail.com");
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            System.out.printf("%d %s %s%n",
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"));
        }
    }
}
```

**Benefits**

* Prevents SQL Injection
* Statement plan caching = faster
* Supports **batch processing** (see § 10)

---

## 7. Calling Stored Procedures – `CallableStatement`

```sql
DELIMITER $$
CREATE PROCEDURE add_user(IN uname VARCHAR(100), IN uemail VARCHAR(100))
BEGIN
  INSERT INTO users(name,email) VALUES(uname,uemail);
END $$
DELIMITER ;
```

Java:

```java
try (CallableStatement cs = conn.prepareCall("{call add_user(?, ?)}")) {
    cs.setString(1, "Bob");
    cs.setString(2, "bob@mail.com");
    cs.execute();
}
```

---

## 8. Working with `ResultSet`

| Method                          | Purpose               |
| ------------------------------- | --------------------- |
| `next()` / `previous()`         | Move cursor           |
| `absolute(int)`                 | Jump to row           |
| `beforeFirst()` / `afterLast()` | Reset cursor          |
| `getXxx()`                      | Retrieve typed column |

Enable scrolling:

```java
Statement st = conn.createStatement(
    ResultSet.TYPE_SCROLL_INSENSITIVE,
    ResultSet.CONCUR_READ_ONLY);
```

Meta info:

```java
ResultSetMetaData rsm = rs.getMetaData();
System.out.println("Columns: " + rsm.getColumnCount());
```

---

## 9. Transactions & Savepoints

```java
conn.setAutoCommit(false);
try {
    // step 1
    // step 2
    Savepoint sp = conn.setSavepoint();
    // step 3 (may fail)
    conn.commit();
} catch (SQLException ex) {
    conn.rollback();            // or conn.rollback(sp);
} finally {
    conn.setAutoCommit(true);
}
```

---

## 10. Batch Processing

```java
String ins = "INSERT INTO users(name,email) VALUES(?,?)";
try (PreparedStatement ps = conn.prepareStatement(ins)) {
    for (User u : users) {
        ps.setString(1, u.getName());
        ps.setString(2, u.getEmail());
        ps.addBatch();
    }
    int[] counts = ps.executeBatch();
    System.out.println(Arrays.stream(counts).sum() + " total rows inserted");
}
```

> **Gain:** 5–20× faster bulk inserts.

---

## 11. BLOB / CLOB Handling

**Binary Large Objects (BLOB)** store files like images, PDFs. **Character Large Objects (CLOB)** store large text.

### Insert BLOB

```java
String sql = "INSERT INTO files(name, data) VALUES(?, ?)";
try (PreparedStatement ps = conn.prepareStatement(sql);
     FileInputStream fis = new FileInputStream("image.jpg")) {
    ps.setString(1, "Image");
    ps.setBinaryStream(2, fis);
    ps.executeUpdate();
}
```

### Read BLOB

```java
String sql = "SELECT data FROM files WHERE name=?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, "Image");
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
        try (InputStream in = rs.getBinaryStream("data")) {
            // Save or process stream
        }
    }
}
```

> CLOB is handled similarly using `getCharacterStream()`.

---

## 12. Database & ResultSet MetaData

```java
DatabaseMetaData meta = conn.getMetaData();
System.out.println(meta.getDatabaseProductName());
System.out.println(meta.getDriverVersion());

ResultSetMetaData rsmd = rs.getMetaData();
int cols = rsmd.getColumnCount();
for (int i = 1; i <= cols; i++) {
    System.out.printf("%s (%s)%n", rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
}
```

---

## 13. Connection Pooling

Use a library like **HikariCP** for production-ready connection pooling:

```kotlin
dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
}
```

Example setup:

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/jdbc_demo");
config.setUsername("root");
config.setPassword("secret");
DataSource ds = new HikariDataSource(config);
```

> Avoid `DriverManager` in high-load apps. Use `DataSource` with pooling.

---

## 14. Exception Handling & Logging

```java
try (Connection conn = ds.getConnection()) {
    // Use conn
} catch (SQLException e) {
    System.err.println("Database error: " + e.getMessage());
    e.printStackTrace();
}
```

Best practices:

* Log `SQLState`, `ErrorCode` for debugging
* Avoid printing stack traces to stdout in production
* Use logging frameworks: **SLF4J**, **Logback**, **Log4j2**

---

## 15. Performance Tuning Checklist

* Use `PreparedStatement` over `Statement`
* Batch inserts for large data
* Use connection pools (HikariCP)
* Index your queries smartly
* Use `EXPLAIN` to profile queries

---

## 16. Security Best Practices

* ❌ Never concatenate user input into SQL (SQL Injection)
* ✔ Always use `PreparedStatement`
* Limit DB user privileges
* Store credentials securely (e.g., environment variables, vaults)
* Sanitize large input (e.g., BLOB/CLOB uploads)

---

## 17. Common Errors & Fixes

| Error                         | Cause                   | Fix                 |
| ----------------------------- | ----------------------- | ------------------- |
| `No suitable driver`          | Driver not on classpath | Add JDBC JAR        |
| `Communications link failure` | Wrong host/port         | Check DB URL        |
| `Access denied`               | Bad credentials         | Verify user/pass    |
| `Table doesn't exist`         | Typo or wrong DB        | Recheck schema      |
| `Too many connections`        | No pooling              | Use connection pool |

---

## 18. Cheat Sheet

```java
DriverManager.getConnection(...);     // Get connection
conn.createStatement();               // Execute SQL
conn.prepareStatement(...);           // Parameterized query
conn.setAutoCommit(false);            // Begin transaction
conn.commit();                        // Commit
conn.rollback();                      // Rollback
ResultSet rs = stmt.executeQuery();   // Query result
rs.next();                            // Next row
rs.getString("column");              // Get value
```

---

## 19. Further Reading

* [JDBC Java Docs](https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html)
* [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
* [Spring JDBC Docs](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#jdbc)
* [SQL Injection Prevention (OWASP)](https://owasp.org/www-community/attacks/SQL_Injection)

---

## 20. GitHub Sample Project

You can find a ready-to-use JDBC sample project on GitHub:

🔗 [**Java JDBC Example Repository**](https://github.com/example/java-jdbc-guide)

It includes:

* Maven setup
* CRUD operations
* Prepared statements
* Connection pooling
* Logging setup

---

## 21. License

This guide is released under the **MIT License**. Use freely with attribution.

---

> ✨ JDBC mastery is foundational. With this knowledge, you're ready to learn Spring JDBC, Hibernate, JPA, and more.
