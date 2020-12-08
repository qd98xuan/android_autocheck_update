package com.example.autocheckupdate.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModel;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.example.autocheckupdate.R;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import lombok.SneakyThrows;

/**
 * 下载到安装到步骤
 * 1、checkUpdate
 * 2、startToDownload
 * 3、startToInstall
 * 4、
 */
public class Update extends ViewModel {
    private Activity activity;
    private String apkPath = "";
    private Uri apkUri = null;
    private String url;
    private String apkName;
    static int lastProgress = 0;

    public Update(Activity activity, String apkPath, String url, String apkName) {
        this.activity = activity;
        this.apkPath = apkPath;
        this.url = url;
        this.apkName = apkName;
    }

    /**
     * 升级检测入口
     *
     * @param nowVersion 查询到的最新版本
     * @param appVersion 当前app的版本
     * @param callBack   回调接口  成功的标志 错误的信息 下载的进度
     */
    public void checkUpdate(int nowVersion, int appVersion, DownloadMessageCallBack callBack) {
        if (nowVersion > appVersion) {
            startToDownload(callBack);
        }
    }

    @SneakyThrows
    public int getApkVersion() {
        PackageManager pm = activity.getPackageManager();
        PackageInfo pi = null;
        pi = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_CONFIGURATIONS);
        if (pi != null) {
            return pi.versionCode;
        }
        return 0;
    }

    public interface DownloadMessageCallBack {
        void downloadCompleted(String mSinglePath, Activity activity);

        void downloadError(Throwable throwable);

        void downloadProgress(BaseDownloadTask task, long soFarBytes, long totalBytes);
    }

    private AlertDialog progressAlert = null;

    private void startToDownload(final DownloadMessageCallBack callBack) {
        final String mSinglePath = activity.getExternalCacheDir() + "download" + File.separator + apkName + ".apk";
        ViewHolder tag=new ViewHolder();
        File file = new File(mSinglePath);
        if (file.exists()) {
            file.delete();
        }
        FileDownloader.init(activity);
        final BaseDownloadTask downloadTask = FileDownloader.getImpl()
                .create(url)
                .setPath(mSinglePath, false)
                .setCallbackProgressTimes(300)
                .setMinIntervalUpdateSpeed(400)
                .setTag(tag)
                .setListener(new FileDownloadLargeFileListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {

                    }

                    @Override
                    protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
                        Log.i("下载进行", String.valueOf(soFarBytes) + ":" + totalBytes);
                        NumberProgressBar numberProgressBar = progressView
                                .findViewById(R.id.number_progressbar);
                        callBack.downloadProgress(task, soFarBytes, totalBytes);
                        ((ViewHolder) task.getTag()).setProgress(totalBytes, soFarBytes);
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        callBack.downloadCompleted(mSinglePath, activity);
                        startToInstall(mSinglePath, activity);
                        if (progressAlert != null) {
                            progressAlert.dismiss();
                        }
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {

                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        ToastUtils.showToast(activity, e.getMessage(), Toast.LENGTH_SHORT);
                        callBack.downloadError(e);
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        while (task.getSmallFileSoFarBytes() != task.getSmallFileTotalBytes()) {//如果存在了相同的任务，那么就继续下载
                        }
                    }
                });
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                int start = downloadTask.start();
                if (start != -1) emitter.onNext(true);
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            progressAlert = showAlert(activity, "下载中请勿退出！");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ToastUtils.showToast(activity, throwable.getMessage(), Toast.LENGTH_SHORT);
                    }
                });
    }


    private void startToInstall(String path, Activity mContext) {
        apkPath = path;
        File fileApk = new File(path);
        if (!fileApk.exists()) {
            ToastUtils.showToast(mContext, "文件丢失，请重新下载", Toast.LENGTH_SHORT);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // 给目标应用一个临时授权
            apkUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileprovider", fileApk);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 8.0以上
                if (mContext.getPackageManager().canRequestPackageInstalls()) { // 是否有8.0安装权限
                    installAPK(intent, mContext, apkUri);
                } else {
                    Uri finalApkUri = apkUri;
                    startInstallPermissionSettingActivity(intent, finalApkUri, mContext);
                }
            } else {
                installAPK(intent, mContext, apkUri);
            }
        } else {
            apkUri = Uri.fromFile(fileApk);
            installAPK(intent, mContext, apkUri);
        }
    }

    private Intent backupIntent;
    private Uri backupUri;
    private static final String PACKAGE_URL_SCHEME = "package:";
    public static final int REQUEST_CODE_INSTALL_PACKAGES = 1;
    private static final String INSTALL_APK_TYPE = "application/vnd.android.package-archive";

    private void startInstallPermissionSettingActivity(final Intent intent, Uri uri, final Activity mContext) {
        backupIntent = intent;
        backupUri = uri;

        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse(PACKAGE_URL_SCHEME + mContext.getPackageName()));
        mContext.startActivityForResult(i, REQUEST_CODE_INSTALL_PACKAGES);
        showAlert(mContext, "下载完成，准备安装", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                installAPK(intent, mContext, apkUri);
            }
        }).show();
    }

    private void installAPK(Intent intent, Context ctx, Uri apkUri) {
        intent.setDataAndType(apkUri, INSTALL_APK_TYPE);
        ctx.startActivity(intent);
    }

    public AlertDialog showAlert(Context context, String title
            , DialogInterface.OnClickListener positiveButton) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton("确认", positiveButton)
                .setCancelable(false).show();
        return alertDialog;
    }

    static View progressView = null;

    public AlertDialog showAlert(Context context, String title) {
        progressView = LayoutInflater.from(context).inflate(R.layout.update_progress, null, false);
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(progressView)
                .setCancelable(false).show();
        return alertDialog;
    }

    private static class ViewHolder {
        public void setProgress(long totalBytes, long soFarBytes) {
            //设置下载进度
            NumberProgressBar numberProgressBar = progressView
                    .findViewById(R.id.number_progressbar);
                numberProgressBar.incrementProgressBy((int)(Log.i("下载进行", String.valueOf(soFarBytes) + ":" + totalBytes)*4)-lastProgress);
                lastProgress=(int)(Log.i("下载进行", String.valueOf(soFarBytes) + ":" + totalBytes))*4;
//                numberProgressBar.setProgress(Log.i("下载进行", String.valueOf(soFarBytes) + ":" + totalBytes));
//                numberProgressBar.setMax((int)(totalBytes/100));
//                numberProgressBar.setProgress((int)(soFarBytes/100));
        }
    }


}
