# XpRoot
XpRoot是一款通过解压APK,动态修改Dex文件,实现注入Xposed模块的应用.  

# 基本原理
![](https://s3.bmp.ovh/imgs/2021/09/ff1ce1531bce91f7.png)

![](https://s3.bmp.ovh/imgs/2021/09/4383d1d1d549fc84.png)

![](https://s3.bmp.ovh/imgs/2021/09/00128e60f63efab6.png)

# 工具使用
## 基本命令
```
java -jar ./ApkRoot.jar -host ./宿主.apk -virus ./xposed模块.apk
```
## debug 命令
修改宿主 Apk 是否变为 debug 模式
```
java -jar ./ApkRoot.jar -host ./宿主.apk -virus ./xposed模块.apk -debug 1
```
## dex 命令
通过直接修改宿主Application注入入口 (可能存在65535问题)
```
java -jar ./ApkRoot.jar -host ./宿主.apk -virus ./xposed模块.apk -dex 1
```

# Thanks
[Xpath](https://github.com/WindySha/Xpatch)