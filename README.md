# 自动检测升级升级的框架
## 导入依赖
```
dependencies {
    implementation 'com.github.qd98xuan:android_autocheck_update:1.0'
}
allprojects {
     repositories {
	...
	maven { url 'https://jitpack.io' }
  }
}
````
## 使用方式
```
Update update = new Update(MainActivity.this,apkPath,downloadUrl,apkName);
update.checkUpdate(3, 2, new Update.DownloadMessageCallBack() {
    @Override
    public void downloadCompleted(String mSinglePath, Activity activity) {
        System.out.println("下载完成");
    }

    @Override
    public void downloadError(Throwable throwable) {

    }

    @Override
    public void downloadProgress(com.liulishuo.filedownloader.BaseDownloadTask task, long soFarBytes, long totalBytes) {
        System.out.println(soFarBytes);
        System.out.println(totalBytes);
    }
});
```
