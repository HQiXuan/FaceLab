package com.andremion.floatingnavigationview.sample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.andremion.floatingnavigationview.FloatingNavigationView;
import com.andremion.floatingnavigationview.sample.util.ImgFileListActivity;
import com.baidu.aip.util.Base64Util;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @使用方法: 亲选择的图片文件数据会在 ImgsActivity.sendfiles方法下进行获取,
 *  *          只需要在该方法里跳转亲所要求的界面即可
 */
public class MatchActivity extends AppCompatActivity {

	//ListView listView;
	ArrayList<String> listfile=new ArrayList<String>();
	private FloatingNavigationView mFloatingNavigationView;
	private ImageView iv_pictrue1;
	private ImageView iv_pictrue2;
	private TextView tv_result_match;
	private ProgressDialog progressDialog;
	private Button btn_match;
	byte[] pictrue1;
	byte[] pictrue2;
	private String mergeResult;
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {

			tv_result_match.setText("相似度： " + mergeResult);

			Log.e("handler result_name: " , "成功heheheheh----");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_match);
		//listView=(ListView) findViewById(R.id.listView1);
		Bundle bundle= getIntent().getExtras();
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		iv_pictrue1 = findViewById(R.id.iv_pictrue11);
		iv_pictrue2 = findViewById(R.id.iv_pictrue22);
		tv_result_match = findViewById(R.id.tv_result_match);
		btn_match = findViewById(R.id.btn_match);
		btn_match.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uploadMatch(v);
			}
		});

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
					intent.setClass(MatchActivity.this, FaceDetectActivity.class);
				}else if (item.getTitle().equals("人脸库添加")){
					intent.setClass(MatchActivity.this, AddPersonActivity.class);
				}else if (item.getTitle().equals("人脸融合")){
					intent.setClass(MatchActivity.this, MergeActivity.class);
				}else if(item.getTitle().equals("人脸识别")){
					intent.setClass(MatchActivity.this, MainActivity.class);
				}else {
					intent.setClass(MatchActivity.this, MatchActivity.class);
				}

				startActivity(intent);
				mFloatingNavigationView.close();
				return true;
			}
		});

		if (bundle!=null) {
			if (bundle.getStringArrayList("files")!=null) {
				listfile= bundle.getStringArrayList("files");
				String path1 = listfile.get(0);
				String path2 = listfile.get(1);
				pictrue1 = image2Bytes(path1);
				pictrue2 = image2Bytes(path2);
				Bitmap bitmap1 = BitmapFactory.decodeByteArray(pictrue1, 0, pictrue1.length);
				iv_pictrue1.setImageBitmap(bitmap1);
				Bitmap bitmap2 = BitmapFactory.decodeByteArray(pictrue2, 0, pictrue2.length);
				iv_pictrue2.setImageBitmap(bitmap2);

				//listView.setVisibility(View.VISIBLE);
				//ArrayAdapter<String> arryAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listfile);
				//listView.setAdapter(arryAdapter);
			}
		}
		
	}

	//文件上传的处理
	public void uploadMatch(View view) {
		progressDialog = ProgressDialog.show(MatchActivity.this, "正在对比......", "");

		new Thread() {
			@Override
			public void run() {
				OkHttpClient client = new OkHttpClient();
				String imageBase1 = Base64Util.encode(pictrue1);
				String imageBase2 = Base64Util.encode(pictrue2);

				//String uploadUrl = "http://192.168.31.94:8080/match";
				String uploadUrl = "http://114.55.65.146:8080/match";
				//String uploadUrl = "http://172.20.10.2:8080/match";

				//整个上传的请求体部分（普通表单+文件上传域）
				RequestBody requestBody = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("imagesBase1", imageBase1)
						.addFormDataPart("imagesBase2", imageBase2)
						.build();

				Request request = new Request.Builder()
						.url(uploadUrl)
						.post(requestBody)
						.build();
				Log.e("requestBody++++++++", requestBody + "....");
				Log.e("request++++++++", request + "....");

				Call call = client.newCall(request);
				call.enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						Looper.prepare();
						Log.e("text======== ", "failure upload!");
						Toast toast = Toast.makeText(MatchActivity.this, "对比图片失败......", Toast.LENGTH_SHORT);
						progressDialog.dismiss();
						toast.show();
						Looper.loop();
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {
						Looper.prepare();
						Log.e("text", "success upload!");
						String json = response.body().string();
						Log.e("success........","成功"+json);
						mergeResult = json;
						//Toast toast = Toast.makeText(MergeActivity.this, showReslt(json)[1], Toast.LENGTH_SHORT);
						progressDialog.dismiss();

						Message msg = new Message();
						handler.sendMessage(msg);
						//toast.show();
						Looper.loop();
					}
				});

			}
		}.start();

	}

	/**
	 * 根据图片路径，把图片转为byte数组
	 * @param imgSrc  图片路径
	 * @return      byte[]
	 */
	public byte[] image2Bytes(String imgSrc)
	{
		FileInputStream fin;
		byte[] bytes = null;
		try {
			fin = new FileInputStream(new File(imgSrc));
			bytes  = new byte[fin.available()];
			//将文件内容写入字节数组
			fin.read(bytes);
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bytes;
	}

	@Override
	public void onBackPressed() {
		if (mFloatingNavigationView.isOpened()) {
			mFloatingNavigationView.close();
		} else {
			super.onBackPressed();
		}
	}

	public void chiseMatch(View v){
		Intent intent = new Intent();
		intent.setClass(this, ImgFileListActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("className","MatchActivity");
		intent.putExtras(bundle);
		startActivity(intent);
	}
}
