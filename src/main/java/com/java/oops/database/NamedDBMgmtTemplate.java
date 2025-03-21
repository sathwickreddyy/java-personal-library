package com.java.oops.database;

import lombok.extern.slf4j.Slf4j;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * NamedDBMgmtTemplate provides JDBC-specific database management functionality.
 * This class extends AbstractDatabaseTemplate to reuse common JDBC operations
 */
@Slf4j
public class NamedDBMgmtTemplate extends AbstractDatabaseTemplate {

    private final DataSource dataSource;

    /**
     * Constructor to initialize NamedDBMgmtTemplate with a DataSource.
     * @param dataSource DataSource for MySQL connections.
     */
    public NamedDBMgmtTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Provides the DataSource for Named DB connections.
     * @return DataSource for Named DB connections.
     */
    @Override
    protected DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Provides a connection to the Named database.
     * @return Connection to the Named database.
     * @throws SQLException if an error occurs while obtaining the connection.
     */
    @Override
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
