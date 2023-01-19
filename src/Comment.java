/**
 * Questa classe rappresenta il commento
 * che viene pubblicato sotto un post da un determinato utente
 * in una determinata data
 */
public class Comment {
    private final String author;
    private final String text;
    private final long timestamp;

    public Comment(String author, String text, long timestamp) {
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
    }

    /**
     *
     * @return l'autore del commento
     */
    public String getAuthor() {
        return this.author;
    }

    /**
     *
     * @return il testo del commento
     */
    public String getText() {
        return this.text;
    }

    /**
     *
     * @return la data del commento
     */
    public long getTimestamp() {
        return this.timestamp;
    }
}