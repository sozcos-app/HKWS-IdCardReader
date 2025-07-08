# 海康威视

身份证阅读器sdk对接

将 jna.jar 安装到本地maven
```shell
mvn install:install-file -Dfile="xxx\sys_jna_lib\jna.jar" -DgroupId=net.java.dev.jna -DartifactId=jna -Dversion=3.0.9 -Dpackaging=jar
```


打包
```shell
mvn clean compile assembly:single
```

检查jar是否包含 jva.jar
```shell
jar tf target/HKWS-IdCardReader-1.0-jar-with-dependencies.jar | findstr "com/sun/jna"
```
包含的话控制台会输出:

```shell
com/sun/jna/
com/sun/jna/darwin/
com/sun/jna/examples/
```

复制 lib 到 target

运行:

lib必须与 jar 同级

```shell
java -jar .\target\HKWS-IdCardReader-1.0-jar-with-dependencies.jar
```


---

海康威视文档:

1、USBSDK下载地址：https://open.hikvision.com/download/5cda567cf47ae80dd41a54b3?type=10&id=cd699e0e3def4519a6e8676f72e1ea1e 

2、集成流程：https://open.hikvision.com/docs/docId?productId=5cda567cf47ae80dd41a54b3&version=%2Fe1416581fafc44968751b78449dd0a40&tagPath=%E8%AF%BB%E5%8D%A1%E5%99%A8%26%E5%BD%95%E5%85%A5%E4%BB%AA-%E8%8E%B7%E5%8F%96%E8%BA%AB%E4%BB%BD%E8%AF%81%E4%BF%A1%E6%81%AF

---

官方技术支持：

Email: SDK_Support@hikvision.com

填写问题需求单发送到邮件，等待回复