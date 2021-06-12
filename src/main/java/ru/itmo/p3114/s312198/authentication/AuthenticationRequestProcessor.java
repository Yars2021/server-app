package ru.itmo.p3114.s312198.authentication;

import org.postgresql.util.PSQLException;
import ru.itmo.p3114.s312198.database.DBHelper;
import ru.itmo.p3114.s312198.exceptions.AuthenticationException;
import ru.itmo.p3114.s312198.transmission.AuthenticationRequest;
import ru.itmo.p3114.s312198.transmission.User;

import java.net.ConnectException;

public class AuthenticationRequestProcessor {
    public Long getUserID(AuthenticationRequest authenticationRequest) throws AuthenticationException {
        if (authenticationRequest == null) {
            return null;
        } else {
            try (DBHelper dbHelper = new DBHelper()) {
                switch (authenticationRequest.getType()) {
                    case "LOG":
                        return dbHelper.validateAccount(authenticationRequest.getUser());
                    case "REG":
                        return dbHelper.registerAccount(authenticationRequest.getUser());
                    default:
                        return null;
                }
            } catch (PSQLException psqlException) {
                throw new AuthenticationException("Database is not connected");
            }
        }
    }

    public User getUser(AuthenticationRequest authenticationRequest) throws AuthenticationException {
        if (authenticationRequest == null) {
            throw new AuthenticationException();
        } else {
            return new User(authenticationRequest.getUser().getUsername(),
                    authenticationRequest.getUser().getCredentials(), getUserID(authenticationRequest));
        }
    }
}
