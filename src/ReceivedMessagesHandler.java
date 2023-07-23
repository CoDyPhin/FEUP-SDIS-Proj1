import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReceivedMessagesHandler implements Runnable {
    private final byte[] received;

    public ReceivedMessagesHandler(byte[] received) {
        this.received = received;
    }

    @Override
    public void run() {
        Message packet = new Message(this.received);
        Random random = new Random();
        switch (packet.getMessage_type()){ // The message is handled based on its type
            case "PUTCHUNK":
                if(Peer.getPeer_id() != packet.getSender_id() && Peer.getStorage().getReclaimedChunks().get(packet.getFile_id() + "_" + packet.getChunk_no()) == null){ // Peer does not handle its own messages
                    System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + " " + packet.getFile_id() + " " + packet.getChunk_no() + " " + packet.getRep_degree() + "\r\n\r\n");
                    // Sleep between 0 and 400 ms
                    try {
                        Thread.sleep(random.nextInt(401));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.receivedPutChunkMessageHandler(packet);
                }
                break;
            case "STORED":
                if(Peer.getPeer_id() != packet.getSender_id()){ // Peer does not handle its own messages
                    if (Peer.getPeer_id() == 1)
                        System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + " " + packet.getFile_id() + " " + packet.getChunk_no() + "\r\n\r\n");
                    Peer.getStorage().addChunkOccurrence(packet.getFile_id() + "_" + packet.getChunk_no()); // Updates its hash map
                }
                break;
            case "GETCHUNK":
                if(Peer.getPeer_id() != packet.getSender_id()){ // Peer does not handle its own messages
                    System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + " " + packet.getFile_id() + " " + packet.getChunk_no() + "\r\n\r\n");
                    // Sleep between 0 and 400 ms
                    try {
                        Thread.sleep(random.nextInt(401));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Peer.getExec().execute(new ReceivedGetChunkMessageHander(packet)); // New Thread for handling
                }
                break;
            case "DELETE":
                if(Peer.getPeer_id() != packet.getSender_id()){ // Peer does not handle its own messages
                    System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + " " + packet.getFile_id() + " " + "\r\n\r\n");
                    Peer.getExec().execute(new ReceivedDeleteMessageHandler(packet)); // New Thread for handling
                }
                break;
            case "DELETED":
                if(Peer.getProtocol_version().equals("1.9")){ // Checking for enhancement version
                    if(Peer.getPeer_id() == packet.getSender_id()){ // Handles only "DELETED" messages sent to the selected peer
                        // getSender_id is the Destination ID, unlike the rest of the messages
                        System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + " " + packet.getFile_id() + " " + packet.getChunk_no() + "\r\n\r\n");
                        Peer.getStorage().updateDelFiles(packet.getFile_id(), packet.getChunk_no());
                    }
                }
                break;
            case "LOGGEDIN":
                if(Peer.getPeer_id() != packet.getSender_id()) { // Peer does not handle its own messages
                    if (packet.getProtocol_version().equals("1.9")) { // Checking for enhancement version
                        System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + "\r\n\r\n");
                        ArrayList<FileC> delfiles = Peer.getStorage().getDeletedFiles();
                        int delsize = delfiles.size();
                        for (int i = 0; i<delsize; i++) { // Iterate through deleted files queue
                            if(delfiles.size() != delsize){
                                i--;
                            }
                            Message deletemsg = new Message("1.0", "DELETE", Peer.getPeer_id(), delfiles.get(i).getFile_id(), -1);
                            Peer.getExec().execute(new SendMessageHandler(deletemsg)); // Resend DELETE messages the peers might have missed
                            System.out.println("SENT: DELETE 1.0 " + deletemsg.getFile_id());
                            try {
                                random = new Random();
                                Thread.sleep(random.nextInt(200));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            case "CHUNK":
                System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + " " + packet.getFile_id() + " " + packet.getChunk_no() + "\r\n\r\n");
                // Sleep between 0 and 400 ms
                try {
                    Thread.sleep(random.nextInt(401));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Peer.getExec().execute(new ReceivedChunkMessageHandler(packet)); // New Thread for handling
                break;
            case "REMOVED":
                if(packet.getSender_id() != Peer.getPeer_id()){
                    System.out.println("RECEIVED " + packet.getMessage_type() + " " + packet.getProtocol_version() + " " + packet.getSender_id() + " " + packet.getFile_id() + " " + packet.getChunk_no() + "\r\n\r\n");
                    // Sleep between 0 and 400 ms
                    try {
                        Thread.sleep(random.nextInt(401));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String s = packet.getFile_id() + "_" + packet.getChunk_no();
                    // Checks if it has the chunk
                    if (Peer.getStorage().getChunkOcurrences().containsKey(s)) {
                        int newRepDegree = Peer.getStorage().getChunkOcurrences().get(s) - 1;
                        Peer.getStorage().getChunkOcurrences().replace(s, newRepDegree);

                        for (Chunk chunk: Peer.getStorage().getBackedChunks()) {
                            if (chunk.getFile_id().equals(packet.getFile_id()) && chunk.getChunk_no() == packet.getChunk_no()){
                                // If the replication degree is lower than desired, initiates backup of that chunk
                                if (Peer.getStorage().getChunkOcurrences().get(s) < chunk.getRep_degree()) {
                                    String message = "1.0" + " PUTCHUNK " + Peer.getPeer_id() + " " + chunk.getFile_id() + " " + chunk.getChunk_no() + " " + chunk.getRep_degree() + "\r\n\r\n";
                                    System.out.println("SENT " + "1.0" + " PUTCHUNK " + Peer.getPeer_id() + " " + chunk.getFile_id() + " " + chunk.getChunk_no() + " " + chunk.getRep_degree() + "\r\n\r\n");
                                    byte[] header = message.getBytes(StandardCharsets.US_ASCII);
                                    byte[] messageToSend;
                                    String path = "../peers/" + Peer.getPeer_id() + "/" + chunk.getFile_id() + "_" + chunk.getChunk_no();
                                    byte[] body = new byte[0];
                                    try {
                                        body = Files.readAllBytes(Paths.get(path));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    messageToSend = new byte[header.length + body.length];
                                    System.arraycopy(header, 0, messageToSend, 0, header.length);
                                    System.arraycopy(body, 0, messageToSend, header.length, body.length);
                                    Message msg = new Message(messageToSend);
                                    SendMessageHandler sendPutChunkThread = new SendMessageHandler(msg);
                                    Peer.getExec().execute(sendPutChunkThread);
                                    Peer.getExec().schedule(new CheckReceivedChunks(messageToSend, 2), 1, TimeUnit.SECONDS);
                                }
                            }
                        }
                    }
                }

                break;
            default:
                break;
        }
    }

    private void receivedPutChunkMessageHandler(Message packet) {
        try {
            Message msg = packet;
            String chunkFileName = msg.getFile_id() + "_" + msg.getChunk_no();
            for (FileC file: Peer.getStorage().getBackedFiles()) { // If the chunk is from a Peer's backed file, it mustn't store it
                if (file.getFile_id().equals(msg.getFile_id())) {
                    return;
                }
            }
            if (!Peer.getProtocol_version().equals("1.0")) { // Replication Degree should only be taken into account on the enhanced version
                if (Peer.getStorage().getSpaceAvailable() - (msg.getData_chunk().length) >= 0) { // Checks if the Peer has space available
                    if (Peer.getStorage().getChunkOcurrences().get(chunkFileName) == null || Peer.getStorage().getChunkOcurrences().get(chunkFileName) < msg.getRep_degree()) {
                        Chunk chunk = new Chunk(msg.getChunk_no(), msg.getFile_id(), msg.getRep_degree(), msg.getData_chunk().length);
                        List<Chunk> storedChunks = Peer.getStorage().getBackedChunks();
                        for (Chunk x : storedChunks) { // Checks if chunk is already stored
                            if (x.getFile_id().equals(chunk.getFile_id()) && x.getChunk_no() == chunk.getChunk_no())
                                return;
                        }
                        Peer.getStorage().addChunkOccurrence(chunkFileName);
                        Peer.getStorage().addBackedChunk(chunk, msg.getData_chunk());
                        String filename = "../peers/" + Peer.getPeer_id() + "/" + msg.getFile_id() + "_" + msg.getChunk_no();

                        // Storing chunk...
                        File file = new File(filename);
                        if (!file.exists()) {
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                        }

                        try (FileOutputStream fos = new FileOutputStream(filename)) {
                            if (msg.getData_chunk() != null)
                                fos.write(msg.getData_chunk());
                        }
                        String message = Peer.getProtocol_version() + " STORED " + Peer.getPeer_id() + " " + msg.getFile_id() + " " + msg.getChunk_no() + "\r\n\r\n";
                        System.out.println("SENT " + message);
                        Peer.getMC().sendMessage(message.getBytes());
                    }
                } else {
                    long necessarySpace = (msg.getData_chunk().length) - Peer.getStorage().getSpaceAvailable();
                    for (Chunk c1 : Peer.getStorage().getBackedChunks()) {
                        if (necessarySpace <= 0)
                            break;
                        else if (c1.getRep_degree() < Peer.getStorage().getChunkOcurrences().get(c1.getFile_id() + "_" + c1.getChunk_no())) {
                            String message = "1.0 REMOVED " + Peer.getPeer_id() + " " + c1.getFile_id() + " " + c1.getChunk_no() + "\r\n\r\n";
                            System.out.println("SENT 1.0 REMOVED " + Peer.getPeer_id() + " " + c1.getFile_id() + " " + c1.getChunk_no() + "\r\n\r\n");
                            byte[] header = message.getBytes(StandardCharsets.US_ASCII);
                            Message newMsg = new Message(header);
                            necessarySpace -= c1.getSize();

                            SendMessageHandler sendGetChunkThread = new SendMessageHandler(newMsg);
                            Peer.getExec().execute(sendGetChunkThread);
                            Thread.sleep(100);
                        }
                    }
                }
            }
            else {
                Chunk chunk = new Chunk(msg.getChunk_no(), msg.getFile_id(), msg.getRep_degree(), msg.getData_chunk().length);
                Peer.getStorage().addChunkOccurrence(chunkFileName);
                Peer.getStorage().addBackedChunk(chunk, msg.getData_chunk());
                String filename = "../peers/" + Peer.getPeer_id() + "/" + msg.getFile_id() + "_" + msg.getChunk_no();

                File file = new File(filename);
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                try (FileOutputStream fos = new FileOutputStream(filename)) {
                    if (msg.getData_chunk() != null)
                        fos.write(msg.getData_chunk());
                }
                String message = Peer.getProtocol_version() + " STORED " + Peer.getPeer_id() + " " + msg.getFile_id() + " " + msg.getChunk_no() + "\r\n\r\n";
                System.out.println("SENT " + message);
                Peer.getMC().sendMessage(message.getBytes());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}