import java.io.IOException;

public class SendMessageHandler implements Runnable {
    private final Message message;

    public SendMessageHandler(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        try {
            switch(message.getMessage_type()){ // Message is handled based on its type
                case "PUTCHUNK":
                    Peer.getMDB().sendMessage(message.getMsg_bytes());
                    break;
                case "GETCHUNK":
                    Peer.getMDR().sendMessage(message.getMsg_bytes());
                    break;
                case "DELETE":
                case "DELETED":
                case "LOGGEDIN":
                case "REMOVED":
                    Peer.getMC().sendMessage(message.getMsg_bytes());
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
