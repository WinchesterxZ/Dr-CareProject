package com.example.drhello.ui.hardware;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.drhello.HardWareService;
import com.example.drhello.R;
import com.example.drhello.Restarter;
import com.example.drhello.ui.chats.StateOfUser;
import com.example.drhello.databinding.ActivityTestHardwareBinding;
import com.example.drhello.ui.chats.GPSTracker;
import com.example.drhello.ui.news.NewsViewModel;
import com.example.drhello.ui.news.Source;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ThrowOnExtraProperties;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class TestHardwareActivity extends AppCompatActivity {

    private ActivityTestHardwareBinding activityTestHardwareBinding;
    private Handler handlerAnimation=new Handler();
    private boolean statusAnimation = false;
    private final int REQUESTPERMISSIONSLOCATION = 10;
    private final int REQUESTPERMISSIONSFINE_LOCATION = 1001;
    Intent mServiceIntent;
    private HardWareService hardWareService;

    private  Runnable runnable = new Runnable() {
        @Override
        public void run() {
            activityTestHardwareBinding.imgAnimation1.animate().scaleX(4f).scaleY(4f).alpha(0f).setDuration(700)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            activityTestHardwareBinding.imgAnimation1.setScaleX( 1f);
                            activityTestHardwareBinding. imgAnimation1.setScaleY(1f);
                            activityTestHardwareBinding.imgAnimation1.setAlpha(1f);
                        }
                    });

            activityTestHardwareBinding.imgAnimation2.animate().scaleX(4f).scaleY(4f).alpha(0f).setDuration(400)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            activityTestHardwareBinding.imgAnimation2.setScaleX( 1f);
                            activityTestHardwareBinding.imgAnimation2.setScaleY(1f);
                            activityTestHardwareBinding.imgAnimation2.setAlpha(1f);
                        }
                    });

            handlerAnimation.postDelayed(this, 1500);
        }
    };

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_hardware);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        activityTestHardwareBinding= DataBindingUtil.setContentView(TestHardwareActivity.this,R.layout.activity_test_hardware);

        //////////////////to get location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(TestHardwareActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(TestHardwareActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(TestHardwareActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled(TestHardwareActivity.this)) {
                    GPSTracker currentLocation = new GPSTracker(getApplicationContext());
                    NewsViewModel newsViewModel;
                    newsViewModel = ViewModelProviders.of(TestHardwareActivity.this).get(NewsViewModel.class);
                    newsViewModel.getWeather(currentLocation.getLatitude(),currentLocation.getLongitude());
                    newsViewModel.weatherMutableLiveData.observe(this, new Observer<Source>() {
                        @Override
                        public void onChanged(Source source) {
                            double temp = Double.parseDouble(source.getWeatherModel().getTemperature());
                            temp = temp - 273.15;
                            activityTestHardwareBinding.txtWeather.setText(Math.round(temp)+"??C");
                        }
                    });
                } else {
                    statusCheck();
                }
                Log.e("checkRunTime : ", "true");


            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUESTPERMISSIONSLOCATION);
            }
        }


        activityTestHardwareBinding.getAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (statusAnimation) {
                    stopPulse();
                }
                else {
                    startPulse();
                }

                hardWareService = new HardWareService();
                mServiceIntent = new Intent(TestHardwareActivity.this, hardWareService.getClass());
                if (!isMyServiceRunning(hardWareService.getClass())) {
                    startService(mServiceIntent);
                }


               /* WorkRequest uploadWorkRequest =
                        new OneTimeWorkRequest.Builder(UploadWorker.class)
                                .build();

                WorkManager.getInstance(TestHardwareActivity.this).enqueue(uploadWorkRequest);


                */

