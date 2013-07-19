package hello;

import javax.sql.DataSource;

import org.junit.Assert;
import org.springframework.autoconfigure.EnableAutoConfiguration;
import org.springframework.bootstrap.SpringApplication;
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
