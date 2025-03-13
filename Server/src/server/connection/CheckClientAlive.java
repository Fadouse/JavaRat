package server.connection;

import server.Server;
import server.utils.ID;

public class CheckClientAlive extends Thread {
    private final ClientConnection clientConnection;
    private final Server server;

    public CheckClientAlive(ClientConnection clientConnection, Server server) {
        this.server = server;
        this.clientConnection = clientConnection;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(200);
                clientConnection.sendMessage(ID.ALIVE(), "C");
            } catch (Exception e) {
                System.err.println("Client " + clientConnection.getIP() + " 掉线，清理连接。");
                server.deleteClient(clientConnection); // 清理客户端资源
                break;
            }
        }
    }
}
