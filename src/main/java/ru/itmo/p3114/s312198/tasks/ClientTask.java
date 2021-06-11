package ru.itmo.p3114.s312198.tasks;

import ru.itmo.p3114.s312198.authentication.AuthenticationRequestProcessor;
import ru.itmo.p3114.s312198.commands.CommandRecord;
import ru.itmo.p3114.s312198.commands.actions.complex.AbstractComplexCommand;
import ru.itmo.p3114.s312198.commands.results.CommandResult;
import ru.itmo.p3114.s312198.commands.types.CommandTypes;
import ru.itmo.p3114.s312198.exceptions.AuthenticationException;
import ru.itmo.p3114.s312198.exceptions.TransmissionException;
import ru.itmo.p3114.s312198.managers.CommandExecutor;
import ru.itmo.p3114.s312198.managers.SynchronizedCollectionManager;
import ru.itmo.p3114.s312198.session.Session;
import ru.itmo.p3114.s312198.session.SessionHashMap;
import ru.itmo.p3114.s312198.transmission.AuthenticationRequest;
import ru.itmo.p3114.s312198.transmission.AuthenticationResponse;
import ru.itmo.p3114.s312198.transmission.CSChannel;
import ru.itmo.p3114.s312198.transmission.PrimaryPack;
import ru.itmo.p3114.s312198.transmission.ResponsePack;
import ru.itmo.p3114.s312198.transmission.SecondaryPack;
import ru.itmo.p3114.s312198.transmission.User;

import java.net.Socket;
import java.util.ArrayList;

public class ClientTask implements Runnable {
    private final CommandExecutor commandExecutor;
    private final Socket socket;
    private final SessionHashMap sessionHashMap;
    private final Long currentSessionUID;
    private Boolean running = Boolean.TRUE;
    private User actor;

    public ClientTask(SessionHashMap sessionHashMap, Long sessionID, Socket socket, SynchronizedCollectionManager synchronizedCollectionManager) {
        this.sessionHashMap = sessionHashMap;
        this.socket = socket;
        this.currentSessionUID = sessionID;
        commandExecutor = new CommandExecutor(synchronizedCollectionManager);
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
                currentSession = new Session(currentSessionUID, actor);
                sessionHashMap.addSession(currentSession);
                System.out.println(currentSessionUID + " " + actor.getId() + " " + actor.getUsername());
                while (running) {
                    PrimaryPack primaryPack = (PrimaryPack) channel.readObject();
                    for (CommandRecord commandRecord : primaryPack.getCommandRecords()) {
                        commandRecord.getCommand().setOwner(actor.getUsername());
                        if (commandRecord.getCommandType() == CommandTypes.COMPLEX_COMMAND) {
                            ArrayList<String> accessResponse = new ArrayList<>();
                            if (((AbstractComplexCommand) commandRecord.getCommand()).canExecute()) {
                                channel.writeObject(new ResponsePack(accessResponse, Boolean.TRUE));
                                SecondaryPack secondaryPack = (SecondaryPack) channel.readObject();
                                ((AbstractComplexCommand) commandRecord.getCommand()).setComplexArgument(secondaryPack.getComplexArgument());
                            } else {
                                accessResponse.add("Access denied");
                                channel.writeObject(new ResponsePack(accessResponse, Boolean.FALSE));
                            }
                        }
                        System.out.println(((AbstractComplexCommand) commandRecord.getCommand()).canExecute());
                        CommandResult commandResult = commandExecutor.executeCommandRecord(commandRecord);
                    }
                }
            }
        } catch (TransmissionException transmissionException) {
            System.out.println(transmissionException.getMessage());
        }
        if (currentSession != null) {
            sessionHashMap.removeSessionByID(actor.getUsername(), currentSessionUID);
        }
    }
}
