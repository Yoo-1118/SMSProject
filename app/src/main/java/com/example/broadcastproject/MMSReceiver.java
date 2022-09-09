package com.example.broadcastproject;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class MMSReceiver extends BroadcastReceiver
{
    private Context _context;

    @Override
    public void onReceive(Context $context, final Intent $intent)
    {
        _context = $context;
        Runnable runn = new Runnable()
        {
            @Override
            public void run()
            {
                parseMMS();
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(runn, 6000); // 시간이 너무 짧으면 못 가져오는게 있더라
    }

    private void parseMMS()
    {
        ContentResolver contentResolver = _context.getContentResolver();
        final String[] projection = new String[] { "_id" };
        Uri uri = Uri.parse("content://mms");
        Cursor cursor = contentResolver.query(uri, projection, null, null, "_id desc limit 1");

        if (cursor.getCount() == 0)
        {
            cursor.close();
            return;
        }

        cursor.moveToFirst();
        String id = cursor.getString(cursor.getColumnIndex("_id"));
        cursor.close();

        String number = parseNumber(id);
        String msg = parseMessage(id);
        Log.i("MMSReceiver.java | parseMMS", "|" + number + "|" + msg);

        if(msg.contains("당일예약")){

            // 수신 날짜/시간 데이터 추출

            String[] receivedMessage = msg.split("\n");

            for(int i=0; i<receivedMessage.length; i++){
                Log.d(TAG, "receivedMessage["+i+"] : "+receivedMessage[i]);
            }

            //문자메세지 추출값
            String name = receivedMessage[2];
            String type = receivedMessage[5];
            String checkIn = receivedMessage[9];
            String checkOut = receivedMessage[10];

            //사용자 지정 필드 임시값
            int couponPrice = 8000;
            String url = "https://is.gd/qTIImC";
            int year = 2021;
            int month = 4;
            int day = 7;

            String mainInfo = String.format(_context.getResources().getString(R.string.opening_info),name); //업소명
            String typeInfo = String.format(_context.getResources().getString(R.string.type_info),type); //객실타입
            String checkInInfo = String.format(_context.getResources().getString(R.string.checkIn_info),checkIn); //입실일자
            String checkOutInfo = String.format(_context.getResources().getString(R.string.checkOut_info),checkOut); //퇴실일자
            String couponInfo = String.format(_context.getResources().getString(R.string.coupon_info),name,couponPrice,url,year,month,day);//업소명,쿠폰가격,사이트,쿠폰기한
            String closingInfo = String.format(_context.getResources().getString(R.string.closing_info),name);//업소명

            String infoMessage = mainInfo + "\n\n" + typeInfo + "\n" + checkInInfo + "\n" + checkOutInfo + "\n\n" + couponInfo + "\n\n" + closingInfo;
            Log.d(TAG, "mainInfo: "+infoMessage);

            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(infoMessage);
            sms.sendMultipartTextMessage(number, null, parts, null, null);
        }
    }

    private String parseNumber(String $id)
    {
        String result = null;

        Uri uri = Uri.parse(MessageFormat.format("content://mms/{0}/addr", $id));
        String[] projection = new String[] { "address" };
        String selection = "msg_id = ? and type = 137";// type=137은 발신자
        String[] selectionArgs = new String[] { $id };

        Cursor cursor = _context.getContentResolver().query(uri, projection, selection, selectionArgs, "_id asc limit 1");

        if (cursor.getCount() == 0)
        {
            cursor.close();
            return result;
        }

        cursor.moveToFirst();
        result = cursor.getString(cursor.getColumnIndex("address"));
        cursor.close();

        return result;
    }

    private String parseMessage(String $id)
    {
        String result = null;

        // 조회에 조건을 넣게되면 가장 마지막 한두개의 mms를 가져오지 않는다.
        Cursor cursor = _context.getContentResolver().query(Uri.parse("content://mms/part"), new String[] { "mid", "_id", "ct", "_data", "text" }, null, null, null);

        Log.i("MMSReceiver.java | parseMessage", "|mms 메시지 갯수 : " + cursor.getCount() + "|");
        if (cursor.getCount() == 0)
        {
            cursor.close();
            return result;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            String mid = cursor.getString(cursor.getColumnIndex("mid"));
            if ($id.equals(mid))
            {
                String partId = cursor.getString(cursor.getColumnIndex("_id"));
                String type = cursor.getString(cursor.getColumnIndex("ct"));
                if ("text/plain".equals(type))
                {
                    String data = cursor.getString(cursor.getColumnIndex("_data"));

                    if (TextUtils.isEmpty(data))
                        result = cursor.getString(cursor.getColumnIndex("text"));
                    else
                        result = parseMessageWithPartId(partId);
                }
            }
            cursor.moveToNext();
        }
        cursor.close();

        return result;
    }


    private String parseMessageWithPartId(String $id)
    {
        Uri partURI = Uri.parse("content://mms/part/" + $id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try
        {
            is = _context.getContentResolver().openInputStream(partURI);
            if (is != null)
            {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (!TextUtils.isEmpty(temp))
                {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        return sb.toString();
    }
}