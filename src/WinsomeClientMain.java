import java.io.*;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;


public class WinsomeClientMain {
    private static String serverAddress;
    private static String registryHost;
    private static String multicastAddress;
    private static int multicastPort;
    private static int regPort;
    private static int tcpPort;
    private static long socketTimeout;
    private static boolean logged = false;
    private static String username;
    private static Registry registry;
    private static RemoteInterface remote;
    private static NotificationFollowInterface stub;
    private static NotificationFollowInterface obj;
    private static LinkedList<String> followers;
    private static ReentrantLock followersLock;


    public static void main(String[] args) {
        File configurationFileClient = new File(args[0]);
        clientConfiguration(configurationFileClient);

        followers = new LinkedList();
        followersLock = new ReentrantLock();

        try {
            //configurazione connessione TCP
            Socket socket = new Socket(InetAddress.getByName(serverAddress), tcpPort);
            socket.setSoTimeout((int) socketTimeout);

            //lettura e settaggio dei parametri multicast
            DataInputStream inR = new DataInputStream(socket.getInputStream());
            String[] infoMcast = inR.readUTF().split(" ");
            multicastPort = Integer.parseInt(infoMcast[0]);
            multicastAddress = infoMcast[1];

            //configurazione multicast
            MulticastSocket mcastSocket = new MulticastSocket(multicastPort);
            InetAddress mcastAddress = InetAddress.getByName(multicastAddress);

            // per consentire più legami allo stesso socket
            mcastSocket.setReuseAddress(true);
            mcastSocket.joinGroup(mcastAddress);

            // lancio il thread che sta in ascolto di notifiche di aggiornamento del wallet
            UDPMulticast rewardNotification = new UDPMulticast(mcastSocket);
            Thread rewardNotificationThread = new Thread(rewardNotification);
            rewardNotificationThread.setDaemon(true);
            rewardNotificationThread.start();

            // configurazione RMI: registrazione con metodo remoto
            registry = LocateRegistry.getRegistry(regPort);
            remote = (RemoteInterface)registry.lookup(registryHost);

            // servizio di notifiche per aggiornamento followers
            obj = new NotificationFollow(followers);
            stub = (NotificationFollowInterface)UnicastRemoteObject.exportObject(obj, 0);


            System.out.println("\n BENVENUTO SU WINSOME A REWARDING SOCIAL MEDIA:\n Accedi o crea un nuovo account!");

            //gestione delle richieste
            requests(socket, rewardNotification);
            socket.close();
            System.exit(0);

        } catch (NotBoundException | IOException e) {
            System.out.println("La connessione con il server si e' interrotta!!");
            System.exit(-1);
        }
    }


