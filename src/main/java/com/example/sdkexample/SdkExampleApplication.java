package com.example.sdkexample;

import com.example.sdkexample.example.ExampleUtil;
import io.neow3j.contract.GasToken;
import io.neow3j.crypto.exceptions.CipherException;
import io.neow3j.wallet.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.math.BigInteger;

@SpringBootApplication
public class SdkExampleApplication implements ApplicationRunner {

	@Autowired
	ExampleUtil exampleUtil;
	@Autowired
	Account committeeAccount;
	@Autowired
	Account userAccount;

	public static void main(String[] args) {
		SpringApplication.run(SdkExampleApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws CipherException, IOException {
		String hash = exampleUtil.transferToken(GasToken.SCRIPT_HASH, committeeAccount.getScriptHash(),
				userAccount.getScriptHash(), BigInteger.valueOf(8));
		System.out.println(hash);
	}
}
