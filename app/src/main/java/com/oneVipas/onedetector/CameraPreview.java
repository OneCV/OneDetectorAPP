package com.oneVipas.onedetector;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;

public class CameraPreview implements SurfaceHolder.Callback, Camera.PreviewCallback{

	private int gPreviewWidth, gPreviewHeight;
	private ImageView gCamPreview = null;
	private int imageFormat;
	private final String tag = "oneDetector";
	private boolean bProcessing = false;
	private byte[] frameData = null;
	private int[] outData = null;
	private Bitmap bitmap = null;
	private SurfaceHolder gDrawHolder;
	Handler mHandler = new Handler(Looper.getMainLooper());
	public native boolean cvObjectDetectionInit();
	public rectObject rectClass = new rectObject();
	public Camera mCamera = null;
	
	public class rectObject{
		public int x;
		public int y;
		public int width;
		public int height;
	}
	
	public CameraPreview(int previewWidth, int previewHeight, SurfaceHolder drawHolder)
	{
		gPreviewWidth = previewWidth;
		gPreviewHeight = previewHeight;
		gDrawHolder = drawHolder;
		//bitmap = camPreview.getDrawingCache();
		//bitmap = Bitmap.createBitmap(gPreviewWidth, gPreviewHeight, Bitmap.Config.ARGB_8888);
		outData = new int[gPreviewWidth*gPreviewHeight];
		cvObjectDetectionInit();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(tag, "surfaceCreated\n");
		mCamera = Camera.open();
	
		try
		{
			// bind surface holder to camera preview display
			mCamera.setPreviewDisplay(holder);
			
			//Camera.PreviewCallback -- for endless loop catch image
			mCamera.setPreviewCallback(this);
		}
		catch (IOException e) // exception, failed for release camera
		{
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// camera parameters maybe runtime change, get latest parameters and reset the camera.
		Log.i(tag, String.format("surfaceChanged width =%d height = %d", width, height));
		Parameters params;
		params = mCamera.getParameters();
		imageFormat = params.getPreviewFormat();
		params.setPreviewSize(gPreviewWidth, gPreviewHeight);
		try	{
			mCamera.setParameters(params);
		}
		catch(Exception e) {
			Log.w(tag,e.toString());
		}
		mCamera.startPreview();

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	protected void onPause() {
		if(mCamera!=null)
		{
			Log.i(tag, "previewclass:onPause CameraPreview Success");
			mCamera.stopPreview();
		}
		else
			Log.i(tag, "previewclass:onPause CameraPreview Failed");				
		
	}
	protected void onResume() {
		if(mCamera!=null)
		{
			Log.i(tag, "previewclass:onResume CameraPreview Success");
			mCamera.startPreview();
		}
		else
			Log.i(tag, "previewclass:onResume CameraPreview Failed");
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// preview call back for camera set callback, endless loop for catch image.
		if (imageFormat == ImageFormat.NV21)// camera preview format.
		{
			if (!bProcessing)
			{
				frameData = data;
				mHandler.post(cvProcess);
			}
		}
	}
	
	
	// load JNI
	// declare JNI api
	public native boolean cvipProcessing(int width, int height, byte[] srcData, int[] outData, rectObject rectClass);
	
	
	static
	{
		System.loadLibrary("OneCV");
		System.loadLibrary("OneCVAPP");
		System.loadLibrary("OneDetectorJni");
	}
	

	
	// thread process image thread;
	private Runnable cvProcess = new Runnable()
	{
		public void run()
		{
			Canvas canvas = null;
			Paint paint = new Paint();			
			bProcessing = true;
			cvipProcessing(gPreviewWidth, gPreviewHeight, frameData, outData, rectClass);
			//Log.i(tag, String.format("outdata=%x", outData[10000]));
			//Log.d(tag, String.format("x=%d y=%d width=%d height=%d", rectClass.x, rectClass.y, rectClass.width, rectClass.height));
			paint.setColor(Color.CYAN);
			try{
				canvas = gDrawHolder.lockCanvas();

				paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
				canvas.drawPaint(paint);
				paint.setXfermode(new PorterDuffXfermode(Mode.SRC));
		        paint.setColor(Color.RED);
		        paint.setStrokeWidth(2);
		        paint.setStyle(Paint.Style.STROKE);
		        canvas.drawRect(rectClass.x, rectClass.y, (rectClass.x+rectClass.width)-1, (rectClass.y+rectClass.height)-1, paint);
		       
			}catch (Exception e) {  
                e.printStackTrace(); 
			}finally{
			//Log.i(tag, String.format("ssssssss"));
				if (canvas != null)
					gDrawHolder.unlockCanvasAndPost(canvas); 
			}
			
			//bitmap.setPixels(outData, 0, gPreviewWidth, 0, 0, gPreviewWidth, gPreviewHeight);
			//gCamPreview.setImageBitmap(bitmap);
			bProcessing = false;
		}
	};
}
