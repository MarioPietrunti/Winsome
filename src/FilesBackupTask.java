import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import java.io.*;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.Set;

/**
 * Questa classe gestite la serializzazione e il salvataggio degli utenti e dei post di Winsome
 */
public class FilesBackupTask extends Thread {
    private WinsomeSocial winsome;//social
    private final File postbackup;//file di backup dei post
    private final File userbackup;//file di backup degli utenti
    private final long timeout;//timeout per il backup

    public FilesBackupTask(WinsomeSocial winsome, File postbackup, File userbackup, long timeout){
        this.winsome = winsome;
        this.postbackup = postbackup;
        this.userbackup = userbackup;
        this.timeout = timeout;
    }

    public void run() {
        while(true){
            if (!Thread.currentThread().isInterrupted()) {
                try {
                    //il backup viene fatto periodicamente
                    Thread.sleep(timeout);
                    savePosts();
                    saveUsers();
                    continue;
                } catch (InterruptedException e) {
                    break;
                } catch (FileNotFoundException e) {
                    System.out.println("Ci sono problemi con i file di backup");
                } catch (IOException e) {
                    continue;
                }
            }
            return;
        }
    }

    /**
     * Metodo per serializzare i post di Winsome
     * @param post
     * @param backupFile
     * @param gson
     * @param writer
     * @throws IOException
     */
    private void serializePost(Post post, File backupFile, Gson gson, JsonWriter writer) throws IOException {
        writer.beginObject();

        Type typeOfLikes = (new TypeToken<LinkedList<Vote>>() {
        }).getType();
        Type typeOfComments = (new TypeToken<LinkedList<Comment>>() {
        }).getType();

        writer.name("Id").value(post.getId());
        writer.name("Autore").value(post.getUsername());
        writer.name("Titolo").value(post.getTitle());
        writer.name("Contenuto").value(post.getContent());
        writer.name("Numero Iterazioni").value((long)post.getn_iter());
        writer.name("Numero Commenti").value((long)post.getN_comments());

        try {
            post.votesLock();
            writer.name("Voti").value(gson.toJson(post.getVotes(), typeOfLikes));
        } finally {
            post.votesUnlock();
        }

        try {
            post.commentsLock();
            writer.name("Commenti").value(gson.toJson(post.getComments(), typeOfComments));
        } finally {
            post.commentsUnlock();
        }

        writer.name("Data dell'ultima ricompensa").value(post.getLastReward());
        writer.endObject();
    }

    /**
     * Metodo per serializzare gli utenti di Winsome
     * @param user
     * @param backupUsers
     * @param gson
     * @param writer
     * @throws IOException
     */
    private void serializeUser(User user, File backupUsers, Gson gson, JsonWriter writer) throws IOException {
        writer.beginObject();
        Type typeOfFollowAndTags = (new TypeToken<LinkedList<String>>() {
        }).getType();
        Type typeOfVotes = (new TypeToken<LinkedList<Long>>() {
        }).getType();
        Type typeOfBlogAndFeed = (new TypeToken<Set<Long>>() {
        }).getType();
        writer.name("Username").value(user.getUsername());
        writer.name("Password").value(user.getPassword());
        writer.name("Tags").value(gson.toJson(user.getTags(), typeOfFollowAndTags));

        try {
            user.followersLock();
            writer.name("Followers").value(gson.toJson(user.getFollowers(), typeOfFollowAndTags));
        } finally {
            user.followersUnlock();
        }

        writer.name("Followed").value(gson.toJson(user.getFollowed(), typeOfFollowAndTags));
        writer.name("Post votati").value(gson.toJson(user.getVotes(), typeOfVotes));
        writer.name("Blog").value(gson.toJson(user.getPosts().keySet(), typeOfBlogAndFeed));
        writer.name("Feed").value(gson.toJson(user.getFeed().keySet(), typeOfBlogAndFeed));
        writer.name("Wallet").value(gson.toJson(user.getWallet()));

        writer.endObject();
    }

    /**
     * Metodo per salvare i post di Winsome
     * @throws IOException
     */
    public synchronized void savePosts() throws IOException{
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(postbackup)));
        writer.setIndent("      ");
        writer.beginArray();

        for(Long idPost : winsome.getAllPosts().keySet()){
            Post post = winsome.getPost(idPost);
            serializePost(post, postbackup, gson, writer);
        }

        writer.endArray();
        writer.close();
    }


    /**
     * Metodo per salvare gli utenti di Winsome
     * @throws IOException
     */
    public synchronized void saveUsers() throws IOException{
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(userbackup)));

        writer.setIndent("      ");
        writer.beginArray();


        for(String s: winsome.getAllUsers().keySet()){
            User user = winsome.getUser(s);
            serializeUser(user, userbackup, gson, writer);
        }

        writer.endArray();
        writer.close();
    }
}
