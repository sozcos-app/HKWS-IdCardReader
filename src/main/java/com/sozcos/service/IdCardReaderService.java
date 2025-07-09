//package com.sozcos.service;
//
//import com.sozcos.component.IdCardWebSocketHandler;
//import com.sozcos.utils.StableIdCardReader;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//
////@Service
//public class IdCardReaderService {
//    private StableIdCardReader reader;
//
//    public void start() {
//        reader = new StableIdCardReader() {
//            @Override
//            protected void handleIdCardInfo(Map<String, String> idCardInfo) {
//                // 通过WebSocket推送信息
//                IdCardWebSocketHandler.broadcastIdCardInfo(idCardInfo);
//            }
//        };
//        reader.startListening();
//    }
//
//    public void stop() {
//        if (reader != null) {
//            reader.stopListening();
//        }
//    }
//}