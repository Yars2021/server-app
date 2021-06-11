package ru.itmo.p3114.s312198.session;

import java.util.ArrayList;
import java.util.HashMap;

public class SessionHashMap {
    private final HashMap<String, ArrayList<Session>> sessions = new HashMap<>();

    public Boolean containsUsername(String username) {
        return sessions.containsKey(username);
    }

    public void addUsername(String username) {
        sessions.put(username, new ArrayList<>());
    }

    public void addSession(Session session) {
        sessions.get(session.getAccount().getUsername()).add(session);
    }

    public void removeSessionByID(String username, Long id) {
        sessions.get(username).removeIf(session -> session.getSessionUID().equals(id));
    }

    public ArrayList<Session> getSessionsByUsername(String username) {
        return sessions.get(username);
    }

    public void deleteByUsername(String username) {
        sessions.remove(username);
    }
}
