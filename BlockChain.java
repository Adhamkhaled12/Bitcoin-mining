import java.util.ArrayList;
import java.util.HashMap;

// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    ArrayList<ArrayList<Block> > blocks;// storing all  blocks at every level
    private HashMap<ByteArrayWrapper, Integer> blockIdx;//storing level of each block 
    private HashMap<ByteArrayWrapper, UTXOPool> blockUtxoPool;//storing level of each block 
    
    int idx = 0 ;
    TransactionPool txPool ;
    
    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        blockIdx = new HashMap<ByteArrayWrapper, Integer>();
        blockUtxoPool= new HashMap<ByteArrayWrapper, UTXOPool>();
        blocks =new ArrayList<ArrayList<Block>>();
        for (int i = 0; i < (CUT_OFF_AGE+1); ++i) 
            blocks.add(new ArrayList<Block>());
        UTXOPool utxoPool = new UTXOPool();
        txPool= new TransactionPool();
        
        ByteArrayWrapper hash = new ByteArrayWrapper(genesisBlock.getHash());
        Transaction coinbase = genesisBlock.getCoinbase();
     
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
        
        blockIdx.put(hash, idx);
        blockUtxoPool.put(hash, utxoPool);
        blocks.get(0).add(genesisBlock);
       
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
              return blocks.get(idx).get(0);
    }
    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        ByteArrayWrapper hash = new ByteArrayWrapper(blocks.get(idx).get(0).getHash());
        UTXOPool temp = blockUtxoPool.get(hash);
        if(temp==null)
            System.out.println("top transaction utxopool is null");
        return temp;
         
        
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - (CUT_OFF_AGE+1))}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to (CUT_OFF_AGE+1) + 1. As soon as
	 * the current blockchain height exceeds (CUT_OFF_AGE+1) + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        
        if(block.getPrevBlockHash()==null) // genesis block 
            return false ;
        
        ByteArrayWrapper parent =  new ByteArrayWrapper(block.getPrevBlockHash());
       
        if(blockIdx.get(parent)==null) //not in memory 
            return false ;
        
        int parentidx = blockIdx.get(parent); 
        UTXOPool currentPool= new UTXOPool(blockUtxoPool.get(parent)); // getting copy of parent pool
        TxHandler handler = new TxHandler(currentPool); 
        
        Transaction[] blockTxs = block.getTransactions().toArray(new Transaction[0]);

        

        if(blockTxs.length!=handler.handleTxs(blockTxs).length)
            return false ;
        
        if(parentidx==idx) //this block will be the maximum height block 
        { 
            // System.out.println("deleting nodes at height "+(idx+1)%(CUT_OFF_AGE+1));
            idx=(idx+1)%(CUT_OFF_AGE+1);
            for(Block b :blocks.get(idx))
            { 
                ByteArrayWrapper curr =  new ByteArrayWrapper(b.getHash());
                // if(b.getPrevBlockHash()==null)
                //  System.out.println("deleting genesis node ");
                blockIdx.remove(curr);
                blockUtxoPool.remove(curr);
            }
            blocks.get(idx).clear();// remove all elements fom memoryk
        }

        ByteArrayWrapper curr= new ByteArrayWrapper(block.getHash());

        blocks.get((parentidx+1)%(CUT_OFF_AGE+1)).add(block);
        blockIdx.put(curr, (parentidx+1)%(CUT_OFF_AGE+1));
        
        
        
        int i =0 ;

        for (Transaction.Output tx:block.getCoinbase().getOutputs())
                currentPool.addUTXO(new UTXO(block.getCoinbase().getHash(), i++), tx);
       
        
                
        blockUtxoPool.put(curr, currentPool);
        return true ;
        
    }
    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }


}