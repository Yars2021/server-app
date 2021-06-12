package ru.itmo.p3114.s312198.tasks;

import ru.itmo.p3114.s312198.commands.CommandHashMap;
import ru.itmo.p3114.s312198.commands.actions.complex.Add;
import ru.itmo.p3114.s312198.commands.actions.complex.AddIfMax;
import ru.itmo.p3114.s312198.commands.actions.complex.RemoveGreater;
import ru.itmo.p3114.s312198.commands.actions.complex.Update;
import ru.itmo.p3114.s312198.commands.actions.simple.Clear;
import ru.itmo.p3114.s312198.commands.actions.simple.ExecuteScript;
import ru.itmo.p3114.s312198.commands.actions.simple.Exit;
import ru.itmo.p3114.s312198.commands.actions.simple.Help;
import ru.itmo.p3114.s312198.commands.actions.simple.History;
import ru.itmo.p3114.s312198.commands.actions.simple.Info;
import ru.itmo.p3114.s312198.commands.actions.simple.Message;
import ru.itmo.p3114.s312198.commands.actions.simple.Nop;
import ru.itmo.p3114.s312198.commands.actions.simple.Permission;
import ru.itmo.p3114.s312198.commands.actions.simple.PrintFieldAscendingGroupAdmin;
import ru.itmo.p3114.s312198.commands.actions.simple.RemoveAllByShouldBeExpelled;
import ru.itmo.p3114.s312198.commands.actions.simple.RemoveAnyByTransferredStudents;
import ru.itmo.p3114.s312198.commands.actions.simple.RemoveById;
import ru.itmo.p3114.s312198.commands.actions.simple.Show;
import ru.itmo.p3114.s312198.commands.types.CommandTypes;
import ru.itmo.p3114.s312198.database.DBHelper;
import ru.itmo.p3114.s312198.managers.SynchronizedCollectionManager;
import ru.itmo.p3114.s312198.session.SessionHashMap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class ListenForClients implements Runnable {
    private final SynchronizedCollectionManager synchronizedCollectionManager = new SynchronizedCollectionManager();
    private final ServerSocket serverSocket;
    private final SessionHashMap sessionHashMap = new SessionHashMap();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private volatile Boolean running = Boolean.TRUE;

    private Long generateSessionUID() {
        return new Random(System.currentTimeMillis()).nextLong();
    }

    public ListenForClients(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void shutdown() {
        running = Boolean.FALSE;
    }

    @Override
    public void run() {
        CommandHashMap validCommands = new CommandHashMap();
        validCommands.addCommandRecord("help", CommandTypes.SIMPLE_COMMAND, new Help());
        validCommands.addCommandRecord("info", CommandTypes.SIMPLE_COMMAND, new Info());
        validCommands.addCommandRecord("show", CommandTypes.SIMPLE_COMMAND, new Show());
        validCommands.addCommandRecord("remove_by_id", CommandTypes.SIMPLE_COMMAND, new RemoveById());
        validCommands.addCommandRecord("clear", CommandTypes.SIMPLE_COMMAND, new Clear());
        validCommands.addCommandRecord("execute_script", CommandTypes.SIMPLE_COMMAND, new ExecuteScript());
        validCommands.addCommandRecord("exit", CommandTypes.SIMPLE_COMMAND, new Exit());
        validCommands.addCommandRecord("history", CommandTypes.SIMPLE_COMMAND, new History());
        validCommands.addCommandRecord("remove_all_by_should_be_expelled", CommandTypes.SIMPLE_COMMAND, new RemoveAllByShouldBeExpelled());
        validCommands.addCommandRecord("remove_any_by_transferred_students", CommandTypes.SIMPLE_COMMAND, new RemoveAnyByTransferredStudents());
        validCommands.addCommandRecord("print_field_ascending_group_admin", CommandTypes.SIMPLE_COMMAND, new PrintFieldAscendingGroupAdmin());
        validCommands.addCommandRecord("nop", CommandTypes.SIMPLE_COMMAND, new Nop());
        validCommands.addCommandRecord("msg", CommandTypes.SIMPLE_COMMAND, new Message());
        validCommands.addCommandRecord("permission", CommandTypes.SIMPLE_COMMAND, new Permission());
        validCommands.addCommandRecord("add", CommandTypes.COMPLEX_COMMAND, new Add());
        validCommands.addCommandRecord("update", CommandTypes.COMPLEX_COMMAND, new Update());
        validCommands.addCommandRecord("add_if_max", CommandTypes.COMPLEX_COMMAND, new AddIfMax());
        validCommands.addCommandRecord("remove_greater", CommandTypes.COMPLEX_COMMAND, new RemoveGreater());

        try (DBHelper dbHelper = new DBHelper()) {
            synchronizedCollectionManager.initialize(dbHelper.getStudyGroups());
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    forkJoinPool.execute(new ClientTask(validCommands, sessionHashMap, generateSessionUID(), clientSocket, synchronizedCollectionManager));
                } catch (IOException ioException) {
                    System.out.println(ioException.getMessage());
                    shutdown();
                }
            }
            forkJoinPool.shutdown();
        } catch (Exception exception) {
            System.out.println("Unable to connect to the database");
        }
        System.out.println("Shutting down");
    }
}
