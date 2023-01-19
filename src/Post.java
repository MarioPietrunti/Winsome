import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Questa classe rappresenta un post del social Winsome
 * con id, unsername dell'utente, titolo, contenuto del post,
 * numero di voti e commenti e le liste dei voti e commenti associati al post
 */
public class Post {
    private final long id;//codice univoco del post
    private final String username;// autore del post
    private final String title;//titolo del post
    private final String content;//contenuto testuale del post
    private int n_iter;//numero di volte che il post è stato valutato
    private int n_comments;//numero di commenti del post
    private final LinkedList<Vote> votes;//voti del post dati dagli altri utenti
    private final LinkedList<Comment> comments;//commenti pubblicati sotto al post
    private long lastReward;//data dell'ultima volta che il post è stato valutato

    //lock sui voti e sui commenti del post
    private final ReentrantLock votesLock;
    private final ReentrantLock commentsLock;


    /**
     * Costruttore per i nuovi post creati
     * @param id
     * @param username
     * @param title
     * @param content
     */
    public Post(long id, String username, String title, String content) {
        this.id = id;
        this.username = username;
        this.title = title;
        this.content = content;
        this.n_iter = 0;
        this.n_comments = 0;
        this.votes = new LinkedList();
        this.comments = new LinkedList();
        this.lastReward = 0;
        this.votesLock = new ReentrantLock();
        this.commentsLock = new ReentrantLock();
    }

    /**
     * Costruttore per ripristinare i post
     * @param id
     * @param username
     * @param title
     * @param content
     * @param n_iter
     * @param n_comments
     * @param votes
     * @param comments
     * @param lastReward
     */
    public Post(long id, String username, String title, String content, int n_iter,
                int n_comments, LinkedList<Vote> votes, LinkedList<Comment> comments, long lastReward) {
        this.id = id;
        this.username = username;
        this.title = title;
        this.content = content;
        this.n_iter = n_iter;
        this.n_comments = n_comments;
        this.votes = votes;
        this.comments = comments;
        this.lastReward = lastReward;
        this.votesLock = new ReentrantLock();
        this.commentsLock = new ReentrantLock();
    }

    /**
     *
     * @return il codice univoco del post
     */
    public long getId() {
        return this.id;
    }

    /**
     *
     * @return l'autore del post
     */
    public String getUsername() {
        return this.username;
    }

    /**
     *
     * @return il titolo del post
     */
    public String getTitle() {
        return this.title;
    }

    /**
     *
     * @return il contenuto testuale del post
     */
    public String getContent() {
        return this.content;
    }

    /**
     *
     * @return il numero di volte che il post è stato valutato
     */
    public int getn_iter() {
        return n_iter;
    }

    /**
     *
     * @return il numero dei commenti pubblicati sotto al post
     */
    public int getN_comments() {
        return this.n_comments;
    }

    /**
     *
     * @return la lista dei voti dati al post
     */
    public List<Vote> getVotes() {
        return this.votes;
    }

    /**
     *
     * @return la lista dei commenti pubblicati sotto al post
     */
    public List<Comment> getComments() {
        return this.comments;
    }

    /**
     *
     * @return il commento con autore e testo del commento come una stringa concatenata
     */
    public String CommentsString() {
        String s = "";

        for(int i = 0; i < this.comments.size(); ++i) {
            String author= comments.get(i).getAuthor();
            String text= comments.get(i).getText();
            s = s.concat("<    " + author + ": " + text + "\n");
        }

        return s;
    }

    /**
     *
     * @return la data dell'ultima volta che è stato ricompensato il post
     */
    public long getLastReward() {
        return this.lastReward;
    }

    /**
     * setta l'ultima data in cui il post è stato valutato per il calcolo delle ricompense
     * @param timestamp
     */
    public void setLastTimeReward(long timestamp) {
        lastReward = timestamp;
    }

    /**
     *
     * @return il numero di voti positivi (likes) ricevuti dal post
     */
    public int positiveVotes() {
        int positivi = 0;
        for(Vote v: votes){
            if(v.getVote() == 1)
                positivi++;
        }
        return positivi;
    }

    /**
     *
     * @return il numero di voti negativi (dislike) ricevuti dal post
     */
    public int negativeVotes() {
        int negativi = 0;
        for(Vote v: votes){
            if(v.getVote() == -1)
                negativi++;
        }
        return negativi;
    }

    /**
     * incrementa il numero di volte che il post è stato valutato
     */
    public void addn_iter() {
        this.n_iter++;
    }

    /**
     * Aggiunge il commento alla lista dei commenti e incrementa il numero dei commenti del post
     * @param username
     * @param text
     */
    public void addComment(String username, String text) {
        Comment comment = new Comment(username, text, System.nanoTime());
        comments.add(comment);
        n_comments++;
    }

    /**
     *
     * @param vote
     * @return true se il voto inserito è ammissibile, altrimenti false
     */
    public static boolean admissibleVote(int vote) {
        return vote == 1 || vote == -1;
    }

    /**
     * Aggiunge il voto al post se quest'ultimo è ammissibile, altrimenti non lo aggiunge
     * @param username autore voto
     * @param vote valore voto
     */
    public void addVote(String username, int vote) {
        Vote v;
        //controllo che il voto sia ammissibile, ovvero vedo se è uguale a 1 oppure -1
        if(admissibleVote(vote)) {
            if (vote == 1)
                v = new Vote(username, 1, System.nanoTime()); //aggiungo voto positivo
            else
                v = new Vote(username, -1, System.nanoTime()); //aggiungo voto negativo

            votes.add(v);
        }
    }

    /**
     * Metodi per le lock dei commenti e dei voti del post
     */
    public void commentsLock() {
        this.commentsLock.lock();
    }

    public void commentsUnlock() {
        this.commentsLock.unlock();
    }

    public void votesLock() {
        this.votesLock.lock();
    }

    public void votesUnlock() {
        this.votesLock.unlock();
    }
}
