package client.utils.check;

import client.ClientMain;
import client.connection.ClientConnection;
import client.connection.ID;

import static client.ClientMain.connect;

public class CheckServerAlive extends Thread{
    private final ClientConnection clientConnection;
    public CheckServerAlive(ClientConnection clientConnection){
        this.clientConnection = clientConnection;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(500);
                clientConnection.sendMessage(ID.ALIVE(),"C");
            } catch (Exception e) {
                if(ClientMain.relieve)
                    break;
                if(clientConnection != null)
                    clientConnection.close();
                try {
                    connect();
                } catch (InterruptedException ignored) {
                }
                break;
            }
        }
    }
}
