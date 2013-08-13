package com.example.myfarmserver;

import android.os.Bundle;
import android.app.Activity;
import android.view.*;
import android.view.View.*;
import android.widget.*;

public class Setting extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		
		final ToggleButton tog_Firesensor = (ToggleButton) findViewById(R.id.firesensor);
		final ToggleButton tog_Invadesensor = (ToggleButton) findViewById(R.id.invadesensor);
		
		
		//화재 센서 리스너
		tog_Firesensor.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Perform action on clicks
				if (tog_Firesensor.isChecked()) {
					Toast.makeText(Setting.this, "화재 센서 작동을 시작합니다.",
							Toast.LENGTH_SHORT).show();

					

				} else {
					Toast.makeText(Setting.this, "화재 센서를 해지합니다.",
							Toast.LENGTH_SHORT).show();

					
				}
			}
		});
		
		
		tog_Invadesensor.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Perform action on clicks
				if (tog_Invadesensor.isChecked()) {
					Toast.makeText(Setting.this, "침입 센서 작동을 시작합니다.",
							Toast.LENGTH_SHORT).show();

					

				} else {
					Toast.makeText(Setting.this, "침입 센서를 해지합니다.",
							Toast.LENGTH_SHORT).show();

					
				}
			}
		});
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.setting, menu);
		return true;
	}

}
