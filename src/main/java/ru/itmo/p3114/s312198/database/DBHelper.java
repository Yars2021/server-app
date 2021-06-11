package ru.itmo.p3114.s312198.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.itmo.p3114.s312198.exceptions.InvalidCredentialsException;
import ru.itmo.p3114.s312198.exceptions.RegistrationException;
import ru.itmo.p3114.s312198.transmission.User;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DBHelper implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DBHelper.class);

    private final HikariDataSource dataSource;

    public DBHelper() {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader("D:\\Labs\\lab7\\server-app\\src\\main\\resources\\db.properties"))) {
            properties.load(reader);
        } catch (FileNotFoundException fileNotFoundException) {
            logger.error(fileNotFoundException.getMessage());
        } catch (IOException ioException) {
            logger.error("An unexpected IOException occurred");
        }
        HikariConfig config = new HikariConfig(properties);
        dataSource = new HikariDataSource(config);
    }

    public Long registerAccount(User user) throws RegistrationException {
        if (user != null) {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO accounts (id, login, credentials) VALUES(nextval('seq_accounts'), ?, ?) RETURNING id")) {
                    statement.setString(1, user.getUsername());
                    statement.setString(2, user.getCredentials());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getLong(1);
                        }
                    }
                }
            } catch (SQLException sqlException) {
                logger.error(sqlException.getMessage());
            }
        }
        throw new RegistrationException();
    }

    public Long validateAccount(User user) throws InvalidCredentialsException {
        if (user != null) {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM accounts WHERE login = ? AND credentials = ?")) {
                    statement.setString(1, user.getUsername());
                    statement.setString(2, user.getCredentials());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getLong(1);
                        }
                    }
                }
            } catch (SQLException sqlException) {
                logger.error(sqlException.getMessage());
            }
        }
        throw new InvalidCredentialsException();
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
