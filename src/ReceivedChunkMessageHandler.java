import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReceivedChunkMessageHandler implements Runnable {
    private static Message msg;
    public ReceivedChunkMessageHandler(Message message) {
        msg = message;
    }

    @Override
    public void run() {
        for (FileC file: Peer.getStorage().getBackedFiles()){
            if (file.getFile_id().equals(msg.getFile_id())) { // Checks if peer has file stored
                // Creating the file
                String filename = "../peers/" + Peer.getPeer_id() + "/restored/" + file.getFile_id() + "_" + file.getFile().getName();
                File newFile = new File(filename);
                if (!newFile.exists()) {
                    newFile.getParentFile().mkdirs();
                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Allocating block of data to its position
                try {
                    RandomAccessFile restoredFile = new RandomAccessFile(filename, "rw");
                    restoredFile.seek(msg.getChunk_no()* 64000L);
                    restoredFile.write(msg.getData_chunk());
                    restoredFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
