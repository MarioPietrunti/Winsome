import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Questa classe riguarda la gestione della chiusura del server
 */
public class ClosureServerTask extends Thread {
    private final ServerSocket socketTcp;
    private final DatagramSocket socketUdp;
    private final FilesBackupTask backup;
    private final UsersRewardTask reward;
    private final ExecutorService threadpool;

    public ClosureServerTask(ServerSocket socketTcp, DatagramSocket socketUdp, FilesBackupTask backup, UsersRewardTask reward, ExecutorService threadpool) {
        this.socketTcp = socketTcp;
        this.socketUdp = socketUdp;
        this.backup = backup;
        this.reward = reward;
        this.threadpool = threadpool;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        String line= "";
        while ("chiudi".compareTo(line) != 0 && "chiudi subito".compareTo(line) != 0) {
            System.out.println("Digita 'chiudi' oppure 'chiudi subito' per chiudere il server");
            System.out.print("> ");
            line = scanner.nextLine();
        }

        System.out.println("Chiusura del server in corso...");

        try {
            //chiudo le socket
            socketTcp.close();
            socketUdp.close();
            // interrompo il thread che calcola le ricompense
            reward.interrupt();

            try {
                this.reward.join(1000);
            } catch (InterruptedException e) {
            }
        }catch (IOException e) {
            System.out.println("Ci sono dei problemi con la chiusura della socket");
            System.exit(-1);
        }

        //chiusura immediata
        if (line.equals("chiudi subito")){
            threadpool.shutdownNow();
        }
        else{//chiusura lenta
            try {
                threadpool.shutdown();
                threadpool.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
            }
        }

        //interruzione backup
        backup.interrupt();

        try {
            //salvataggio dei post e degli utenti di Winsome prima della chiusura
            backup.savePosts();
            backup.saveUsers();
            System.out.print("Il server e' chiuso!");
            System.exit(0);
        }catch (IOException e) {
            System.out.println(" Problema con l'ultimo backup!");
            System.exit(-1);
        }
        scanner.close();
    }
}

