package me.soels.tocairn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ToCairnApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToCairnApplication.class, args);
    }

}
