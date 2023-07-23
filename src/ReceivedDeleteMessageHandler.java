import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ReceivedDeleteMessageHandler implements Runnable {
    private static Message msg;
    public ReceivedDeleteMessageHandler(Message message) {
        msg = message;
    }

    @Override
    public void run() {
        if (Peer.getPeer_id() != msg.getSender_id()){ // Peer does not handle its own messages
            int removedchunks = 0;
            try {
                removedchunks = Peer.getStorage().removeChunks(msg.getFile_id()); // Removes chunks with the received file id
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(Peer.getProtocol_version().equals("1.9")){ // Check if enhancement is required
                if (removedchunks>0) {
                    Message response = new Message("1.9", "DELETED", msg.getSender_id(), msg.getFile_id(), removedchunks); // structure: 1.9 DELETED DestinationID DelFileID NumberOfChunksDeleted
                    SendMessageHandler sendDeledThread = new SendMessageHandler(response);
                    Peer.getExec().execute(sendDeledThread);
                    System.out.println("SENT: 1.9 DELETED " + response.getSender_id() + " " + response.getFile_id() + " " + response.getChunk_no());
                }
            }
        }
    }
}