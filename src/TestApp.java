import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException {
        if (args.length > 4) {
            System.out.println("Wrong Format! Must be: TestApp <peer_ap> <sub_protocol> <opnd_1>");
            return;
        }

        String peer_ap, host, hostPeer, access_point, sub_protocol, opnd_1;
        String[] peer_ap_args;
        peer_ap = args[0];
        peer_ap_args = args[0].split("/");
        host = peer_ap_args[0];
        hostPeer = peer_ap_args[1];
        sub_protocol = args[1];
        Registry reg = LocateRegistry.getRegistry(host);
        RMIRemoteObject peer = (RMIRemoteObject) reg.lookup(hostPeer);
        String path;

        switch(sub_protocol) {
            case "BACKUP":
                if (args.length != 4) {
                    System.out.println("Wrong Format! Backup must be: TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
                    return;
                }
                path = args[2];
                Integer repDegree = Integer.parseInt(args[3]);
                peer.backup(path, repDegree);
                break;
            case "RESTORE":
                if (args.length != 3) {
                    System.out.println("Wrong Format! Backup must be: TestApp <peer_ap> RESTORE <file_path>");
                    return;
                }
                path = args[2];
                peer.restore(path);
                break;
            case "DELETE":
                if (args.length != 3) {
                    System.out.println("Wrong Format! Backup must be: TestApp <peer_ap> DELETE <file_path>");
                    return;
                }
                path = args[2];
                peer.delete(path);
                break;
            case "RECLAIM":
                if (args.length != 3) {
                    System.out.println("Wrong Format! Backup must be: TestApp <peer_ap> RECLAIM <space>");
                    return;
                }
                int space = Integer.parseInt(args[2]);
                peer.reclaim(space);
                break;
            case "STATE":
                peer.state();
                break;
            default:
                break;
        }

    }
}
