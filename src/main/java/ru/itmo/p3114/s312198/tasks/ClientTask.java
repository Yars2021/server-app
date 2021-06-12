package ru.itmo.p3114.s312198.tasks;

import ru.itmo.p3114.s312198.authentication.AuthenticationRequestProcessor;
import ru.itmo.p3114.s312198.commands.CommandHashMap;
import ru.itmo.p3114.s312198.commands.CommandRecord;
import ru.itmo.p3114.s312198.commands.HistoryRecord;
import ru.itmo.p3114.s312198.commands.actions.complex.AbstractComplexCommand;
import ru.itmo.p3114.s312198.commands.actions.simple.Help;
import ru.itmo.p3114.s312198.commands.actions.simple.History;
import ru.itmo.p3114.s312198.commands.results.CommandResult;
import ru.itmo.p3114.s312198.commands.types.CommandTypes;
import ru.itmo.p3114.s312198.database.DatabaseCommandExecutor;
import ru.itmo.p3114.s312198.exceptions.AuthenticationException;
import ru.itmo.p3114.s312198.exceptions.TransmissionException;
import ru.itmo.p3114.s312198.managers.HistoryManager;
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
import java.time.LocalDate;
import java.util.ArrayList;

public class ClientTask implements Runnable {
    private final CommandHashMap commandHashMap;
    private final HistoryManager historyManager = new HistoryManager(5);
    private final DatabaseCommandExecutor commandExecutor;
    private final Socket socket;
    private final SessionHashMap sessionHashMap;
    private final Long currentSessionUID;
    private Boolean running = Boolean.TRUE;
    private User actor;

    public ClientTask(CommandHashMap commandHashMap, SessionHashMap sessionHashMap, Long sessionID, Socket socket, SynchronizedCollectionManager synchronizedCollectionManager) {
        this.sessionHashMap = sessionHashMap;
        this.socket = socket;
        this.currentSessionUID = sessionID;
        commandExecutor = new DatabaseCommandExecutor(synchronizedCollectionManager);
        this.commandHashMap = commandHashMap;
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
                Boolean skipped = Boolean.FALSE;
                channel.writeObject(new AuthenticationResponse(Boolean.TRUE,
                        "Welcome, " + actor.getUsername(), actor));
                if (!sessionHashMap.containsUsername(actor.getUsername())) {
                    sessionHashMap.addUsername(actor.getUsername());
                }
                currentSession = new Session(currentSessionUID, actor);
                sessionHashMap.addSession(currentSession);
                System.out.println("A new session has been formed: " + currentSessionUID + " " + actor.getUsername());
                while (running) {
                    PrimaryPack primaryPack = (PrimaryPack) channel.readObject();
                    ArrayList<String> result = new ArrayList<>();
                    for (CommandRecord commandRecord : primaryPack.getCommandRecords()) {
                        if ("help".equals(commandRecord.getCommand().getCommandName())) {
                            ((Help) commandRecord.getCommand()).setCommandHashMap(commandHashMap);
                        }
                        if ("history".equals(commandRecord.getCommand().getCommandName())) {
                            ((History) commandRecord.getCommand()).setHistoryManager(historyManager);
                        }
                        if ("exit".equals(commandRecord.getCommand().getCommandName())) {
                            shutdown();
                        }
                        if (running) {
                            commandRecord.getCommand().setOwner(actor.getUsername());
                            if (commandRecord.getCommandType() == CommandTypes.COMPLEX_COMMAND) {
                                if (((AbstractComplexCommand) commandRecord.getCommand()).getComplexArgument() == null) {
                                    ArrayList<String> accessResponse = new ArrayList<>();
                                    if (((AbstractComplexCommand) commandRecord.getCommand()).canExecute()) {
                                        channel.writeObject(new ResponsePack(accessResponse, Boolean.TRUE));
                                        SecondaryPack secondaryPack = (SecondaryPack) channel.readObject();
                                        if (secondaryPack == null) {
                                            System.out.println("Client requested disconnect");
                                            shutdown();
                                        } else {
                                            secondaryPack.getComplexArgument().setCreatorId(actor.getId());
                                            secondaryPack.getComplexArgument().setOwner(actor.getUsername());
                                            secondaryPack.getComplexArgument().setCreationDate(LocalDate.now());
                                            ((AbstractComplexCommand) commandRecord.getCommand()).setComplexArgument(secondaryPack.getComplexArgument());
                                            System.out.println(secondaryPack.getComplexArgument().toReadableString());
                                        }
                                    } else {
                                        accessResponse.add("Access denied");
                                        channel.writeObject(new ResponsePack(accessResponse, Boolean.FALSE));
                                        skipped = Boolean.TRUE;
                                    }
                                } else {
                                    ((AbstractComplexCommand) commandRecord.getCommand()).getComplexArgument().setCreatorId(actor.getId());
                                    ((AbstractComplexCommand) commandRecord.getCommand()).getComplexArgument().setOwner(actor.getUsername());
                                    ((AbstractComplexCommand) commandRecord.getCommand()).getComplexArgument().setCreationDate(LocalDate.now());
                                }
                            }
                            if (!skipped) {
                                CommandResult commandResult = commandExecutor.executeCommandRecord(commandRecord, actor);
                                historyManager.push(new HistoryRecord(commandRecord.getCommand(), commandResult));
                                result.addAll(commandResult.getOutput());
                                result.add("");
                            }
                            skipped = Boolean.FALSE;
                        }
                    }
                    channel.writeObject(new ResponsePack(result, Boolean.FALSE));
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
