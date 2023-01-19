import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface RemoteInterface extends Remote {
    public boolean register(String username, String password, LinkedList<String> tags) throws RemoteException;

    public void registrationCallback(NotificationFollowInterface ClientInterface, String username) throws RemoteException;

    public void unregistrationCallback(NotificationFollowInterface ClientInterface, String username) throws RemoteException;

    public LinkedList<String> backupFollowers(String username) throws RemoteException;
}