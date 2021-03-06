package com.example.myfarmserver;

import java.util.*;

import android.app.*;
import android.os.*;
import android.webkit.*;

public class Cctv extends Activity {
	// private static final String MOVIE_URL = "http://192.168.0.219:8080/?action=stream";

	
	private TimerTask mTask;
    private Timer mTimer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_cctv);

		final WebView wv = (WebView) findViewById(R.id.webView1);
		wv.setFocusable(false);
		
		
		mTask = new TimerTask() {
          
            @Override
			public void run() {
            	if (wv != null) {
        			wv.loadUrl( "http://192.168.0.251:8080/?action=snapshot");
        		}
            }
        };
         
        mTimer = new Timer();

        mTimer.schedule(mTask, 0, 100);

	}
	
}
