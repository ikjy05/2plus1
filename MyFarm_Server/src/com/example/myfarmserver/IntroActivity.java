package com.example.myfarmserver;

import java.io.DataOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

public class IntroActivity extends Activity {

	Handler h;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro);
		
		try {
			shellCommand("mjpg-streamer -i \"input_uvc.so -d /dev/video4\" -o \"output_http.so -p 9000\"\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Log.e("shell", "Camera On Failed");
			Toast.makeText(this, "Camera on failed", Toast.LENGTH_SHORT).show();
		}
		
		h = new Handler();
		h.postDelayed(irun, 1000); // 4�� ���� ��Ʈ�� ȭ��
	}
	
	Runnable irun = new Runnable(){
		@Override
		public void run(){
			Intent i = new Intent(IntroActivity.this, MainActivity.class);
			startActivity(i);
			finish();
			
			//fade in ���� �����Ͽ� fade out ���� ��Ʈ�� ȭ���� ������ �ִϸ��̼� �߰� 
		
			overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
	};

	@Override
	public void onBackPressed(){
		super.onBackPressed();
		h.removeCallbacks(irun);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.intro, menu);
		return true;
	}
	void shellCommand(String cmd) throws IOException {


        Process p = Runtime.getRuntime().exec("sh");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        //from here all commands are executed with su permissions
        os.writeBytes(cmd); // \n executes the command
        os.writeBytes("exit\n");
        os.flush();
        //InputStream stdout = p.getInputStream();
        //read method will wait forever if there is nothing in the stream
        //so we need to read it in another way than while((read=stdout.read(buffer))>0)

        
        //do something with the output
	
	}
}
