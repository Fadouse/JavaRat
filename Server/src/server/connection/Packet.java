package server.connection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Packet {
    private ByteArrayOutputStream baos;
    private DataOutputStream dos;

    public Packet() {
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
    }

    public void sendHead(int head) throws IOException {
        dos.writeInt(head);
    }

    public void sendData(byte[] data) throws IOException {
        dos.writeInt(data.length);
        dos.write(data);
    }

    public void sendLong(long data) throws IOException {
        dos.writeLong(data);
    }

    public void sendTo(DataOutputStream out) throws IOException {
        baos.writeTo(out);
    }

    public void reset() {
        baos.reset();
    }
}