package modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener {
    private SocketManager socketManager;
    private boolean exit = false;
    private FtpInterpreter interpreter;


    public Listener(String address) throws IOException {
        socketManager = new SocketManager(address);
        interpreter = new FtpInterpreter(socketManager);
    }

    public void listen() throws IOException {
        ServerSocket serverSocket = socketManager.createControlSocket();
        System.out.println("Server listening on " + serverSocket.getLocalSocketAddress());
        Socket ctrlSocket = serverSocket.accept();
        PrintWriter writer = new PrintWriter(ctrlSocket.getOutputStream(), true);
        writer.println("220-Service ready for new user.");
        writer.println("220 Connection to server succeed !");
        ctrlSocket.setReuseAddress(true);
        ctrlSocket.setKeepAlive(true);
        System.out.println("New client connected on " + ctrlSocket.getRemoteSocketAddress());
        while(!exit){

            BufferedReader reader = new BufferedReader(new InputStreamReader(ctrlSocket.getInputStream()));

            if(ctrlSocket.getInputStream().available() != 0) {


                String line = reader.readLine();
                if(line != null){
                    System.out.println(line);
                    exit = interpreter.interpret(line, ctrlSocket);
                }
                //System.out.println("Running");

            }

        }
        System.out.println("Client has leave");
    }
}
