package com.oneVipas.onedetector;


import android.hardware.Camera;
import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import java.util.List;

public class CameraPreviewActivity extends ActionBarActivity {

	private String tag = "oneDetector";
	private int dpi;
	private int previewWidth, previewHeight;
	private CameraPreview camPreview = null;
	private SurfaceView camView = null;
	private SurfaceHolder camHolder = null;
	private SurfaceView drawView = null;
	private SurfaceHolder drawHolder = null; 
	private FrameLayout frameLayout;
	private Intent it;
	private String recordStatus;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);	
		it = getIntent();
		recordStatus = it.getStringExtra("record");	
		if (recordStatus.equals("pass")){
			Log.i(tag, "camera activity!");
			enableCameraPreview();
		}else{
			Log.e(tag, "unexception login status");
		}

	}

	public void enableCameraPreview(){
		setContentView(R.layout.activity_camera_preview);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		dpi = metrics.densityDpi;
		if (metrics.widthPixels > metrics.heightPixels) {
			previewWidth = metrics.widthPixels;
			previewHeight = metrics.heightPixels;
		} else {
			previewWidth = metrics.heightPixels;
			previewHeight = metrics.widthPixels;
		}
		Log.i(tag, "dpi = " + dpi + " , w = " + previewWidth+ " , h = " + previewHeight);
/*
        Camera.Parameters params;
        Camera mCamera = Camera.open();
        params = mCamera.getParameters();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        int length = previewSizes.size();
        for (int i = 0; i < length; i++) {
            Log.d("CV","SupportedPreviewSizes : " + previewSizes.get(i).width + "x" + previewSizes.get(i).height);
            if(previewSizes.get(i).height<previewWidth)
            {   i=3;
                if(i==0)    i+=1;
                mCamera.release();
                previewWidth = previewSizes.get(i-1).width;
                previewHeight= previewSizes.get(i-1).height;
                Log.i("CV", "dpi = "+dpi+" , w = "+previewWidth+" , h = "+previewHeight);
                break;
            }
        }
*/
        camView = new SurfaceView(this);
		camHolder = camView.getHolder();
		drawView = new SurfaceView(this);
		drawHolder = drawView.getHolder();

		camPreview = new CameraPreview(previewWidth, previewHeight, drawHolder);
		camHolder.addCallback(camPreview);
		camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		drawView.setZOrderOnTop(true);
		drawHolder.setFormat(PixelFormat.TRANSPARENT);
		frameLayout = (FrameLayout) findViewById(R.id.FrameLayout1);
		//frameLayout.addView(camView, new LayoutParams(1196,720));
		//frameLayout.addView(drawView, new LayoutParams(1196,720));  // previewWidth,previewHeight
        frameLayout.addView(camView, new LayoutParams(previewWidth,previewHeight));
        frameLayout.addView(drawView, new LayoutParams(previewWidth,previewHeight));  // previewWidth,previewHeight
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera_preview, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onPause() {
		super.onPause();
		Log.i(tag, "preview activity onPause");
		//recordStatus = "resume";
		//camPreview.mCamera.stopPreview();

	}

	protected void onResume() {

		super.onResume();
		Log.i(tag, "preview activity onResume");
		/*
		if(recordStatus.equals("resume")){
			Log.i(tag, "login status resume");
			startActivity(new Intent(this, LoginActivity.class));
			finish();
		}
		*/
	}
}