    /**
     * Metodo per la gestione delle richieste effettuate da linea di comando
     * @param socket
     * @param rewardNotification
     * @return
     * @throws IOException
     */
    public static boolean requests(Socket socket, UDPMulticast rewardNotification) throws IOException {

        Scanner scanner = new Scanner(System.in);

        String[] request = null;//richiesta
        String response = null;//risposta
        String command = null;//comando inserito

        DataOutputStream outW = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        DataInputStream inR = new DataInputStream(socket.getInputStream());

        do {
            System.out.print("> ");
            command = scanner.nextLine();
            request = command.split(" ");

            if (request.length >= 1 && request[0] != "") {

                switch (request[0]){
                    case "register":
                        //controllo il comando inserito
                        if (request.length < 4 || request.length > 8) {
                            System.out.println(
                                    "< Comando non valido. Suggerimento: register <username> <password> <tags> (massimo 5 tags)");
                            break;
                        }
                        LinkedList<String> tags = new LinkedList<>();
                        for (int i = 3; i < request.length; i++) {
                            tags.add(request[i]);
                        }
                        if (!logged)
                            register(request[1], request[2], tags);
                        else
                            System.out.println(
                                    "< Non e' possibile registrare un nuovo utente. Qualcuno sta gia' utilizzando questo dispositivo");
                        break;

                    case "login":
                        // controllo il comando inserito
                        if (request.length != 3) {
                            System.out.println("<Comando non valido. Suggerimento: login <username> <password>");
                            break;
                        }
                        if (!logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);

                            // il login è stato effettuato
                            if (response.startsWith("LOGIN")) {
                                logged = true;
                                rewardNotification.login();
                                username = request[1];
                                followers.clear();
                                followers.addAll(remote.backupFollowers(username));
                                remote.registrationCallback(stub, username);
                            }

                        } else {
                            System.out.println("< Impossibile fare il login, qualcuno e' gia' loggato su questo dispositivo");
                        }
                        break;

                    case "logout":
                        // controllo il comando inserito
                        if (request.length != 1) {
                            System.out.println("< Comando non valido. Suggerimento: logout");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);

                            logged = false;
                            rewardNotification.logout();
                            remote.unregistrationCallback(stub, username);
                            username = null;
                        } else
                            System.out.println("Nessuno e' loggato");
                        break;

                    case "list":
                        // controllo il comando inserito
                        if (request.length != 2 || (!request[1].equals("users") && !request[1].equals("followers") && !request[1].equals("following"))) {
                            System.out
                                    .println(
                                            "<  Comando non valido. Suggerimento: list users oppure list followers oppure list following");
                            break;
                        }

                        if (logged) {
                            if (request[1].equals("followers")) {
                                try {
                                    followersLock.lock();
                                    System.out.println("< " + followers.size() + " followers:");
                                    for (String s : followers) {
                                        System.out.println("<   " + s);
                                    }
                                } finally {
                                    followersLock.unlock();
                                }
                                break;
                            }

                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            System.out.println(String.format("< %-15s| ", "User") + "Tags");
                            System.out.println(
                                    "< -------------------------------------------------------------------------------------------");
                            response = inR.readUTF();
                            int dim = Integer.parseInt(response);

                            for (int i = 0; i < dim; i++) {
                                response = inR.readUTF();
                                System.out.println("< " + response);
                            }
                        } else
                            System.out.println("Nessuno e' loggato");
                        break;

                    case "follow", "unfollow":
                        // controllo il comando inserito
                        if (request[0].equals("follow") && request.length != 2) {
                            System.out.println("< Comando non valido. Suggerimento: follow <username>");
                            break;
                        }
                        if (request[0].equals("unfollow") && request.length != 2) {
                            System.out.println("< Comando non valido. Suggerimento: unfollow <username>");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);
                        } else
                            System.out.println("Nessuno e' loggato");
                        break;



                    case "blog":
                        //controllo il comando inserito
                        if (request.length != 1) {
                            System.out.println("< Comando non valido. Suggerimento: blog");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            System.out.println(
                                    String.format("< %-10s| ", "Id") + String.format("%-15s| ", "Author") + "Title");
                            System.out.println(
                                    "< ----------------------------------------------------------------------------------------------------");
                            response = inR.readUTF();
                            int dim2 = Integer.parseInt(response);

                            for (int i = 0; i < dim2; i++) {
                                response = inR.readUTF();
                                System.out.println("< " + response);
                            }

                        } else
                            System.out.println("Nessuno e' loggato");
                        break;


                    case "post":
                        // ccontrollo il comando inserito
                        if (request.length == 1 || !request[1].startsWith("\"")) {
                            System.out.println("< Comando non valido. Suggerimento: post \"<title>\" \"<content>\"");
                            break;
                        }

                        // ricompongo titolo e testo e conto se ho un numero pari di virgolette
                        String s = request[1];
                        for (int i = 2; i < request.length; i++) {
                            s = s.concat(" " + request[i]);
                        }

                        char tmp;
                        int cont = 0;
                        for (int i = 0; i < s.length(); i++) {
                            tmp = s.charAt(i);
                            if (tmp == '\"') {
                                cont++;
                            }
                        }

                        String info[] = s.split("\"");
                        if (info.length != 4 || cont != 4) {
                            System.out.println("< Comando non valido. Suggerimento: post \"<title>\" \"<content>\"");
                            break;
                        }

                        if (info[1].length() >= 15) {
                            System.out.println("< Titolo troppo lungo. Puoi usare massimo 15 caratteri");
                            break;
                        }

                        if (info[3].length() >= 500) {
                            System.out.println("< Contenuto del post troppo lungo. Puoi usare massimo 500 caratteri");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);
                        } else
                            System.out.println("Nessuno e' loggato");
                        break;

