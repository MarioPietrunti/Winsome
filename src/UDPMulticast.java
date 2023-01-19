import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class UDPMulticast implements Runnable {

    private final MulticastSocket multicastSocket;
    private boolean logged = false;

    public UDPMulticast(MulticastSocket multicastSocket) {
        this.multicastSocket = multicastSocket;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while(!Thread.currentThread().isInterrupted()) {
            try {
                // si blocca in attesa dell'arrivo di un messaggio
                multicastSocket.receive(packet);
                String msg = new String(packet.getData(), StandardCharsets.UTF_8);
                msg = msg.replace("\u0000", "");
                if (logged) {
                    System.out.println("\n< " + msg);
                    System.out.print("> ");
                }
            }catch (IOException e) {
                System.out.println("Ci sono problemi con il multicast");
                continue;
            }
        }
    }

    public void login() {
        this.logged = true;
    }

    public void logout() {
        this.logged = false;
    }
}
