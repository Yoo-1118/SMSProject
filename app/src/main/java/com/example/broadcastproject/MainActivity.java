package com.example.broadcastproject;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private Intent intent;
    private RestartService restartService;
    private static final int REQUEST_CODE_PERMISSIONS = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        //권한 확인
        checkPermission();

        /*int permissionChceked_receive = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        int permissionChceked_send = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        if(permissionChceked_send == PackageManager.PERMISSION_GRANTED && permissionChceked_receive == PackageManager.PERMISSION_GRANTED ){
            Toast.makeText(getApplicationContext(), "SMS 송수신 권한 있음.",Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(getApplicationContext(), "SMS 송수신 권한 없음.",Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},1);
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS},1);
        }*/

        initData();

        //Intent intent = new Intent(getApplicationContext(),PersistentService.class);
        //startService(intent);

        // (1) 리시버에 의해 해당 액티비티가 새롭게 실행된 경우
        Intent passedIntent = getIntent();
        processIntent(passedIntent);
    }

    // (2) 이미 실행된 상태였는데 리시버에 의해 다시 켜진 경우
    // (이러한 경우 onCreate()를 거치지 않기 때문에 이렇게 오버라이드 해주어야 모든 경우에 SMS문자가 처리된다!
    @Override
    protected void onNewIntent(Intent intent) {
        Log.v("onNewIntent 진입","onNewIntent");
        processIntent(intent);
        super.onNewIntent(intent);
    }

    private void processIntent(Intent intent){
        if(intent != null){
            Log.v("processIntent 진입","intent not null");
            // 인텐트에서 전달된 데이터를 추출하여, 활용한다.(여기서는 edittext를 통하여 내용을 화면에 뿌려주었다.)
            String msg = intent.getStringExtra("string"); //문자 내용
            String command = intent.getStringExtra("command");
            String name = intent.getStringExtra("name");
            //Log.v("check string",msg);
        }
    }

    //권한 확인
    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPermission() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE,
        };

        Context context = this.getApplicationContext();
        //요청
        ActivityCompat.requestPermissions(this,permissions,REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode ==1 ){
            if (grantResults.length>0) {
                grantResults[0] = PackageManager.PERMISSION_GRANTED;
                Toast.makeText(getApplicationContext(), "SMS 권한을 사용자가 승인함.",Toast.LENGTH_LONG).show();
            } else{
                Toast.makeText(getApplicationContext(), "SMS 권한을 사용자가 거부함.",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(restartService);
    }

    private void initData(){
        //리스타트 서비스 생성
        restartService = new RestartService();
        intent = new Intent(MainActivity.this, PersistentService.class);

        IntentFilter intentFilter = new IntentFilter(".PersistentService");
        //브로드 캐스트에 등록
        registerReceiver(restartService,intentFilter);
        // 서비스 시작
        startService(intent);
    }


}