package com.oneVipas.onedetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ServerControl {
	
	public String url_log_in = "http://54.64.165.196/log_in.php";
	public String url_sign_up = "http://54.64.165.196/sign_up.php";
	public String url_file = "http://54.64.165.196/file.php";
	public String url_upload = "http://54.64.165.196/upload.php";
	public final int REFRESH_DATA = 0x00000002,LOG_IN = 0x00000001,FILE = 0x00000003,UPLOAD = 0x00000004;
	public LoginActivity loginActivity;
	public static String user;
	public ServerControl(LoginActivity loginClass) {
		loginActivity = loginClass;
	}
	
	public void httpHandleCmd(String url, List<NameValuePair> post, int msg){	
		new Thread(new HTTP_Runnable(url,post,msg)).start();	
	}
	
	class HTTP_Runnable implements Runnable{
    	String URL;
    	List<NameValuePair> POST;
    	int MSG;
    	HTTP_Runnable(String url,List<NameValuePair> post,int msg)
    	{
    		URL = url;
    		POST = post;
    		MSG = msg;
    	}
        @Override
        public void run() {
        	byte[] result = null;
    		HttpPost httpRequest = new HttpPost(URL);			   		
    		
    		try{
    			httpRequest.addHeader("Content-Transfer-Encoding", "Binary");
    			if(POST != null) {
                    POST.add(new BasicNameValuePair("USER", user));
                    Log.i(tag,"POST user = " + user);
                    httpRequest.setEntity(new UrlEncodedFormEntity(POST, HTTP.UTF_8));
                }
    			HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
    			if(httpResponse.getStatusLine().getStatusCode() == 200) {
    				byte[] strResult = EntityUtils.toByteArray(httpResponse.getEntity());
    				result = strResult;
    			}
    		} catch (Exception e)
    		{
    			//Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
    			e.printStackTrace();
    			result = null;
    		}
    		mHandler.obtainMessage(MSG,result).sendToTarget();
        }
    }


    private String tag = "Server";
    Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			byte[] result = null;
			String str;
			
			//loginActivity.setText("");
			if(msg.obj instanceof byte[])
				result = (byte[]) msg.obj;
			//Log.i("USB",str);
			if(result!=null)
			{
				switch(msg.what)
				{
					case LOG_IN:
						str = new String(result);
						if(str.equals("login success!"))
							loginActivity.change_ui(true);
						else
							loginActivity.setText("login failed \n"+str);
						break;
					case REFRESH_DATA:
						str = new String(result);
						loginActivity.setText(str);
						//Toast.makeText(MainActivity.this,result,Toast.LENGTH_LONG).show();
						break;
					case FILE:
                        str = "";
                        try {
                            str = new String(Base64.decode(result));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(loginActivity.getParent(), str, Toast.LENGTH_LONG).show();
						/*
						 try{

//						        String data01 = "This is OutputStream Data01!";
//						        String data02 = "\n";
//						        String data03 = "Hello! This is Data02!!";
//						        String data04 = "\n";

						        //建立FileOutputStream物件，路徑為SD卡中的output.txt
						        File dir = new File(Environment.getExternalStorageDirectory()+"/USB/");
						        dir.mkdirs();

						        FileOutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory()+"/USB/output2.txt");
						        //FileOutputStream output = new FileOutputStream("/sdcard/output.txt");

//						        output.write(data01.getBytes());  //write()寫入字串，並將字串以byte形式儲存。
//						        output.write(data02.getBytes());   //利用getBytes()將字串內容換為Byte
//						        output.write(data03.getBytes());
//						        output.write(data04.getBytes());

						        output.write(result);
						        output.close();
						    }catch(Exception e){
						        e.printStackTrace();
						    }
						 */
						 break;
                    case UPLOAD:
                        str = new String(result);
                        //loginActivity.setText(str);
                        //Toast.makeText(loginActivity.getParent(),str,Toast.LENGTH_LONG).show();
                        //Toast.makeText(application,str,Toast.LENGTH_LONG).show();
                        Log.i(tag,str);
                        break;
				}
			}
		}
	};

}