                    case "show":
                        // controllo il comando inserito
                        if (request.length == 1) {
                            System.out.println("< Comando non valido. Suggerimento: show feed or show post <id>");
                            break;
                        }

                        if (((request.length == 2 || request.length > 3) && request[1].equals("post"))
                                || (request.length == 3 && !request[1].equals("post"))) {
                            System.out.println("< Comando non valido. Suggerimento: show post <id>");
                            break;
                        }

                        if ((request.length == 2 && !request[1].equals("feed"))) {
                            System.out.println("< Comando non valido. Suggerimento: show feed");
                            break;
                        }
                        if (request.length > 2 && request[1].equals("feed")) {
                            System.out.println("< Comando non valido. Suggerimento: show feed");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            if (request[1].equals("post")) {
                                // leggo la risposta e la stampo
                                response = inR.readUTF();
                                System.out.print(response);
                            }
                            if (request[1].equals("feed")) {
                                response = inR.readUTF();
                                int dim3 = Integer.parseInt(response);
                                System.out.println(
                                        String.format("< %-10s| ", "Id") + String.format("%-15s| ", "Author") + "Title");
                                System.out.println(
                                        "< ----------------------------------------------------------------------------------------------------");
                                for (int i = 0; i < dim3; i++) {
                                    response = inR.readUTF();
                                    System.out.println("< " + response);
                                }
                            }

                        } else
                            System.out.println("Nessuno e' loggato");
                        break;

