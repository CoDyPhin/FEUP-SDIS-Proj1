import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.io.File;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements RMIRemoteObject{
    private static int peer_id;
    private static String protocol_version;
    private static String rmt_obj_name;

    private static MCChannel MC; // Control Channel
    private static MDBChannel MDB; // Backup Channel
    private static MDRChannel MDR; // Restore Channel
    private static ScheduledThreadPoolExecutor exec;
    private static Storage storage;

    private Peer(String MCAddress, int MCPort, String MDBaddress, int MDBport, String MDRAddress, int MDRPort) throws IOException, ClassNotFoundException {
        exec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(128);
        MC = new MCChannel(MCAddress, MCPort);
        MDB = new MDBChannel(MDBaddress, MDBport);
        MDR = new MDRChannel(MDRAddress, MDRPort);
    }

    public static String getProtocol_version() {
        return protocol_version;
    }

    public static MDBChannel getMDB() {
        return MDB;
    }

    public static MCChannel getMC() {
        return MC;
    }

    public static MDRChannel getMDR() {
        return MDR;
    }

    public static int getPeer_id() {
        return peer_id;
    }

    public static Storage getStorage() {
        return storage;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if(args.length != 9){
            System.out.println("Usage:\tPeer <ProtocolVersion> <PeerId> <PeerAP> <MCAddress> <MCPort> <MDBAddress> <MDBPort> <MDRAddress> <MDRPort>\n");
            return;
        }
        // Parsing arguments
        protocol_version = args[0];
        peer_id = Integer.parseInt(args[1]);
        rmt_obj_name = args[2];

        try {
            Peer obj = new Peer(args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
            RMIRemoteObject stub = (RMIRemoteObject) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(rmt_obj_name, stub);
            System.err.println("Peer ready");
        } catch (Exception e) {
            System.err.println("Peer exception: " + e.toString());
            e.printStackTrace();
        }
        deserialize();
        exec.execute(MC);
        exec.execute(MDB);
        exec.execute(MDR);

        Runtime.getRuntime().addShutdownHook(new Thread(Peer::serialize)); // If the program is shut down, it serializes the storages and terminates
        if(protocol_version.equals("1.9")){
            Message msg = new Message("1.9", "LOGGEDIN", Peer.getPeer_id(), null, -1);
            SendMessageHandler sendLoggedInThread = new SendMessageHandler(msg);
            exec.execute(sendLoggedInThread);
            System.out.println("SENT: 1.9 LOGGEDIN " + Peer.getPeer_id());
        }
    }

    public static void serialize() {
        System.out.println("Serializing Storage...\n");
        String filename = "../peers/" + peer_id + "/storage/storage.ser";

        File file = new File(filename);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream fileStream = new FileOutputStream(filename);
            ObjectOutputStream outputStream = new ObjectOutputStream(fileStream);
            outputStream.writeObject(storage);
            outputStream.close();
            fileStream.close();
            System.out.println("Serialized data is saved in " + filename);
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    public static void deserialize() throws IOException, ClassNotFoundException {
        System.out.println("Deserializing Storage...\n");
        String filename = "../peers/" + peer_id + "/storage/storage.ser";
        File file = new File(filename);
        if (!file.exists()) {
            storage = new Storage(peer_id);
        }
        else {
            try {
                FileInputStream fileInput = new FileInputStream(filename);
                ObjectInputStream inputObj = new ObjectInputStream(fileInput);
                storage = (Storage) inputObj.readObject();
                inputObj.close();
                fileInput.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void backup(String path, int repDegree) throws IOException, InterruptedException {
        FileC file = new FileC(path, repDegree);
        storage.addFile(file); // Add desired file to backed up files list
        byte[] fileBuffer = new byte[64000];
        FileInputStream fileInput = new FileInputStream(file.getPath());
        BufferedInputStream bufferInput = new BufferedInputStream(fileInput);
        int read;
        int cnt = 0;
        while ((read = bufferInput.read(fileBuffer)) > 0) { // Loop for reading chunks
            byte[] body = Arrays.copyOf(fileBuffer, read);
            Chunk chunk = new Chunk(cnt, file.getFile_id(), file.getRepDegree(), body.length);
            file.addChunk(chunk); // Add chunk to desired file's chunk list
            String message = protocol_version + " PUTCHUNK " + Peer.getPeer_id() + " " + chunk.getFile_id() + " " + chunk.getChunk_no() + " " + chunk.getRep_degree() + "\r\n\r\n";
            System.out.println("SENT " + "1.0" + " PUTCHUNK " + peer_id + " " + chunk.getFile_id() + " " + chunk.getChunk_no() + " " + chunk.getRep_degree() + "\r\n\r\n");
            byte[] header = message.getBytes(StandardCharsets.US_ASCII);
            byte[] messageToSend;
            messageToSend = new byte[header.length + body.length]; // Converts the message to bytes
            System.arraycopy(header, 0, messageToSend, 0, header.length);
            System.arraycopy(body, 0, messageToSend, header.length, body.length);
            Message msg = new Message(messageToSend);
            SendMessageHandler sendPutChunkThread = new SendMessageHandler(msg);
            exec.execute(sendPutChunkThread); // Uses thread for sending message
            cnt++;
            fileBuffer = new byte[64000];
            exec.schedule(new CheckReceivedChunks(messageToSend, 2), 1, TimeUnit.SECONDS); // Schedules a thread to check if chunks were correctly stored
            Thread.sleep(100); // Sleep in order to not overload the channel
        }
    }

    @Override
    public void state() throws IOException {
        int cnt = 0;
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("PEER " + Peer.getPeer_id() + " STATE");
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("Backed Up Files\n");
        if (storage.getBackedFiles().isEmpty()) {
            System.out.println("No Backed Up Files Yet");
            System.out.println("-------------------------------------------------------------------------------------");
        }
        for (FileC file: storage.getBackedFiles()){
            cnt++;
            System.out.println("File " + cnt);
            System.out.println("Pathname: " + file.getPath());
            System.out.println("Backup ID: " + file.getFile_id());
            System.out.println("Replication Degree: " + file.getRepDegree() + "\n");
            System.out.println("File Backed Up Chunks [ID, Replication Degree]");

            for (Chunk chunk: file.getChunks()){
                System.out.println("[" + chunk.getChunk_no() + ", " + storage.getChunkOcurrences().get(chunk.getFile_id() + "_" + chunk.getChunk_no()) + "]");
            }
            System.out.println("-------------------------------------------------------------------------------------");
        }
        if(Peer.getProtocol_version().equals("1.9")){
            System.out.println("Deleted files: ");
            if (storage.getDeletedFiles().isEmpty())
                System.out.println("No Deleted Files Queue");
            for (FileC file: storage.getDeletedFiles()) {
                System.out.println("Backup ID: " + file.getFile_id());
                System.out.println("Number of Chunks to Remove: " + file.getRepDegree() + "\n");
            }
        }
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("Backed Up Chunks [ID, Size, Desired Replication Degree, Perceived Replication Degree]\n");
        if (storage.getBackedChunks().isEmpty())
            System.out.println("No Backed Up Chunks Yet");
        for (Chunk chunk: storage.getBackedChunks()) {
            System.out.println("[" + chunk.getFile_id() + "_" + chunk.getChunk_no() + ", " + chunk.getSize() + ", " + chunk.getRep_degree() + ", " + storage.getChunkOcurrences().get(chunk.getFile_id() + "_" + chunk.getChunk_no()) + "]");
        }
        System.out.println("\n-------------------------------------------------------------------------------------");
        System.out.println("Storage Capacity: " + storage.getCapacity());
        System.out.println("Available Space: " + storage.getSpaceAvailable());
        System.out.println("Ocuppied Space: " + storage.getSpaceOccupied());
        System.out.println("-------------------------------------------------------------------------------------");
    }

    @Override
    public void restore(String path) throws InterruptedException {
        boolean fileFound = false; // Used for checking if file was backed up already
        for (FileC file: storage.getBackedFiles()){
            if (file.getFile().getName().equals(path) || file.getPath().equals(path) || file.getFile().getAbsolutePath().equals((path))) {
                if(fileFound) {
                    System.out.println("Ambiguous file name, restoring another file with the same filename.\n");
                }
                fileFound = true;
                for (Chunk chunk: file.getChunks()) {
                    Message msg = new Message("1.0", "GETCHUNK", peer_id, chunk.getFile_id(), chunk.getChunk_no());
                    SendMessageHandler sendGetChunkThread = new SendMessageHandler(msg);
                    exec.execute(sendGetChunkThread);
                    System.out.println("SENT 1.0 GETCHUNK " + peer_id + " " + chunk.getFile_id() + " " + chunk.getChunk_no() + " " + "\r\n\r\n");
                    Thread.sleep(100);
                }
            }

        }
        if (!fileFound) // If the file is not in the backed files list, it doesn't make sense to restore it
            System.out.println("File hasn't been backed up yet!");
    }

    @Override
    public void delete(String path) throws IOException, InterruptedException {
        boolean fileFound = false;
        for (int i = 0; i < storage.getBackedFiles().size(); i++){ // Iterate throught backed files
            FileC file = storage.getBackedFiles().get(i);
            if (file.getFile().getName().equals(path) || file.getPath().equals(path) || file.getFile().getAbsolutePath().equals((path))) { // Found the specified file
                if(fileFound) {
                    System.out.println("Ambiguous filename, deleting another file with the same filename.\n"); // In case only the filename (and extention) is provided, all files with that name will be deleted
                }
                fileFound = true;
                Message msg = new Message("1.0","DELETE",Peer.getPeer_id(),file.getFile_id(),-1);
                SendMessageHandler sendDelThread = new SendMessageHandler(msg);
                Peer.getStorage().updateDelFiles(msg.getFile_id(), -1); // Update backed files and backed chunks data
                i--; // Update the iterator since a file will be deleted from the backed files in the previous function
                exec.execute(sendDelThread);
                System.out.println("SENT " +  "1.0" + " " +  msg.getMessage_type() +  " " + msg.getSender_id() + " " + msg.getFile_id() + "\r\n\r\n");
            }
        }
        if (!fileFound) // If the file is not in the backed files list, it doesn't make sense to delete it
            System.out.println("File hasn't been backed up yet!");
    }

    @Override
    public void reclaim(long space) throws InterruptedException {
        long spaceUsed = storage.getSpaceAvailable() - storage.getSpaceOccupied(); // Space used before reclaim
        List<Chunk> chunksToRemove = new ArrayList<>(); // List that will store all chunks that need deletion
        if (space >= spaceUsed) { // Doesn't make sense to reclaim on a peer that already meets the requirements
            System.out.println("Peer already meets the requirements! No need to delete any chunks.");
            Peer.getStorage().setCapacity(space);
            Peer.getStorage().setSpaceavailable(space - Peer.getStorage().getSpaceOccupied());
        }
        else {
            List<Chunk> chunks = Peer.getStorage().getBackedChunks();
            chunks.sort(Comparator.comparing(Chunk::sortChunks)); // Used for sorting which chunks should be deleted first
            Collections.reverse(chunks);
            for (Chunk c: chunks) {
                if (space >= spaceUsed) // When the requirements are met, there is no need for more deletion
                    break;
                else {
                    spaceUsed -= c.getSize();
                    chunksToRemove.add(c);
                    storage.addReclaimedChunk(c);

                    Message msg = new Message("1.0", "REMOVED", peer_id, c.getFile_id(), c.getChunk_no());

                    SendMessageHandler sendGetChunkThread = new SendMessageHandler(msg);
                    exec.execute(sendGetChunkThread);
                    System.out.println("SENT 1.0 REMOVED " + peer_id + " " + c.getFile_id() + " " + c.getChunk_no() + "\r\n\r\n");
                    Thread.sleep(100); // Sleep in order to not overload the channel
                }
            }

            // After we know which files ought to be deleted, we delete them
            for (Chunk c2: chunksToRemove) {
                storage.getBackedChunks().remove(c2);
                storage.removeChunk("../peers/" + Peer.getPeer_id() + "/" + c2.getFile_id() + "_" + c2.getChunk_no());
            }

            Peer.getStorage().setCapacity(space);
            Peer.getStorage().setSpaceavailable(space - Peer.getStorage().getSpaceOccupied());
            System.out.println("New Space Available: " + Peer.getStorage().getSpaceAvailable());
        }
    }


    static ScheduledExecutorService getExec() {
        return exec;
    }
}
