package com.oneVipas.onedetector;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class MenulistActivity extends ActionBarActivity {
	private String tag = "oneDetector";
	private Button button1, button2, button3, button4, button5;
	private TextView text1, text2;
	private ImageView imvMain, imvPain;
	private Uri imgUri;
	private projectPt startPt, endPt;
	private Canvas canvasMain;
	private Canvas canvasPain;
	private Bitmap bmpMain, bmpPain;
	private LinkedList <roiInfo> roiSetting;
    private roiInfo tempRoi;
	private int recordStatus= 0;
    private int previewWidth, previewHeight;
    private int numRoi;
    private final int ratio16_9 = 1, ratio4_3 = 2;
	//ArrayList <roiInfo>
    private int save_num = 2;   //Todo::  must change with user data

    public ServerControl httpControl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);	
		setContentView(R.layout.activity_menulist);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // get preview width  and height
        if (metrics.widthPixels > metrics.heightPixels) {
            previewWidth = metrics.widthPixels;
            previewHeight = metrics.heightPixels;
        } else {
            previewWidth = metrics.heightPixels;
            previewHeight = metrics.widthPixels;
        }
		handleButton();
        httpControl = new ServerControl(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menulist, menu);
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
	
	private void handleButton(){
		button1 = (Button) findViewById(R.id.button1);
		button2 = (Button) findViewById(R.id.button2);
		button3 = (Button) findViewById(R.id.button3);
		button4 = (Button) findViewById(R.id.button4);
		button5 = (Button) findViewById(R.id.button5);
		
		imvMain = (ImageView) findViewById(R.id.imageView1);
		imvPain = (ImageView) findViewById(R.id.imageView2);
		
		button3.setVisibility(View.INVISIBLE);
		button4.setVisibility(View.INVISIBLE);
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Log.i(tag, "onclick menulist button1");
				goExistingRecord();				
			}
		});
		
		button2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Log.i(tag, "onclick menulist button2 ");
				goCamera();				
			}
		});
		
	}
	
	private void goExistingRecord(){
		Intent it = new Intent(this, ExistingRecordActivity.class);
		it.putExtra("xxx", "xxx");
		startActivity(it);
	}

    /**
     *
     */
    private void goCamera(){
		String fname = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";
		String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
		imgUri = Uri.parse("file://"+ dir + "/" + fname);
		Log.i(tag, "uri="+imgUri.toString());
		Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		it.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
		startActivityForResult(it, 100);
	}

    private int getRatio(int width, int height) {
        double ratio;
        ratio = (double)width/(double)height;
        if(ratio  > 1.7)
            return ratio16_9;
        else
            return ratio4_3;
    }

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		text1 = (TextView) findViewById(R.id.textView1);
		text2 = (TextView) findViewById(R.id.textView2);
        button1.setVisibility(View.INVISIBLE);
        button2.setVisibility(View.INVISIBLE);
		Log.i(tag, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 100 && resultCode == Activity.RESULT_OK){
			Log.i(tag, "capture picture");
			
			//immutable BMP
			Bitmap bmp = BitmapFactory.decodeFile(imgUri.getPath());			
			Config config;
			if(bmp.getConfig() != null){
				config = bmp.getConfig();
			}else{
				config = Config.ARGB_8888;
			}
			
			//mutable BMP
			bmpMain = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), config);
			text2.setText("bitmap width:"+bmp.getWidth()+"height:"+ bmp.getHeight());
			
			canvasMain = new Canvas(bmpMain);
			canvasMain.drawBitmap(bmp, 0, 0, null);
			imvMain.setImageBitmap(bmpMain);
			
			bmpPain = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), config);
			canvasPain = new Canvas(bmpPain);
			imvPain.setImageBitmap(bmpPain);


            imvMain.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    int action = event.getAction();
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    //text1.setText(String.valueOf(x));
                    //text2.setText(String.valueOf(y));
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            text1.setText("ACTION_DOWN (" + x + " , " + y + ")");
                            startPt = projectXY((ImageView) v, bmpPain, x, y);
                            endPt = new projectPt();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            text1.setText("ACTION_MOVE (" + x + " , " + y + ")");
                            drawOnRectProjectedBitMap((ImageView) v, bmpPain, x, y);
                            break;
                        case MotionEvent.ACTION_UP:
                            text1.setText("ACTION_UP (" + x + " , " + y + ")");
                            drawOnRectProjectedBitMap((ImageView) v, bmpPain, x, y);
                            finalizeDrawing();
                            break;
                    }
                    /*
					 * Return 'true' to indicate that the event have been
					 * consumed. If auto-generated 'false', your code can detect
					 * ACTION_DOWN only, cannot detect ACTION_MOVE and
					 * ACTION_UP.
					 */
                    return true;
                }

            });
		}else{
			Toast.makeText(this, "no picture", Toast.LENGTH_LONG).show();
		}
	}
	
	class projectPt {
		int x;
		int y;

		projectPt(int tx, int ty) {
			x = tx;
			y = ty;
		}
		
		projectPt(){
			
		}
	}

	private projectPt projectXY(ImageView iv, Bitmap bp, int x, int y) {
		if (x < 0 || y < 0 || x > iv.getWidth() || y > iv.getHeight()) {
			// outside ImageView
			return null;
		} else {
			int projectedX = (int)((double)x * ((double)bp.getWidth()/(double)iv.getWidth()));
			int projectedY = (int)((double)y * ((double)bp.getHeight()/(double)iv.getHeight()));

			return new projectPt(projectedX, projectedY);
		}
	}
	
	
	private void drawOnRectProjectedBitMap(ImageView iv, Bitmap bp, int x, int y) {
		if (x < 0 || y < 0 || x > iv.getWidth() || y > iv.getHeight()) {
			// outside ImageView
			return;
		} else {
			int projectedX = (int)((double)x * ((double)bp.getWidth()/(double)iv.getWidth()));
			int projectedY = (int)((double)y * ((double)bp.getHeight()/(double)iv.getHeight()));
			Paint paint = new Paint();
			// clear canvasDrawingPane
            canvasPain.drawColor(Color.TRANSPARENT, Mode.CLEAR);

			if(recordStatus == 0){
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(20);
                canvasPain.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
                paint.setStyle(Paint.Style.FILL);
                canvasPain.drawCircle(startPt.x, startPt.y, 60, paint);
                canvasPain.drawCircle(projectedX, projectedY, 60, paint);
                canvasPain.drawCircle(startPt.x+(projectedX-startPt.x), startPt.y , 60, paint);
                canvasPain.drawCircle(startPt.x, startPt.y+(projectedY-startPt.y) , 60, paint);
                paint.setStrokeWidth(5);
                canvasPain.drawLine(startPt.x, startPt.y+(projectedY-startPt.y)/3, projectedX, startPt.y+(projectedY-startPt.y)/3, paint);
                canvasPain.drawLine(startPt.x, startPt.y+(projectedY-startPt.y)/3*2, projectedX, startPt.y+(projectedY-startPt.y)/3*2, paint);
                canvasPain.drawLine(startPt.x+(projectedX-startPt.x)/3, startPt.y, startPt.x+(projectedX-startPt.x)/3, projectedY, paint);
                canvasPain.drawLine(startPt.x+(projectedX-startPt.x)/3*2, startPt.y, startPt.x+(projectedX-startPt.x)/3*2, projectedY, paint);
			}else if(recordStatus == 1){
                paint.setStyle(Paint.Style.STROKE);
				paint.setColor(Color.RED);
                paint.setStrokeWidth(10);
                canvasPain.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
			}else if(recordStatus >= 2){
                paint.setStyle(Paint.Style.STROKE);
				paint.setColor(Color.YELLOW);
                paint.setStrokeWidth(10);
                canvasPain.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
			}
            imvMain.invalidate();
            endPt.x = projectedX;
            endPt.y = projectedY;
		}
	}
	
	private void finalizeDrawing(){
		button3.setVisibility(View.VISIBLE);
		button4.setVisibility(View.VISIBLE);
//		button5.setVisibility(View.INVISIBLE);
		button3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				text1.setText("OK");
				if(recordStatus == 0){
					roiSetting = new LinkedList <roiInfo> ();
					roiInfo temp = new roiInfo("ROI", startPt.x, startPt.y, endPt.x, endPt.y);
					roiSetting.add(temp);
				}else if(recordStatus == 1){
					if(roiSetting != null){
						roiInfo temp = new roiInfo("Banner", startPt.x, startPt.y, endPt.x, endPt.y);
						roiSetting.add(temp);
					}
				}else {
					if(roiSetting != null){

						String target = String.format("target%d", (recordStatus-2));
						roiInfo temp = new roiInfo(target, startPt.x, startPt.y, endPt.x, endPt.y);
						roiSetting.add(temp);
						button5.setVisibility(View.VISIBLE);
						button5.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View arg0) {
								// send the data to server control(picture and axis)
                                    transferDKDK();
                                }
						});
					}
				}
				
				canvasMain.drawBitmap(bmpPain, 0, 0, null);
				button3.setVisibility(View.INVISIBLE);
				button4.setVisibility(View.INVISIBLE);
				recordStatus++;
			}
		});
		
		button4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				text1.setText("Clean");
				Paint paint = new Paint();	
				paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
				canvasPain.drawPaint(paint);
				paint.setXfermode(new PorterDuffXfermode(Mode.SRC));
				button3.setVisibility(View.INVISIBLE);
				button4.setVisibility(View.INVISIBLE);
			}
		});
	}

	class rect {
		int sx,sy;
		int ex,ey;
	}
	
	class roiInfo {
		String roiName;
		int id;
		rect roi;
		roiInfo(String name, int sx, int sy, int ex, int ey){
			roi = new rect();
			roi.sx = sx;
			roi.sy = sy;
			roi.ex = ex;
			roi.ey = ey;
			roiName = name;
		}
		
		roiInfo(){
			roi = new rect();
		}
	}

    private boolean transferDKDK()
    {

        // upload picture
        Log.i(tag,"upload picture  " + imgUri.getPath());
        Bitmap bmp=BitmapFactory.decodeFile(imgUri.getPath());
        if(bmp==null)
            return false;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG , 100 , stream);
        byte [] data = stream.toByteArray();
        List<NameValuePair> post = new ArrayList<NameValuePair>();

        post.add(new BasicNameValuePair("FILE", Base64.encodeBytes(data)));
        post.add(new BasicNameValuePair("Img_W", Integer.toString(bmp.getWidth())));
        post.add(new BasicNameValuePair("Img_H", Integer.toString(bmp.getHeight())));

        numRoi = roiSetting.size();
        Log.i(tag, "roi size ="+numRoi);
        post.add(new BasicNameValuePair("TagNum", Integer.toString(numRoi-1)));

        tempRoi = roiSetting.get(0);
        post.add(new BasicNameValuePair("ROI_X", Integer.toString(tempRoi.roi.sx)));
        post.add(new BasicNameValuePair("ROI_Y", Integer.toString(tempRoi.roi.sy)));
        post.add(new BasicNameValuePair("ROI_W", Integer.toString(tempRoi.roi.sx-tempRoi.roi.ex)));
        post.add(new BasicNameValuePair("ROI_H", Integer.toString(tempRoi.roi.sy-tempRoi.roi.ey)));
        Log.i(tag, "ROI: name:" + tempRoi.roiName + " SX:" + tempRoi.roi.sx);
        for(int i=1; i<=numRoi-1; i++) {
            tempRoi = roiSetting.get(i);
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_T", " "));  // Tag Type
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_X", Integer.toString(tempRoi.roi.sx)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_Y", Integer.toString(tempRoi.roi.sy)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_W", Integer.toString(tempRoi.roi.sx-tempRoi.roi.ex)));
            post.add(new BasicNameValuePair("TAG"+Integer.toString(i)+"_H", Integer.toString(tempRoi.roi.sy-tempRoi.roi.ey)));
            Log.i(tag, "TAG:" + i + " name:" + tempRoi.roiName + " SX:" + tempRoi.roi.sx);
        }
        post.add(new BasicNameValuePair("NUM", Integer.toString(save_num)));
        httpControl.httpHandleCmd(httpControl.url_upload, post,httpControl.UPLOAD);
        bmp.recycle();
        return true;
    }

	@Override
	public void onPause() {
		super.onPause();

	}
	
	@Override
	public void onResume() {
		super.onResume();

	}
	
}
