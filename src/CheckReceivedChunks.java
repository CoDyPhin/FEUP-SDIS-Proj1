import java.io.IOException;

public class CheckReceivedChunks implements Runnable {
    private final byte[] rawMsg;
    private Message msg;
    private int tries;
    private int waitingTime;

    public CheckReceivedChunks(byte[] msg, int tries) {
        this.msg = new Message(msg);
        this.rawMsg = msg; // Raw version of the received message
        this.tries = tries;
        this.waitingTime = 1;
    }

    @Override
    public void run() {
        String chunkFileName = msg.getFile_id() + "_" + msg.getChunk_no();
        int numberOfReceived;
        if (Peer.getStorage().getChunkOcurrences().get(chunkFileName) == null)
            numberOfReceived = 0;
        else numberOfReceived = Peer.getStorage().getChunkOcurrences().get(chunkFileName);

        if (msg.getRep_degree() > numberOfReceived && this.tries <= 5){ // Only handle if the Perceived Replication Degree is lower than the Desired one and the number of tries isn't higher than 5
            try {
                System.out.println("TRY " + this.tries);
                Peer.getMDB().sendMessage(rawMsg); // Resending the message
                System.out.println("RESENT " + Peer.getProtocol_version() + " PUTCHUNK " + Peer.getPeer_id() + " " + msg.getFile_id() + " " + msg.getChunk_no() + " " + msg.getRep_degree() + "\r\n\r\n");
                this.tries += 1; // Increment tries
                this.waitingTime = this.waitingTime * 2; // Duplicate the waiting time
                Thread.sleep(this.waitingTime * 1000);
                Peer.getExec().execute(new CheckReceivedChunks(rawMsg, this.tries)); // Recheck
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}
