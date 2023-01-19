/**
 * Questa classe rappresenta il voto
 * che viene dato ad un post da un determinato utente
 * in una determinata data
 */
public class Vote {
    private final String username; //autore del voto
    private final int vote; //voto
    private final long timestamp;//data del voto


    public Vote(String username, int vote, long timestamp) {
        this.username = username;
        this.vote = vote;
        this.timestamp = timestamp;
    }

    /**
     *
     * @return l'autore del voto
     */
    public String getUsername() {
        return this.username;
    }

    /**
     *
     * @return il voto
     */
    public int getVote() {
        return this.vote;
    }

    /**
     *
     * @return la data del voto
     */
    public long getTimestamp() {
        return this.timestamp;
    }
}