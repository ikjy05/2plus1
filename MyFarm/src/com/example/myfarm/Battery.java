package com.example.myfarm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.view.Menu;
import android.widget.ProgressBar;

public class Battery extends Activity {

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			updateBattery(MainActivity.Battery_value);
			mHandler.sendEmptyMessageDelayed(0, 100);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_battery);

		ProgressBar progressBar;
		progressBar = (ProgressBar) findViewById(R.id.pbar_adk);
		progressBar.setMax(130);

		mHandler.sendEmptyMessageDelayed(0, 100);
	}

	private void updateBattery(short value) {
		// Log.e(TAG, "updateBattery = " + value);
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.pbar_adk);
		progressBar.setProgress(value - 520);
	}
}
