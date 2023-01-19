/**
 * Questa classe rappresenta la singola transazione effettuata
 * che poi verrà aggiunta nella lista delle transazioni del portafoglio dell'utente
 */
public class Transaction{
    private final double amount; //importo della transazione
    private final String reason; //causale della transazione

    public Transaction(double amount, String reason){
        this.amount = amount;
        this.reason = reason;
    }

    /**
     *
     * @return l'importo della transazione
     */
    public double getAmount(){
        return this.amount;
    }

    /**
     *
     * @return la causale della transazione
     */
    public String getReason(){
        return this.reason;
    }

}