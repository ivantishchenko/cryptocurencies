//package com.company;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TxHandler {

    private UTXOPool myUtxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        myUtxoPool = new UTXOPool(utxoPool);
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
        // IMPLEMENT THIS
        /*
        //1
        ArrayList <Transaction.Output> claimedOut = tx.getOutputs();
        ArrayList <Transaction.Input> claimedInp = tx.getInputs();
        ArrayList <UTXO> cacheUTXO = new ArrayList<>();
        int sumInp = 0;
        int sumOut = 0;

        ArrayList<UTXO> allUTXO = myUtxoPool.getAllUTXO();
        for (UTXO utxo: allUTXO) {
            Transaction.Output out = myUtxoPool.getTxOutput(utxo);
            if ( !claimedOut.contains(out) || cacheUTXO.contains(out) ) {
                return false;
            }
        }
        //2

        for ( int i = 0; i < tx.numInputs(); i++ ) {
            Transaction.Input inp = tx.getInput(i);
            UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
            Transaction.Output prevTxOut = myUtxoPool.getTxOutput(utxo);

            PublicKey pubKey = prevTxOut.address;
            byte[] signature = inp.signature;
            byte[] msg = tx.getRawDataToSign(i);

            if ( !Crypto.verifySignature(pubKey, msg, signature) ) {
                return false;
            }
            //3
            cacheUTXO.add(utxo);
            sumInp += prevTxOut.value;

        }


        //4
        for (Transaction.Output out: claimedOut) {
            if ( out.value < 0) {
                return false;
            }
        }

        //5


        for (Transaction.Output out: claimedOut) {
            sumOut += out.value;
        }

        if (sumInp < sumOut) {
            return false;
        }

        return true;
        */
        return inputValid(myUtxoPool, tx) && outputValid(tx) && notNull(tx);

    }

    private boolean notNull(Transaction tx) {
        return tx != null;
    }

    private double inSum = 0, outSum = 0;

    private boolean inputValid(UTXOPool pool, Transaction tx) {
        inSum = 0;
        ArrayList<UTXO> usedTxs = new ArrayList<>();
        // check for rule #1
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            if (input == null) {
                return false;
            }
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (!pool.contains(utxo) || usedTxs.contains(utxo)) {
                return false;
            }
            Transaction.Output prevTxOut = pool.getTxOutput(utxo);

            // verify rule #2
            PublicKey pubKey = prevTxOut.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) {
                return false;
            }

            // rule #3
            // if the signatures are valid, then remove the associated output from the pool
            // by removing the item, we ensure that no UTXO is claimed multiple times
            //this.pool.removeUTXO(utxo);
            usedTxs.add(utxo);

            inSum += prevTxOut.value;
        }

        return true;
    }

    private boolean outputValid(Transaction tx) {
        // check for rule #4
        outSum = 0;
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output out = tx.getOutput(i);
            if (out.value < 0) {
                return false;
            }
            outSum += out.value;
        }

        // check for rule #5
        return inSum >= outSum;
    }


    private void applyTx(Transaction tx) {
        if (tx == null) {
            return;
        }
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            myUtxoPool.removeUTXO(utxo);
        }
        byte[] txHash = tx.getHash();
        int index = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            UTXO utxo = new UTXO(txHash, index);
            index += 1;
            myUtxoPool.addUTXO(utxo, output);
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        return Stream.of(possibleTxs).filter(tx -> {
            if (isValidTx(tx)) {
                IntStream.range(0, tx.getInputs().size()).forEach(i -> myUtxoPool.removeUTXO(new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex)));
                IntStream.range(0, tx.getOutputs().size()).forEach(i -> myUtxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i)));
                return true;
            } else {
                return false;
            }
        }).toArray(Transaction[]::new);
    }




}
