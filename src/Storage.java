import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Storage implements Serializable {
    private final int peer_id;
    private final ArrayList<Chunk> backedChunks; // Backed Chunks List
    private final ArrayList<FileC> backedFiles; // Backed Files List
    private ArrayList<Chunk> receivedBackedChunks;
    private ConcurrentHashMap<String, Integer> chunkOcurrences;
    private ConcurrentHashMap<String, Integer> reclaimedChunks;
    private long capacity;
    private long spaceavailable;
    private ArrayList<FileC> deletedFiles;

    public Storage(int peer_id) throws IOException, ClassNotFoundException {
        this.peer_id = peer_id;
        this.backedChunks = new ArrayList<>();
        this.backedFiles = new ArrayList<>();
        this.deletedFiles = new ArrayList<>();
        this.receivedBackedChunks = new ArrayList<>();
        this.chunkOcurrences = new ConcurrentHashMap<>();
        this.reclaimedChunks = new ConcurrentHashMap<>();
        this.capacity = 64000000000L;
        this.spaceavailable = this.capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public ArrayList<Chunk> getReceivedBackedChunks() {
        return receivedBackedChunks;
    }

    public ConcurrentHashMap<String, Integer> getReclaimedChunks() {
        return reclaimedChunks;
    }

    // Adds file to the backedFiles list and checks if it has been already backed up
    public void addFile(FileC file) {
        for(FileC fileaux : this.backedFiles){
            if(fileaux.getFile_id().equals(file.getFile_id())) {
                if ((fileaux.getRepDegree() == file.getRepDegree())) return;
                fileaux.setRepDegree(file.getRepDegree());
            }
        }
        this.backedFiles.add(file);
    }

    public void updateDelFiles(String file_id, int flag){ // Updates the information on backed files and the deleted file queue, if the enhancement is being used
        if(flag == -1){ // Flag -1 if a new file had its deletion requested
            for(int i = 0; i<backedFiles.size(); i++){ // Deletes backed files and chunk occurences whose deletion was requested
                if(backedFiles.get(i).getFile_id().equals(file_id)){
                    int no_chunks = 0;
                    for(Chunk chunk : backedFiles.get(i).getChunks()){
                        String filename = chunk.getFile_id()+"_"+chunk.getChunk_no();
                        try{
                            if(chunkOcurrences.getOrDefault(filename,0)!=null)
                                no_chunks+= chunkOcurrences.getOrDefault(filename,0);
                            chunkOcurrences.remove(filename);
                        } catch (NullPointerException e){
                            System.out.println("This file is not backed up anywhere!");
                        }
                    }
                    backedFiles.remove(backedFiles.get(i));
                    i--;
                    if(Peer.getProtocol_version().equals("1.9")){ // Checks if the enhancement is in use
                        FileC newfile = new FileC(no_chunks, file_id);
                        this.deletedFiles.add(newfile); // Adds deleted files queue
                    }
                }
            }
        }
        else{ // Updates the number of chunks that still need to be deleted (Enhancement only)
            for (int i = 0; i<deletedFiles.size(); i++){
                if(deletedFiles.get(i).getFile_id().equals(file_id)){
                    deletedFiles.get(i).setRepDegree(deletedFiles.get(i).getRepDegree()-flag);
                }
                if(deletedFiles.get(i).getRepDegree() <= 0){
                    this.deletedFiles.remove(deletedFiles.get(i));
                }
            }
        }
    }

    public long getSpaceAvailable() { return this.spaceavailable; }

    public ArrayList<Chunk> getBackedChunks() {
        return this.backedChunks;
    }

    public ConcurrentHashMap<String, Integer> getChunkOcurrences() {
        return this.chunkOcurrences;
    }

    public ArrayList<FileC> getBackedFiles() {
        return backedFiles;
    }

    public ArrayList<FileC> getDeletedFiles() {
        return deletedFiles;
    }

    public void setSpaceavailable(long spaceavailable) {
        this.spaceavailable = spaceavailable;
    }

    // Updates hash map user for calculating Replication Degree
    public void addChunkOccurrence(String s) {
        // If there is no ocurrence, add an entry
        if (!this.chunkOcurrences.containsKey(s)) {
            this.chunkOcurrences.put(s, 1);
        }
        else { // Else update it
            int newRepDegree = this.chunkOcurrences.get(s) + 1;
            this.chunkOcurrences.replace(s, newRepDegree);
        }
    }

    public int removeChunks(String file_id) throws IOException { // Removes all chunks belonging to the given file from the file system and updates space
        List<Chunk> chunks = new ArrayList<>();
        int retval = 0;
        long spacefreed = 0;
        for(Chunk chunk : this.backedChunks){
            if(chunk.getFile_id().equals(file_id)){
                chunks.add(chunk);
                retval+=1;
            }
        }
        for(Chunk chunk : chunks){
            spacefreed += chunk.getSize();
            String filename = chunk.getFile_id() + "_" + chunk.getChunk_no();
            String filepath = "../peers/" + peer_id + "/" + filename;
            File file = new File(filepath);
            file.delete();
            this.backedChunks.remove(chunk);
            this.chunkOcurrences.remove(filename);
        }
        this.spaceavailable+=spacefreed;
        return retval;
    }

    public long getCapacity() {
        return capacity;
    }

    public void addBackedChunk(Chunk chunk, byte[] body) throws IOException {
        this.backedChunks.add(chunk);
        if (body != null)
            this.spaceavailable -= body.length;
    }

    public void addReceivedBackedChunk(Chunk chunk) {
        this.receivedBackedChunks.add(chunk);
    }

    public void removeChunk(String path) { // Removes a specific chunk from the file system
        File file = new File(path);
        long size = file.length();
        if(file.delete()) {
            System.out.println("Deleted " + file.getName());
            System.out.println("Delete File Size: " + size + "\n");
            this.spaceavailable += size;
        }
    }

    public void addReclaimedChunk(Chunk c) {
        String s = c.getFile_id() + "_" + c.getChunk_no();
        this.reclaimedChunks.put(s, 1);
    }

    public long getSpaceOccupied() {
        long space = 0;
        for (Chunk chunk: backedChunks)
            space += chunk.getSize();

        return space;
    }
}
