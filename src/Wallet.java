import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


/**
 * Questa classe rappresenta il portafoglio di un utente
 * con l'ammontare totale e la lista delle transazioni
 */
public class Wallet {

    private final String owner; //proprietario del portafoglio
    private final List<Transaction> transactions; //lista delle transazioni del portafoglio
    private double amount;// ammontare del portafoglio


    public Wallet(String owner) {
        this.owner = owner;
        this.transactions = new LinkedList();
        this.amount = 0.0;
    }

    /**
     *
     * @return il proprietario del portafoglio
     */
    public String getOwner() {
        return this.owner;
    }

    /**
     *
     * @return la lista delle transazioni del portafoglio
     */
    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    /**
     *
     * @return la il numero della la lista delle transazioni
     */
    public int getTransactionsSize() {
        return this.transactions.size();
    }

    /**
     *
     * @return l'ammontare totale del portafoglio
     */
    public double getAmount() {
        return this.amount = this.getTotalWallet();
    }

    /**
     *
     * @return il totale della somma delle transazioni
     */
    private double getTotalWallet() {
        double tot = 0.0;

        for(Transaction t : transactions){
            tot+= t.getAmount();
        }
        return tot;
    }

    /**
     *
     * @param reason causale della transazione
     * @param add importo della transazione da aggiungere all'ammontare del portafoglio
     * @return l'ammontare del portafoglio aggiornato e aggiunge la nuova transazione alla lista
     */
    public double walletUpdate(String reason, double add) {
        this.transactions.add(new Transaction(add, reason));
        this.amount += add;
        return this.amount;
    }

    /**
     *
     * @param total ammontare totale portafoglio
     * @return il valore dell'ammontare del portafoglio in bitcoin
     * @throws IOException
     */
    public double toBitcoin(double total) throws IOException {

        URL randomOrg = new URL("https://www.random.org/decimal-fractions/?num=1&dec=10&col=2&format=plain&rnd=new");
        InputStream urlReader = randomOrg.openStream();
        BufferedReader buff = new BufferedReader(new InputStreamReader(urlReader));
        String randomValue = buff.readLine();
        buff.close();
        urlReader.close();

        return Double.parseDouble(randomValue) * total;
    }

}