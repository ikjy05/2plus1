package com.example.myfarmserver;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

public class Board extends Activity implements OnClickListener {

	// 설정 온도 저장 변수
	String str_low_temp = "20";
	String str_high_temp = "30";
	EditText edit_low_temp = null;
	EditText edit_high_temp = null;

	// 설정 조도 저장 변수
	String str_low_lux = "350";
	String str_high_lux = "400";
	EditText edit_low_lux = null;
	EditText edit_high_lux = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_board);

		// 각 이미지 추출 및 객체로 변환
		edit_low_temp = (EditText) findViewById(R.id.edit_low_temp);
		edit_high_temp = (EditText) findViewById(R.id.edit_high_temp);

		edit_low_lux = (EditText) findViewById(R.id.edit_low_lux);
		edit_high_lux = (EditText) findViewById(R.id.edit_high_lux);

		Button btn_save = (Button) findViewById(R.id.btn_save);
		btn_save.setOnClickListener(this);

		// 온도데이터 메모리에서 추출해서 사용
		SharedPreferences sp_low_temp = getSharedPreferences("LowTemp", 0);
		SharedPreferences sp_high_temp = getSharedPreferences("HighTemp", 0);
		String low_temp = sp_low_temp.getString("LT", "");
		String high_temp = sp_high_temp.getString("HT", "");
		edit_low_temp.setText(low_temp);
		edit_high_temp.setText(high_temp);

		// 조도데이터 메모리에서 추출해서 사용
		SharedPreferences sp_low_lux = getSharedPreferences("LowLux", 0);
		SharedPreferences sp_high_lux = getSharedPreferences("HighLux", 0);
		String low_lux = sp_low_lux.getString("LL", "");
		String high_lux = sp_high_lux.getString("HL", "");
		edit_low_lux.setText(low_lux);
		edit_high_lux.setText(high_lux);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.board, menu);
		return true;
	}

	@Override
	public void onClick(View v) {

		// EditText의 온도 입력값 스트링 변수에 저장
		str_low_temp = edit_low_temp.getText().toString();
		str_high_temp = edit_high_temp.getText().toString();

		// EditText의 조도 입력값 스트링 변수에 저장
		str_low_lux = edit_low_lux.getText().toString();
		str_high_lux = edit_high_lux.getText().toString();

		// 형변환 (String => Int) 후 스트링변수에 데이터 저장
		MainActivity.set_low_temp = Integer.valueOf(str_low_temp);
		MainActivity.set_high_temp = Integer.valueOf(str_high_temp);

		MainActivity.set_low_lux = Integer.valueOf(str_low_lux);
		MainActivity.set_high_lux = Integer.valueOf(str_high_lux);

		// 온도 데이터 메모리에 저장해 놓기
		SharedPreferences sp_low_temp = getSharedPreferences("LowTemp", 0);
		SharedPreferences sp_high_temp = getSharedPreferences("HighTemp", 0);
		SharedPreferences.Editor editor_LT = sp_low_temp.edit();
		SharedPreferences.Editor editor_HT = sp_high_temp.edit();
		editor_LT.putString("LT", str_low_temp);
		editor_LT.commit(); // 저장
		editor_HT.putString("HT", str_high_temp);
		editor_HT.commit(); // 저장

		// 조도 데이터 메모리에 저장해 놓기
		SharedPreferences sp_low_lux = getSharedPreferences("LowLux", 0);
		SharedPreferences sp_high_lux = getSharedPreferences("HighLux", 0);
		SharedPreferences.Editor editor_LL = sp_low_lux.edit();
		SharedPreferences.Editor editor_HL = sp_high_lux.edit();
		editor_LL.putString("LL", str_low_lux);
		editor_LL.commit(); // 저장
		editor_HL.putString("HL", str_high_lux);
		editor_HL.commit(); // 저장

	}

}
