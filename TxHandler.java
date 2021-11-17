import java.util.HashSet;
import java.util.ArrayList;

public class TxHandler {

    public UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool =  utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Input> inputs =tx.getInputs();
        ArrayList<Transaction.Output> outputs =tx.getOutputs();
        HashSet<UTXO> used = new HashSet<UTXO>(); // used utxo in this transactions make sure no double spending happens in the transaction
        int idx = 0 ;
        double sum=0 ;
        for(Transaction.Input in:inputs)
        {
            UTXO pointed = new UTXO(in.prevTxHash,in.outputIndex);  
            Transaction.Output out = utxoPool.getTxOutput(pointed);

            if(out==null)
             return false ;
            if(!Crypto.verifySignature(out.address,tx.getRawDataToSign(idx) , in.signature) )//2
               return false ;
            if(in.prevTxHash==null || !utxoPool.contains(pointed) || used.contains(pointed)) // 1,3
                return false ;
            
            used.add(pointed); // prevent double spending ;
            sum+=out.value;
            idx++;
        }
        for(Transaction.Output out:outputs)
        {
            if(out.value<0) //4
             return false ;
            sum-=out.value; 
        }
        if(sum<0) // 5 
            return false ;
        return true ;
        
    }

    void applyTx(Transaction tx)
    {
        ArrayList<Transaction.Input> inputs =tx.getInputs();
        ArrayList<Transaction.Output> outputs =tx.getOutputs();
        int index = 0 ;
        
        for(Transaction.Output out:outputs)
        {
            utxoPool.addUTXO(new UTXO(tx.getHash(), index), out);
            index++;
        }
        

        for(Transaction.Input in:inputs)
        {
            UTXO pointed = new UTXO(in.prevTxHash,in.outputIndex);  
            utxoPool.removeUTXO(pointed);
            
        }
        
            
    }
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> valid = new ArrayList<Transaction>();
        int clean = 0 ;
        while(clean==0){
            clean = 1;
            for(Transaction tx : possibleTxs)
            {
                if(!valid.contains(tx)&& isValidTx(tx))
                {
                    applyTx(tx);
                    valid.add(tx);
                    clean = 0 ;
                }
            }
        }
        Transaction[] arr={};
        if(valid.size()==0)
            return arr;
        return valid.toArray(new Transaction[0]) ;        
    }

}
