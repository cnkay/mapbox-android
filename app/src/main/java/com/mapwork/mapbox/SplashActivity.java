package com.mapwork.mapbox;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Permissions;
import java.security.acl.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class SplashActivity extends AppCompatActivity {
    private final int PERMISSIONS_REQUEST_CODE = 1;

    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        //Check tree
        checkTree();
    }

    private void checkTree() {
        if (checkGPS())
            if (checkNetwork())
                if (checkPermissions())
                    startApp();
    }

    private boolean checkNetwork() {
        if (isNetworkAvailable(this)) {
            return true;
        } else {
            showDialog("", "Herhangi bir ağa bağlı değilsiniz!", "Ağ Ayarları", (DialogInterface dialog, int which) -> {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);


            }, "Çıkış", (DialogInterface dialog, int which) -> {
                finish();
            }, false);

        }
        return false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        checkTree();
    }


    private void startApp() {
        Intent intent = new Intent(this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private boolean checkPermissions() {

        List<String> permissionsNeed = new ArrayList<>();

        //Granted Permissions

        for (String perm : permissions) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PERMISSION_GRANTED) {
                permissionsNeed.add(perm);
            }
        }

        //Non-granted permissions

        if (!permissionsNeed.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeed.toArray(new String[permissionsNeed.size()]),
                    PERMISSIONS_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }
            //If all permissions granted

            if (deniedCount == 0) {
                startApp();
            } else {
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {

                    String permName = entry.getKey();
                    int permResult = entry.getValue();

                    // Is the permission denied on first time and "never ask again" nor checked
                    // So ask again with permission explain
                    // shouldShowRequestPermissionRationale will return true

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {

                        showDialog("", "MapWork uygulamasının sorunsuz çalışabilmesi için konum bilgisi gerekmektedir", "İzin ver",
                                (DialogInterface dialog, int which) -> {
                                    dialog.dismiss();
                                    checkPermissions();

                                }, "Çıkış", (DialogInterface dialog, int which) -> {

                                    dialog.dismiss();
                                    finish();

                                }, false);
                    }
                    // Permissions are denied (never ask again checked)
                    // shouldShowRequestPermissionRationale will return false
                    else {
                        showDialog("", "Konum izni alınamadı. Uygulama Bilgisi > İzinler yolu ile izinleri düzenleyebilirsiniz.", "Ayarlar",
                                (DialogInterface dialog, int which) -> {
                                        dialog.dismiss();
                                        //App Settings Intent
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                        finish();

                                }, "Uygulamadan Çık", (DialogInterface dialog, int which) -> {
                                        dialog.dismiss();
                                        finish();

                                }, false);
                        break;
                    }
                }
            }
        }


    }

    private AlertDialog showDialog(String title, String msg, String positiveText, DialogInterface.OnClickListener positiveOnClick,
                                   String negativeText, DialogInterface.OnClickListener negativeOnClick, boolean isCancelable) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveText, positiveOnClick);
        builder.setNegativeButton(negativeText, negativeOnClick);
        builder.setCancelable(isCancelable);
        builder.setOnCancelListener((DialogInterface dialog) -> finish());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;


    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isConnected = false;
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            isConnected = (activeNetwork != null) && (activeNetwork.isConnectedOrConnecting());
        }

        return isConnected;
    }

    private boolean checkGPS() {

        if (!isGPSAvailable()) {
            showDialog("GPS", "Lütfen GPS hizmetini açınız", "Ayarlar", (DialogInterface dialog, int which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);

            }, "Çıkış", (DialogInterface dialog, int which) -> { finish(); }, false);
            return false;
        }
        return true;

    }

    private boolean isGPSAvailable() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return false;
        }
        return true;
    }
}


