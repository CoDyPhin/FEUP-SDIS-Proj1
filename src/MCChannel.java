import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class MCChannel implements Runnable {
    private int port;
    private InetAddress address;
    private MulticastSocket socket;


    public MCChannel(String MCCaddress, int MCCport) throws UnknownHostException {
        this.port = MCCport;
        this.address = InetAddress.getByName(MCCaddress);
    }


    @Override
    public void run() {
        byte[] buf = new byte[64500];
        try {
            this.socket = new MulticastSocket(this.port);
            socket.joinGroup(this.address);

            while(true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                byte[] packetCopy = Arrays.copyOf(buf, packet.getLength());
                Peer.getExec().execute(new ReceivedMessagesHandler(packetCopy));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message, message.length, address, port);
        this.socket.send(packet);
    }
}
