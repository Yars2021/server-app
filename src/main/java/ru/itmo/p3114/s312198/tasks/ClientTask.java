package ru.itmo.p3114.s312198.tasks;

import ru.itmo.p3114.s312198.authentication.AuthenticationRequestProcessor;
import ru.itmo.p3114.s312198.exceptions.AuthenticationException;
import ru.itmo.p3114.s312198.exceptions.TransmissionException;
import ru.itmo.p3114.s312198.session.Session;
import ru.itmo.p3114.s312198.session.SessionHashMap;
import ru.itmo.p3114.s312198.transmission.AuthenticationRequest;
import ru.itmo.p3114.s312198.transmission.AuthenticationResponse;
import ru.itmo.p3114.s312198.transmission.CSChannel;
import ru.itmo.p3114.s312198.transmission.PrimaryPack;
import ru.itmo.p3114.s312198.transmission.ResponsePack;
import ru.itmo.p3114.s312198.transmission.User;

import java.net.Socket;

public class ClientTask implements Runnable {
    private final Socket socket;
    private final SessionHashMap sessionHashMap;
    private final Long currentSessionID;
    private Boolean running = Boolean.TRUE;
    private User actor;

    public ClientTask(SessionHashMap sessionHashMap, Long sessionID, Socket socket) {
        this.sessionHashMap = sessionHashMap;
        this.socket = socket;
        this.currentSessionID = sessionID;
    }

    public void shutdown() {
        running = Boolean.FALSE;
    }

    @Override
    public void run() {
        Session currentSession = null;
        try (CSChannel channel = new CSChannel(socket)) {
            AuthenticationRequestProcessor authenticationRequestProcessor = new AuthenticationRequestProcessor();
            AuthenticationRequest authenticationRequest = (AuthenticationRequest) channel.readObject();
            try {
                actor = authenticationRequestProcessor.getUser(authenticationRequest);
            } catch (AuthenticationException authenticationException) {
                channel.writeObject(new AuthenticationResponse(Boolean.FALSE,
                        authenticationException.getMessage(), authenticationRequest == null ? null : authenticationRequest.getUser()));
                shutdown();
            }
            if (actor != null) {
                channel.writeObject(new AuthenticationResponse(Boolean.TRUE,
                        "Welcome, " + actor.getUsername(), actor));
                if (!sessionHashMap.containsUsername(actor.getUsername())) {
                    sessionHashMap.addUsername(actor.getUsername());
                }
                currentSession = new Session(currentSessionID, actor);
                sessionHashMap.addSession(currentSession);
                System.out.println(currentSessionID + " " + actor.getId() + " " + actor.getUsername());
                while (running) {
                    PrimaryPack primaryPack = (PrimaryPack) channel.readObject();
                    System.out.println(primaryPack.size());
                }
            }
        } catch (TransmissionException transmissionException) {
            System.out.println(transmissionException.getMessage());
        }
        if (currentSession != null) {
            sessionHashMap.removeSessionByID(actor.getUsername(), currentSessionID);
        }
    }
}
