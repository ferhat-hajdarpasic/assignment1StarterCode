import java.math.BigInteger;
import java.security.*;

public class Main {

   public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        /*
         * Generate key pairs, for Scrooge & Alice
         */
        KeyPair pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_alice   = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        /*
         * Set up the root transaction:
         *
         * Generating a root transaction tx out of thin air, so that Scrooge owns a coin of value 10
         * By thin air I mean that this tx will not be validated, I just need it to get
         * a proper Transaction.Output which I then can put in the UTXOPool, which will be passed
         * to the TXHandler.
         */
        Tx tx = new Tx();
        tx.addOutput(10, pk_scrooge.getPublic());

        // This value has no meaning, but tx.getRawDataToSign(0) will access it in prevTxHash;
        byte[] initialHash = BigInteger.valueOf(0).toByteArray();
        tx.addInput(initialHash, 0);

        tx.signTx(pk_scrooge.getPublic(), pk_scrooge.getPrivate(), 0);

        /*
         * Set up the UTXOPool
         */
        // The transaction output of the root transaction is the initial unspent output.
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(),0);
        utxoPool.addUTXO(utxo, tx.getOutput(0));

        /*  
         * Set up a test Transaction
         */
        Tx tx2 = new Tx();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);

        // I split the coin of value 10 into 3 coins and send all of them for simplicity to
        // the same address (Alice)
        tx2.addOutput(5, pk_alice.getPublic());
        tx2.addOutput(3, pk_alice.getPublic());
        tx2.addOutput(2, pk_alice.getPublic());
        // Note that in the real world fixed-point types would be used for the values, not doubles.
        // Doubles exhibit floating-point rounding errors. This type should be for example BigInteger
        // and denote the smallest coin fractions (Satoshi in Bitcoin).

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Scrooge, therefore I have to sign with the private key from Scrooge
        tx2.signTx(pk_scrooge.getPublic(), pk_scrooge.getPrivate(), 0);
        
        /*
         * Start the test
         */
        // Remember that the utxoPool contains a single unspent Transaction.Output which is
        // the coin from Scrooge.
        TxHandler txHandler = new TxHandler(utxoPool);
        System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
        System.out.println("txHandler.handleTxs(new Transaction[]{tx2}) returns: " +
            txHandler.handleTxs(new Transaction[]{tx2}).length + " transaction(s)");



            Tx tx3 = new Tx();
            
                    // the Transaction.Output of tx at position 0 has a value of 10
                    tx3.addInput(tx2.getHash(), 0);
                    tx3.addInput(tx2.getHash(), 1);
                    tx3.addInput(tx2.getHash(), 2);
                    
                    // I split the coin of value 10 into 3 coins and send all of them for simplicity to
                    // the same address (Alice)
                    tx3.addOutput(10, pk_scrooge.getPublic());
                    // Note that in the real world fixed-point types would be used for the values, not doubles.
                    // Doubles exhibit floating-point rounding errors. This type should be for example BigInteger
                    // and denote the smallest coin fractions (Satoshi in Bitcoin).
            
                    // There is only one (at position 0) Transaction.Input in tx2
                    // and it contains the coin from Scrooge, therefore I have to sign with the private key from Scrooge
                    tx3.signTx(pk_alice.getPublic(), pk_alice.getPrivate(), 0);
                    tx3.signTx(pk_alice.getPublic(), pk_alice.getPrivate(), 1);
                    tx3.signTx(pk_alice.getPublic(), pk_alice.getPrivate(), 2);
                    
                    /*
                     * Start the test
                     */
                    // Remember that the utxoPool contains a single unspent Transaction.Output which is
                    // the coin from Scrooge.
                    System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
                    System.out.println("txHandler.handleTxs(new Transaction[]{tx2}) returns: " +
                        txHandler.handleTxs(new Transaction[]{tx3}).length + " transaction(s)");
            

    }
    public static class Tx extends Transaction { 
        public void signTx(PublicKey pubKey, PrivateKey sk, int input) throws SignatureException {
            byte[] message = this.getRawDataToSign(input);
            Signature sig = null;
            try {
                sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(sk);
                sig.update(message);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
            byte[] signature = sig.sign();
            //boolean success = Crypto.verifySignature(pubKey, message, signature);
            this.addSignature(signature,input);
            // Note that this method is incorrectly named, and should not in fact override the Java
            // object finalize garbage collection related method.
            this.finalize();
        }
    }
}
