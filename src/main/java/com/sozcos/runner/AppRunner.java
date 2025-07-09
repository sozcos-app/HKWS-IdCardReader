package com.sozcos.runner;


import com.sozcos.component.IdCardWebSocketHandler;
import com.sozcos.utils.StableIdCardReader;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppRunner implements ApplicationRunner {

    private final IdCardWebSocketHandler handler;

    public AppRunner(IdCardWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run(ApplicationArguments args) {
        StableIdCardReader.setWebSocketHandler(handler);
        new Thread(() -> new StableIdCardReader().startListening()).start();
    }
}