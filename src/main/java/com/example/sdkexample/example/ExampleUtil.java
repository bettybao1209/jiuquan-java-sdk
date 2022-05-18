package com.example.sdkexample.example;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.PolicyContract;
import io.neow3j.contract.SmartContract;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.WIF;
import io.neow3j.crypto.exceptions.CipherException;
import io.neow3j.protocol.Neow3j;
import io.neow3j.script.ScriptBuilder;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

@Log4j2
@Component
public class ExampleUtil {

    @Autowired
    Neow3j neow3j;
    @Autowired
    Account committeeAccount;
    @Autowired
    Account userAccount;

    /**
     * create a wallet and persists the NEP6 wallet to a file
     * @param password
     * @param walletPath
     * @return
     * @throws CipherException
     * @throws IOException
     */
    public Wallet createWallet(String password, String walletPath) throws CipherException, IOException {
        File destinationPath = new File(walletPath);
        Wallet wallet  = Wallet.create(password, destinationPath);
        return wallet;
    }
    /**
     * get private key and public key from an account
     * @return
     * @throws IOException
     */
    public void generateKeyPair() throws IOException, CipherException {

//        String absoluteFileName = "/path/to/your/NEP6.wallet";
//        Wallet w = Wallet.fromNEP6Wallet(absoluteFileName)
//                .name("NewName");

        Wallet wallet = Wallet.create();
        Account account = wallet.getDefaultAccount();
        ECKeyPair ecKeyPair = account.getECKeyPair();

        // get public key
        ECKeyPair.ECPublicKey publicKey = ecKeyPair.getPublicKey();
        String publicKeyString = publicKey.getEncodedCompressedHex();

        // get wif from private key
        ECKeyPair.ECPrivateKey privateKey = ecKeyPair.getPrivateKey();
        String wif = WIF.getWIFFromPrivateKey(privateKey.getBytes());
    }

    /**
     * get address string from a public key
     * @param publicKeyString
     * @return
     */
    public String getAddress(String publicKeyString) {
        ECKeyPair.ECPublicKey publicKey = new ECKeyPair.ECPublicKey(publicKeyString);
        byte[] script = ScriptBuilder.buildVerificationScript((publicKey.getEncoded(true)));
        Hash160 addressHash = Hash160.fromScript(script);
        return addressHash.toAddress();
    }

    /**
     * transfer tokens from one account to another account
     * NOTE: should add the `from` account into the unrestricted account first if it is not one of the admin accounts
     * @param tokenHash
     * @param committee
     * @param user
     * @param amount
     * @return
     */
    public String transferToken(Hash160 tokenHash, Hash160 committee, Hash160 user, BigInteger amount) {
        try {
            // transfer tokens from committee account to user account
            Transaction tx = new FungibleToken(tokenHash, neow3j)
                    .transfer(committeeAccount, user, amount).sign();

            // transfer tokens from user account to committee account (should call unrestrictAccount(user) first)
//            Transaction tx = new FungibleToken(tokenHash, neow3j)
//                    .transfer(userAccount, committee, amount).sign();
            tx.send().throwOnError();
            return Numeric.prependHexPrefix(tx.getTxId().toString());
        } catch (Throwable throwable) {
            log.error("error occurs when transferring tokens", throwable);
        }
        return "";
    }

    /**
     * block a contract or user account
     * @param blockedHash contract hash or user account hash to be blocked
     * @return
     */
    public String blockAccount(Hash160 blockedHash) {
        try {
            Transaction transaction = new SmartContract(PolicyContract.SCRIPT_HASH, neow3j)
                    .invokeFunction("blockAccount", ContractParameter.hash160(blockedHash))
                    .signers(AccountSigner.calledByEntry(committeeAccount)).getUnsignedTransaction();
            // add a signature of the committee
            transaction.addWitness(committeeAccount);
            transaction.send().throwOnError();
            return Numeric.prependHexPrefix(transaction.getTxId().toString());
        } catch (Throwable throwable) {
            log.error("error occurs when blocking a account", throwable);
        }
        return "";
    }

    /**
     * unblock a contract or account
     * @param unblockedHash contract hash or user account hash to be unblocked
     * @return
     */
    public String unblockAccount(Hash160 unblockedHash) {
        try {
            Transaction transaction = new SmartContract(PolicyContract.SCRIPT_HASH, neow3j)
                    .invokeFunction("unblockAccount", ContractParameter.hash160(unblockedHash))
                    .signers(AccountSigner.calledByEntry(committeeAccount)).getUnsignedTransaction();
            // add a signature of the committee
            transaction.addWitness(committeeAccount);
            transaction.send().throwOnError();
            return Numeric.prependHexPrefix(transaction.getTxId().toString());
        } catch (Throwable throwable) {
            log.error("error occurs when unblocking a account", throwable);
        }
        return "";
    }

    /**
     * check if the account is blocked
     * @param accountHash
     * @return
     * @throws IOException
     */
    public boolean isBlocked(Hash160 accountHash) throws IOException {
        return new SmartContract(PolicyContract.SCRIPT_HASH, neow3j)
                .callInvokeFunction("isBlocked", Arrays.asList(ContractParameter.hash160(accountHash))).getInvocationResult()
                .getStack().get(0).getBoolean();
    }

    /**
     * add an account into the unrestrictAccount list to allow transferring tokens
     * @param account
     * @return
     */
    public String unrestrictAccount(Hash160 account) {
        try{
            Transaction tx = new SmartContract(PolicyContract.SCRIPT_HASH, neow3j)
                    .invokeFunction("unrestrictAccount", ContractParameter.hash160(account))
                    .signers(AccountSigner.calledByEntry(committeeAccount))
                    .sign();
            tx.send().throwOnError();
            return Numeric.prependHexPrefix(tx.getTxId().toString());
        } catch (Throwable throwable) {
            log.error("error occurs when unrestricting account", throwable);
        }
        return "";
    }

    /**
     * restrict account to disallow transferring tokens
     * @param account
     * @return
     */
    public String restrictAccount(Hash160 account) {
        try{
            Transaction tx = new SmartContract(PolicyContract.SCRIPT_HASH, neow3j)
                    .invokeFunction("restrictAccount", ContractParameter.hash160(account))
                    .signers(AccountSigner.calledByEntry(committeeAccount))
                    .sign();
            tx.send().throwOnError();
            return Numeric.prependHexPrefix(tx.getTxId().toString());
        } catch (Throwable throwable) {
            log.error("error occurs when restricting account", throwable);
        }
        return "";
    }

    /**
     * check if the account is restriced
     * @param accountHash
     * @return
     * @throws IOException
     */
    public boolean isRestricted(Hash160 accountHash) throws IOException {
        return new SmartContract(PolicyContract.SCRIPT_HASH, neow3j)
                .callInvokeFunction("isBlocked", Arrays.asList(ContractParameter.hash160(accountHash))).getInvocationResult()
                .getStack().get(0).getBoolean();
    }
}
