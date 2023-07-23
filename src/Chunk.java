import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Chunk implements Serializable {
    private int chunk_no; // Stores the chunk number
    private String file_id; // Stores the chunk file ID
    private int rep_degree; // Stores the desired replication degree
    private int size = 0; // Stores the file's size

    public Chunk(int chunk_no, String file_id, int rep_degree, int size){
        this.chunk_no = chunk_no;
        this.file_id = file_id;
        this.rep_degree = rep_degree;
        this.size = size;
    }

    public int getChunk_no() {
        return chunk_no;
    }

    public int getRep_degree() {
        return rep_degree;
    }

    public String getFile_id() {
        return file_id;
    }

    public int getSize() {
        return size;
    }

    public int sortChunks() {
        int priority = 0;
        /*
        * Sort is done by:
        *   (1) perceived replication degree > desired replication degree
        *   (2) highest replication degree
        *   (3) biggest size
        * */
        priority += ((Peer.getStorage().getChunkOcurrences().get(file_id + "_" + chunk_no) - rep_degree) * 100000);
        priority += (Peer.getStorage().getChunkOcurrences().get(file_id + "_" + chunk_no) * 1000);
        priority += size;

        return priority;
    }
}
