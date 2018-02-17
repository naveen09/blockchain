import java.util.HashSet;
import java.util.Set;

public class TxHandler {

	private UTXOPool utxoPool;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public TxHandler(UTXOPool utxoPool) {
		this.utxoPool = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if: 
	 * (1) all outputs claimed by {@code tx} are in the current.
	 *         UTXO pool, 
	 *(2) the signatures on each input of {@code tx} are
	 *         valid, 
	 *(3) no UTXO is claimed multiple times by {@code tx}, 
	 *(4 all of {@code tx}s output values are non-negative, and 
	 *(5) the sum of {@code tx}s input values is greater than or equal to the
	 *         sum of its output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		double ip = 0.0;
		double op = 0.0;
		UTXOPool allUTXOs = new UTXOPool();
		for (int i = 0; i < tx.getInputs().size(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
			Transaction.Output output = utxoPool.getTxOutput(utxo);

			//3
			if (allUTXOs.contains(utxo))
				return false;
			
			allUTXOs.addUTXO(utxo, output);

			//1
			if (!this.utxoPool.contains(utxo))
				return false;

			//2
			if (in == null
					|| output == null
					|| tx.getRawDataToSign(i) == null
					|| !Crypto.verifySignature(output.address,
							tx.getRawDataToSign(i), in.signature))
				return false;

			ip += output.value;
		}
		for (Transaction.Output out : tx.getOutputs()) {
			if (out.value < 0)
				return false;
			op += out.value;
		}
		//5
		return ip >= op;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness, returning a
	 * mutually valid array of accepted transactions, and updating the current
	 * UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		Set<Transaction> tList = new HashSet<>();
		for (Transaction transaction : possibleTxs) {
			if (isValidTx(transaction))
				tList.add(transaction);
			for (Transaction.Input in : transaction.getInputs()) {
				UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
				utxoPool.removeUTXO(utxo);
			}
			for (int i = 0; i < transaction.numOutputs(); i++) {
				Transaction.Output out = transaction.getOutput(i);
				UTXO utxo = new UTXO(transaction.getHash(), i);
				utxoPool.addUTXO(utxo, out);
			}
		}
		return tList.toArray(new Transaction[tList.size()]);
	}
}
