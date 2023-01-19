import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Questa classe rappresenta un utente del social Winsome
 * con username, password, lista dei post pubblicati, feed,
 * seguaci, seguiti, lista dei tag e post votati
 */
public class User {
    private final String username; //nome dell'utente
    private final String password; //password dell'utente
    private final ConcurrentHashMap<Long, Post> posts; //post pubblicati dall'utente
    private final ConcurrentHashMap<Long, Post> feed; //post dei seguiti dell'utente
    private final LinkedList<String> followed;//lista degli account seguiti dall'utente
    private final LinkedList<String> followers;//lista di seguaci dell'utente
    private final LinkedList<String> tags;//lista dei tags dell'utente
    private final LinkedList<Long> votes;//lista dei post votati dall'utente
    private final Wallet wallet;//portafoglio dell'utente

    private boolean logged = false;//mi dice se l'utente è loggato o meno

    private final ReentrantLock followersLock;//lock per i followers

    /**
     * Costruttore per i nuovi utenti che si registrano
     * @param username
     * @param password
     * @param tags
     * @throws NoSuchAlgorithmException
     */
    public User(String username, String password, LinkedList<String> tags) throws NoSuchAlgorithmException {
        this.username = username;
        this.password = password;
        this.tags = tags;
        this.posts = new ConcurrentHashMap();
        this.feed = new ConcurrentHashMap();
        this.followed = new LinkedList();
        this.followers = new LinkedList();
        this.votes = new LinkedList();
        this.wallet = new Wallet(username);
        this.followersLock = new ReentrantLock();
    }

    /**
     * Costruttore per ripristinare gli utenti
     * @param username
     * @param password
     * @param tags
     * @param followers
     * @param followed
     * @param votes
     * @param posts
     * @param feed
     * @param wallet
     */
    public User(String username, String password, LinkedList<String> tags, LinkedList<String> followers, LinkedList<String> followed, LinkedList<Long> votes, ConcurrentHashMap<Long, Post> posts, ConcurrentHashMap<Long, Post> feed, Wallet wallet) {
        this.username = username;
        this.password = password;
        this.tags = tags;
        this.followed = followed;
        this.followers = followers;
        this.votes = votes;
        this.posts = posts;
        this.feed = feed;
        this.wallet = wallet;
        this.followersLock = new ReentrantLock();
    }


    /**
     *
     * @return l'username dell'utente
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     * @return la password dell'utente
     */
    public String getPassword() {
        return password;
    }


    /**
     *
     * @return i post pubblicati dall'utente (blog)
     */
    public ConcurrentHashMap<Long, Post> getPosts() {
        return posts;
    }

    /**
     *
     * @return il feed dell'utente
     */
    public ConcurrentHashMap<Long, Post> getFeed() {
        return feed;
    }

    /**
     *
     * @return la lista dei followers dell'utente
     */
    public LinkedList<String> getFollowers() {
        return followers;
    }


    /**
     *
     * @return la lista dei seguiti dall'utente
     */
    public LinkedList<String> getFollowed() {
        return followed;
    }

    /**
     *
     * @return la lista dei tags dell'utente
     */
    public LinkedList<String> getTags() {
        return tags;
    }

    /**
     *
     * @return la lista dei post votati dall'utente
     */
    public LinkedList<Long> getVotes() {
        return votes;
    }

    /**
     *
     * @return il portafoglio dell'utente
     */
    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Metodi per le lock dei followers
     */
    public void followersLock() {
        followersLock.lock();
    }

    public void followersUnlock() {
        followersLock.unlock();
    }

    /**
     *
     * @return true se l'utente è loggato, altrimenti false
     */
    public boolean isLogged() {
        return logged;
    }

    /**
     * imposta la variabile logged a false per il logout dell'utente
     */
    public void logout() {
        logged = false;
        return;
    }

    /**
     * Aggiunge il post alla lista dei post pubblicati dall'utente
     * @param p
     */
    public void addPost(Post p) {
        this.posts.putIfAbsent(p.getId(), p);
    }

    /**
     * Aggiunge il post al feed dell'utente
     * @param p
     */
    public void addPostToFeed(Post p) {
        this.feed.putIfAbsent(p.getId(), p);
    }

    /**
     * Aggiunge un utente alla lista dei followers
     * @param username
     */
    public void addFollowers(String username) {
        this.followers.add(username);
    }

    /**
     * Aggiunge un nuovo utente alla lista dei seguiti
     * @param username
     */
    public void addFollowed(String username) {
        this.followed.add(username);
    }

    /**
     * Aggiunge il post votato alla lista dei post votati dall'utente
     * @param id
     */
    public void addVote(long id) {
        this.votes.add(id);
    }

    /**
     * Rimuove il post dai post pubblicati dall'utente quindi dal blog
     * @param id
     */
    public void removePost(long id) {
        this.posts.remove(id);
    }

    /**
     * Rimuove un post dal feed dell'utente
     * @param id
     */
    public void removePostFeed(long id) {
        this.feed.remove(id);
    }

    /**
     *
     * @param tags
     * @return una stringa per poter stampare i tags dell'utente
     */
    public String printTags(LinkedList<String> tags) {
        String printTgs = "";
        int i;
        for (i = 0; i < tags.size() - 1; i++)
            printTgs = printTgs.concat(tags.get(i) + ", ");

        printTgs = printTgs.concat(tags.get(tags.size() - 1));

        return printTgs;
    }
}

