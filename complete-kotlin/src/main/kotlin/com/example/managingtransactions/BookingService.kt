package com.example.managingtransactions

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BookingService(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun book(vararg persons: String?) {
        persons.forEach { person ->
            logger.info("Booking {} in a seat...", person)
            jdbcTemplate.update("insert into BOOKINGS(FIRST_NAME) values (?)", person)
        }
    }

    fun findAllBookings(): List<String> =
        jdbcTemplate.query("select FIRST_NAME from BOOKINGS") { rs, _ ->
            rs.getString("FIRST_NAME")
        }
}