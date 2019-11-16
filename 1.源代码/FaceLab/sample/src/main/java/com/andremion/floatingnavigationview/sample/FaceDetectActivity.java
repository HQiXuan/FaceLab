package com.andremion.floatingnavigationview.sample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FaceDetectActivity extends AppCompatActivity {

    private FloatingNavigationView mFloatingNavigationView;
    private Button btn_detect;
    private ImageView photo_detect;
    private Button btn_detect_sorce;
    private TextView tv_detect_result;
    private String uploadFileName;
    private byte[] fileBuf;
    private ProgressDialog progressDialog;
    String result;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            tv_detect_result.setText(result);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);
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
                Intent intent = new Intent();
                if(item.getTitle().equals("人脸评分")){
                    intent.setClass(FaceDetectActivity.this, FaceDetectActivity.class);
                }else if (item.getTitle().equals("人脸库添加")){
                    intent.setClass(FaceDetectActivity.this, AddPersonActivity.class);
                }else if (item.getTitle().equals("人脸融合")){
                    intent.setClass(FaceDetectActivity.this, MergeActivity.class);
                }else if (item.getTitle().equals("人脸识别")){
                    intent.setClass(FaceDetectActivity.this, MainActivity.class);
                }else{
                    intent.setClass(FaceDetectActivity.this, MatchActivity.class);
                }
                startActivity(intent);
                mFloatingNavigationView.close();
                return true;
            }
        });

        btn_detect = (Button) findViewById(R.id.btn_detect);
        btn_detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_detect_result.setText("");
                tv_detect_result.setHint("评分结果为...");
                select(v);
            }
        });

        photo_detect = findViewById(R.id.photo_detect);
        tv_detect_result = (TextView) findViewById(R.id.tv_detect_result);
        btn_detect_sorce = (Button)findViewById(R.id.btn_detect_sorce);
        btn_detect_sorce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadSearch(v);
                //tv_detect_result.setText(result);
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
            photo_detect.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cursor.close();
    }

    //打开相册,进行照片的选择
    private void openGallery(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,1);
    }

    //文件上传的处理
    public void uploadSearch(View view) {
        progressDialog = ProgressDialog.show(FaceDetectActivity.this, "正在评分...", "");

        new Thread() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                String imageBase64 = Base64Util.encode(fileBuf);
                String uploadUrl = "http://114.55.65.146:8080/detect";
                //String uploadUrl = "http://192.168.31.94:8080/detect";
                //String uploadUrl = "http://172.20.10.2:8080/detect";
                //上传文件域的请求体部分
                RequestBody formBody = RequestBody
                        .create(fileBuf, MediaType.parse("image/jpeg"));
                //整个上传的请求体部分（普通表单+文件上传域）
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("imagesBase64", imageBase64)
                        .build();


                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .build();

                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Looper.prepare();
                        Log.e("text======== ", "failure upload!");
                        Toast toast = Toast.makeText(FaceDetectActivity.this, "上传图片失败......", Toast.LENGTH_SHORT);
                        progressDialog.dismiss();
                        toast.show();
                        Looper.loop();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Looper.prepare();
                        Log.e("text", "detect success upload!");
                        String json = response.body().string();
                        Log.e("json:---",json);
                        JSONObject jsonObject = null;
                        try{
                            jsonObject = new JSONObject(json);
                            String beauty = jsonObject.get("beauty").toString();
                            String age = jsonObject.get("age").toString();
                            String face_token = jsonObject.get("face_token").toString();
                            result = "魅力值：" + beauty + "\n"
                                    + "人脸唯一标识：" + face_token + "\n"
                                    + "年龄：" + age;
                            Log.e("Detect result: " , result);

                            Toast toast = Toast.makeText(FaceDetectActivity.this, beauty, Toast.LENGTH_SHORT);
                            progressDialog.dismiss();
                            Message msg = new Message();
                            handler.sendMessage(msg);
                            toast.show();
                            Looper.loop();
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                });

            }
        }.start();

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
