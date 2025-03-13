package server.connection;

import server.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HandleOutbound implements Runnable {
    private final BlockingQueue<Packet> packetQueue;
    private final DataOutputStream out;
    private volatile boolean running;

    public HandleOutbound(DataOutputStream out) {
        this.out = out;
        this.packetQueue = new LinkedBlockingQueue<>();
        this.running = true;
    }

    public void addPacket(Packet packet) {
        packetQueue.offer(packet);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Packet packet = packetQueue.take();
                packet.sendTo(out);
                out.flush();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                break;
            }
        }
    }

    public void stop() {
        running = false;
    }
}