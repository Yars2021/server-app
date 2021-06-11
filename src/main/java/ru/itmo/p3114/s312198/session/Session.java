package ru.itmo.p3114.s312198.session;

import ru.itmo.p3114.s312198.transmission.User;

public class Session {
    private final Long sessionUID;
    private final User account;

    public Session(Long sessionUID, User account) {
        this.sessionUID = sessionUID;
        this.account = account;
    }

    public Long getSessionUID() {
        return sessionUID;
    }

    public User getAccount() {
        return account;
    }
}