/*
                Constraints constraints = new Constraints.Builder().setRequiresCharging(true)
                        .setRequiredNetworkType(NetworkType.UNMETERED).build();

                final PeriodicWorkRequest periodicWorkRequest1 = new PeriodicWorkRequest.Builder(UploadWorker.class,
                        30, TimeUnit.MILLISECONDS)
                        .setInitialDelay(1,TimeUnit.MILLISECONDS)
                        .setConstraints(constraints).setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .build();


                workManager =  WorkManager.getInstance(TestHardwareActivity.this);
                workManager.enqueue(periodicWorkRequest1);
                workManager.getWorkInfoByIdLiveData(periodicWorkRequest1.getId())
                        .observe(TestHardwareActivity.this, new Observer<WorkInfo>() {
                            @Override
                            public void onChanged(@Nullable WorkInfo workInfo) {
                                if (workInfo != null) {
                                    Log.e("periodicWorkRequest", "Status changed to : " + workInfo.getState());
                                }
                            }
                        });

 */


            }
        });


        activityTestHardwareBinding.txtStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If User Checked 'Don't Show Again' checkbox for runtime permission, then navigate user to Settings
                AlertDialog.Builder dialog = new AlertDialog.Builder(TestHardwareActivity.this);
                dialog.setTitle("Permission Required");
                dialog.setCancelable(false);
                dialog.setMessage("You have to Allow permission to background activity to enable work service in background , " +
                        "can make this by click on button Allow then choose (Dr-Care) after this in ( power consumption activity )," +
                        " please choose -- Allow background activity --  ");
                dialog.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent powerUsageIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                        ResolveInfo resolveInfo = getPackageManager().resolveActivity(powerUsageIntent, 0);
                        if(resolveInfo != null){
                            startActivity(powerUsageIntent);
                        }
                    }
                });
                dialog.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getApplicationContext(), "i am sorry hardware part not work ", Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog alertDialog = dialog.create();
                alertDialog.show();


            }
        });

        activityTestHardwareBinding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        activityTestHardwareBinding.shimmer.startShimmerAnimation();
        activityTestHardwareBinding.shimmerRight.startShimmerAnimation();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Hardware hardware = snapshot.getValue(Hardware.class);
                activityTestHardwareBinding.txtTemp.setText(hardware.getTemperature_C()+" ??C/ "+hardware.getTemperature_F()+"??F");
                activityTestHardwareBinding.txtCo2.setText(hardware.getSPO2()+" %");
                activityTestHardwareBinding.txtHearyRate.setText(hardware.getHeart_Rate()+" pulse/min");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Toast.makeText(getApplicationContext(),"Service status Running",Toast.LENGTH_SHORT).show();
                Log.i ("Service status", "Running");
                return true;
            }
        }
        Toast.makeText(getApplicationContext(),"Service status Not Running",Toast.LENGTH_SHORT).show();
        Log.i ("Service status", "Not running");
        return false;
    }

    private void startPulse() {
        runnable.run();
    }

    private void stopPulse() {
        handlerAnimation.removeCallbacks(runnable);
    }

    public boolean isGPSEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUESTPERMISSIONSLOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(TestHardwareActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(TestHardwareActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(TestHardwareActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED) {
                        Log.e("checkRunTime : ", "true");
                        if (isGPSEnabled(TestHardwareActivity.this)) {
                            GPSTracker currentLocation = new GPSTracker(getApplicationContext());
                            NewsViewModel newsViewModel;
                            newsViewModel = ViewModelProviders.of(TestHardwareActivity.this).get(NewsViewModel.class);
                            newsViewModel.getWeather(currentLocation.getLatitude(),currentLocation.getLongitude());
                            newsViewModel.weatherMutableLiveData.observe(this, new Observer<Source>() {
                                @Override
                                public void onChanged(Source source) {
                                    double temp = Double.parseDouble(source.getWeatherModel().getTemperature());
                                    temp = temp - 273.15;
                                    activityTestHardwareBinding.txtWeather.setText(Math.round(temp)+"??C");
                                }
                            });
                        } else {
                            statusCheck();
                        }
                        return;
                    }
                } else {
                    Log.e("onRequestPermissions : ", "false");
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) TestHardwareActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // If User Checked 'Don't Show Again' checkbox for runtime permission, then navigate user to Settings
                        AlertDialog.Builder dialog = new AlertDialog.Builder(TestHardwareActivity.this);
                        dialog.setTitle("Permission Required");
                        dialog.setCancelable(false);
                        dialog.setMessage("You have to Allow permission to access user location");
                        dialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package",
                                        TestHardwareActivity.this.getPackageName(), null));
                                //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivityForResult(i, REQUESTPERMISSIONSFINE_LOCATION);
                            }
                        });
                        AlertDialog alertDialog = dialog.create();
                        alertDialog.show();
                    }
                    //code for deny
                }

                return;

        }
    }

    public void statusCheck() {
        LocationManager manager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StateOfUser stateOfUser = new StateOfUser();
        stateOfUser.changeState("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        StateOfUser stateOfUser = new StateOfUser();
        stateOfUser.changeState("Offline");
    }

    @Override
    protected void onDestroy() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }
}