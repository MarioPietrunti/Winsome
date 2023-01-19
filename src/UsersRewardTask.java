import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Questa classe gestiste il calcolo e l'assegnamento delle ricompense
 * agli eventuali autori e curatori dei post di Winsome
 */
public class UsersRewardTask extends Thread {
    private final WinsomeSocial winsome;
    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;
    private final double authorPercent;
    private final long timeout;

    public UsersRewardTask(WinsomeSocial winsome, DatagramSocket socket, InetAddress address, int port, double authorPercent, long timeout) {
        this.winsome = winsome;
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.authorPercent = authorPercent;
        this.timeout = timeout;
    }

    public void run() {
        while(true) {
            if (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(timeout);
                    assignRewards();
                    byte[] buffer = "Le ricompense sono state aggiornate".getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

                    try {
                        this.socket.send(packet);
                    } catch (IOException e) {
                        System.out.println("Il messaggio multicast non è stato inviato");
                    }
                    continue;
                } catch (InterruptedException e) {
                }
            }
            return;
        }
    }

    /**
     * Metodo per il calcolo della ricompensa di un determinato post
     * @param post
     * @param curators
     * @return il totale della ricompensa del post
     */
    private double calculationReward(Post post, Set<String> curators) {
        double reward = 0;
        double sommavoti = 0;
        double sommacommenti = 0;
        double sommaparziale = 0;
        post.addn_iter();
        int n_iter = post.getn_iter();

        try {
            post.votesLock();
            post.commentsLock();

            List<Vote> votes = post.getVotes();
            List<Vote> recentVotes = new LinkedList<>();//lista dei voti
            int votipositivi = 0;
            int votinegativi = 0;

            // seleziono solo i like inseriti dopo la data dell'ultima ricompensa
            for (Vote vote : votes) {
                if (vote.getTimestamp() > post.getLastReward())
                    recentVotes.add(vote);
            }
            for (Vote v : recentVotes) {
                if (v.getVote() == 1) { // voto positivo
                    votipositivi++;
                    curators.add(v.getUsername()); // aggiungo il curatore del like ai curatori
                } else {
                    votinegativi++; // i curatori di voti negativi non ricevono ricompense
                }
            }

            sommavoti = Math.log(Math.max(0, (votipositivi - votinegativi)) + 1);
            List<Comment> comments = post.getComments();
            List<Comment> recentComments = new LinkedList<>();
            Set<String> recentCommentsAuthors = new TreeSet<>();

            //seleziono solo i commenti pubblicati dopo la data dell'ultima ricompensa
            for(Comment comment : comments) {
                if (comment.getTimestamp() > post.getLastReward()) {
                    recentComments.add(comment);
                    recentCommentsAuthors.add(comment.getAuthor());
                    curators.add(comment.getAuthor());
                }
            }

            for (String s : recentCommentsAuthors) {
                int Cp = 0;
                for (Comment comment : recentComments) {
                    if (comment.getAuthor() == s) {
                        Cp++;
                    }
                }
                sommaparziale += 2 / (1 + Math.pow(Math.E, -Cp + 1));
            }
            
            //logaritmo naturale della somma dei commenti
            sommacommenti = Math.log(sommaparziale + 1);
            //divido il totale tra voti e commenti per il numero di iterazioni
            reward = (sommavoti + sommacommenti) / n_iter;
        } finally {
            post.setLastTimeReward(System.nanoTime());
            post.votesUnlock();
            post.commentsUnlock();
        }
        return reward;
    }


    /**
     * Metodo per assegnare le eventuali ricompense ai curatori e agli autori dei post
     */
    private void assignRewards() {
        //prendo tutti i post di Winsome
        ConcurrentHashMap<Long, Post> posts = winsome.getAllPosts();

        for(Long id : posts.keySet()) {
            double reward = 0;

            Post post = posts.get(id);//post
            String author = post.getUsername();//autore del post

            Set<String> curators = new TreeSet();//curatori

            reward += calculationReward(post, curators); //richiamo il metodo che mi calcola la ricompensa del post

            int numCurators = curators.size() == 0 ? 1 : curators.size();
            double authorReward = reward * authorPercent;//calcolo la parte di ricompensa per l'autore del post
            double curatorsReward = reward * (1 - authorPercent) / numCurators; //divido la ricompensa per i curatori in base alla percentuale

            if (curatorsReward > 0) {//assegno la ricompensa ad ogni curatore se quest'ultima è maggiore di 0
                for(String cur : curators) {
                    winsome.getUser(cur).getWallet().walletUpdate("ricompensa per aver votato positivamente il post id: " + post.getId() + "  in data " + new Timestamp(System.currentTimeMillis()).toString(), curatorsReward);
                }
            }

            if (authorReward > 0){//assegno la ricompensa all'autore del post se quest'ultima è maggiore di 0
                winsome.getUser(author).getWallet().walletUpdate(" ricompensa del post id: " + post.getId() + "  pubblicato in data : " + new Timestamp(System.currentTimeMillis()).toString(), authorReward);
            }
        }
    }
}
