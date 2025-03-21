package com.java.oops.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Functional interface for extracting data from a ResultSet.
 * @param <T> Type of the extracted data.
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {
    /**
     * Extracts data from the provided ResultSet.
     * @param rs ResultSet to extract data from.
     * @return Extracted data of type T.
     * @throws SQLException if an error occurs while extracting data.
     */
    T extract(ResultSet rs) throws SQLException;
}
