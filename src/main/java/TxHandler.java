import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
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
        return 
            allOutputsClaimedByTxAreInTheCurrentUTXOPool(tx) && 
            theSignaturesOnEachInputOfTxAreValid(tx) && 
            noUtxoIsClaimedMultipleTimesByTx(tx) &&
            allOfTxOutputValuesAreNonNegative(tx) &&
            sumOfTxInputValuesIsGreaterThanOrEqualToTheSumOfItsOutputValues(tx);
    }

    private boolean allOutputsClaimedByTxAreInTheCurrentUTXOPool(Transaction tx) {
        return tx.getInputs().stream().allMatch(input -> {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            return utxoPool.contains(utxo);
        });
	}

    private boolean theSignaturesOnEachInputOfTxAreValid(Transaction tx) {
        for(int inputIndex = 0; inputIndex < tx.getInputs().size(); inputIndex++) {
            Transaction.Input input = tx.getInputs().get(inputIndex);
            PublicKey address = getInputCoinOwner(input);
            if(!Crypto.verifySignature(address, tx.getRawDataToSign(inputIndex), input.signature)) {
                return false;
            }
        }
        return true;
	}

    private boolean noUtxoIsClaimedMultipleTimesByTx(Transaction tx) {
        Set<UTXO> utxoInThisTransaction = new HashSet<UTXO>();
        return tx.getInputs().stream().allMatch(input -> {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            return utxoInThisTransaction.add(utxo);
        });
	}

    private boolean allOfTxOutputValuesAreNonNegative(Transaction tx) {
        return tx.getOutputs().stream().allMatch(output -> {
            return output.value >= 0;
        });
    }
    
    private boolean sumOfTxInputValuesIsGreaterThanOrEqualToTheSumOfItsOutputValues(Transaction tx) {
        double sumOfInputs = sumOfInputs(tx);
        double sumOfOutputs = sumOfOutputs(tx);
        return sumOfInputs >= sumOfOutputs;
    }

    private double sumOfInputs(Transaction tx) {
        return tx.getInputs().stream().mapToDouble(input -> {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            return utxoPool.getTxOutput(utxo).value;
        }).sum();
    }
    
    private double sumOfOutputs(Transaction tx) {
        return tx.getOutputs().stream().mapToDouble(output -> {
            return output.value;
        }).sum();
    }
    
    private PublicKey getInputCoinOwner(Transaction.Input input) {
        Transaction.Output inputCameFromOutput = utxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
		return inputCameFromOutput.address;
	}

	/**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> result = new ArrayList<Transaction>();
        for(Transaction tx : possibleTxs) {
            if(this.isValidTx(tx)) {
                removeConsumedOutputs(tx);
                registerCreatedOutputs(tx);
                result.add(tx);
            }
        }
        return result.toArray(new Transaction[0]);
    }

	private void registerCreatedOutputs(Transaction tx) {
		for(int outputIndex = 0; outputIndex < tx.getOutputs().size(); outputIndex++) {
            UTXO utxo = new UTXO(tx.getHash(), outputIndex);
            utxoPool.addUTXO(utxo, tx.getOutputs().get(outputIndex));                    
        }
	}

	private void removeConsumedOutputs(Transaction tx) {
		tx.getInputs().stream().forEach(input -> {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            utxoPool.removeUTXO(utxo);;
        });
	}

}
