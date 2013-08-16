This guide walks you through the process of wrapping database operations with non-intrusive transactions.

What you'll build
-----------------

You will see how you can easily make a database operation transactional without having to write [specialized JDBC code](http://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html#commit_transactions).


What you'll need
----------------

 - About 15 minutes
 - A favorite text editor or IDE
 - [JDK 6][jdk] or later
 - [Gradle 1.7+][gradle] or [Maven 3.0+][mvn]

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[gradle]: http://www.gradle.org/
[mvn]: http://maven.apache.org/download.cgi


How to complete this guide
--------------------------

Like all Spring's [Getting Started guides](/guides/gs), you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To **start from scratch**, move on to [Set up the project](#scratch).

To **skip the basics**, do the following:

 - [Download][zip] and unzip the source repository for this guide, or clone it using [Git][u-git]:
`git clone https://github.com/springframework-meta/gs-managing-transactions.git`
 - cd into `gs-managing-transactions/initial`.
 - Jump ahead to [Create a booking service](#initial).

**When you're finished**, you can check your results against the code in `gs-managing-transactions/complete`.
[zip]: https://github.com/springframework-meta/gs-managing-transactions/archive/master.zip
[u-git]: /understanding/Git


<a name="scratch"></a>
Set up the project
------------------

First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Gradle](http://gradle.org) and [Maven](https://maven.apache.org) is included here. If you're not familiar with either, refer to [Building Java Projects with Gradle](/guides/gs/gradle/) or [Building Java Projects with Maven](/guides/gs/maven).

### Create the directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

    └── src
        └── main
            └── java
                └── hello

### Create a Gradle build file

`build.gradle`
```gradle
buildscript {
    repositories {
        maven { url "http://repo.springsource.org/libs-snapshot" }
        mavenLocal()
    }
}

apply plugin: 'java'
apply plugin: 'eclipse'
apply plugin: 'idea'

jar {
    baseName = 'gs-managing-transactions'
    version =  '0.1.0'
}

repositories {
    mavenCentral()
    maven { url "http://repo.springsource.org/libs-snapshot" }
}

dependencies {
    compile("org.springframework.boot:spring-boot-starter:0.5.0.BUILD-SNAPSHOT")
    compile("org.springframework:spring-tx:4.0.0.M2")
    compile("org.springframework:spring-jdbc:4.0.0.M2")
    compile("com.h2database:h2:1.3.172")
    compile("junit:junit")
    testCompile("junit:junit:4.11")
}

task wrapper(type: Wrapper) {
    gradleVersion = '1.7'
}
```

This guide is using [Spring Boot's starter POMs](/guides/gs/spring-boot/).


<a name="initial"></a>
Create a booking service
------------------------
First, use the `BookingService` class to create a JDBC-based service that books people into the system by name. 

`src/main/java/hello/BookingService.java`
```java
package hello;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

public class BookingService {
    
    @Autowired
    JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void book(String... persons) {
        for (String person : persons) {
            System.out.println("Booking " + person + " in a seat...");
            jdbcTemplate.update("insert into BOOKINGS(FIRST_NAME) values (?)", person);
        }
    };

    public List<String> findAllBookings() {
        return jdbcTemplate.query("select FIRST_NAME from BOOKINGS", new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("FIRST_NAME");
            }
        });
    }

}
```

The code has an autowired `JdbcTemplate`, a handy template class that does all the database interactions needed by the code below.

You also have a `book` method aimed at booking multiple people. It loops through the list of people, and for each person, inserts them into the `BOOKINGS` table using the `JdbcTemplate`. This method is tagged with `@Transactional`, meaning that any failure causes the entire operation to roll back to its previous state, and to re-throw the original exception. This means that none of the people will be added to `BOOKINGS` if one person fails to be added.

You also have a `findAllBookings` method to query the database. Each row fetched from the database is converted into a `String` and then assembled into a `List`.

Build an application
-----------------------
`src/main/java/hello/Application.java`
```java
package hello;

import javax.sql.DataSource;

import org.junit.Assert;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration
public class Application {
    
    @Bean
    BookingService bookingService() {
        return new BookingService();
    }

    @Bean
    DataSource dataSource() {
        return new SimpleDriverDataSource() {{
            setDriverClass(org.h2.Driver.class);
            setUsername("sa");
            setUrl("jdbc:h2:mem");
            setPassword("");
        }};
    }
    
    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        System.out.println("Creating tables");
        jdbcTemplate.execute("drop table BOOKINGS if exists");
        jdbcTemplate.execute("create table BOOKINGS(" +
                "ID serial, FIRST_NAME varchar(5) NOT NULL)");
        return jdbcTemplate;
    }
    
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        
        BookingService bookingService = ctx.getBean(BookingService.class);
        bookingService.book("Alice", "Bob", "Carol");
        Assert.assertEquals("First booking should work with no problem",
                3, bookingService.findAllBookings().size());
        
        try {
            bookingService.book("Chris", "Samuel");
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
        
        Assert.assertEquals("'Samuel' should have triggered a rollback",
                3, bookingService.findAllBookings().size());

        try {
            bookingService.book("Buddy", null);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
        
        Assert.assertEquals("'null' should have triggered a rollback",
                3, bookingService.findAllBookings().size());

    }

}
```

You configure your beans in the `Application` configuration class. The `bookingService` method wires in an instance of `BookingService`.

As shown earlier in this guide, `JdbcTemplate` is autowired into `BookingService`, meaning you now need to define it in the `Application` code:
    
> **Note:** `SimpleDriverDataSource` is a convenience class and is _not_ intended for production. For production, you usually want some sort of JDBC connection pool to handle multiple requests coming in simultaneously.

The `jdbcTemplate` method where you create an instance of `JdbcTemplate` also contains some DDL to declare the `BOOKINGS` table.

> **Note:** In production systems, database tables are usually declared outside the application.

The `main()` method defers to the [`SpringApplication`][] helper class, providing `Application.class` as an argument to its `run()` method. This tells Spring to read the annotation metadata from `Application` and to manage it as a component in the _[Spring application context][u-application-context]_.

Note two especially valuable parts of this application configuration:
- `@EnableTransactionManagement` activates Spring's seamless transaction features, which makes `@Transactional` function.
- [`@EnableAutoConfiguration`][] switches on reasonable default behaviors based on the content of your classpath. For example, it detects that you have **spring-tx** on the path as well as a `DataSource`, and automatically creates the other beans needed for transactions. Auto-configuration is a powerful, flexible mechanism. See the [API documentation][`@EnableAutoConfiguration`] for further details.


Now that your `Application` class is ready, you simply instruct the build system to create a single, executable jar containing everything. This makes it easy to ship, version, and deploy the service as an application throughout the development lifecycle, across different environments, and so forth.

Add the following dependency to your Gradle **build.gradle** file's `buildscript` section:

```groovy
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:0.5.0.BUILD-SNAPSHOT")
    }
```

Further down inside `build.gradle`, add the following to the list of plugins:

```groovy
apply plugin: 'spring-boot'
```

The [Spring Boot gradle plugin][spring-boot-gradle-plugin] collects all the jars on the classpath and builds a single "über-jar", which makes it more convenient to execute and transport your service.
It also searches for the `public static void main()` method to flag as a runnable class.

Now run the following command to produce a single executable JAR file containing all necessary dependency classes and resources:

```sh
$ ./gradlew build
```

Now you can run the JAR by typing:

```sh
$ java -jar build/libs/gs-managing-transactions-0.1.0.jar
```

[spring-boot-gradle-plugin]: https://github.com/SpringSource/spring-boot/tree/master/spring-boot-tools/spring-boot-gradle-plugin

> **Note:** The procedure above will create a runnable JAR. You can also opt to [build a classic WAR file](/guides/gs/convert-jar-to-war/) instead.

Run the service
-------------------
Run your service at the command line:

```sh
$ ./gradlew clean build && java -jar build/libs/gs-managing-transactions-0.1.0.jar
```


You should see the following output:

```sh
Creating tables
Booking Alice in a seat...
Booking Bob in a seat...
Booking Carol in a seat...
Booking Chris in a seat...
Booking Samuel in a seat...
Jul 11, 2013 10:20:14 AM org.springframework.beans.factory.xml.XmlBeanDefinitionReader loadBeanDefinitions
INFO: Loading XML bean definitions from class path resource [org/springframework/jdbc/support/sql-error-codes.xml]
Jul 11, 2013 10:20:14 AM org.springframework.jdbc.support.SQLErrorCodesFactory <init>
INFO: SQLErrorCodes loaded: [DB2, Derby, H2, HSQL, Informix, MS-SQL, MySQL, Oracle, PostgreSQL, Sybase]
PreparedStatementCallback; SQL [insert into BOOKINGS(FIRST_NAME) values (?)]; Value too long for column "FIRST_NAME VARCHAR(5) NOT NULL": "'Samuel' (6)"; SQL statement:
insert into BOOKINGS(FIRST_NAME) values (?) [22001-171]; nested exception is org.h2.jdbc.JdbcSQLException: Value too long for column "FIRST_NAME VARCHAR(5) NOT NULL": "'Samuel' (6)"; SQL statement:
insert into BOOKINGS(FIRST_NAME) values (?) [22001-171]
Booking Buddy in a seat...
Booking null in a seat...
PreparedStatementCallback; SQL [insert into BOOKINGS(FIRST_NAME) values (?)]; NULL not allowed for column "FIRST_NAME"; SQL statement:
insert into BOOKINGS(FIRST_NAME) values (?) [23502-171]; nested exception is org.h2.jdbc.JdbcSQLException: NULL not allowed for column "FIRST_NAME"; SQL statement:
insert into BOOKINGS(FIRST_NAME) values (?) [23502-171]
```

The `BOOKINGS` table has two constraints on the **first_name** column:
- Names cannot be longer than five characters.
- Names cannot be null.

The first three names inserted are **Alice**, **Bob**, and **Carol**. The application asserts that three people were added to that table. If that had not worked, the application would have exited early.

Next, another booking is done for **Chris** and **Samuel**. Samuel's name is deliberately too long, forcing an insert error. Transactional behavior stipulates that both Chris and Samuel; that is, this transaction, should be rolled back. Thus there should still be only three people in that table, which the assertion demonstrates.

Finally, **Buddy** and **null** are booked. As the output shows, null causes a rollback as well, leaving the same three people booked.

Summary
-------
Congratulations! You've just used Spring to develop a simple JDBC application wrapped with non-intrusive transactions.

[u-application-context]: /understanding/application-context
[`SpringApplication`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.BUILD-SNAPSHOT/javadoc-api/org/springframework/bootstrap/SpringApplication.html
[`@EnableAutoConfiguration`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.BUILD-SNAPSHOT/javadoc-api/org/springframework/bootstrap/context/annotation/SpringApplication.html

