# Simple Railway Reservation System

## Requirements

- Java 17 or later
- MySQL Server
- MySQL Connector/J JAR file

## 1. Create the database

Open MySQL Command Line Client and run:

```sql
source C:/path/to/simple-railway-reservation/schema.sql
```

## 2. Configure the Java file

Open `RailwayReservationSystem.java` and replace:

```java
private static final String DB_PASSWORD = "YOUR_MYSQL_PASSWORD";
```

with your real MySQL password.

## 3. Download MySQL Connector/J

Download the Connector/J ZIP from the official MySQL website and copy the JAR file into this project folder. Rename it to:

```text
mysql-connector-j.jar
```

## 4. Compile on Windows Command Prompt

Open Command Prompt in this folder:

```cmd
cd C:\path\to\simple-railway-reservation
```

Compile:

```cmd
javac -cp "mysql-connector-j.jar" RailwayReservationSystem.java
```

Run:

```cmd
java -cp ".;mysql-connector-j.jar" RailwayReservationSystem
```

## Features

1. View trains
2. Book one passenger ticket
3. Search ticket by PNR
4. Cancel a ticket
5. Transaction-based seat update