                    case "delete":
                        //controllo il comando inserito
                        if (request.length != 2) {
                            System.out.println("< Comando non valido. Suggerimento: delete <idPost>");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);
                        } else
                            System.out.println("Nessuno e' loggato");

                        break;

                    case "rewin":
                        // controllo il comando inserito
                        if (request.length != 2) {
                            System.out.println("< Comando non valido. Suggerimento: rewin <idPost>");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);
                        } else
                            System.out.println("Nessuno e' loggato");
                        break;

                    case "rate":
                        // controllo il comando inserito
                        if (request.length != 3) {
                            System.out.println("< Comando non valido. Suggerimento: rate <idPost> <vote>");
                            break;
                        }
                        if (Integer.parseInt(request[2]) != 1 && Integer.parseInt(request[2]) != -1) {
                            System.out.println("< Il voto deve essere 1 oppure -1");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);
                        } else
                            System.out.println("Nessuno e' loggato");
                        break;

                    case "comment":
                        // controllo il comando inserito
                        if (request.length <= 2 || !request[2].startsWith("\"")) {
                            System.out.println("< Comando non valido. Suggerimento: comment <idPost> \"<comment>\"");
                            break;
                        }
                        String st = request[2];
                        for (int i = 3; i < request.length; i++) {
                            st = st.concat(" " + request[i]);
                        }
                        char temp2;
                        int occ2 = 0;
                        for (int i = 0; i < st.length(); i++) {
                            temp2 = st.charAt(i);
                            if (temp2 == '\"') {
                                occ2++;
                            }
                        }
                        String info2[] = st.split("\"");
                        if (info2.length != 2 || occ2 != 2) {
                            System.out.println("< Comando non valido. Suggerimento: comment <idPost> \"<comment>\"");
                            break;
                        }
                        if (info2[1].length() >= 100) {
                            System.out.println("< Commento troppo lungo. Puoi usare massimo 100 caratteri");
                            break;
                        }

                        if (logged) {
                            // invio la richietsa al server
                            outW.writeUTF(command);
                            outW.flush();

                            // leggo la risposta e la stampo
                            response = inR.readUTF();
                            System.out.println("< " + response);

                        } else
                            System.out.println("Nessuno e' loggato");
                        break;

                    case "wallet":
                        // controllo il comando inserito
                        if (request.length > 2 || (request.length == 2 && !request[1].equals("btc"))) {
                            System.out.println("< Comando non valido. Suggerimento: wallet or wallet btc");
                            break;
                        }

                        if (logged) {
                            // invio la richiesta al server
                            outW.writeUTF(command);
                            outW.flush();

                            // richiesta = wallet
                            if (request.length == 1) {
                                response = inR.readUTF();
                                System.out.println("< " + response);

                                int dim4 = Integer.parseInt(inR.readUTF());
                                for (int i = 0; i < dim4; i++) {
                                    response = inR.readUTF();
                                    System.out.println("< " + response);
                                }
                                break;
                            }

                            // richiesta = wallet btc
                            if (request.length == 2) {
                                response = inR.readUTF();
                                System.out.println("< " + response);
                            }

                        } else
                            System.out.println("Nessuno e' loggato");
                        break;


                    case "help":
                        help();
                        break;

                    case "quit":
                        // controllo il comando inserito
                        if (request.length != 1) {
                            System.out.println("< Comando non valido. Suggerimento: quit");
                            break;
                        }
                        if (logged) {
                            outW.writeUTF(command);
                            outW.flush();
                            inR.close();
                            outW.close();
                        }
                        break;
                }
            }
            else
                {
                System.out.println("< Operazione non valida!");
                help();
            }
        } while(!request[0].equals("quit"));//esco quando viene inserito quit

        scanner.close();
        outW.close();
        inR.close();
        socket.close();
        return true;
    }


    /**
     * Metodo per configurare il client attraverso il file Client.txt
     * @param confile
     */
    public static void clientConfiguration(File confile) {
        try {
            Scanner scanner = new Scanner(confile);

            while(scanner.hasNextLine()) {
                try {
                    String l = scanner.nextLine();
                    if (!l.isEmpty() && !l.startsWith("#")) {
                        String[] split_l = l.split("=");
                        if (l.startsWith("SERVER ADDRESS")) {
                            serverAddress = split_l[1];
                        } else if (l.startsWith("TCP PORT")) {
                            tcpPort = Integer.parseInt(split_l[1]);
                        } else if (l.startsWith("REGISTRY HOST")) {
                            registryHost = split_l[1];
                        } else if (l.startsWith("REG PORT")) {
                            regPort = Integer.parseInt(split_l[1]);
                        } else if (l.startsWith("TIMEOUT SOCKET")) {
                            socketTimeout = Long.parseLong(split_l[1]);
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Analisi errata dei parametri\n");
                }
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("File di configurazione non trovato\n");
        }

    }

    /**
     * Metodo per poter registrare un nuovo utente, che invoca il metodo remoto della RemoteInterface
     * @param username
     * @param password
     * @param tags
     */
    private static void register(String username, String password, LinkedList<String> tags) {
        try {
            remote = (RemoteInterface)registry.lookup(registryHost);
            if (remote.register(username, password, tags))
                System.out.println("Registazione effettuata!");
            else
                System.out.println("Username gia' in uso, scegliere un altro username");

        } catch (RemoteException | NotBoundException e) {
            System.out.println("Registrazione fallita!");
            System.exit(-1);
        } catch (IllegalArgumentException e) {
            System.out.println("La password deve avere una lunghezza maggiore di 8 caratteri!");
        }

    }

    /**
     *Metodo utilizzato per poter stampare la guida dei comandi che l'utente può utilizzare
     */
    private static void help() {
        System.out.println("< Guida :\n" +
                "<      register <username> <password> <tags>\n" +
                "<      login <username> <password>\n" +
                "<      logout\n" +
                "<      list users\n" +
                "<      list followers\n" +
                "<      list following\n" +
                "<      follow <user>\n" +
                "<      unfollow <user>\n" +
                "<      blog\n" +
                "<      post \"<title>\" \"<content>\"\n" +
                "<      show feed\n" +
                "<      show post <idPost>\n" +
                "<      delete <idPost>\n" +
                "<      rewin <idPost>\n" +
                "<      rate <idPost> <vote>\n" +
                "<      comment <idPost> \"<comment>\"\n" +
                "<      wallet\n" +
                "<      wallet btc\n" +
                "<      quit\n" +
                "<      help\n");
    }
}

