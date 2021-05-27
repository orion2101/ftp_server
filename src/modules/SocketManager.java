package modules;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketManager {
    private ServerSocket serverDataSocket;
    private ServerSocket serverSocket;
    private InetAddress serverAddress;

    public SocketManager(String address) throws IOException{
        serverAddress = InetAddress.getByName(address);
    }

    public ServerSocket createControlSocket() throws IOException {
        serverSocket = new ServerSocket(0, 1, serverAddress);
        System.out.println("Server is bound to socket " + serverSocket.getLocalSocketAddress());
        return serverSocket;
    }

    public ServerSocket createDataSocket() throws IOException{
        serverDataSocket = new ServerSocket(0,1,serverAddress);
        System.out.println("Server is bounded to " + serverDataSocket.getLocalSocketAddress());
        return serverDataSocket;
    }

    public void closeControlSocket() throws IOException{
        serverSocket.close();
    }

    public void closeDataSocket() throws IOException{
        serverDataSocket.close();
    }

    public InetAddress getServerAddress(){
        return serverAddress;
    }

    public ServerSocket getServerSocket(){
        return serverSocket;
    }

    public ServerSocket getServerDataSocket(){
        return serverDataSocket;
    }
}
