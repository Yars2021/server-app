package ru.itmo.p3114.s312198;

import ru.itmo.p3114.s312198.tasks.ListenForClients;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    public static void main(String[] args) {
        System.out.println("Server is up");
        try {
            Thread clientListener = new Thread(new ListenForClients(new ServerSocket(7035)));
            clientListener.setName("Client listener thread");
            clientListener.start();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
