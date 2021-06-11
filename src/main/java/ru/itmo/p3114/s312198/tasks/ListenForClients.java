package ru.itmo.p3114.s312198.tasks;

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
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                forkJoinPool.execute(new ClientTask(sessionHashMap, generateSessionUID(), clientSocket, synchronizedCollectionManager));
            } catch (IOException ioException) {
                System.out.println(ioException.getMessage());
                shutdown();
            }
        }
        forkJoinPool.shutdown();
    }
}
