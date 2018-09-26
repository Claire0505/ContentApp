package com.claire.contentapp;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
//權限取得前，先import以下兩種
import android.Manifest;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import static android.Manifest.permission.*;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class MainActivity extends AppCompatActivity {

    //定義一個常數，代表向使用者要求讀取聯絡人的辦識值
    private static final int REQUEST_CONTACTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //檢查權限android6.0以上 ，檢查應用程式是否已向使用者要求讀取聯絡人權限
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        //判斷檢查後的結果permission值
        if (permission != PackageManager.PERMISSION_GRANTED){
            //未取得權限，向使用者要求允許權限
            ActivityCompat.requestPermissions(this,
                    new String[]{READ_CONTACTS, WRITE_CONTACTS},
                    REQUEST_CONTACTS);
        }else{
            //已有權限，可進行檔案存取
            readContacts();
        }
    }

    //不論使用者選擇Deny拒絕或Allow允許，都會自動執行onRequestPermissionsResult方法
    // 覆寫 onRequestPermissionsResult方法
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //在方法中實作判斷使用者選擇
        switch (requestCode){
            case REQUEST_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //取得聯絡人權限，進行存取
                    readContacts();
                } else {
                    //使用者拒絕權限，顯示對話框告知
                    new AlertDialog.Builder(this)
                            .setMessage("必須允許聯絡人權限才能顯示資料")
                            .setPositiveButton("OK", null)
                            .show();
                }
                return;
        }
    }

    private void readContacts() {
        //1.前取很ContentResolver物件
        ContentResolver resolver = getContentResolver();
        //2.查詢手機中的所有聯絡人，並得到cursor物件
        //當不需要顯示無電話的聯絡人時，可使用[Implicit Join]隱性合併查詢的方式呼叫
        String[] projection = {Contacts._ID,
                Contacts.DISPLAY_NAME,
                Phone.NUMBER};

        Cursor cursor = resolver.query(
             Phone.CONTENT_URI,
            projection,
            null,
            null,
            null);

        ListView list = findViewById(R.id.list);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                new String[]{Contacts.DISPLAY_NAME, Phone.NUMBER}, //String[]from字串陣列
                new int[]{android.R.id.text1, android.R.id.text2}, //欄位對應畫面上應顯示的ID值陣列
                1);
        list.setAdapter(adapter);

    }
}
