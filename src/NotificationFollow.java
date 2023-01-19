import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Questa classe rappresenta l'aggiornamento dei followers dell'utente con l'invio della notifica di follow o unfollow
 */
public class NotificationFollow extends RemoteObject implements NotificationFollowInterface {
    private LinkedList<String> followers;
    private Lock lock;

    public NotificationFollow(LinkedList<String> followers){
        this.followers = followers;
        lock = new ReentrantLock();
    }

    /**
     * Aggiorna la lista dei followers e manda una notifica all'utente che viene seguito
     * @param username
     * @throws RemoteException
     */
    public void notificationFollowed(String username) throws RemoteException {
        try{
            lock.lock();
            followers.add(username);
            System.out.println(username + " ha iniziato a seguirti!");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Aggiorna la lista dei followers e manda una notifica all'utente che viene unfollowato
     * @param username
     * @throws RemoteException
     */
    public void notificationUnfollowed(String username) throws RemoteException {
        try{
            lock.lock();
            followers.remove(username);
            System.out.println(username + " ha smesso di seguirti!");
        } finally {
            lock.unlock();
        }
    }
}

