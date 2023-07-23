import javax.xml.crypto.Data;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Message {
    private String protocol_version;
    private String message_type;
    private int sender_id;
    private String file_id;
    private int chunk_no = -1;
    private int rep_degree = -1;
    private byte[] data_chunk;
    private byte[] msg_bytes;

    public Message(byte[] msg_bytes){ // Received message constructor
        this.msg_bytes = msg_bytes;
        parsePacket();
    }

    public Message(String protocol_version, String message_type, int sender_id, String file_id, int chunk_no){ // Message to send constructor
        this.protocol_version = protocol_version;
        this.message_type = message_type;
        this.sender_id = sender_id;
        if(!this.message_type.equals("LOGGEDIN")){
            this.file_id = file_id;
        }
        if(this.message_type.equals("DELETED")){
            this.chunk_no = chunk_no;
        }
        else if(!this.message_type.equals("DELETE")){
            this.chunk_no = chunk_no;
        }

        createMsgBytes();
    }

    private void parsePacket(){ // Received message parser
        String msg = new String(msg_bytes,0,msg_bytes.length);
        String[] split_msg = msg.split("\r\n\r\n", 2);
        String header = split_msg[0].trim();
        String[] split_header = header.split(" ");
        this.protocol_version = split_header[0];
        this.message_type = split_header[1];
        if(this.message_type.equals("PUTCHUNK") || this.message_type.equals("CHUNK")){
            int bodysize = msg.length()-header.length()-4;
            if(bodysize>0){
                this.data_chunk = new byte[bodysize];
                System.arraycopy(this.msg_bytes, header.length() + 4, this.data_chunk, 0, this.data_chunk.length);
            }
            else this.data_chunk = null;
        }
        this.sender_id = Integer.parseInt(split_header[2]);
        this.file_id = split_header[3];
        if (this.message_type.equals("DELETED") && split_header.length > 4){
            this.chunk_no = Integer.parseInt(split_header[4]);
        }
        else if(!this.message_type.equals("DELETE") && split_header.length > 4) this.chunk_no = Integer.parseInt(split_header[4]);
        if(this.message_type.equals("PUTCHUNK") && split_header.length > 5) this.rep_degree = Integer.parseInt(split_header[5]);
    }

    private void createMsgBytes(){ // Message to send creation
        String header = this.protocol_version  + " " + this.message_type + " " + this.sender_id + " " + this.file_id + " ";
        if(this.chunk_no != -1) header+=this.chunk_no + " ";
        if(this.rep_degree != -1) header+=this.rep_degree + " ";
        header += "\r\n\r\n";
        byte[] header_bytes = header.getBytes(StandardCharsets.US_ASCII);
        this.msg_bytes = new byte[header_bytes.length];
        System.arraycopy(header_bytes, 0, this.msg_bytes, 0, header_bytes.length);
    }

    public byte[] getData_chunk() {
        return data_chunk;
    }

    public String getMessage_type() {
        return message_type;
    }

    public byte[] getMsg_bytes() {
        return msg_bytes;
    }

    public int getSender_id() {
        return sender_id;
    }

    public String getFile_id() {
        return file_id;
    }

    public int getRep_degree() {
        return rep_degree;
    }

    public int getChunk_no() {
        return chunk_no;
    }

    public String getProtocol_version() {
        return protocol_version;
    }
}
