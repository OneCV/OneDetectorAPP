package com.oneVipas.onedetector;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CameraPreviewActivity extends ActionBarActivity {

	private String tag = "CameraPreviewActivity";
	private int dpi;
	private int previewWidth, previewHeight;
	private CameraPreview camPreview = null;
	private SurfaceView camView = null, bitmapView = null, drawView = null, bufferView = null;
	private SurfaceHolder camHolder = null, bitmapHolder = null, bufferHolder = null, drawHolder = null;
	private FrameLayout frameLayout;
	private Intent it;
	private String recordStatus, serviceStatus;
    private Button captureButton, doneButton;
    private Button okButton, resetButton;
    private TextView debugText;
    private int status = 0, settingStatus = 0;
    private final int GET_TRAINING = 1;
    private projectPt startPt, endPt;
    private Bitmap originBitmap;
    private LinkedList <drawHistory> drawList;
    private settingStep setStep = new settingStep();
    private ServerControl httpControl;
    private int save_num = 0;   //Todo::  must change with user data

    class projectPt {
        int x;
        int y;

        projectPt(int tx, int ty) {
            x = tx;
            y = ty;
        }

        projectPt(){
            x = 0;
            y = 0;
        }
    }

    class drawHistory{
        int type;
        projectPt start, end;

        drawHistory(int typeInit, projectPt startInit, projectPt endInit){
            start = new projectPt();
            end = new projectPt();
            type = typeInit;
            start.x = startInit.x;
            start.y = startInit.y;
            end.x = endInit.x;
            end.y = endInit.y;
        }

        drawHistory(){
            start = new projectPt();
            end = new projectPt();
        }
    }

    final class settingStep{
        public static final int BEGIN = 0;
        public static final int ROI = 1;
        public static final int BANNER = 2;
        public static final int TARGET = 3;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.i(tag,"CameraPreviewActivity onCreate");
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera_preview);
        drawList = new LinkedList<drawHistory> ();
        captureButton = (Button)findViewById(R.id.button);
        frameLayout = (FrameLayout) findViewById(R.id.FrameLayout1);
        okButton = (Button)findViewById(R.id.button1);
        resetButton = (Button)findViewById(R.id.button2);
        doneButton = (Button)findViewById(R.id.button3);
        debugText = (TextView)findViewById(R.id.textView1);
        httpControl = new ServerControl();
		it = getIntent();
		recordStatus = it.getStringExtra("record");
        if(recordStatus != null){
		    if (recordStatus.equals("pass")){
			    Log.d(tag, "camera activity!");
			    enableCameraPreview();
		    }else{
			    Log.e(tag, "unexception login status");
		    }
        }
        serviceStatus = it.getStringExtra("newTraining");
        if (serviceStatus != null) {
            if (serviceStatus.equals("goCamera")) {
                Log.d(tag, "camera  training activity!");
                enableTrainingSetting();
            } else {
                Log.e(tag, "unexception login status");
            }
        }

        handleButton();


	}

    public void enableTrainingSetting(){

        //Initial setting

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

        camView = new SurfaceView(this);
        camHolder = camView.getHolder();
        drawView = new SurfaceView(this);
        drawHolder = drawView.getHolder();
        bitmapView = new SurfaceView(this);
        bitmapHolder = bitmapView.getHolder();
        bufferView = new SurfaceView(this);
        bufferHolder = bufferView.getHolder();

        camPreview = new CameraPreview(previewWidth, previewHeight, drawHolder);
        camPreview.sendStatus(camPreview.TRAINING_TASK);
        camHolder.addCallback(camPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        bitmapView.setZOrderMediaOverlay(true);
        bitmapHolder.setFormat(PixelFormat.TRANSPARENT);
        bufferView.setZOrderOnTop(true);
        bufferHolder.setFormat(PixelFormat.TRANSPARENT);
        drawView.setZOrderOnTop(true);
        drawHolder.setFormat(PixelFormat.TRANSPARENT);

        frameLayout.addView(camView, new LayoutParams(previewWidth,previewHeight));
        frameLayout.addView(bitmapView, new LayoutParams(previewWidth,previewHeight));
        frameLayout.addView(bufferView, new LayoutParams(previewWidth, previewHeight));
        frameLayout.addView(drawView, new LayoutParams(previewWidth,previewHeight));  // previewWidth,previewHeight

        frameLayout.removeView(captureButton);
        frameLayout.addView(captureButton);
        frameLayout.removeView(okButton);
        frameLayout.addView(okButton);
        frameLayout.removeView(resetButton);
        frameLayout.addView(resetButton);
        frameLayout.removeView(doneButton);
        frameLayout.addView(doneButton);
        frameLayout.removeView(debugText);
        frameLayout.addView(debugText);
    }

	public void enableCameraPreview(){
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

        camView = new SurfaceView(this);
		camHolder = camView.getHolder();
		drawView = new SurfaceView(this);
		drawHolder = drawView.getHolder();

		camPreview = new CameraPreview(previewWidth, previewHeight, drawHolder);
        camPreview.sendStatus(camPreview.CVPROCESS_TASK);
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

    private projectPt projectXY(SurfaceView sv, Bitmap bp, int x, int y) {
        if (x < 0 || y < 0 || x > sv.getWidth() || y > sv.getHeight()) {
            // outside ImageView
            return null;
        } else {
            int projectedX = (int)((double)x * ((double)bp.getWidth()/(double)sv.getWidth()));
            int projectedY = (int)((double)y * ((double)bp.getHeight()/(double)sv.getHeight()));

            return new projectPt(projectedX, projectedY);
        }
    }

    private void drawSettingList(LinkedList <drawHistory> list, Canvas canvas, Paint paint){
            drawHistory temp;
            int size;

            size = list.size();

            debugText.setText("listNum:"+size);
            for(int i=0; i<size; i++){
                temp = list.get(i);
                if(temp.type == setStep.ROI){
                    Log.i(tag, "sx="+temp.start.x+"sy="+temp.start.y+"ex="+temp.end.x+"ey="+temp.end.y);
                    drawROI(canvas, temp.start, temp.end, paint);
                }else if(temp.type == setStep.BANNER){
                    Log.i(tag, "sx="+temp.start.x+"sy="+temp.start.y+"ex="+temp.end.x+"ey="+temp.end.y);
                    drawBanner(canvas, temp.start, temp.end, paint);
                }else if(temp.type == setStep.TARGET){
                    Log.i(tag, "sx="+temp.start.x+"sy="+temp.start.y+"ex="+temp.end.x+"ey="+temp.end.y);
                    drawTarget(canvas, temp.start, temp.end, paint);
                }
            }
    }

    private void drawROI(Canvas canvas, projectPt start, projectPt end, Paint paint){
        if(canvas != null && paint != null){
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(10);
            canvas.drawRect(start.x, start.y, end.x, end.y, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(start.x, start.y, 30, paint);
            canvas.drawCircle(end.x, end.y, 30, paint);
            canvas.drawCircle(start.x+(end.x-start.x), start.y , 30, paint);
            canvas.drawCircle(start.x, start.y+(end.y-start.y) , 30, paint);
            paint.setStrokeWidth(3);
            canvas.drawLine(start.x, start.y+(end.y-start.y)/3, end.x, start.y+(end.y-start.y)/3, paint);
            canvas.drawLine(start.x, start.y+(end.y-start.y)/3*2, end.x, start.y+(end.y-start.y)/3*2, paint);
            canvas.drawLine(start.x+(end.x-start.x)/3, start.y, start.x+(end.x-start.x)/3, end.y, paint);
            canvas.drawLine(start.x+(end.x-start.x)/3*2, start.y, start.x+(end.x-start.x)/3*2, end.y, paint);
        }
    }

    private void drawBanner(Canvas canvas, projectPt start, projectPt end, Paint paint){
        if(canvas != null && paint != null){
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(10);
            canvas.drawRect(start.x, start.y, end.x, end.y, paint);
        }
    }

    private void drawTarget(Canvas canvas, projectPt start, projectPt end, Paint paint){
        if(canvas != null && paint != null){
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.YELLOW);
            paint.setStrokeWidth(10);
            canvas.drawRect(start.x, start.y, end.x, end.y, paint);
        }
    }


    private void drawOnRectProjectedBitMap(SurfaceView sv, Bitmap bp, int x, int y) {
        projectPt end = new projectPt();
        if (x < 0 || y < 0 || x > sv.getWidth() || y > sv.getHeight()) {
            // outside ImageView
            return;
        } else {
            int projectedX = (int) ((double) x * ((double) bp.getWidth() / (double) sv.getWidth()));
            int projectedY = (int) ((double) y * ((double) bp.getHeight() / (double) sv.getHeight()));

            Canvas canvas = null;
            Paint paint = new Paint();
            // clear canvasDrawingPane
            canvas = drawHolder.lockCanvas();
            end.x = projectedX;
            end.y = projectedY;
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            if(settingStatus == setStep.ROI) {
                drawROI(canvas, startPt, end, paint);
            }else if(settingStatus == setStep.BANNER){
                drawBanner(canvas, startPt, end, paint);
            }else if(settingStatus >= setStep.TARGET){
                drawTarget(canvas, startPt, end, paint);
            }
            if(canvas != null)
                drawHolder.unlockCanvasAndPost(canvas);

            endPt.x = projectedX;
            endPt.y = projectedY;
        }
    }

    private void finalizeDrawing(){
        okButton.setVisibility(View.VISIBLE);
        resetButton.setVisibility(View.VISIBLE);
        if(settingStatus >= setStep.TARGET){
            doneButton.setVisibility(View.VISIBLE);
        }
    }

    public boolean onTouchEvent(MotionEvent event){
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        if(status != GET_TRAINING) {
            if (action == MotionEvent.ACTION_DOWN) {
                Log.d(tag, "focusing now");
                camPreview.autofocus();
            }
        }

        if(status == GET_TRAINING) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    debugText.setText("ACTION_DOWN (" + x + " , " + y + ")");
                    startPt = projectXY(bitmapView, originBitmap, x, y);
                    endPt = new projectPt();
                    break;
                case MotionEvent.ACTION_MOVE:
                    debugText.setText("ACTION_MOVE (" + x + " , " + y + ")");
                    drawOnRectProjectedBitMap(bitmapView, originBitmap, x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    debugText.setText("ACTION_UP (" + x + " , " + y + ")");
                    drawOnRectProjectedBitMap(bitmapView, originBitmap, x, y);
                    finalizeDrawing();
                    break;
            }
        }
        return true;
    }

    private void handleButton(){
        if(captureButton == null){
            Log.e(tag, "captureButton = Null");
        }else {
            captureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Canvas canvas = null;
                    Log.i(tag, "capture image!");
                    originBitmap = camPreview.captureImage();
                    try{
                        Rect dest = new Rect(0, 0, originBitmap.getWidth(), originBitmap.getHeight());
                        Paint paint = new Paint();
                        paint.setFilterBitmap(true);
                        canvas = bitmapHolder.lockCanvas();
                        canvas.drawBitmap(originBitmap, null, dest, paint);
                        Log.i(tag, "width = "+originBitmap.getWidth()+"height = "+originBitmap.getHeight());
                        captureButton.setVisibility(View.INVISIBLE);
                        okButton.setVisibility(View.VISIBLE);
                        resetButton.setVisibility(View.VISIBLE);
                    }catch (Exception E){

                    }finally{
                        if (canvas != null)
                            bitmapHolder.unlockCanvasAndPost(canvas);
                    }

                }
            });
        }

        if(okButton == null){
            Log.d(tag, "okButton = Null");
        }else {
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if(settingStatus == setStep.BEGIN) {
                        Log.i(tag, "ok!");
                        captureButton.setVisibility(View.INVISIBLE);
                        okButton.setVisibility(View.INVISIBLE);
                        resetButton.setVisibility(View.INVISIBLE);
                        // stop preview();
                        camPreview.surfaceDestroyed(camHolder);
                        status = GET_TRAINING;
                        settingStatus++;
                    }else if(settingStatus == setStep.ROI ){
                        Log.i(tag, "setting ROI ok!");
                        Canvas canvas = null;
                        Paint paint = new Paint();
                        canvas = bufferHolder.lockCanvas();
                        drawHistory temp = new drawHistory(setStep.ROI, startPt, endPt);
                        drawList.add(temp);
                        drawSettingList(drawList, canvas, paint);
                        if(canvas != null)
                            bufferHolder.unlockCanvasAndPost(canvas);
                        okButton.setVisibility(View.INVISIBLE);
                        resetButton.setVisibility(View.INVISIBLE);
                        settingStatus++;
                    }else if(settingStatus == setStep.BANNER){
                        Log.i(tag, "setting Banner ok!");
                        Canvas canvas = null;
                        Paint paint = new Paint();
                        canvas = bufferHolder.lockCanvas();
                        drawHistory temp = new drawHistory(setStep.BANNER, startPt, endPt);
                        drawList.add(temp);
                        drawSettingList(drawList, canvas, paint);
                        if(canvas != null)
                            bufferHolder.unlockCanvasAndPost(canvas);
                        okButton.setVisibility(View.INVISIBLE);
                        resetButton.setVisibility(View.INVISIBLE);
                        settingStatus++;
                    }else if(settingStatus >= setStep.TARGET){
                        Log.i(tag, "setting target ok!");
                        Canvas canvas = null;
                        Paint paint = new Paint();
                        canvas = bufferHolder.lockCanvas();
                        drawHistory temp = new drawHistory(setStep.TARGET, startPt, endPt);
                        drawList.add(temp);
                        drawSettingList(drawList, canvas, paint);
                        if(canvas != null)
                            bufferHolder.unlockCanvasAndPost(canvas);
                        okButton.setVisibility(View.INVISIBLE);
                        resetButton.setVisibility(View.INVISIBLE);
                        doneButton.setVisibility(View.INVISIBLE);
                        settingStatus++;
                    }
                }
            });
        }

        if(resetButton == null){
            Log.d(tag, "resetButton = Null");
        }else {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Canvas canvas = null;
                    Log.i(tag, "reset!");

                    if(settingStatus == setStep.BEGIN) {
                        captureButton.setVisibility(View.VISIBLE);
                        okButton.setVisibility(View.INVISIBLE);
                        resetButton.setVisibility(View.INVISIBLE);
                        try{
                            canvas = bitmapHolder.lockCanvas();
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        }catch (Exception E){

                        }finally{
                            if (canvas != null)
                                bitmapHolder.unlockCanvasAndPost(canvas);
                        }
                    }else if(settingStatus >= setStep.ROI){
                        okButton.setVisibility(View.INVISIBLE);
                        resetButton.setVisibility(View.INVISIBLE);
                        doneButton.setVisibility(View.INVISIBLE);
                        try{
                            canvas = drawHolder.lockCanvas();
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        }catch (Exception E){

                        }finally{
                            if (canvas != null)
                                drawHolder.unlockCanvasAndPost(canvas);
                        }
                    }

                }
            });
        }

        if(doneButton == null){
            Log.d(tag, "doneButton = Null");
        }else{
            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(tag, "Done transfer data to the server");
                    transferDKDK();
                }
            });
        }
    }


    private boolean transferDKDK()
    {
        int numRoi;
        drawHistory tempRoi;
        // upload picture
        Log.i(tag,"upload picture and training data!");
        if(originBitmap == null)
            return false;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        originBitmap.compress(Bitmap.CompressFormat.JPEG , 100 , stream);
        byte [] data = stream.toByteArray();
        List<NameValuePair> post = new ArrayList<NameValuePair>();

        post.add(new BasicNameValuePair("FILE", Base64.encodeBytes(data)));
        post.add(new BasicNameValuePair("Img_W", Integer.toString(originBitmap.getWidth())));
        post.add(new BasicNameValuePair("Img_H", Integer.toString(originBitmap.getHeight())));

        numRoi = drawList.size();
        Log.i(tag, "roi size ="+numRoi);
        post.add(new BasicNameValuePair("TagNum", Integer.toString(numRoi-1)));

        tempRoi = drawList.get(0);
        post.add(new BasicNameValuePair("ROI_X", Integer.toString(tempRoi.start.x)));
        post.add(new BasicNameValuePair("ROI_Y", Integer.toString(tempRoi.start.y)));
        post.add(new BasicNameValuePair("ROI_W", Integer.toString(tempRoi.start.x-tempRoi.end.x))); //abs?
        post.add(new BasicNameValuePair("ROI_H", Integer.toString(tempRoi.start.y-tempRoi.end.y)));
        Log.i(tag, "ROI: name:" + tempRoi.type + " SX:" + tempRoi.start.x);
        for(int i=1; i<=numRoi-1; i++) {
            tempRoi = drawList.get(i);
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_T", " "));  // Tag Type
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_X", Integer.toString(tempRoi.start.x)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_Y", Integer.toString(tempRoi.start.y)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_W", Integer.toString(tempRoi.start.x-tempRoi.end.x)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_H", Integer.toString(tempRoi.start.y-tempRoi.end.y)));
            Log.i(tag, "TAG:" + i + " name:" + tempRoi.type + " SX:" + tempRoi.start.x);
        }
        post.add(new BasicNameValuePair("NUM", Integer.toString(save_num)));
        httpControl.httpHandleCmd(httpControl.url_upload, post,new ServerDone() {
            @Override
            public void execute(String result) {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            }
        });
        if(originBitmap != null) {
            originBitmap.recycle();
        }
        return true;
    }

}
