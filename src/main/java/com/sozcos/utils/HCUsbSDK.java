package com.sozcos.utils;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public interface HCUsbSDK extends StdCallLibrary {

    HCUsbSDK INSTANCE = (HCUsbSDK) Native.loadLibrary("lib\\HCUSBSDK", HCUsbSDK.class);

    public static final int MAX_MANUFACTURE_LEN = 32;  //制造商名称最大长度
    public static final int MAX_DEVICE_NAME_LEN = 32;  //设备名称最大长度
    public static final int MAX_SERIAL_NUM_LEN = 48;  //序列号最大长度
    public static final int MAX_USB_DEV_LEN = 64;
    public static final int ERR_LEVEL = 1;
    public static final int DBG_LEVEL = 2;
    public static final int INF_LEVEL = 3;
    public static final int MAX_USERNAME_NAME = 32;  //用户名最大长度
    public static final int MAX_PASSWORD_LEN = 16;  //密码最大长度
    public static final int INVALID_USER_ID = -1;  //登录返回ID,登录失败为-1，有效ID 0-128
    public static final int USB_ERROR_BASE = 0;
    public static final int USB_SUCCESS = (HCUsbSDK.USB_ERROR_BASE + 0);
    public static final int USB_GET_ACTIVATE_CARD = 1004;
    public static final int USB_GET_CERTIFICATE_INFO = 1020;
    public static final int MAX_SERIAL_NUMBER_LEN = 48;
    public static final int MAX_USERNAME_LEN = 32;
    public static final int HPR_MAX_PATH = 260;

    /**
     * < windows MAX_PATH
     */
    public enum LOG_LEVEL_ENUM {
        NONE,                //填补0这个位置，后面1，2，3
        ENUM_ERROR_LEVEL,
        ENUM_DEBUG_LEVEL,
        ENUM_INFO_LEVEL
    }

    ;

    public static class USB_DEVICE_INFO extends Structure {
        public int dwSize;   //结构体大小
        public int dwIndex; // 设备索引
        public int dwVID;   //设备VID
        public int dwPID;   //设备PID
        public byte[] szManufacturer = new byte[MAX_MANUFACTURE_LEN/*32*/];//制造商（来自描述符）
        public byte[] szDeviceName = new byte[MAX_DEVICE_NAME_LEN/*32*/];//设备名称（来自描述符）
        public byte[] szSerialNumber = new byte[MAX_SERIAL_NUMBER_LEN/*48*/];//设备序列号（来自描述符）
        public byte byHaveAudio;    // 是否具有音频
        public byte iColorType;     // 1.RGB路， 2.IR路
        public byte[] szDevicePath = new byte[HPR_MAX_PATH];
        public byte byDeviceType;        //设备类型， 4-音频，5-视频
        public int dwBCD;                //设备软件版本号
        public byte[] byRes = new byte[249];
    }

    public static class USB_USER_LOGIN_INFO extends Structure {
        public int dwSize; //结构体大小
        public int dwTimeout; //登录超时时间（单位：毫秒）
        public int dwDevIndex; //设备下标
        public int dwVID;  //设备VID，枚举设备时得到
        public int dwPID;  //设备PID，枚举设备时得到
        public byte[] szUserName = new byte[MAX_USERNAME_LEN/*32*/]; //用户名，获取激活状态时无需填
        public byte[] szPassword = new byte[MAX_PASSWORD_LEN/*16*/]; //密码，获取激活状态时无需填
        public byte[] szSerialNumber = new byte[MAX_SERIAL_NUMBER_LEN/*48*/]; //设备序列号，枚举设备时得到
        public byte byLoginMode; //0-认证登陆 1-默认账号登陆（不关心用户名密码）（权限不同，部分功能需认证登陆才支持）
        public byte[] byRes2 = new byte[3];
        public int dwFd; //设备描述符fd (android下没有root权限登录时使用)
        public byte[] byRes = new byte[248];
    }


    public static class USB_DEVICE_REG_RES extends Structure {
        public int dwSize;                        //结构体大小
        public byte[] szDeviceName = new byte[MAX_DEVICE_NAME_LEN /*32*/]; //设备名称
        public byte[] szSerialNumber = new byte[MAX_SERIAL_NUMBER_LEN /*48*/]; //设备序列号
        public int dwSoftwareVersion;                //软件版本号,高16位是主版本,低16位是次版本
        public short wYear;
        public byte byMonth;
        public byte byDay;
        public byte byRetryLoginTimes;                            //剩余可尝试登陆的次数
        public byte[] byRes1 = new byte[3];             //保留
        public int dwSurplusLockTime;                            //剩余时间，单位秒 用户锁定时，此参数有效
        public byte[] byRes = new byte[256];
    }


    public static class USB_ACTIVATE_CARD_RES extends Structure {
        public int dwSize;
        public byte byCardType;                        // 卡类型（0-TypeA m1卡，1-TypeA cpu卡,2-TypeB,3-125kHz Id卡,4-Felica Card 5-Desfire Card）
        public byte bySerialLen;                    //卡物理序列号字节长度
        public byte[] bySerial = new byte[10];        //卡物理序列号
        public byte bySelectVerifyLen;                //选择确认长度
        public byte[] bySelectVerify = new byte[3];  //选择确认(3字节)
        public byte[] byRes = new byte[12];
    }

    public static class USB_WAIT_SECOND extends Structure {
        public int dwSize;  //结构体大小
        public byte byWait; //1Byte操作等待时间（0-一直执行直到有卡响应，其他对应1S单位）
        public byte[] byRes = new byte[27];
    }//32字节

    public static class USB_CONFIG_INPUT_INFO extends Structure {
        public Pointer lpCondBuffer;  //指向条件缓冲区
        public int dwCondBufferSize;  //条件缓冲区大小
        public Pointer lpInBuffer;  //指向输出缓冲区
        public int dwInBufferSize;  //输入缓冲区大小
        public byte[] byRes = new byte[48];
    }//64字节

    public static class USB_CONFIG_OUTPUT_INFO extends Structure {
        public Pointer lpOutBuffer;  //指向输出缓冲区
        public int dwOutBufferSize;  //输出缓冲区大小
        public byte[] byRes = new byte[56];
    }//64字节

    public static class USB_CERTIFICATE_INFO extends Structure {
        public int dwSize;  //结构体大小
        public short wWorldInfoSize;  //文字信息长度
        public short wPicInfoSize;    //相片信息长度
        public short wFingerPrintInfoSize; //指纹信息长度
        public byte byCertificateType; //证件类型：0-身份证，1-中国绿卡
        public byte byRes2;
        public byte[] byWordInfo = new byte[256];  //文字信息
        public byte[] byPicInfo = new byte[1024];  //相片信息
        public byte[] byFingerPrintInfo = new byte[1024];  //指纹信息
        public byte[] byRes = new byte[40];
    }

    public static class OUT_USB_DEVICE_INFO extends Structure {
        public USB_DEVICE_INFO[] struDeviceArr;

        public void init(int devCount) {
            struDeviceArr = new USB_DEVICE_INFO[devCount];
            for (int i = 0; i < devCount; i++) {
                struDeviceArr[i] = new USB_DEVICE_INFO();
            }
        }
    }

    boolean USB_Init();

    boolean USB_Cleanup();

    int USB_GetLastError();

    //int USB_GetDeviceCount();
    int USB_GetDeviceCount();

    //新增获取设备列表信息接口，替换原有回调的方式去获取设备信息
    boolean USB_EnumDevices(int dwCount, Pointer pDevInfoList);

    Pointer USB_GetErrorMsg(int dwErrorCode);

    boolean USB_SetLogToFile(int dwLogLevel, String strLogDir, boolean bAutoDel);

    int USB_GetSDKVersion();

    int USB_Login(USB_USER_LOGIN_INFO pUsbLoginInfo, USB_DEVICE_REG_RES pDevRegRes);

    boolean USB_Logout(int lUserID);

    boolean USB_GetDeviceConfig(int lUserID, int dwCommand, USB_CONFIG_INPUT_INFO InputInfo, USB_CONFIG_OUTPUT_INFO OutputInfo);
}
