package com.claire.contentapp;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
//權限取得前，先import以下兩種
import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import static android.Manifest.permission.*;
import android.provider.ContactsContract.Contacts;
import android.widget.TextView;

import java.util.ArrayList;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.OperationApplicationException;
import android.os.RemoteException;

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

        //新增聯絡人
        //insertContact();
        //updateContact();
        //deleteContact();
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

        Cursor cursor = resolver.query(
             Contacts.CONTENT_URI,
            null,
            null,
            null,
            null);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                new String[]{Contacts.DISPLAY_NAME,Contacts.HAS_PHONE_NUMBER}, //String[]from字串陣列
                new int[]{android.R.id.text1, android.R.id.text2}, //欄位對應畫面上應顯示的ID值陣列
                1){
                //客製化
                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    super.bindView(view, context, cursor);
                    //先取得單列中第二個用來顯示電話號碼的TextView元件
                    TextView phone = view.findViewById(android.R.id.text2);
                    //由cursor取得HAS_PHONE_NUMBER欄位的值，若該列的值是0，代表聯絡人無電話資料，顯示空字串
                    if (cursor.getInt(cursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) == 0){
                        phone.setText("");
                    }else {
                        //取得聯絡人的ID值
                        int id = cursor.getInt(cursor.getColumnIndex(Contacts._ID));

                        //進行二次查詢，查詢電話號碼表格
                        Cursor pCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                //查詢條件是電話表格中的外鍵值Phone_CONTACT_ID
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID +"=?",
                                new String[]{String.valueOf(id)}, //為上面條件提供資料
                                null);

                        //先將第二次查詢結果的pCursor往下移一筆，若有資料則取得第一個電話顯示在第二個欄位的TextView元件中
                        if (pCursor.moveToFirst()){
                            String number = pCursor.getString(pCursor.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.DATA));
                            phone.setText(number);
                        }
                    }
                }
        };

        ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);

    }

    private void insertContact(){
        //準備一個ArrayList集合，存放內容提供者操作
        ArrayList ops = new ArrayList();
        //準備索引值預設為0
        int index = ops.size();
        //建立一個新增資料操作，並加到操作集合中，資料對象是RawContacts，新增後會得到其ID值
        ops.add(ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null).build());
        //建立一個新增資料操作，並加到操作集合中，資料對象是ContactsContract.Data，
        // 取得上一個新增至RawContacts記錄的ID值，此段主要是寫入聯絡人姓名
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID,index)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, "John").build());
        //建立新增到Phone的電話號碼操作，亦使用到第一個新增RawContacts操作後得到的ID值
        ops.add(ContentProviderOperation
                .newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID,index)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, "0900123456")
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());

        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

    }

    private void updateContact(){
        // WHERE的條件敘述，「名稱 = ? AND 資料格式=?」
        String where = Phone.DISPLAY_NAME + " =? AND " + Data.MIMETYPE + " =?";
        // 條件敘述中的隕料值，對應上面的兩個問號位置
        String[] params = new String[]{"John", Phone.CONTENT_ITEM_TYPE};
        //準備一個ArrayList集合，存放內容提供者操作
        ArrayList ops = new ArrayList();
        //建立一個更新操作，並加到集合中
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(where, params)
                .withValue(Phone.NUMBER, "0900333333")
                .build());
        try {
            //批次執行操作集合
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e){
            e.printStackTrace();
        } catch (OperationApplicationException e){
            e.printStackTrace();
        }
    }

    private void deleteContact(){
        // WHERE的條件敘述，「名稱 =?」
        String where = Data.DISPLAY_NAME + " =? ";
        String[] params = new String[]{"Jane"};
        ArrayList ops = new ArrayList();
        ops.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI)
                    .withSelection(where, params)
                    .build());
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e){
            e.printStackTrace();
        } catch (OperationApplicationException e){
            e.printStackTrace();
        }
    }

}
