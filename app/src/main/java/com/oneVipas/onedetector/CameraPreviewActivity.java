package com.oneVipas.onedetector;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CameraPreviewActivity extends ActionBarActivity {

	private String tag = "CameraPreviewActivity";
	private int dpi;
	private int previewWidth, previewHeight;
    private int screenWidth, screenHeight;
	private CameraPreview camPreview = null;
	private SurfaceView camView = null, bitmapView = null, drawView = null, bufferView = null;
	private SurfaceHolder camHolder = null, bitmapHolder = null, bufferHolder = null, drawHolder = null;
	private FrameLayout frameLayout;
	private Intent it;
	private String recordStatus, serviceStatus;
    private Button captureButton, doneButton;
    private Button okButton, resetButton;
    private TextView debugText;
    private ProgressBar progressBar;
    private int status = 0, settingStatus = 0;
    private final int GET_TRAINING = 1;
    private projectPt startPt, endPt;
    private Bitmap resizeBitmap;
    private LinkedList <drawHistory> drawList;
    private settingStep setStep = new settingStep();
    private ServerControl httpControl;
    private int save_num;
    private Bitmap originBitmap;

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
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        frameLayout.removeView(progressBar);
        httpControl = new ServerControl();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);


        dpi = metrics.densityDpi;
        if (metrics.widthPixels > metrics.heightPixels) {
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        } else {
            screenWidth = metrics.heightPixels;
            screenHeight = metrics.widthPixels;
        }
        Log.i(tag, "Screen dpi = " + dpi + " , w = " + screenWidth+ " , h = " + screenHeight);

        Camera.Parameters params;
        Camera mCamera = Camera.open();
        params = mCamera.getParameters();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        int length = previewSizes.size();
        int i;
        for (i = length-1; i >= 0; i--) {
            Log.d("CV","SupportedPreviewSizes : " + previewSizes.get(i).width + "x" + previewSizes.get(i).height);
            if(previewSizes.get(i).height>=480 && previewSizes.get(i).width>=640)
            {
                previewWidth = previewSizes.get(i).width;
                previewHeight = previewSizes.get(i).height;
                Log.i("CV", "CameraPreviewSizes = " + previewWidth + "x" + previewHeight);
                break;
            }
        }
        mCamera.release();
        if(i==-1)
            Log.e("CV", "CameraPreviewSizes do not support");

        // previewSize = screenSize     output_sp.apk
        //previewWidth = screenWidth;
        //previewHeight = screenHeight;

        it = getIntent();
		recordStatus = it.getStringExtra("record");
        if(recordStatus != null){
		    if (recordStatus.equals("pass")){
			    Log.d(tag, "camera activity!");
			    enableCameraPreview(it.getByteArrayExtra("JNI"));
		    }
            else
			    Log.e(tag, "unexception login status");

        }
        serviceStatus = it.getStringExtra("newTraining");
        if (serviceStatus != null) {
            if (serviceStatus.equals("goCamera")) {
                Log.d(tag, "camera  training activity!");
                enableTrainingSetting();
            }
            else
                Log.e(tag, "unexception login status");
        }
        handleButton();

	}

    public void enableTrainingSetting(){
        // setting save_num
        if(httpControl.saves<3)
            save_num = httpControl.saves+1;
        else
        {
            LayoutInflater li = LayoutInflater.from(getBaseContext());
            View promptsView = li.inflate(R.layout.activity_prompts2, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CameraPreviewActivity.this);
            alertDialogBuilder.setView(promptsView);
            final EditText userInput = (EditText)promptsView.findViewById(R.id.editTextDialogUserInput);
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            int idx = Integer.parseInt(userInput.getText().toString());
                            if (idx >= 1 && idx <= 3)
                                save_num = idx;
                            else
                            {
                                Toast.makeText(getBaseContext(), "Must between 1~3", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            finish();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        //Initial setting
        camView = new SurfaceView(this);
        camHolder = camView.getHolder();
        drawView = new SurfaceView(this);
        drawHolder = drawView.getHolder();
        bitmapView = new SurfaceView(this);
        bitmapHolder = bitmapView.getHolder();
        bufferView = new SurfaceView(this);
        bufferHolder = bufferView.getHolder();

        camPreview = new CameraPreview(previewWidth, previewHeight, screenWidth, screenHeight, drawHolder,null);
        camPreview.sendStatus(camPreview.TRAINING_TASK);
        camHolder.addCallback(camPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        bitmapView.setZOrderMediaOverlay(true);
        bitmapHolder.setFormat(PixelFormat.TRANSPARENT);
        bufferView.setZOrderOnTop(true);
        bufferHolder.setFormat(PixelFormat.TRANSPARENT);
        drawView.setZOrderOnTop(true);
        drawHolder.setFormat(PixelFormat.TRANSPARENT);

        frameLayout.addView(camView, new LayoutParams(screenWidth,screenHeight));
        frameLayout.addView(bitmapView, new LayoutParams(screenWidth,screenHeight));
        frameLayout.addView(bufferView, new LayoutParams(screenWidth, screenHeight));
        frameLayout.addView(drawView, new LayoutParams(screenWidth,screenHeight));  // previewWidth,previewHeight

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

	public void enableCameraPreview(byte[] JNI){
        camView = new SurfaceView(this);
		camHolder = camView.getHolder();
		drawView = new SurfaceView(this);
		drawHolder = drawView.getHolder();

		camPreview = new CameraPreview(previewWidth, previewHeight, screenWidth, screenHeight, drawHolder,JNI);
        camPreview.sendStatus(camPreview.CVPROCESS_TASK);
		camHolder.addCallback(camPreview);
		camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		drawView.setZOrderOnTop(true);
		drawHolder.setFormat(PixelFormat.TRANSPARENT);
		frameLayout = (FrameLayout) findViewById(R.id.FrameLayout1);
		//frameLayout.addView(camView, new LayoutParams(1196,720));
		//frameLayout.addView(drawView, new LayoutParams(1196,720));  // previewWidth,previewHeight
        frameLayout.addView(camView, new LayoutParams(screenWidth,screenHeight));
        frameLayout.addView(drawView, new LayoutParams(screenWidth,screenHeight));  // previewWidth,previewHeight

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

    private projectPt projectXY(int fromWidth,int fromHeight, int toWidth, int toHeight, int x, int y) {

        if (x < 0 || y < 0 || x > fromWidth || y > fromHeight) {
            // outside ImageView
            return null;
        } else {
            int projectedX = (int)((double)x * ((double)toWidth/(double)fromWidth));
            int projectedY = (int)((double)y * ((double)toHeight/(double)fromHeight));

            return new projectPt(projectedX, projectedY);
        }
    }

    private projectPt projectXY(SurfaceView sv, Bitmap bp, int x, int y) {
        return projectXY(sv.getWidth(),sv.getHeight(),bp.getWidth(),bp.getHeight(),x,y);
    }

    private drawHistory projectROI(Bitmap resizeBitmap, Bitmap originBitmap, drawHistory tempRoi) {
        tempRoi.start = projectXY(resizeBitmap.getWidth(),resizeBitmap.getHeight(),originBitmap.getWidth(),originBitmap.getHeight(),tempRoi.start.x,tempRoi.start.y);
        tempRoi.end = projectXY(resizeBitmap.getWidth(),resizeBitmap.getHeight(),originBitmap.getWidth(),originBitmap.getHeight(),tempRoi.end.x,tempRoi.end.y);
        return tempRoi;
    }

    private void drawSettingList(LinkedList <drawHistory> list, Canvas canvas, Paint paint){
            drawHistory temp;
            int size;

            size = list.size();

            debugText.setText("listNum:"+size);
            for(int i=0; i<size; i++){
                temp = list.get(i);
                if(temp.type == setStep.ROI){
                    Log.i(tag, "sx="+temp.start.x+" sy="+temp.start.y+" ex="+temp.end.x+" ey="+temp.end.y);
                    drawROI(canvas, temp.start, temp.end, paint);
                }else if(temp.type == setStep.BANNER){
                    Log.i(tag, "sx="+temp.start.x+" sy="+temp.start.y+" ex="+temp.end.x+" ey="+temp.end.y);
                    drawBanner(canvas, temp.start, temp.end, paint);
                }else if(temp.type == setStep.TARGET){
                    Log.i(tag, "sx="+temp.start.x+" sy="+temp.start.y+" ex="+temp.end.x+" ey="+temp.end.y);
                    drawTarget(canvas, temp.start, temp.end, paint);
                }
            }
    }
    private drawHistory fixROI(drawHistory pt)
    {
        int temp;
        if(pt.start.x > pt.end.x) {
            temp = pt.start.x;
            pt.start.x = pt.end.x;
            pt.end.x = temp;
        }
        if(pt.start.y > pt.end.y) {
            temp = pt.start.y;
            pt.start.y = pt.end.y;
            pt.end.y = temp;
        }
        return pt;
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
                    startPt = projectXY(bitmapView, resizeBitmap, x, y);
                    endPt = new projectPt();
                    break;
                case MotionEvent.ACTION_MOVE:
                    debugText.setText("ACTION_MOVE (" + x + " , " + y + ")");
                    drawOnRectProjectedBitMap(bitmapView, resizeBitmap, x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    debugText.setText("ACTION_UP (" + x + " , " + y + ")");
                    drawOnRectProjectedBitMap(bitmapView, resizeBitmap, x, y);
                    finalizeDrawing();
                    break;
            }
        }
        return true;
    }

    private void handleButton(){
        if(captureButton == null)
            Log.e(tag, "captureButton = Null");
        else {
            captureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Canvas canvas = null;
                    Log.i(tag, "capture image!");
                    originBitmap = camPreview.captureImage();


                    float scaleWidth = screenWidth / (float) originBitmap.getWidth();
                    float scaleHeight = screenHeight / (float) originBitmap.getHeight();
                    Log.i("Alex", "SettingManager====> set scale value : " + scaleWidth + ":" + scaleHeight);
                    Matrix matrix = new Matrix();
                    matrix.postScale(scaleWidth, scaleHeight);
                    resizeBitmap = Bitmap.createBitmap(originBitmap, 0, 0, originBitmap.getWidth(), originBitmap.getHeight(), matrix, true);


                    try {
                        Rect dest = new Rect(0, 0, resizeBitmap.getWidth(), resizeBitmap.getHeight());
                        Paint paint = new Paint();
                        paint.setFilterBitmap(true);
                        canvas = bitmapHolder.lockCanvas();
                        canvas.drawBitmap(resizeBitmap, null, dest, paint);
                        Log.i(tag, "width = " + resizeBitmap.getWidth() + "height = " + resizeBitmap.getHeight());
                        captureButton.setVisibility(View.INVISIBLE);
                        okButton.setVisibility(View.VISIBLE);
                        resetButton.setVisibility(View.VISIBLE);
                    } catch (Exception E) {
                    } finally {
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
                    if(settingStatus >= setStep.TARGET){
                        Log.i(tag, "setting target ok!(done)");
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
                    transferDKDK();
                    //known issue, if you press done after send to server...won't fix due to must go to jni process

                }
            });
        }
    }

    private ByteArrayOutputStream convertBMP(Bitmap jpeg)
    {

        int WIDTH = jpeg.getWidth();
        int HEIGHT = jpeg.getHeight();
        int PADDING = (4-WIDTH*3%4)%4;
        int IMG_SIZE = (WIDTH*3+PADDING)*HEIGHT;

        short header[] = {
                0x4d42,
                (short)((IMG_SIZE+54)<<16>>16),(short)((IMG_SIZE+54)>>16),
                0,0,
                0x0036,0,
                0x0028,0,
                (short)(WIDTH<<16>>16),(short)(WIDTH>>16),
                (short)(HEIGHT<<16>>16),(short)(HEIGHT>>16),
                1,
                0x18,
                0,0,
                (short)(IMG_SIZE<<16>>16),(short)(IMG_SIZE>>16),
                0,0,
                0,0,
                0,0,
                0,0
        };

        int i,j;
        byte pad[] = new byte[3];		// padding
        byte pixel[] = new byte[3];			// B G R
        pad[0]=0;
        pad[1]=0;
        pad[2]=0;
        ByteArrayOutputStream mp;
        try {
            //mp = fopen("bmp.bmp","wb");
            //FileOutputStream mp = new FileOutputStream(Environment.getExternalStorageDirectory()+"/DK.bmp");
            mp = new ByteArrayOutputStream();

            //fwrite(header, 2, sizeof(header) / 2, mp);
            for(i=0;i<header.length;i++) {
                mp.write((byte) (header[i] & 0xff));        // low byte
                mp.write((byte) (header[i]>>8 & 0xff));     // high byte
            }

            int[] data = new int[WIDTH * HEIGHT];
            jpeg.getPixels(data, 0, WIDTH, 0, 0, WIDTH, HEIGHT);

            for (i = 0; i < HEIGHT; ++i)
            {
                for (j = 0; j < WIDTH; ++j)
                {
                    int index = (HEIGHT-i-1) * WIDTH + j;   // *Up Down Reverse to get correct BMP
                    byte r = (byte)((data[index] >> 16) & 0xff);
                    byte g = (byte)((data[index] >> 8) & 0xff);
                    byte b = (byte)(data[index] & 0xff);

                    pixel[2] = r;   // R
                    pixel[1] = g; 	// G
                    pixel[0] = b; 	// B

                    //fwrite(pixel,1,3,mp);
                    mp.write(pixel);
                }
                //fwrite(&pad,1,PADDING,mp);		// 填充成 DWORD = 4byte
                mp.write(pad,1,PADDING);
            }
                //fclose(mp);
                mp.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return mp;
    }
    private boolean transferDKDK()
    {
        int numRoi;
        drawHistory tempRoi;
        frameLayout.addView(progressBar);
        // upload picture
        Log.i(tag,"upload picture and training data!");
        if(originBitmap == null)
            return false;
        //ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //originBitmap.compress(Bitmap.CompressFormat.JPEG , 60 , stream);

        Log.i(tag, "forming bmp ... ");
        ByteArrayOutputStream stream = convertBMP(originBitmap);
        Log.i(tag, "forming bmp ... finished! ");

        byte [] data = stream.toByteArray();
        List<NameValuePair> post = new ArrayList<NameValuePair>();

        post.add(new BasicNameValuePair("FILE", Base64.encodeBytes(data)));
        post.add(new BasicNameValuePair("Img_W", Integer.toString(originBitmap.getWidth())));
        post.add(new BasicNameValuePair("Img_H", Integer.toString(originBitmap.getHeight())));

        numRoi = drawList.size();
        Log.i(tag, "roi size ="+numRoi);
        post.add(new BasicNameValuePair("TagNum", Integer.toString(numRoi-1)));

        tempRoi = drawList.get(0);
        tempRoi = fixROI(tempRoi);
        tempRoi = projectROI(resizeBitmap,originBitmap,tempRoi); // resize => origin
        post.add(new BasicNameValuePair("ROI_X", Integer.toString(tempRoi.start.x)));
        post.add(new BasicNameValuePair("ROI_Y", Integer.toString(tempRoi.start.y)));
        post.add(new BasicNameValuePair("ROI_W", Integer.toString(tempRoi.end.x-tempRoi.start.x))); //abs?
        post.add(new BasicNameValuePair("ROI_H", Integer.toString(tempRoi.end.y-tempRoi.start.y)));
        //Log.i(tag, "ROI: name:" + tempRoi.type + " SX:" + tempRoi.start.x + " SY:" + tempRoi.start.y + " EX:" + tempRoi.end.x + " EY:" + tempRoi.end.y);
        Log.i(tag, "ROI: name:" + tempRoi.type + " X:" + tempRoi.start.x + " Y:" + tempRoi.start.y + " W:" + Integer.toString(tempRoi.end.x-tempRoi.start.x) + " H:" + Integer.toString(tempRoi.end.y-tempRoi.start.y));
        for(int i=1; i<=numRoi-1; i++) {
            tempRoi = drawList.get(i);
            tempRoi = fixROI(tempRoi);
            tempRoi = projectROI(resizeBitmap, originBitmap, tempRoi); // resize => origin
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_T", " "));  // Tag Type
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_X", Integer.toString(tempRoi.start.x)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_Y", Integer.toString(tempRoi.start.y)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_W", Integer.toString(tempRoi.end.x-tempRoi.start.x)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_H", Integer.toString(tempRoi.end.y-tempRoi.start.y)));
            //Log.i(tag, "TAG:" + i + " name:" + tempRoi.type + " SX:" + tempRoi.start.x + " SY:" + tempRoi.start.y + " EX:" + tempRoi.end.x + " EY:" + tempRoi.end.y);
            Log.i(tag, "TAG:" + i + " name:" + tempRoi.type + " X:" + tempRoi.start.x + " Y:" + tempRoi.start.y + " W:" + Integer.toString(tempRoi.end.x-tempRoi.start.x) + " H:" + Integer.toString(tempRoi.end.y-tempRoi.start.y));
        }
        post.add(new BasicNameValuePair("NUM", Integer.toString(save_num)));
        httpControl.httpHandleCmd(httpControl.url_upload, post,new ServerDone() {
            @Override
            public void execute(byte[] result) {
                String str = new String(result);

                Log.i(tag, str);
                frameLayout.removeView(progressBar);
                if(str.substring(str.length()-15).equals("Upload Success!") && httpControl.saves<3) {
                    httpControl.saves += 1; // == save_num
                    Toast.makeText(getBaseContext(), "Saving to #"+save_num, Toast.LENGTH_LONG).show();
                }
                else
                    Toast.makeText(getBaseContext(), str, Toast.LENGTH_LONG).show();
                finish();
            }
        });

        if(resizeBitmap != null)
            resizeBitmap.recycle();

        if(originBitmap != null)
            originBitmap.recycle();

        return true;
    }


}
