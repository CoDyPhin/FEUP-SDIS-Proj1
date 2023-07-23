import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ReceivedGetChunkMessageHander implements Runnable {
    private static Message msg;
    public ReceivedGetChunkMessageHander(Message message) {
        msg = message;
    }

    @Override
    public void run() {
        for (Chunk chunk: Peer.getStorage().getBackedChunks()){
            if (chunk.getFile_id().equals(msg.getFile_id()) && chunk.getChunk_no() == msg.getChunk_no()) { // Checks if the peer has the chunk
                String messageHeader = "1.0 CHUNK " + Peer.getPeer_id() + " " + chunk.getFile_id() + " " + chunk.getChunk_no() + "\r\n\r\n";
                System.out.println("SENT 1.0 CHUNK " + Peer.getPeer_id() + " " + chunk.getFile_id() + " " + chunk.getChunk_no() + "\r\n\r\n");
                byte[] header = messageHeader.getBytes(StandardCharsets.US_ASCII);
                String path = "../peers/" + Peer.getPeer_id() + "/" + chunk.getFile_id() + "_" + chunk.getChunk_no();
                byte[] body = new byte[0];
                try {
                    body = Files.readAllBytes(Paths.get(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] messageToSend;
                if (body != null)
                    messageToSend = new byte[header.length + body.length];
                else messageToSend = new byte[header.length];
                System.arraycopy(header, 0, messageToSend, 0, header.length);
                if (body != null)
                    System.arraycopy(body, 0, messageToSend, header.length, body.length);

                try {
                    Peer.getMC().sendMessage(messageToSend);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
