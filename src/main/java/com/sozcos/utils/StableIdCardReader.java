package com.sozcos.utils;

import com.alibaba.fastjson2.JSONObject;
import com.sozcos.component.IdCardWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class StableIdCardReader {

    private static IdCardWebSocketHandler webSocketHandler;

    public static void setWebSocketHandler(IdCardWebSocketHandler handler) {
        webSocketHandler = handler;
    }

    // 静态初始化确保USB_Init只执行一次
    private static final HCUsbSDK hcUsbSDK = HCUsbSDK.INSTANCE;

    static {
        if (!hcUsbSDK.USB_Init()) {
            throw new RuntimeException("USB_Init 初始化失败");
        }
        hcUsbSDK.USB_SetLogToFile(3, "./sdkLog/", false);
        System.out.println("[INFO] SDK初始化完成");
    }

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "12345";
    private static final int POLL_INTERVAL = 1000;       // 基础轮询间隔1秒
    private static final int READ_COOLDOWN = 4000;       // 读取冷却时间4秒
    private static final int RETRY_INTERVAL = 500;       // 重试间隔500毫秒
    private static final int MAX_RETRY = 3;               // 最大重试次数

    // 身份证字段偏移量和长度
    private static final int NAME_OFFSET = 0;
    private static final int NAME_LENGTH = 30;
    private static final int GENDER_CODE_OFFSET = 30;
    private static final int GENDER_CODE_LENGTH = 2;
    private static final int ETHNIC_CODE_OFFSET = 32;
    private static final int ETHNIC_CODE_LENGTH = 4;
    private static final int BIRTH_DATE_OFFSET = 36;
    private static final int BIRTH_DATE_LENGTH = 16;
    private static final int ADDRESS_OFFSET = 52;
    private static final int ADDRESS_LENGTH = 70;
    private static final int ID_NUMBER_OFFSET = 122;
    private static final int ID_NUMBER_LENGTH = 36;
    private static final int ISSUING_AUTHORITY_OFFSET = 158;
    private static final int ISSUING_AUTHORITY_LENGTH = 30;
    private static final int VALID_FROM_OFFSET = 188;
    private static final int VALID_FROM_LENGTH = 16;
    private static final int VALID_TO_OFFSET = 204;
    private static final int VALID_TO_LENGTH = 16;

    private int loginId = -1;
    private AtomicBoolean running = new AtomicBoolean(false);
    private long lastReadTime = 0;
    private boolean deviceReady = false;

    public static void main(String[] args) {
        StableIdCardReader reader = new StableIdCardReader();
        reader.startListening();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[INFO] 程序退出，释放资源...");
            reader.stopListening();
        }));

        // 保持主线程运行
        try {
            synchronized (reader) {
                while (true) {
                    reader.wait();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void startListening() {
        if (running.get()) {
            return;
        }

        if (!initializeDevice()) {
            System.err.println("[ERROR] 设备初始化失败");
            return;
        }

        running.set(true);
        new Thread(this::stablePolling, "IdCard-Poller").start();
        System.out.println("[INFO] 开始监听身份证读卡器...");
    }

    public synchronized void stopListening() {
        running.set(false);
        if (loginId != -1) {
            hcUsbSDK.USB_Logout(loginId);
            loginId = -1;
        }
        deviceReady = false;
        notifyAll();
    }

    private void stablePolling() {
        while (running.get()) {
            try {
                long now = System.currentTimeMillis();

                if (now - lastReadTime < READ_COOLDOWN) {
                    Thread.sleep(POLL_INTERVAL);
                    continue;
                }

                Map<String, String> idCardInfo = readWithRetry();
                if (!idCardInfo.isEmpty()) {
                    lastReadTime = System.currentTimeMillis();

                    // 通过WebSocket推送信息
                    if (!idCardInfo.isEmpty()) {
                        lastReadTime = System.currentTimeMillis();
                        handleIdCardInfo(idCardInfo); // 改为回调方式
                        Thread.sleep(1000);
                    }
                    Thread.sleep(1000);
                } else {
                    Thread.sleep(POLL_INTERVAL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[ERROR] 轮询异常: " + e.getMessage());
                handleReadError();
            }
        }
    }


    private Map<String, String> readWithRetry() {
        for (int i = 0; i < MAX_RETRY && running.get(); i++) {
            try {
                Map<String, String> info = readIdCard();
                if (!info.isEmpty()) {
                    return info;
                }

                if (i < MAX_RETRY - 1) {
                    Thread.sleep(RETRY_INTERVAL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[WARN] 读取失败(尝试 " + (i + 1) + "/" + MAX_RETRY + "): " + e.getMessage());
            }
        }
        return new HashMap<>();
    }

    private void handleReadError() {
        try {
            // 尝试重新初始化设备
            if (loginId != -1) {
                hcUsbSDK.USB_Logout(loginId);
                loginId = -1;
            }

            Thread.sleep(3000);
            initializeDevice();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized boolean initializeDevice() {
        if (deviceReady) return true;

        try {
            int deviceCount = hcUsbSDK.USB_GetDeviceCount();
            if (deviceCount <= 0) {
                System.err.println("[ERROR] 未检测到设备");
                return false;
            }

            HCUsbSDK.USB_DEVICE_INFO deviceInfo = getFirstDeviceInfo();
            if (deviceInfo == null) {
                return false;
            }

            if (!loginDevice(deviceInfo)) {
                return false;
            }

            deviceReady = true;
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] 设备初始化异常: " + e.getMessage());
            return false;
        }
    }

    private HCUsbSDK.USB_DEVICE_INFO getFirstDeviceInfo() {
        try {
            HCUsbSDK.OUT_USB_DEVICE_INFO outInfo = new HCUsbSDK.OUT_USB_DEVICE_INFO();
            int deviceCount = hcUsbSDK.USB_GetDeviceCount();
            outInfo.init(deviceCount);

            if (!hcUsbSDK.USB_EnumDevices(deviceCount, outInfo.getPointer())) {
                System.err.println("[ERROR] 枚举设备失败: " + hcUsbSDK.USB_GetLastError());
                return null;
            }

            outInfo.read();
            return outInfo.struDeviceArr[0];
        } catch (Exception e) {
            System.err.println("[ERROR] 获取设备信息异常: " + e.getMessage());
            return null;
        }
    }

    private boolean loginDevice(HCUsbSDK.USB_DEVICE_INFO deviceInfo) {
        try {
            HCUsbSDK.USB_USER_LOGIN_INFO loginInfo = new HCUsbSDK.USB_USER_LOGIN_INFO();
            HCUsbSDK.USB_DEVICE_REG_RES regRes = new HCUsbSDK.USB_DEVICE_REG_RES();

            loginInfo.dwSize = loginInfo.size();
            loginInfo.dwTimeout = 5000;
            loginInfo.dwVID = deviceInfo.dwVID;
            loginInfo.dwPID = deviceInfo.dwPID;
            loginInfo.szSerialNumber = deviceInfo.szSerialNumber;
            loginInfo.szUserName = DEFAULT_USERNAME.getBytes();
            loginInfo.szPassword = DEFAULT_PASSWORD.getBytes();
            loginInfo.write();

            regRes.dwSize = regRes.size();

            loginId = hcUsbSDK.USB_Login(loginInfo, regRes);
            if (loginId == -1) {
                System.err.println("[ERROR] 登录失败: " + hcUsbSDK.USB_GetLastError());
                return false;
            }

            System.out.println("[INFO] 设备登录成功");
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] 登录设备异常: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> readIdCard() {
        Map<String, String> result = new HashMap<>();

        if (loginId == -1) {
            return result;
        }

        try {
            HCUsbSDK.USB_CERTIFICATE_INFO certInfo = new HCUsbSDK.USB_CERTIFICATE_INFO();
            certInfo.dwSize = certInfo.size();

            HCUsbSDK.USB_CONFIG_OUTPUT_INFO outputInfo = new HCUsbSDK.USB_CONFIG_OUTPUT_INFO();
            outputInfo.dwOutBufferSize = certInfo.dwSize;
            outputInfo.lpOutBuffer = certInfo.getPointer();
            certInfo.write();

            if (hcUsbSDK.USB_GetDeviceConfig(loginId, HCUsbSDK.USB_GET_CERTIFICATE_INFO, null, outputInfo)) {
                certInfo.read();

                // 检查证件类型 (0-身份证，1-中国绿卡)
                byte certificateType = certInfo.byCertificateType;
                if (certificateType == 0) {
                    parseChineseIdCard(certInfo.byWordInfo, result);
                } else if (certificateType == 1) {
                    parseGreenCard(certInfo.byWordInfo, result);
                } else {
                    System.err.println("[WARN] 未知证件类型: " + certificateType);
                }

                return result;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 读取身份证异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 解析中国居民身份证信息
     */
    private void parseChineseIdCard(byte[] wordInfo, Map<String, String> result) {
        // 姓名 (UTF-16LE)
        result.put("name", parseUtf16String(wordInfo, NAME_OFFSET, NAME_LENGTH));

        // 性别代码 (GB/T 2261.1)
        String genderCode = parseAsciiString(wordInfo, GENDER_CODE_OFFSET, GENDER_CODE_LENGTH);
        result.put("gender", convertGenderCode(genderCode));

        // 民族代码 (GB/T 3304)
        String ethnicCode = parseUtf16String(wordInfo, ETHNIC_CODE_OFFSET, ETHNIC_CODE_LENGTH);
        result.put("ethnic", convertEthnicCode(ethnicCode));

        // 出生日期 (ASCII格式: YYYY MM DD)
        String birthDate = parseUtf16String(wordInfo, BIRTH_DATE_OFFSET, BIRTH_DATE_LENGTH);

        result.put("birthDate", formatDate(birthDate));

        // 住址 (UTF-16LE)
        result.put("address", parseUtf16String(wordInfo, ADDRESS_OFFSET, ADDRESS_LENGTH));

        // 公民身份号码 (UTF-16LE)
        String idNumber = parseUtf16String(wordInfo, ID_NUMBER_OFFSET, ID_NUMBER_LENGTH);
        result.put("idNumber", idNumber);

        // 签发机关 (UTF-16LE)
        result.put("issuingAuthority", parseUtf16String(wordInfo, ISSUING_AUTHORITY_OFFSET, ISSUING_AUTHORITY_LENGTH));

        // 有效期起始日期 (ASCII格式: YYYY MM DD)
        String validFrom = parseUtf16String(wordInfo, VALID_FROM_OFFSET, VALID_FROM_LENGTH);

        result.put("validFrom", formatDate(validFrom));

        // 有效期截止日期 (ASCII格式: YYYY MM DD)
        String validTo = parseUtf16String(wordInfo, VALID_TO_OFFSET, VALID_TO_LENGTH);

        result.put("validTo", formatDate(validTo));

        // 设置证件类型
        result.put("certificateType", "居民身份证");
    }

    /**
     * 解析外国人永久居留身份证(中国绿卡)信息
     */
    private void parseGreenCard(byte[] wordInfo, Map<String, String> result) {
        // 这里实现绿卡解析逻辑，根据文档提供的字段偏移量
        // 由于您主要关注身份证，这里暂不实现，可根据需要补充
        result.put("certificateType", "中国绿卡");
        result.put("error", "绿卡解析功能未实现");
    }

    // 辅助解析方法
    private String parseUtf16String(byte[] data, int offset, int length) {
        byte[] buffer = new byte[length];
        System.arraycopy(data, offset, buffer, 0, length);
        return new String(buffer, StandardCharsets.UTF_16LE).trim();
    }

    private String parseAsciiString(byte[] data, int offset, int length) {
        byte[] buffer = new byte[length];
        System.arraycopy(data, offset, buffer, 0, length);
        return new String(buffer, StandardCharsets.US_ASCII).trim();
    }

    private String convertGenderCode(String code) {
        // GB/T 2261.1 性别代码转换
        switch (code) {
            case "1":
                return "男";
            case "2":
                return "女";
            case "0":
                return "未知";
            case "9":
                return "未说明";
            default:
                return code;
        }
    }

    private String convertEthnicCode(String code) {
        // 这里可以添加GB/T 3304民族代码转换逻辑
        // 简化的示例，实际应用中应该使用完整的民族代码表
        if (code.equals("01")) return "汉族";
        return "民族代码: " + code;
    }

    private String formatDate(String asciiDate) {
        // 将ASCII格式的"YYYY MM DD"转换为"YYYY-MM-DD"
        if (asciiDate.length() >= 8) {
            return asciiDate.replace(" ", "").replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");
        }
        return asciiDate;
    }

    private void printIdCardInfo(Map<String, String> info) {
        System.out.println("\n========= 身份证信息 =========");
        System.out.println("证件类型: " + info.getOrDefault("certificateType", "N/A"));
        System.out.println("姓名: " + info.getOrDefault("name", "N/A"));
        System.out.println("性别: " + info.getOrDefault("gender", "N/A"));
        System.out.println("民族: " + info.getOrDefault("ethnic", "N/A"));
        System.out.println("出生日期: " + info.getOrDefault("birthDate", "N/A"));
        System.out.println("住址: " + info.getOrDefault("address", "N/A"));
        System.out.println("公民身份号码: " + info.getOrDefault("idNumber", "N/A"));
        System.out.println("签发机关: " + info.getOrDefault("issuingAuthority", "N/A"));
        System.out.println("有效期: " + info.getOrDefault("validFrom", "N/A") + " 至 " +
                info.getOrDefault("validTo", "N/A"));
        System.out.println("读取时间: " + new java.util.Date());
        System.out.println("===========================\n");
    }

    // 供子类重写的回调方法
    protected void handleIdCardInfo(Map<String, String> idCardInfo) {
        printIdCardInfo(idCardInfo);
        if (webSocketHandler != null) {
            // 通过 websoket 推送给前端

            String json = new JSONObject(idCardInfo).toString();
            webSocketHandler.sendIdCardInfo(json);
        }
    }
}