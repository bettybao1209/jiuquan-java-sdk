package com.example.sdkexample.config;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.wallet.Account;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class Config {

    @Value("${rpc.url}")
    private String url;

    @Value("${wallet.wif.user}")
    private String userWif;


    @Value("${wallet.wif.committee}")
    private String committeeWif;

    @Bean
    public Account userAccount(){
        return Account.fromWIF(userWif);
    }

    @Bean
    public Account committeeAccount(){
        return Account.fromWIF(committeeWif);
    }

    @Bean
    public Neow3j neow3j() {
        return Neow3j.build(new HttpService(url));
    }
}