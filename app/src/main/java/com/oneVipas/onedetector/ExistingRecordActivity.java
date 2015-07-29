package com.oneVipas.onedetector;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ExistingRecordActivity extends ActionBarActivity {
    private String tag = "oneDetector";
    private int choose_record;

    private Button button[];
    private ServerControl httpControl;

    private Intent intent;
    private ProgressBar progressbar;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.i(tag, "ExistingRecordActivity onCreate");
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_existing_record);
		buttonHandler();
        httpControl = new ServerControl();

        intent = getIntent();

        progressbar = (ProgressBar) findViewById(R.id.progressBar2);

        choose_record = intent.getIntExtra("choose",0);
        Log.i(tag, "Choose " + choose_record);
	}

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.i(tag, "ExistingRecordActivity onStop");
        Log.i(tag, "Choose " + choose_record);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(tag, "ExistingRecordActivity onResume");
        Log.i(tag, "Choose " + choose_record);
        buttonHandler();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.existing_record, menu);
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

    public void buttonHandler() {
        int i;
        button = new Button[3];
        button[0] = (Button) findViewById(R.id.button1);
        button[1] = (Button) findViewById(R.id.button2);
        button[2] = (Button) findViewById(R.id.button3);
        for (i=0;i<ServerControl.saves;i++)
        {
            final int idx = i;
            button[idx].setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View arg0)
                {
                    if (choose_record != 0)
                        button[choose_record - 1].setBackgroundResource(R.drawable.button_selector_original);
                    loadFile(idx + 1);
                }
            });
            if(choose_record != idx+1)
            {
                button[idx].setBackgroundResource(R.drawable.button_selector_original);
                Log.i(tag, "Choose " + idx + " false");
            }
            else
            {
                button[idx].setBackgroundResource(R.drawable.button_selector_focused);
                Log.i(tag, "Choose " + idx + " true");
            }
        }
        for (;i<3;i++)
            button[i].setVisibility(View.INVISIBLE);

    }
    private void loadFile(int selected)
    {
        progressbar.setVisibility(View.VISIBLE);

        intent.putExtra("choose",0);
        choose_record = 0;

        List<NameValuePair> post = new ArrayList<NameValuePair>();
        post.add(new BasicNameValuePair("NUM",""+selected));

        httpControl.httpHandleCmd(httpControl.url_download, post, new ServerDone() {
            @Override
            public void execute(byte[] result) {
                progressbar.setVisibility(View.INVISIBLE);
                try {
                    // result is the DK001.dkdk.dk file streaming

                    //todo: import result to JNI

                    //建立FileOutputStream物件，路徑為SD卡中的output.txt
                    /* Saving DK001.dkdk.dk to file for checking
                    File dir = new File(Environment.getExternalStorageDirectory() + "/USB/");
                    dir.mkdirs();

                    FileOutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/USB/DK001.dkdk.dk");

                    output.write(result);
                    output.close();
                    */

                    Toast.makeText(ExistingRecordActivity.this, "Finish Download", Toast.LENGTH_LONG).show();

                    Intent it = new Intent(ExistingRecordActivity.this, CameraPreviewActivity.class);
                    it.putExtra("record", "pass");
                    it.putExtra("JNI", result);
                    startActivity(it);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        //finish();
    }
}
