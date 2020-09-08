package com.hf.t2mediademo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private static final String[] sPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private static final int PERMISSION_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_camera1_1_3).setOnClickListener(view -> {
            Log.d(TAG, "start Activity Camera1_1_3");
            Intent intent = new Intent(MainActivity.this, Camera1_1_3.class);
            MainActivity.this.startActivity(intent);
        });

        findViewById(R.id.btn_camera2_1_3).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Camera2_1_3.class);
            MainActivity.this.startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // check permission
        ArrayList<String> requestPermission = new ArrayList<>();
        for (String permission : sPermissions) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "permission should be requested: " + permission);
                requestPermission.add(permission);
            }
        }

        // request permission
        if (requestPermission.size() > 0) {
            Log.i(TAG, "not all permission granted, request them.");
            requestPermissions(requestPermission.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            Log.i(TAG, "permission already granted.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean permissionGranted = true;
        for (int i=0; i< grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "permission not granted: " + permissions[i]);
                permissionGranted = false;
                break;
            }
        }

        if (permissionGranted) {
            Log.i(TAG, "permission granted.");
        } else {
            Log.e(TAG, "finish activity due to permission not granted.");
            mHandler.post(this::finish);
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}