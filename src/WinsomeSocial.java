import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Questa classe rappresenta la gestione di tutte le funzionalità principali
 * che un utente può effettuare su Winsome e anche della registrazione alla callback
 * per i vari aggiornamenti dei followers dell'utente
 */

public class WinsomeSocial extends RemoteObject implements RemoteInterface {

    private ConcurrentHashMap<Long, Post> posts;//post di Winsome
    private ConcurrentHashMap<String, User> users;//utenti di Winsome
    private final ConcurrentHashMap<String, NotificationFollowInterface> callbackReg;//registrazione callback

    private volatile AtomicLong postId;

    public WinsomeSocial() {
        posts = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        callbackReg = new ConcurrentHashMap<>();
        postId = new AtomicLong(0);
    }

    /**
     * Metodo per registrare l'utente al servizio di callback per aggiornamento dei followers
     * @param ClientInterface
     * @param username
     * @throws RemoteException
     */
    public synchronized void registrationCallback(NotificationFollowInterface ClientInterface, String username) throws RemoteException {
        callbackReg.putIfAbsent(username, ClientInterface);
    }

    /**
     * Metodo per disiscrivere l'utente dal servizio di callback per aggiornamento dei followers
     * @param ClientInterface
     * @param username
     * @throws RemoteException
     */
    public synchronized void unregistrationCallback(NotificationFollowInterface ClientInterface, String username) throws RemoteException {
        callbackReg.remove(username, ClientInterface);
    }

    /**
     * Metodo per recuperare la lista dei followers quando un utente si riconnette
     * @param username
     * @return
     * @throws RemoteException
     */
    public LinkedList<String> backupFollowers(String username) throws RemoteException {
        User user = users.get(username);
        if (user != null) {
            LinkedList followers;
            try {
                user.followersLock();
                followers = new LinkedList(user.getFollowers());
            } finally {
                user.followersUnlock();
            }
            return followers;
        } else
            return new LinkedList();

    }

