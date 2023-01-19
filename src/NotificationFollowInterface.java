import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotificationFollowInterface extends Remote {
    void notificationFollowed(String username) throws RemoteException;

    void notificationUnfollowed(String username) throws RemoteException;
}
