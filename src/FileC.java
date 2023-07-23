import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FileC implements Serializable{
    private String path;
    private String file_id;
    private File file;
    private int repDegree;
    private List<Chunk> chunks; // List of all of the File's Chunks

    public FileC(String path, Integer repDegree) throws IOException {
        this.file = new File(path);
        this.path = path;
        this.repDegree = repDegree;
        this.chunks = new ArrayList<>();
        this.generateFileID(); // converts filename to a unique ID
    }

    public FileC(Integer repDegree, String file_id){
        this.repDegree = repDegree;
        this.file_id = file_id;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public String getFile_id() {
        return file_id;
    }

    public String getPath() {
        return path;
    }

    public File getFile() { return file;}

    public int getRepDegree() {
        return repDegree;
    }

    public void setRepDegree(int repDegree) {
        this.repDegree = repDegree;
    }

    // Used for hashing
    public String toHexString(byte[] hash)
    {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    private void generateFileID() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
            String metadata = Peer.getPeer_id() + this.file.getAbsolutePath() + this.file.lastModified();
            this.file_id = toHexString(md.digest(metadata.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void addChunk(Chunk newChunk) {
        this.chunks.add(newChunk);
    }
}
