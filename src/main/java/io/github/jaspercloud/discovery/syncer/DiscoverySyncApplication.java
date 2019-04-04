package io.github.jaspercloud.discovery.syncer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class DiscoverySyncApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(DiscoverySyncApplication.class, args);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
