import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Questa classe rappresenta l'esecuzione delle richieste ricevute dal Client
 *
 */
public class ExecutorRequest implements Runnable {
    private final WinsomeSocial social;
    private final Socket socket;
    private String clientName;

    public ExecutorRequest(WinsomeSocial social, Socket clientSocket) {
        this.social = social;
        this.socket = clientSocket;
    }

    /**
     * Metodo per la gestione delle richieste ricevute dal Client
     * @param functionality
     * @param outputStream
     * @throws IOException
     */
    private void requestFromClient(String functionality, DataOutputStream outputStream) throws IOException {
        //splitto la richiesta del client in un array di stringhe in modo tale da controllare
        // parametri immessi più facilmente
        String[] request = functionality.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        //dichiaro la risposta del server
        String response = null;

        //controllo il primo parametro della richiesta che corrisponde alla funzionalità da effettuare

        //comando di login
        if (request[0].equals("login")) {
            User user = social.getUser(request[1]);
            if (user != null) {
                if (!user.isLogged()) {
                    if (social.login(request[1], request[2])) {
                        clientName = request[1];
                        response = "LOGIN EFFETTUATO : " + clientName + " e' online!";
                    }
                    else
                        response = "Spiacenti, la tua password non era corretta. Ricontrollala.";
                } else
                    response = "L'utente e' già online su un altro dispositivo";

            } else
                response = "L'utente non esiste su Winsome";


            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando di logout
        if (request[0].equals("logout")) {
            social.logout(clientName);
            clientName = null;
            response = "LOGOUT EFFETTUATO";
            outputStream.writeUTF(response);
            outputStream.flush();
        }


        //comando di list che giù si divide on in users o following
        if (request[0].equals("list")) {
            //comando della lista di utenti con almeno un tag in comune con l'utente che fa la richiesta
            if (request[1].equals("users")) {
                response = social.listUsers(clientName);
                String [] listUsers = response.split("\n");
                Integer len = listUsers.length;

                outputStream.writeUTF(len.toString());
                outputStream.flush();

                for(int i = 0; i < len; ++i) {
                    response = listUsers[i];
                    outputStream.writeUTF(response);
                    outputStream.flush();
                }
            }

            //comando della lista dei seguiti dell'utente
            if (request[1].equals("following")) {
                response = social.listFollowing(clientName);
                String [] listFollowing = response.split("\n");
                Integer len = listFollowing.length;
                outputStream.writeUTF(len.toString());
                outputStream.flush();

                for(int i = 0; i < len; ++i) {
                    response = listFollowing[i];
                    outputStream.writeUTF(response);
                    outputStream.flush();
                }
            }
        }

        //comando di seguire un utente
        if (request[0].equals("follow")) {
            //controllo che l'utente non voglia seguire se stesso
            if (request[1].equals(clientName)) {
                response = "Non puoi seguire te stesso!";
            } else if (social.getUser(request[1]) != null) {
                if (social.followUser(clientName, request[1])) {
                    response = "Hai iniziato a seguire l'utente: " + request[1];
                } else
                    response = "L'utente: " + request[1] + " e' gia' tra i tuoi seguiti";

            } else
                response = "L'utente che vuoi seguire non esiste!";


            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando di smettere di seguire un utente
        if (request[0].equals("unfollow")) {
            //controllo che l'utente non voglia smettere di seguire se stesso
            if (clientName == request[1]) {
                response = "Non puoi smettere di seguire te stesso!";
            } else if (social.getUser(request[1]) != null) { //controllo che l'utente da unfolloware esista nel social
                if (social.unfollowUser(clientName, request[1])) {
                    response = "Hai smesso di seguire l'utente: " + request[1];
                } else
                    response = "Non puoi unfolloware un utente che non segui!";

            } else
                response = "L'utente che vuoi unfolloware non esiste!";


            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando di visualizzare la lista dei post pubblicati dall'utente
        if (request[0].equals("blog")) {
            response = social.viewBlog(clientName);
            String[] blog = response.split("\n");
            Integer len = blog.length;

            outputStream.writeUTF(len.toString());
            outputStream.flush();

            for(int i = 0; i < len; ++i) {
                response = blog[i];
                outputStream.writeUTF(response);
                outputStream.flush();
            }
        }

        //comando per creare un post
        if (request[0].equals("post")) {
            long id;
            if ((id = social.createPost(clientName, request[1], request[2])) > 0) {
                response = "Il post e' stato creato id = " + id;
            } else {
                response = "Il post non e' stato creato!";
            }

            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando di show che giù si divide o show feed o show post
        if (request[0].equals("show")) {
            //comando di show feed
            if (request[1].equals("feed")) {
                response = social.showFeed(clientName);
                String [] feed = response.split("\n");
                Integer len = feed.length;
                outputStream.writeUTF(len.toString());
                outputStream.flush();

                for(int i = 0; i < len; ++i) {
                    response = feed[i];
                    outputStream.writeUTF(response);
                    outputStream.flush();
                }
            }

            //comando di show post
            if (request[1].equals("post")) {
                String post = social.showPost(Long.parseLong(request[2]));
                if (post != null)
                    response = post;
                 else
                    response = "Il post con id: " + request[2] + " non e' stato trovato! \n";


                outputStream.writeUTF(response);
                outputStream.flush();
            }
        }

        //comando per cancellare un determinato post
        Long id;
        if (request[0].equals("delete")) {
            id = Long.parseLong(request[1]);
            if (social.getPost(id) != null) {
                if (social.deletePost(id, clientName)) {
                    response = "Il post con id: " + id + " e' stato cancellato!";
                } else {
                    response = "Non puoi cancellare il post se non ne sei l'autore!";
                }
            } else {
                response = "Il post con id: " + id + " non esiste!";
            }

            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando per fare il rewind di un post
        if (request[0].equals("rewin")) {
            id = Long.parseLong(request[1]);
            if (social.getPost(id) != null) {
                if (social.rewinPost(id, clientName))
                    response = "Rewin del post con id: " + id + " effettuato con successo!";
                 else
                    response = "Non è possibile effettuare il rewin di questo post!";

            }
            else
                response = "Il post con id: " + id + " non esiste!";


            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando per votare un determinato post
        if (request[0].equals("rate")) {
            id = Long.parseLong(request[1]);
            int vote = Integer.parseInt(request[2]);
            if (social.ratePost(id, vote, clientName))
                response = "Il post con id: " + id + " e' stato votato con successo!";
             else
                response = "Non e' possibile votare il post";


            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando per pubblicare un commento sotto ad un post
        if (request[0].equals("comment")) {
            id = Long.parseLong(request[1]);
            if (social.addComment(clientName, id, request[2]))
                response = "Commento pubblicato con successo sotto al post con id: " + id;
            else
                response = "Non e' possibile commentare il post con id: " + id;


            outputStream.writeUTF(response);
            outputStream.flush();
        }

        //comando per visualizzare il portafoglio di un utente
        if (request[0].equals("wallet")) {
            Wallet wallet;
            if (request.length == 2) { //caso wallet btc
                try {
                    wallet = social.getUser(clientName).getWallet();
                    response = "Il totale del portafoglio in bitcoin e' : " + wallet.toBitcoin(wallet.getAmount());
                } catch (IOException e) {
                    response = "Ci sono problemi con il calcolo del totale, riprovare più tardi!";
                }

                outputStream.writeUTF(response);
                outputStream.flush();
            }

            if (request.length == 1) { //caso wallet in wincoin
                wallet = social.getUser(clientName).getWallet();
                response = "Il totale del portafoglio in wincoin e' : " + wallet.getAmount();

                outputStream.writeUTF(response);
                outputStream.flush();

                Integer len = wallet.getTransactionsSize();
                outputStream.writeUTF(len.toString());
                outputStream.flush();

                List<Transaction> transactions = wallet.getTransactions();

                for(Transaction tran: transactions){
                    outputStream.writeUTF(tran.getAmount() + " " + tran.getReason());
                    outputStream.flush();
                }
            }
        }

        //comando per uscire dal client
        if (request[0].equals("quit"))
            social.getUser(clientName).logout();
    }

    public void run() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            while(!Thread.currentThread().isInterrupted()) {
                String request = in.readUTF();
                requestFromClient(request, out);
            }
        } catch (IOException e) {
            try {
                //il server chiude la connessione con il client se non arrivano nuove richieste
                social.getUser(clientName).logout();
                socket.close();
            } catch (NullPointerException e1) {
                try {
                    socket.close();
                } catch (IOException e2) {
                }
            } catch (IOException exception) {
                System.err.println("Problemi con la chiusura della socket del client");
            }
        }

    }
}


