package com.nexlora.nexbusiness.bi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

/** Separate connection as the nex_readonly MySQL user (SELECT on 3 tables only). */
@Component
public class ReadOnlyDb {

    private final JdbcTemplate jdbc;

    public ReadOnlyDb(
            @Value("${readonly.db.url:${spring.datasource.url}}") String url,
            @Value("${readonly.db.username:nex_readonly}") String username,
            @Value("${readonly.db.password:}") String password) {
        DriverManagerDataSource ds = new DriverManagerDataSource(url, username, password);
        this.jdbc = new JdbcTemplate(ds);
        this.jdbc.setMaxRows(SqlValidator.MAX_ROWS + 1); // +1 lets us detect truncation
        this.jdbc.setQueryTimeout(5);                    // seconds
    }

    public JdbcTemplate jdbc() { return jdbc; }
}
