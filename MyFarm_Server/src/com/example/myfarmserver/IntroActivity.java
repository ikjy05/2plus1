package com.example.myfarmserver;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;

public class IntroActivity extends Activity {

	Handler h;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro);
		
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

}
