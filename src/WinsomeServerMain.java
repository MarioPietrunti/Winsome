import java.io.*;
import java.lang.reflect.Type;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class WinsomeServerMain {

    private static String serverAddress;
    private static String multicastAddress;
    private static String registryHost;
    private static int tcpPort;
    private static int udpPort;
    private static int regPort;
    private static long socketTimeout;
    private static long backupTimeout;
    private static long rewardTimeout;
    private static double authorPercentage;

    public static void main(String[] args) {
        File configurationFileServer =new File(args[0]);
        configServer(configurationFileServer);


        // dichiarazione e creazione dei file per il backup
        File backupUsers = new File("..//FilesBackup//usersBackup.json");
        File backupPosts = new File("..//FilesBackup//postsBackup.json");
        try {
            backupUsers.createNewFile();
        } catch (IOException e1) {
            System.out.println("Ci sono problemi con la creazione del file di backup degli utenti");
            System.exit(-1);
        }
        try {
            backupPosts.createNewFile();
        } catch (IOException e1) {
            System.out.println("Ci sono problemi con la creazione del file di backup dei post");
            System.exit(-1);
        }

        // creazione social network winsome
        WinsomeSocial winsome = new WinsomeSocial();

        // ripristino le informazioni del social se presenti
        try {
            deserializeSocial(winsome, backupUsers, backupPosts);
        } catch (IOException e) {
            System.err.println("Ci sono problemi con il ripristino del backup");
            System.exit(-1);
        }

        // avvio il thread di backup
        FilesBackupTask backupThread = new FilesBackupTask(winsome, backupPosts, backupUsers, backupTimeout);
        backupThread.setDaemon(true);
        backupThread.start();

        // threadpool per gestire richieste dei client
        ExecutorService threadPool = Executors.newCachedThreadPool();

        // configurazione RMI
        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(winsome, 0);
            LocateRegistry.createRegistry(regPort);
            Registry registry = LocateRegistry.getRegistry(regPort);
            registry.rebind(registryHost, stub);
        } catch (RemoteException e) {
            System.err.println("Ci sono problemi con l'RMI");
            System.exit(-1);
        }

        // configurazione connessione multicast
        DatagramSocket socketUDP = null;
        InetAddress multicastAddress = null;
        try {
            // creazione socket per multicast
            multicastAddress = InetAddress.getByName(WinsomeServerMain.multicastAddress);
            socketUDP = new DatagramSocket();
        } catch (IOException e) {
            System.out.println("Ci sono problemi con la creazione della multicast socket");
            System.exit(-1);
        }

        // avvio il thread per il calcolo della ricompensa
        UsersRewardTask rewardThread = new UsersRewardTask(winsome,socketUDP, multicastAddress, udpPort, authorPercentage, rewardTimeout);
        rewardThread.setDaemon(true);
        rewardThread.start();

        // configurazione connessioni tcp
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(tcpPort, 70, InetAddress.getByName(serverAddress));
            System.out.println("Il server e' pronto sulla porta: " + tcpPort);
        } catch (IOException e) {
            System.out.println("Ci sono problemi con la socket del server. Chiusura del server in corso....");
            System.exit(-1);
        }

        // avvio il thread che si occupa della chiusura del server
        ClosureServerTask closerThread = new ClosureServerTask(listener, socketUDP, backupThread, rewardThread,threadPool);
        closerThread.setDaemon(true);
        closerThread.start();

        // Server in ascolto. Attende richieste e le inoltra al threadpool
        while (true) {
            try {
                Socket socket = listener.accept();
                socket.setSoTimeout((int) socketTimeout);

                // invio dati per configurazione multicast
                DataOutputStream outWriter = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                outWriter.writeUTF(udpPort + " " + WinsomeServerMain.multicastAddress);
                outWriter.flush();

                // Gestione del client da parte di un thread del pool
                threadPool.execute(new ExecutorRequest(winsome, socket));
            } catch (IOException e) {
                continue;
            }
        }
    }


    /**
     * Metodo per configurare il server attraverso il file Server.txt
     * @param config_file
     */
    private static void configServer(File config_file) {
        try {
            Scanner scanner = new Scanner(config_file);
            while (scanner.hasNextLine()) {
                try {
                    String line = scanner.nextLine();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] split_line = line.split("=");

                        if (line.startsWith("SERVER ADDRESS"))
                            serverAddress = split_line[1];

                        else if (line.startsWith("TCP PORT"))
                            tcpPort = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("UDP PORT"))
                            udpPort = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("MULTICAST ADDRESS"))
                            multicastAddress = split_line[1];

                        else if (line.startsWith("REGISTRY HOST"))
                            registryHost = split_line[1];

                        else if (line.startsWith("REGISTRY PORT"))
                            regPort = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("SOCKET TIMEOUT"))
                            socketTimeout = Long.parseLong(split_line[1]);

                        else if (line.startsWith("REWARD TIMEOUT"))
                            rewardTimeout = Long.parseLong(split_line[1]);

                        else if (line.startsWith("BACKUP TIMEOUT"))
                            backupTimeout = Long.parseLong(split_line[1]);


                        else if (line.startsWith("AUTHOR PERCENTAGE"))
                            authorPercentage = Double.parseDouble(split_line[1]);

                    }
                } catch (NumberFormatException e) {
                    System.out.println("Ci sono problemi con i parametri!");
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("File di configurazione non trovato!");
        }
    }

    /**
     * Metodo per la deserializzazione del social network
     * @param winsome
     * @param usersBackup
     * @param postsBackup
     * @throws IOException
     */
    private static void deserializeSocial(WinsomeSocial winsome, File usersBackup, File postsBackup)
            throws IOException {
        JsonReader usersReader = new JsonReader(new InputStreamReader(new FileInputStream(usersBackup)));
        JsonReader postsReader = new JsonReader(new InputStreamReader(new FileInputStream(postsBackup)));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (postsBackup.length() > 0) {
            deserializePosts(winsome, postsReader, gson);
        }
        if (usersBackup.length() > 0) {
            deserializeUsers(winsome, usersReader, gson);
        }

    }

    /**
     * Metodo per la deserializzazione degli utenti di Winsome
     * @param winsome
     * @param reader
     * @param gson
     * @throws IOException
     */
    private static void deserializeUsers(WinsomeSocial winsome, JsonReader reader, Gson gson) throws IOException {

        ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
        Type typeOfFollowAndTags = new TypeToken<LinkedList<String>>() {
        }.getType();
        Type typeOfVotes = new TypeToken<LinkedList<Long>>() {
        }.getType();
        Type typeOfBlogAndFeed = new TypeToken<Set<Long>>() {
        }.getType();

        reader.beginArray();

        while (reader.hasNext()) {
            reader.beginObject();
            // parametri utente
            String username = null;
            String password = null;
            LinkedList<String> tags = null;
            LinkedList<String> followers = null;
            LinkedList<String> followed = null;
            LinkedList<Long> votes = null;
            ConcurrentHashMap<Long, Post> blog = new ConcurrentHashMap<>();
            ConcurrentHashMap<Long, Post> feed = new ConcurrentHashMap<>();
            Wallet wallet = null;

            while (reader.hasNext()) {
                String next = reader.nextName();
                if (next.equals("Username"))
                    username = reader.nextString();
                else if (next.equals("Password"))
                    password = reader.nextString();
                else if (next.equals("Tags"))
                    tags = gson.fromJson(reader.nextString(), typeOfFollowAndTags);
                else if (next.equals("Followers"))
                    followers = gson.fromJson(reader.nextString(), typeOfFollowAndTags);
                else if (next.equals("Followed"))
                    followed = gson.fromJson(reader.nextString(), typeOfFollowAndTags);
                else if (next.equals("Post votati"))
                    votes = gson.fromJson(reader.nextString(), typeOfVotes);
                else if (next.equals("Blog")) {
                    Set<Long> set = gson.fromJson(reader.nextString(), typeOfBlogAndFeed);
                    for (Long id : set) {
                        blog.putIfAbsent(id, winsome.getPost(id));
                    }
                } else if (next.equals("Feed")) {
                    Set<Long> set = gson.fromJson(reader.nextString(), typeOfBlogAndFeed);
                    for (Long id : set) {
                        feed.putIfAbsent(id, winsome.getPost(id));
                    }
                } else if (next.equals("Wallet"))
                    wallet = gson.fromJson(reader.nextString(), Wallet.class);
                else
                    reader.skipValue();
            }
            reader.endObject();

            if (username != null) {
                User user = new User(username, password, tags, followers, followed, votes, blog, feed, wallet);
                users.putIfAbsent(username, user);
            }
        }
        reader.endArray();
        reader.close();
        winsome.setAllUsers(users);
    }

    /**
     * Metodo per la deserializzazione dei post del social network
     * @param winsome
     * @param reader
     * @param gson
     * @throws IOException
     */
    private static void deserializePosts(WinsomeSocial winsome, JsonReader reader, Gson gson) throws IOException {
        ConcurrentHashMap<Long, Post> posts = new ConcurrentHashMap<>();
        Type typeOfLikes = new TypeToken<LinkedList<Vote>>() {
        }.getType();
        Type typeOfComments = new TypeToken<LinkedList<Comment>>() {
        }.getType();

        // utilizzata per tenere traccia anche dell'ultimo id utilizzato
        long id = 0;

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            //parametri del post
            String username = null;
            String title = null;
            String content = null;
            int n_iter = 0;
            int n_comments = 0;
            LinkedList<Vote> likes = null;
            LinkedList<Comment> comments = null;
            long lastTimeReward = 0;

            while (reader.hasNext()) {
                String next = reader.nextName();
                if (next.equals("Id"))
                    id = reader.nextLong();
                else if (next.equals("Autore"))
                    username = reader.nextString();
                else if (next.equals("Titolo"))
                    title = reader.nextString();
                else if (next.equals("Contenuto"))
                    content = reader.nextString();
                else if (next.equals("Numero Iterazioni"))
                    n_iter = reader.nextInt();
                else if (next.equals("Numero Commenti"))
                    n_comments = reader.nextInt();
                else if (next.equals("Voti"))
                    likes = gson.fromJson(reader.nextString(), typeOfLikes);
                else if (next.equals("Commenti"))
                    comments = gson.fromJson(reader.nextString(), typeOfComments);
                else if (next.equals("Data dell'ultima ricompensa 1"))
                    lastTimeReward = reader.nextLong();
                else
                    reader.skipValue();
            }
            reader.endObject();
            // controllo dell'idoneità dei valori di base
            if (id != 0 || username != null || title != null || content != null) {
                Post post = new Post(id, username, title, content, n_iter, n_comments, likes, comments, lastTimeReward);
                posts.putIfAbsent(id, post);
            }
        }
        reader.endArray();
        reader.close();
        winsome.setAllPosts(posts);
        winsome.setPostId(id);
    }
}
