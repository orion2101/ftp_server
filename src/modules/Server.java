package modules;

import java.io.IOException;

public class Server {
    private Listener commandListener;

    public Server(String address) throws IOException {
        commandListener = new Listener(address);
    }

    public void run()throws IOException{
        commandListener.listen();
    }
}