package com.example.myfarm;

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
		h.postDelayed(irun, 1000); // 4초 동안 인트로 화면
	}
	
	Runnable irun = new Runnable(){
		public void run(){
			Intent i = new Intent(IntroActivity.this, MainActivity.class);
			startActivity(i);
			finish();
			
			//fade in 으로 시작하여 fade out 으로 인트로 화면이 꺼지는 애니메이션 추가 
		
			overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
	};

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