    /**
     * Metodo che notifica al Client se qualcuno ha iniziato a seguirlo
     * @param usernameFollowed
     * @param follower
     * @return true se l'utente è stato seguito, altrimenti false
     */
    public synchronized boolean callbackFollow(String usernameFollowed, String follower) {

        NotificationFollowInterface client = callbackReg.get(usernameFollowed);
        try {
            client.notificationFollowed(follower);
            return true;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    /**
     * Metodo che notifica al Client se qualcuno l'ha smesso di seguire
     * @param usernameUnfollowed
     * @param unfollower
     * @return true se l'utente è stato unfollowato, altrimenti false
     */
    public synchronized boolean callbackUnfollow(String usernameUnfollowed, String unfollower) {
        NotificationFollowInterface client = callbackReg.get(usernameUnfollowed);

        try {
            client.notificationUnfollowed(unfollower);
            return true;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    /*
    *
    * FUNZIONALITA' PRINCIPALI
    *
    *
     */

    /**
     * Metodo per la registrazione di un nuovo utente
     * @param username
     * @param password
     * @param tags
     * @return true se la registrazione è avvenuta, altrimenti false
     * @throws RemoteException
     */
    public boolean register(String username, String password, LinkedList<String> tags) throws RemoteException {
        if (password.length() >= 8){
            try{
                User newUser = new User(username, password, tags);
                return users.putIfAbsent(username, newUser) == null;
            } catch (NoSuchAlgorithmException e) {
                return false;
            }
        }
        else
            throw new IllegalArgumentException();

    }

    /**
     * Metodo per effettuare il login che mi controlla username e password dell'utente
     * @param username
     * @param password
     * @return true se il login è stato effettuato, altrimenti false
     */
    public boolean login(String username, String password) {
        try {
            return (users.containsKey(username) && (users.get(username)).getPassword().equals(password));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Metodo per il logout di un utente di Winsome
     * @param username
     */
    public void logout(String username) {
        User user = users.get(username);
        user.logout();
    }

    /**
     *
     * @param username
     * @return la lista degli utente che hanno almeno un tag in comune con l'utente
     */
    public String listUsers(String username) {
        User user = users.get(username);
        String listUsers = "";
        boolean sametgs = false;
        LinkedList<String> tags = user.getTags();


        for(String s: users.keySet()) {
            if (!s.equals(username)) {// un utente non può seguire se stesso
                User u = users.get(s);
                for (String tag : tags) {
                    if (u.getTags().contains(tag)) {
                        sametgs = true;
                    }
                }
                if (sametgs) {
                    listUsers = listUsers.concat(String.format("%-15s| ", s) + u.printTags(u.getTags()) + "\n");
                }
            }
        }
        return listUsers;
    }

    /**
     *
     * @param username
     * @return la lista degli utenti seguiti dall'utente
     */
    public String listFollowing(String username) {
        User user = users.get(username);
        String listFollowing = "";

        for (String s : user.getFollowed()){
            User u = users.get(s);
            listFollowing = listFollowing.concat(String.format("%-15s| ", s) + u.printTags(u.getTags()) + "\n");
        }

        return listFollowing;
    }

    /**
     * Metodo per seguire un utente su Winsome
     * @param username
     * @param usernameToFollow
     * @return true se l'utente è stato seguito, altrimenti false
     */
    public boolean followUser(String username, String usernameToFollow) {
        User userF = users.get(usernameToFollow);
        User user = users.get(username);
        boolean seguito = true;

        try {
            userF.followersLock();
            //controllo se d
            if (userF.getFollowers().contains(username)) {
                seguito = false;
            } else {
                user.addFollowed(usernameToFollow);
                userF.addFollowers(username);

                // aggiunta di tutti i post del nuovo seguito al feed dell'utente
                for (Post p : userF.getPosts().values()) {
                    user.addPostToFeed(p);
                }

                callbackFollow(usernameToFollow, username);
            }
        } finally {
            userF.followersUnlock();
        }

        return seguito;
    }

    /**
     * Metodo per smettere di seguire un utente
     * @param username
     * @param usernameToUnfollow
     * @return true se l'utente è stato unfollowato, altrimenti false
     */
    public boolean unfollowUser(String username, String usernameToUnfollow) {
        User userUF = users.get(usernameToUnfollow);
        User user = users.get(username);
        boolean unfollow = false;

        try {
            userUF.followersLock();
            // controllo se l'utente segue l'utente che non vuole più seguire
            if (userUF.getFollowers().contains(username)) {

                user.getFollowed().remove(usernameToUnfollow);
                userUF.getFollowers().remove(username);
                // rimuovo tutti i post dell'utente unfollowato
                // tranne per i casi di rewin da parte di altri utenti seguiti
                for (Long idPost : userUF.getPosts().keySet()) {
                    boolean keep = false;
                    for (String u : user.getFollowed()) {
                        User followed = users.get(u);
                        if (followed.getPosts().containsKey(idPost))
                            keep = true;
                    }
                    if (!keep)
                        user.removePostFeed(idPost);
                }

                callbackUnfollow(usernameToUnfollow, username);
                unfollow = true;
            }
        } finally {
            userUF.followersUnlock();
        }
        return unfollow;
    }

    /**
     *
     * @param username
     * @return la lista dei post pubblicati da un utente
     */
    public String viewBlog(String username) {
        User user = users.get(username);
        ConcurrentHashMap<Long, Post> blog = user.getPosts();
        String blogUser = "";

        for (Long key : blog.keySet()){
            blogUser = blogUser
                    .concat(String.format("%-10d| ", key) + String.format("%-15s| ", blog.get(key).getUsername())
                            + blog.get(key).getTitle() + "\n");
        }
        return blogUser;
    }

    /**
     * Metodo per creare un post su Winsome
     * @param username
     * @param title
     * @param content
     * @return  l'id del post se quest'ultimo è stato creato, altrimenti 0
     */
    public long createPost(String username, String title, String content) {
        long idPost = postId.addAndGet(1);
        User user = users.get(username);
        Post p = new Post(idPost, username, title, content);
        if (posts.putIfAbsent(idPost, p) == null) {
            user.addPost(p);

            try {
                user.followersLock();
                for (String f : user.getFollowers()) {
                    users.get(f).addPostToFeed(p);
                }
            } finally {
                user.followersUnlock();
            }

            return idPost;
        }

        return 0;

    }

    /**
     *
     * @param username
     * @return il feed di un determinato utente di Winsome
     */
    public String showFeed(String username) {
        User user = users.get(username);
        ConcurrentHashMap<Long, Post> feed = user.getFeed();
        String feedUser = "";

        for (Long key : feed.keySet()) {
            feedUser = feedUser.concat(String.format("%-10d| ", key) + String.format("%-15s| ", feed.get(key).getUsername())
                            + feed.get(key).getTitle() + "\n");
        }

        return feedUser;
    }

    /**
     *
     * @param idPost
     * @return il post con un determinato id che si vuole visualizzare
     */
    public String showPost(long idPost) {
        String showedPost = "";
        Post post = posts.get(idPost);
        if (post  != null) {
            try {
                post.votesLock();
                post.commentsLock();
                showedPost = "< Title: " + post.getTitle() + "\n< Content: " + post.getContent() +
                        "\n< Votes: \n<    Positive: " + post.positiveVotes() + "\n<    Negative: " + post.negativeVotes() +
                        "\n< Comments: " + post.getN_comments() + "\n" + post.CommentsString();
            } finally {
                post.votesUnlock();
                post.commentsUnlock();
            }

            return showedPost;
        }

        return null;

    }

    /**
     * Metodo per cancellare un post da Winsome
     * @param idPost
     * @param username
     * @return true se il post è stato cancellato, altrimenti false
     */
    public boolean deletePost(long idPost, String username) {
        Post post = posts.get(idPost);
        if (post != null) {
            if(post.getUsername().equals(username)) {
                //rimuovo il post dai post
                posts.remove(idPost);

                User user = users.get(post.getUsername());
                //rimuovo il post dal blog dell'autore
                user.removePost(idPost);
                user.removePostFeed(idPost);

                // rimuovo il post dal feed e dal blog in caso di rewin di tutti i followers
                for (String u : user.getFollowers()) {
                    users.get(u).removePost(idPost);
                    users.get(u).removePostFeed(idPost);
                }

                return true;
            }
        }

        return false;

    }

    /**
     * Metodo per effettuare il rewin di un post
     * @param id
     * @param username
     * @return true se il rewin è andato a buon fine, altrimenti false
     */
    public boolean rewinPost(Long id, String username) {
        Post post = posts.get(id);
        User user = users.get(username);
        if (post  != null){
            // controllo che il post sia nel feed dell'utente che vuole ricondividere e che
            // non sia l'autore
            if (user.getFeed().containsKey(id) && !post.getUsername().equals(username)) {

                // aggiungo il post al blog dell'utente
                user.addPost(post);

                // aggiungo il post al feed dei followers dell'utente che fa il rewin
                User follower;
                try {
                    user.followersLock();
                    for (String u : user.getFollowers()) {
                        follower = users.get(u);
                        follower.addPostToFeed(post);
                    }
                } finally {
                    user.followersUnlock();
                }
                return true;
            }
        }
            return false;
    }

    /**
     * Metodo per votare un post
     * @param id
     * @param vote
     * @param username
     * @return true se il post è stato postato
     */
    public boolean ratePost(Long id, int vote, String username) {
        User user = users.get(username);
        Post post = posts.get(id);
        if (post  != null) {
            if(post.getUsername() != username && user.getFeed().containsKey(id) && !user.getVotes().contains(id)) {
                try {
                    post.votesLock();
                    post.addVote(username, vote);
                } finally {
                    post.votesUnlock();
                }

                //aggiungo l'id del post votato alla lista dei post già votati dall'utente
                user.addVote(id);
                return true;
            }
        }
        return false;

    }

    /**
     * Metodo per l'aggiunta di un commento al post
     * @param username
     * @param id
     * @param comment
     * @return true se il commento è stato aggiunto al post, altrimenti false
     */
    public boolean addComment(String username, Long id, String comment) {
        Post post = posts.get(id);
        User user = users.get(username);
        //controllo che il post non sia nullo
        if(post != null) {
            // controllo se il commentante non è l'autore del post e se ha il post nel proprio feed
            if (post.getUsername() != username && user.getFeed().get(id) != null) {
                try {
                    post.commentsLock();
                    post.addComment(username, comment); //aggiungo il commento al post
                } finally {
                    post.commentsUnlock();
                }
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param username
     * @return l'username dell'utente
     */
    public User getUser(String username) {
        return users.get(username);
    }

    /**
     *
     * @param id
     * @return l'id del post
     */
    public Post getPost(Long id) {
        return posts.get(id);
    }

    /**
     *
     * @return la lista di tutti gli utenti di Winsome
     */
    public ConcurrentHashMap<String, User> getAllUsers() {
        return users;
    }

    /**
     *
     * @return la lista di tutti i post di Winsome
     */
    public ConcurrentHashMap<Long, Post> getAllPosts() {
        return posts;
    }


    /**
     * Metodi per il settaggio degli utenti e post
     * @param users
     */
    public void setAllUsers(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    public void setAllPosts(ConcurrentHashMap<Long, Post> posts) {
        this.posts = posts;
    }

    public void setPostId(Long id) {
        postId.set(id);
    }
}
