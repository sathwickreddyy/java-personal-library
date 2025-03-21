package com.java.oops.database;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;


/**
 * AbstractDatabaseTemplate provides a generic JDBC template for common database operations.
 * This class uses the Template Method Pattern to allow database-specific implementations
 * to provide their own connection details while reusing common logic for query execution.
 */
@Slf4j
public abstract class AbstractDatabaseTemplate {

    /**
     * Abstract method to be implemented by subclasses to provide a DataSource.
     * @return DataSource for database connections.
     */
    protected abstract DataSource getDataSource();

    /**
     * Abstract method to be implemented by subclasses to provide a database connection.
     * @return Connection to the database.
     * @throws SQLException if an error occurs while obtaining the connection.
     */
    protected abstract Connection getConnection() throws SQLException;


    /**
     * Executes a query and processes the ResultSet using the provided ResultSetExtractor.
     * @param sql SQL query to execute.
     * @param params Parameters for the query.
     * @param extractor ResultSetExtractor to process the ResultSet.
     * @return Optional containing the extracted result or empty if an error occurs.
     */
    protected <T> Optional<T> executeQuery(String sql, Object[] params, ResultSetExtractor<T> extractor)
    {
        try (Connection connection = getConnection()){
            PreparedStatement statement = connection.prepareStatement(sql);
            setParameters(statement, params);
            try (ResultSet rs = statement.executeQuery()){
                return Optional.ofNullable(extractor.extract(rs));
            }
        }
        catch (SQLException e) {
            log.error("Error executing query: {}", sql, e);
            return Optional.empty();
        }
    }

    /**
     * Executes an update query (INSERT, UPDATE, DELETE).
     * @param sql SQL query to execute.
     * @param params Parameters for the query.
     */
    protected void executeUpdate(String sql, Object[] params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);
            stmt.executeUpdate();
            log.debug("Executed update successfully: {}", sql);
        } catch (SQLException e) {
            log.error("Failed executing update: {}", sql, e);
        }
    }

    /**
     * Sets parameters for a PreparedStatement.
     * @param stmt PreparedStatement to set parameters for.
     * @param params Parameters to set.
     * @throws SQLException if an error occurs while setting parameters.
     */
    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }
}
