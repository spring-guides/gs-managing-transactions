
# Getting Started: Managing Transactions


What you'll build
-----------------

This guide walks you through the process of wrapping database operations with non-intrusive transactions. You will see how you can easily make a database operation transactional without having to write [specialized JDBC code](http://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html#commit_transactions).


What you'll need
----------------

 - About 15 minutes
 - A favorite text editor or IDE
 - [JDK 6][jdk] or later
 - [Maven 3.0][mvn] or later

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[mvn]: http://maven.apache.org/download.cgi


How to complete this guide
--------------------------

Like all Spring's [Getting Started guides](/getting-started), you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To **start from scratch**, move on to [Set up the project](#scratch).

To **skip the basics**, do the following:

 - [Download][zip] and unzip the source repository for this guide, or clone it using [git](/understanding/git):
`git clone https://github.com/springframework-meta/gs-managing-transactions.git`
 - cd into `gs-managing-transactions/initial`
 - Jump ahead to [Create a booking service](#initial).

**When you're finished**, you can check your results against the code in `gs-managing-transactions/complete`.
[zip]: https://github.com/springframework-meta/gs-managing-transactions/archive/master.zip


<a name="scratch"></a>
Set up the project
------------------

First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Maven](https://maven.apache.org) and [Gradle](http://gradle.org) is included here. If you're not familiar with either, refer to [Building Java Projects with Maven](../gs-maven/README.md) or [Building Java Projects with Gradle](../gs-gradle/README.md).

### Create the directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

    └── src
        └── main
            └── java
                └── hello

### Create a Maven POM

`pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.springframework</groupId>
    <artifactId>gs-managing-transactions-complete</artifactId>
    <version>0.1.0</version>

    <parent>
        <groupId>org.springframework.bootstrap</groupId>
        <artifactId>spring-bootstrap-starters</artifactId>
        <version>0.5.0.BUILD-SNAPSHOT</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.bootstrap</groupId>
            <artifactId>spring-bootstrap-starter</artifactId>
        </dependency>
        <dependency>
        	<groupId>org.springframework</groupId>
        	<artifactId>spring-tx</artifactId>
        </dependency>
        <dependency>
        	<groupId>org.springframework</groupId>
        	<artifactId>spring-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
        </dependency>
    </dependencies>
    
    <properties>
        <start-class>hello.Application</start-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>http://repo.springsource.org/snapshot</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>http://repo.springsource.org/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>http://repo.springsource.org/snapshot</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>
</project>
```

TODO: mention that we're using Spring Bootstrap's [_starter POMs_](../gs-bootstrap-starter) here.

Note to experienced Maven users who are unaccustomed to using an external parent project: you can take it out later, it's just there to reduce the amount of code you have to write to get started.


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
import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
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

Add the following configuration to your existing Maven POM:

`pom.xml`
```xml
    <properties>
        <start-class>hello.Application</start-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```

The `start-class` property tells Maven to create a `META-INF/MANIFEST.MF` file with a `Main-Class: hello.Application` entry. This entry enables you to run the jar with `java -jar`.

The [Maven Shade plugin][maven-shade-plugin] extracts classes from all jars on the classpath and builds a single "über-jar", which makes it more convenient to execute and transport your service.

Now run the following to produce a single executable JAR file containing all necessary dependency classes and resources:

    mvn package

[maven-shade-plugin]: https://maven.apache.org/plugins/maven-shade-plugin

> **Note:** The procedure above will create a runnable JAR. You can also opt to [build a classic WAR file](/guides/gs/convert-jar-to-war/content) instead.

Run the application
-------------------
Run your application with `java -jar` at the command line:

    java -jar target/gs-managing-transactions-0.1.0.jar



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

