package com.example.autocheckupdate.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;

public class CheckPermission {
    public interface PermissionCallback {
        void accept(Boolean b);

        void accept(Throwable e);
    }

    public static void check(Activity activity, PermissionCallback callback, String... permissions) {
        new RxPermissions(activity)
                .request(permissions)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        callback.accept(aBoolean);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        callback.accept(throwable);
                    }
                });

    }

    public static void check(Activity activity, PermissionCallback callback) {
        new RxPermissions(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , Manifest.permission.READ_EXTERNAL_STORAGE
                        , Manifest.permission.ACCESS_WIFI_STATE
                        , Manifest.permission.INTERNET)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        callback.accept(aBoolean);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        callback.accept(throwable);
                    }
                });

    }
}
