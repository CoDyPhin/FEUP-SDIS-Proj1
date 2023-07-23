import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIRemoteObject extends Remote {
    void backup(String path, int repDegree) throws IOException, InterruptedException;
    void state() throws IOException;
    void restore(String path) throws RemoteException, InterruptedException;
    void delete(String path) throws IOException, InterruptedException;
    void reclaim(long space) throws IOException, InterruptedException;
}