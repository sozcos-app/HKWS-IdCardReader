package com.sozcos.utils;


import com.sun.jna.Pointer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class IdCardReader {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "12345";
    private static final int LOGIN_TIMEOUT = 5000; // 5秒
    private static final int POLL_INTERVAL = 1000; // 轮询间隔1秒

    private HCUsbSDK hcUsbSDK = HCUsbSDK.INSTANCE;
    private int loginId = -1;

    private Thread pollingThread;
    private boolean running = false;
    private Consumer<Map<String, String>> callback;

    /**
     * 初始化并开始监听身份证
     *
     * @param callback 当读取到身份证信息时的回调函数
     */
    public void startListening(Consumer<Map<String, String>> callback) {
        this.callback = callback;

        if (!initialize()) {
            throw new RuntimeException("Failed to initialize ID card reader");
        }

        running = true;
        pollingThread = new Thread(this::pollIdCard);
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    /**
     * 停止监听
     */
    public void stopListening() {
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        cleanup();
    }

    private void pollIdCard() {
        while (running) {
            try {
                Map<String, String> idCardInfo = readIdCard();
                if (!idCardInfo.isEmpty() && callback != null) {
                    callback.accept(idCardInfo);
                }
                Thread.sleep(POLL_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error polling ID card: " + e.getMessage());
            }
        }
    }


    /**
     * 初始化SDK并登录设备
     *
     * @return 是否初始化成功
     */
    public boolean initialize() {
        if (!hcUsbSDK.USB_Init()) {
            System.err.println("USB_Init failed");
            return false;
        }

        hcUsbSDK.USB_SetLogToFile(3, "./sdkLog/", false);

        int deviceCount = hcUsbSDK.USB_GetDeviceCount();
        if (deviceCount <= 0) {
            System.err.println("No devices found");
            return false;
        }

        // 获取第一个设备信息
        HCUsbSDK.USB_DEVICE_INFO deviceInfo = getFirstDeviceInfo();
        if (deviceInfo == null) {
            return false;
        }

        // 登录设备
        return loginDevice(deviceInfo);
    }

    /**
     * 读取身份证信息
     *
     * @return 包含身份证信息的Map，key为字段名，value为字段值
     */
    public Map<String, String> readIdCard() {
        Map<String, String> idCardInfo = new HashMap<>();

        if (loginId == -1) {
            System.err.println("Device not logged in");
            return idCardInfo;
        }

        HCUsbSDK.USB_CERTIFICATE_INFO certInfo = new HCUsbSDK.USB_CERTIFICATE_INFO();
        certInfo.dwSize = certInfo.size();

        HCUsbSDK.USB_CONFIG_OUTPUT_INFO configOutput = new HCUsbSDK.USB_CONFIG_OUTPUT_INFO();
        configOutput.dwOutBufferSize = certInfo.dwSize;
        Pointer ptrCertInfo = certInfo.getPointer();
        certInfo.write();
        configOutput.lpOutBuffer = ptrCertInfo;

        if (hcUsbSDK.USB_GetDeviceConfig(loginId, HCUsbSDK.USB_GET_CERTIFICATE_INFO, null, configOutput)) {
            certInfo.read();

            // 解析姓名 (UTF-16LE编码)
            byte[] nameBytes = new byte[30];
            System.arraycopy(certInfo.byWordInfo, 0, nameBytes, 0, 30);
            String name = new String(nameBytes, StandardCharsets.UTF_16LE).trim();
            idCardInfo.put("name", name);

            // 解析身份证号
            byte[] idNumberBytes = new byte[36];
            System.arraycopy(certInfo.byWordInfo, 122, idNumberBytes, 0, 36);
            String idNumber = new String(idNumberBytes, StandardCharsets.UTF_16LE).trim();
            idCardInfo.put("idNumber", idNumber);

            // 可以继续解析其他字段...

        } else {
            System.err.println("Failed to read ID card info, error code: " + hcUsbSDK.USB_GetLastError());
        }

        return idCardInfo;
    }

    /**
     * 释放资源并登出
     */
    public void cleanup() {
        if (loginId != -1) {
            hcUsbSDK.USB_Logout(loginId);
            loginId = -1;
        }
        hcUsbSDK.USB_Cleanup();
    }

    private HCUsbSDK.USB_DEVICE_INFO getFirstDeviceInfo() {
        int deviceCount = hcUsbSDK.USB_GetDeviceCount();
        if (deviceCount <= 0) return null;

        HCUsbSDK.OUT_USB_DEVICE_INFO outDeviceInfo = new HCUsbSDK.OUT_USB_DEVICE_INFO();
        outDeviceInfo.init(deviceCount);
        Pointer pDeviceInfo = outDeviceInfo.getPointer();

        if (!hcUsbSDK.USB_EnumDevices(deviceCount, pDeviceInfo)) {
            System.err.println("USB_EnumDevices failed, error code: " + hcUsbSDK.USB_GetLastError());
            return null;
        }

        outDeviceInfo.read();
        return outDeviceInfo.struDeviceArr[0];
    }

    private boolean loginDevice(HCUsbSDK.USB_DEVICE_INFO deviceInfo) {
        HCUsbSDK.USB_USER_LOGIN_INFO loginInfo = new HCUsbSDK.USB_USER_LOGIN_INFO();
        HCUsbSDK.USB_DEVICE_REG_RES regRes = new HCUsbSDK.USB_DEVICE_REG_RES();

        loginInfo.dwSize = loginInfo.size();
        loginInfo.dwTimeout = LOGIN_TIMEOUT;
        loginInfo.dwVID = deviceInfo.dwVID;
        loginInfo.dwPID = deviceInfo.dwPID;
        loginInfo.szSerialNumber = deviceInfo.szSerialNumber;
        loginInfo.szUserName = DEFAULT_USERNAME.getBytes();
        loginInfo.szPassword = DEFAULT_PASSWORD.getBytes();
        loginInfo.write();

        regRes.dwSize = regRes.size();

        loginId = hcUsbSDK.USB_Login(loginInfo, regRes);
        if (loginId == -1) {
            System.err.println("USB_Login failed, error code: " + hcUsbSDK.USB_GetLastError());
            return false;
        }

        return true;
    }
}