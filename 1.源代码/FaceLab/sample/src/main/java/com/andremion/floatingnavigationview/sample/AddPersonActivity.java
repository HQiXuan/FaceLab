package com.andremion.floatingnavigationview.sample;

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
import android.text.Editable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.baidu.aip.util.Base64Util;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddPersonActivity extends AppCompatActivity {
    private FloatingNavigationView mFloatingNavigationView;
    private Button btn_photo;
    private ImageView photo_add;
    private Button btn_addsearch;
    private String uploadFileName;
    private byte[] fileBuf;
    private ProgressDialog progressDialog;
    private String result;
    EditText et_name;

    private EditText et_userid;
    private RadioButton cb_male;
    private RadioButton cb_famale;
    private RadioGroup rgSex;
    List<CheckBox> radios; // 单选组
    private String gender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);
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
                    intent.setClass(AddPersonActivity.this, FaceDetectActivity.class);
                }else if (item.getTitle().equals("人脸库添加")){
                    intent.setClass(AddPersonActivity.this, AddPersonActivity.class);
                }else if (item.getTitle().equals("人脸融合")){
                    intent.setClass(AddPersonActivity.this, MergeActivity.class);
                }else if (item.getTitle().equals("人脸识别")){
                    intent.setClass(AddPersonActivity.this, MainActivity.class);
                }else {
                    intent.setClass(AddPersonActivity.this, MatchActivity.class);
                }
                startActivity(intent);
                mFloatingNavigationView.close();
                return true;
            }
        });

        btn_photo = (Button) findViewById(R.id.btn_photo);
        photo_add = findViewById(R.id.photo_add);
        btn_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select(v);
            }
        });
        et_name = findViewById(R.id.et_name);
        et_userid = findViewById(R.id.et_userid);
        cb_male = findViewById(R.id.cb_male);
        cb_famale = findViewById(R.id.cb_famale);
        rgSex = findViewById(R.id.rgSex);
        rgSex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.cb_male:
                        gender = cb_male.getText().toString().trim();
                        break;
                    case R.id.cb_famale:
                        gender = cb_famale.getText().toString().trim();
                        break;
                }
//                Toast toast = Toast.makeText(AddPersonActivity.this, gender, Toast.LENGTH_SHORT);
//                toast.show();
            }
        });

        btn_addsearch = (Button)findViewById(R.id.btn_addsearch);
        btn_addsearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadSearch(v);
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
            photo_add.setImageBitmap(bitmap);
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
        progressDialog = ProgressDialog.show(AddPersonActivity.this, "正在添加...", "");

        new Thread() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                String imageBase64 = Base64Util.encode(fileBuf);
                //String uploadUrl = "http://192.168.31.94:8080/add";
                String uploadUrl = "http://114.55.65.146:8080/add";
                //String uploadUrl = "http://172.20.10.2:8080/add";
                String name = et_name.getText().toString();
                String userid = et_userid.getText().toString();

                if(name == null || name.equals("") || userid == null || userid.equals("") || gender.equals("")){
                    Toast toast = Toast.makeText(AddPersonActivity.this, "用户名或用户ID为空", Toast.LENGTH_SHORT);
                    Intent intent = new Intent();
                    intent.setClass(AddPersonActivity.this, AddPersonActivity.class);

                }
                //上传文件域的请求体部分
                RequestBody formBody = RequestBody
                        .create(fileBuf, MediaType.parse("image/jpeg"));
                //整个上传的请求体部分（普通表单+文件上传域）
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("imagesBase64", imageBase64)
                        .addFormDataPart("name", name)
                        .addFormDataPart("userid", userid)
                        .addFormDataPart("gender", gender)
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
                        Toast toast = Toast.makeText(AddPersonActivity.this, "添加失败......", Toast.LENGTH_SHORT);
                        progressDialog.dismiss();
                        toast.show();
                        Looper.loop();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Looper.prepare();
                        Log.e("text", "detect success upload!");
                        String result = response.body().string();
                        Toast toast = null;

                        try{
                            if(result.equals("添加失败")){
                                toast = Toast.makeText(AddPersonActivity.this, "请重新上传", Toast.LENGTH_SHORT);

                            }else{
                                toast = Toast.makeText(AddPersonActivity.this, "添加成功", Toast.LENGTH_SHORT);
                                Intent intent = new Intent();
                                intent.setClass(AddPersonActivity.this, AddPersonActivity.class);
                                startActivity(intent);
                            }
                            progressDialog.dismiss();
                            toast.show();
                            Looper.loop();
                        }catch (Exception e){
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