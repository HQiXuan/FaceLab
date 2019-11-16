package com.andremion.floatingnavigationview.sample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.baidu.aip.util.Base64Util;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


import cz.msebera.android.httpclient.entity.mime.Header;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;


public class MainActivity extends AppCompatActivity {

    private FloatingNavigationView mFloatingNavigationView;
    private Button btnOne;
    private ImageView photo;
    private Button btn_search;
    private TextView tv_search_result;
    private String uploadFileName;
    private byte[] fileBuf;
    private ProgressDialog progressDialog;
    String result_name;
    String result_show = "";
    private android.os.Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            tv_search_result.setText(result_name);

            //Log.e("handler result_name: " , result_name);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFloatingNavigationView = (FloatingNavigationView) findViewById(R.id.floating_navigation_view);
        mFloatingNavigationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFloatingNavigationView.open();
            }
        });
        mFloatingNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                //Snackbar.make((View) mFloatingNavigationView.getParent(), item.getTitle() + " Selected!", Snackbar.LENGTH_SHORT).show();
                //入口
                Intent intent = new Intent();
                //setClass函数的第一个参数是一个Context对象
                //Context是一个类，Activity是Context类的子类，也就是说，所有的Activity对象，都可以向上转型为Context对象
                //setClass函数的第二个参数是一个Class对象，在当前场景下，应该传入需要被启动的Activity类的class对象
                if(item.getTitle().equals("人脸评分")){
                    intent.setClass(MainActivity.this, FaceDetectActivity.class);
                }else if (item.getTitle().equals("人脸库添加")){
                    intent.setClass(MainActivity.this, AddPersonActivity.class);
                }else if (item.getTitle().equals("人脸融合")){
                    intent.setClass(MainActivity.this, MergeActivity.class);
                }else if (item.getTitle().equals("人脸对比")){
                    intent.setClass(MainActivity.this, MatchActivity.class);
                }else {
                    intent.setClass(MainActivity.this, MainActivity.class);
                }

                startActivity(intent);
                mFloatingNavigationView.close();
                return true;
            }
        });

        btnOne = (Button) findViewById(R.id.btnOne);
        btnOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_search_result.setText("");
                tv_search_result.setHint("识别结果为...");
                select(v);
            }
        });

        photo = findViewById(R.id.photo);
        tv_search_result = (TextView) findViewById(R.id.tv_search_result);
        btn_search = (Button)findViewById(R.id.btn_search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadSearchBak(v);
                //tv_search_result.setText(result_name);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mFloatingNavigationView.isOpened()) {
            mFloatingNavigationView.close();
        } else {
            super.onBackPressed();
        }
    }

    //按钮点击事件
    public void select(View view) {
        String[] permissions=new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        //进行sdcard的读写请求
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,permissions,1);
        }
        else{
            openGallery(); //打开相册，进行选择
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    openGallery();
                }
                else{
                    Toast.makeText(this,"读相册的操作被拒绝", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                //Toast.makeText(this,"handleSelect", Toast.LENGTH_LONG).show();
                handleSelect(data);
        }
    }
    //选择后照片的读取工作
    private void handleSelect(Intent intent){
        Cursor cursor = null;
        Uri uri = intent.getData();
        cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            uploadFileName = cursor.getString(columnIndex);
        }
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            fileBuf=convertToBytes(inputStream);
            Bitmap bitmap = BitmapFactory.decodeByteArray(fileBuf, 0, fileBuf.length);
            photo.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cursor.close();
    }

    //打开相册,进行照片的选择
    private void openGallery() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }


    //文件上传的处理
    public void uploadSearchBak(View view) {
        //progressDialog = ProgressDialog.show(MainActivity.this, "正在识别......", "");

        new Thread() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                String imageBase64 = Base64Util.encode(fileBuf);
                //String uploadUrl = "http://172.20.10.2:8080/uploadPictrue";
                //String uploadUrl = "http://192.168.31.94:8080/uploadPictrue";
                String uploadUrl = "http://114.55.65.146:8080/uploadPictrue";
                //上传文件域的请求体部分
                RequestBody formBody = RequestBody
                        .create(fileBuf, MediaType.parse("image/jpeg"));
                //整个上传的请求体部分（普通表单+文件上传域）
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("images", imageBase64)
                        //filename:avatar,originname:abc.jpg
                        //.addFormDataPart("avatar", uploadFileName, formBody)
                        .build();


                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .build();

                Log.e("requestBody++++++++", requestBody + "....");
                Log.e("request++++++++", request + "....");

                /*try {
                    Response response = client.newCall(request).execute();
                    name = response.body().string();
                    Log.e("response++++++++", name + "....");
                    //将结果返回到界面
                    //tv_search_result.setText("结果是：" + name);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Looper.prepare();
                        Log.e("text======== ", "failure upload!");
                        Toast toast = Toast.makeText(MainActivity.this, "上传图片失败......", Toast.LENGTH_SHORT);
                        //progressDialog.dismiss();
                        toast.show();
                        Looper.loop();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Looper.prepare();
                        Log.e("text", "success upload!");
                        String json = response.body().string();

                        //result_name = json;
                        try {
                            if("".equals(json) || json == null){
                                result_name = "人脸库中没有匹配的人脸哦！";
                            }else{
                                result_name = show(json);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //Log.e("result_name: ----" , result_name);
                        //tv_search_result.setText("嘿嘿嘿~" + json);
                        Log.e("success........","成功"+json);
                        Toast toast = Toast.makeText(MainActivity.this, "识别成功", Toast.LENGTH_SHORT);
                        //progressDialog.dismiss();

                        Message msg = new Message();
                        handler.sendMessage(msg);
                        toast.show();
                        Looper.loop();
                    }
                });

            }
        }.start();

    }

    public String show(String string) throws Exception{
        JSONObject dataJson = new JSONObject(string);// 把字符串转成json数据
        String showResult = "";
        JSONObject re = (JSONObject)dataJson.get("result");

        JSONArray face_lists = re.getJSONArray("face_list");
        for (int i = 0; i < face_lists.length(); i++) {// 循环数组，取出想要的数据
            JSONObject face1 = (JSONObject) face_lists.get(i);

            System.out.println("location"+face1.get("location"));
            JSONObject location = (JSONObject)face1.get("location");
            JSONArray user_lists = face1.getJSONArray("user_list");
            for (int j = 0; j < user_lists.length(); j++) {// 循环数组，取出想要的数据
                JSONObject user1 = (JSONObject) user_lists.get(j);

                showResult += user1.getString("user_info") + "\n";

                System.out.println("userInfo: " + user1.getString("user_info"));
            }
        }
        return showResult;
    }

    private byte[] convertToBytes(InputStream inputStream) throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = inputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        inputStream.close();
        return  out.toByteArray();
    }
}
