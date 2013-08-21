package com.example.myfarmserver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnClickListener {

	/*** 소켓 정의 ***/
	private static ServerSocket serverSocket;
	private static int port = 8888;
	static TextView tv_soc;
	static Handler handler;

	public static String recMessage = "CMD";
	public static String sndMessage = "CMD";
	private String tempData = null;

	public static boolean soc_flag;

	/*******************************************/
	MjpegView mv;
	final static int BUFF_LEN = 4112;
	
	private final static String TAG = "ODROID";

	/*** 블루투스 송/수신 메시지 정의 ***/
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// 송신 메시지 정의
	public static final byte SEND_LETTER_I2C = 'i';
	public static final byte SEND_LETTER_BH1780 = 'l';
	public static final byte SEND_LETTER_BMP180 = 'b';
	public static final byte SEND_LETTER_FIRMWARE_VERSION = 'v';
	public static final byte SEND_LETTER_CONSTANT = 'c';
	public static final byte SEND_LETTER_TEMPERATURE = 't';
	public static final byte SEND_LETTER_PRESSURE = 'p';
	public static final byte SEND_LETTER_BATT = 'b';
	public static final byte SEND_LETTER_MOTOR = 'm';
	public static final byte SEND_LETTER_LUX = 'r';
	public static final byte SEND_LETTER_GPIO_CONFIG = 'g';
	public static final byte SEND_LETTER_GPIO_INPUT = 's';
	public static final byte SEND_LETTER_GPIO_OUTPUT = 'o';
	public static final byte SEND_LETTER_ADC_CONFIG = 'a';

	// 수신 메시지 정의
	public static final byte RECEIVED_LETTER_BATT = 'B';
	public static final byte RECEIVED_LETTER_I2C = 'I';
	public static final byte RECEIVED_LETTER_BH1780 = 'L';
	public static final byte RECEIVED_LETTER_BMP180 = 'B';
	public static final byte RECEIVED_LETTER_PRESSURE = 'P';
	public static final byte RECEIVED_LETTER_TEMPERATURE = 'T';
	public static final byte RECEIVED_LETTER_CALIBRATIONDATA = 'C';
	public static final byte RECEIVED_LETTER_FIRMWARE_VERSION = 'V';
	public static final byte RECEIVED_LETTER_GPIO_CONFIG = 'G';
	public static final byte RECEIVED_LETTER_GPIO_INPUT = 'S';
	public static final byte RECEIVED_LETTER_ADC = 'A';
	public static final byte RECEIVED_LETTER_LUX = 'R';

	// 포트 정의
	public static final int ADC_PORT = 2;
	public static final int LED_2_PORT = 2;
	public static final int KEY_3_PORT = 3;
	public static final int KEY_4_PORT = 4;
	public static final int LED_5_PORT = 5;

	// 변수 정의
	public static final int HIGH = 1; // 입력 데이터 디지털 값으로 표시를 위한 변수 초기화
	public static final int LOW = 0; // 입력 데이터 디지털 값으로 표시를 위한 변수 초기화
	public static boolean inputSMOG_flag; // 연기센서 감지 판별을 위한 boolean 변수
	public static boolean inputPROX_flag; // 근접센서 감지 판별을 위한 boolean 변수
	public static boolean inputTEMP_flag; // 온도센서 감지 판별을 위한 boolean 변수
	public static int lux; // 조도값 저장 변수
	public static long temperature; // 온도값 저장 변수
	public static short Battery_value; // 배터리 용량값 저장 변수

	// 버퍼 정의
	private byte[] mBuf = new byte[22]; // 수신데이터 저장 버퍼
	private int mIndex = 0; // 수신데이터 버퍼 인덱스
	private byte[] commandPacket = new byte[5]; // 송신데이터 저장 버퍼

	// message type
	public String mMessageType;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private BMP180 mPressureSensor; // 온도센서 저장 변수

	// 블루투스 연결을 위한 변수 정의
	private final static String ROBOT_BT_ADDRESS = "MACAddress"; // 블루투스 재연결을 위한
																	// 변수
	private String mMACAddress;
	private SharedPreferences settings;

	/*** 실시간 데이터 송/수신을 위한 타이머 정의 ***/

	private Timer mRefreshTimer;
	private RefreshTask mRefreshTask;
	private Handler mTimerHandler;

	class RefreshTask extends TimerTask {
		private int count = 0;

		@Override
		public void run() {
			mTimerHandler.sendEmptyMessage(count);
			count++;
			if (count == 21)
				count = 0;
		}
	}

	private void startTimer() {
		Log.e(TAG, "startTimer");
		mRefreshTimer = new Timer();
		mRefreshTask = new RefreshTask();
		mRefreshTimer.schedule(mRefreshTask, 0, 40);
	}

	private void stopTimer() {
		if (mRefreshTimer != null) {
			mRefreshTimer.cancel();
			mRefreshTimer.purge();
		}
	}

	// 시간지연을 위한 쓰레드 정의
	class DelayThread extends Thread {

		@Override
		public void run() {
			try {
				Thread.sleep(2000); // 2초마다
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// 개폐기를 닫기 위한 패킷을 정의
			commandPacket[0] = 'm';
			commandPacket[1] = 0;
			commandPacket[2] = 2;
			commandPacket[3] = '-';
			commandPacket[4] = 0;

			mCmdSendService.write(commandPacket); // 패킷 송신

			controlWindowsopener_flag = false; // 수동모드(우선순위)를 비활성화
		}
	};

	// Name of the connected device
	private String mConnectedDeviceName = null;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	static BluetoothMotorControlService mCmdSendService = null;
	private boolean bluetoothPaired = false;

	// Buttons menu
	private final static int MENU_ITEM_CONNECT = Menu.FIRST;
	private final static int MENU_ITEM_DISCONNECT = Menu.FIRST + 1;
	private final static int MENU_IP_ADDRESS = Menu.FIRST + 2;
	private final static int MENU_ITEM_EXIT = Menu.FIRST + 3;

	/*** 이미지 정의 ***/
	// 버튼 이미지 정의
//	static final int[] BUTTONS = { R.id.cctv, R.id.battery, R.id.setting,
//			R.id.lux, R.id.fan, R.id.heater };
	static final int[] BUTTONS = { R.id.battery, R.id.setting,
		R.id.lux, R.id.fan, R.id.heater };

	// 텍스트뷰 이미지 정의
	static final int[] TEXTVIEW = { R.id.textView1, R.id.textView2,
			R.id.textView4, R.id.textView5 };
	private TextView tv_temp, tv_lux, tv_hum;

	// 리니어 레이아웃 정의
	private LinearLayout linear_board;

	/*********************************/

	// 온도부
	public static int set_low_temp = 20; // default 값
	public static int set_high_temp = 30;

	private boolean flag_heater_off;
	private boolean flag_fan_off;

	// 조도부
	public static int set_low_lux = 350; // default 값
	public static int set_high_lux = 400;

	private boolean controlLight_flag = false;

	// 감지부(연기,근접)
	private TextView key_tv, smog_tv;

	// 스프링쿨러, 개폐기를 비활성화
	private boolean sprinkler_flag = false;
	private boolean windowsopener_flag = false;
	// 수동모드(우선순위)를 비활성화
	private boolean controlSprinkler_flag = false;
	private boolean controlWindowsopener_flag = false;

	/*********************************/

	// AlertDialog를 띄우기 위한 변수
	private AlertDialog ad_lux, ad_fan, ad_heater;

	// 화면 전환 출력 위한 int형 flag
	private int sprinkler_int_flag = 0;
	private int windowopener_int_flag = 0;
	private int lux_int_flag = 0;
	private int fan_int_flag = 0;
	private int heater_int_flag = 0;

	ToggleButton togbtnSprinkler;
	ToggleButton togbtnWindowsOpener;

	// 실시간 출력을 위한 핸들러
	private Handler mmHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			/*** 온도 출력 ****************************/

			tv_temp.setTypeface(Typeface.createFromAsset(getAssets(),
					"DS-DIGI.TTF")); // 외부 폰트 적용
			tv_temp.setTextSize(60); // 글씨 크기 적용
			tv_temp.setText(String.format("%.1f", temperature * 0.1)); // 온도값 출력

			if (inputTEMP_flag) { // 온도감지 시

				// 형 변환 (Long -> String -> Int)
				String temp_str = Long.toString(temperature);
				int temp = Integer.valueOf(temp_str);
				temp *= 0.1;

				if (rcvthread.socRec_flag) {

					set_low_temp = Integer.valueOf(rcvthread.rcvLowSetTemp);
					set_high_temp = Integer.valueOf(rcvthread.rcvHighSetTemp);
				}
				// 설정 온도에 따른 제어 함수
				updateTemp(temp, set_low_temp, set_high_temp);

			}

			/*** 조도 출력 ****************************/

			tv_lux.setTypeface(Typeface.createFromAsset(getAssets(),
					"DS-DIGI.TTF")); // 외부 폰트 적용
			tv_lux.setTextSize(60); // 글씨 크기 적용
			tv_lux.setText("" + lux); // 조도값 출력

			if (!controlLight_flag) { // 조도감지 시

				if (rcvthread.socRec_flag) {

					set_low_lux = Integer.valueOf(rcvthread.rcvLowSetLux);
					set_high_lux = Integer.valueOf(rcvthread.rcvHighSetLux);
				}

				// 설정 조도에 따른 제어 함수
				updateLux(lux, set_low_lux, set_high_lux);
			}

			/*** 접근 감지 ****************************/
			// 접근감지에 따른 제어 함수
			updateKeyState(inputPROX_flag);

			/*** 연기 감지 ****************************/
			// 연기감지에 따른 제어 함수
			updateSmogState(inputSMOG_flag);

			if (sprinkler_flag && !controlSprinkler_flag) {

				// 스프링쿨러 ON을 위한 패킷을 정의
				commandPacket[0] = 'm';
				commandPacket[1] = 0;
				commandPacket[2] = 1;
				commandPacket[3] = '+';
				commandPacket[4] = 11;

				mCmdSendService.write(commandPacket); // 패킷 송신
			} else if (!sprinkler_flag && !controlSprinkler_flag) {

				// 스프링쿨러 OFF을 위한 패킷을 정의
				commandPacket[0] = 'm';
				commandPacket[1] = 0;
				commandPacket[2] = 1;
				commandPacket[3] = '-';
				commandPacket[4] = 0;

				mCmdSendService.write(commandPacket); // 패킷 송신
			}
			if (windowsopener_flag && !controlWindowsopener_flag) {

				// 개폐기 ON을 위한 패킷을 정의
				commandPacket[0] = 'm';
				commandPacket[1] = 0;
				commandPacket[2] = 2;
				commandPacket[3] = '+';
				commandPacket[4] = 11;

				mCmdSendService.write(commandPacket); // 패킷 송신
			} else if (!windowsopener_flag && !controlWindowsopener_flag) {

				// 개폐기 OFF을 위한 패킷을 정의
				commandPacket[0] = 'm';
				commandPacket[1] = 0;
				commandPacket[2] = 2;
				commandPacket[3] = '-';
				commandPacket[4] = 0;

				mCmdSendService.write(commandPacket); // 패킷 송신
			}

			/***************************************/

			Button lux = (Button) findViewById(R.id.lux);
			Button fan = (Button) findViewById(R.id.fan);
			Button heater = (Button) findViewById(R.id.heater);

			switch (lux_int_flag) {
			case 0:
				lux.setBackgroundResource(R.drawable.lightoff);
				break;
			case 1:
				lux.setBackgroundResource(R.drawable.light1);
				break;
			case 2:
				lux.setBackgroundResource(R.drawable.light2);
				break;
			default:
				break;
			}

			switch (fan_int_flag) {
			case 0:
				fan.setBackgroundResource(R.drawable.fanoff);
				break;
			case 1:
				fan.setBackgroundResource(R.drawable.fan1);
				break;
			case 2:
				fan.setBackgroundResource(R.drawable.fan2);
				break;
			default:
				break;
			}

			switch (heater_int_flag) {
			case 0:
				heater.setBackgroundResource(R.drawable.heateroff);
				break;
			case 1:
				heater.setBackgroundResource(R.drawable.heater1);
				break;
			case 2:
				heater.setBackgroundResource(R.drawable.heater2);
				break;
			default:
				break;
			}

			switch (sprinkler_int_flag) {
			case 0:
				togbtnSprinkler.setBackgroundResource(R.drawable.sprinkleroff);
				break;
			case 1:
				togbtnSprinkler.setBackgroundResource(R.drawable.sprinkleron);
				break;
			default:
				break;
			}

			switch (windowopener_int_flag) {
			case 0:
				togbtnWindowsOpener
						.setBackgroundResource(R.drawable.windowsclose);
				break;
			case 1:
				togbtnWindowsOpener
						.setBackgroundResource(R.drawable.windowsopen);
				break;
			default:
				break;
			}

			mmHandler.sendEmptyMessageDelayed(0, 500);
		}
	};

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build()); 


		try {
			shellCommand("mjpg-streamer -i \"input_uvc.so -d /dev/video4\" -o \"output_http.so -p 9000\"\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Log.e("shell", "Camera On Failed");
			Toast.makeText(this, "Camera on failed", Toast.LENGTH_SHORT).show();
		}
		//shellCommand("ls");

		/*********************************************/

		
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				if (msg.what == 1) {
					tempData = msg.obj.toString();

//					Log.e("****tempData", tempData);
//					// tempData = rcvStrData;
//
//					if (tempData.equals(recMessage)) {
//						recMessage = "X";
//
//					} else if (!tempData.equals(recMessage)) {
//						Log.e("####recMessage", recMessage);
						recMessage = tempData;
//						Log.e("@@@@recMessage", recMessage);
//						// 모든 데이터 삽입
//					}

					if (recMessage.equals("SN")) {
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 1;
						commandPacket[3] = '+';
						commandPacket[4] = 11;

						mCmdSendService.write(commandPacket);

						sprinkler_int_flag = 1;

					} else if (recMessage.equals("SF")) {
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 1;
						commandPacket[3] = '-';
						commandPacket[4] = 0;

						mCmdSendService.write(commandPacket);

						sprinkler_int_flag = 0;
					} else if (recMessage.equals("OO")) {
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 2;
						commandPacket[3] = '+';
						commandPacket[4] = 11;

						mCmdSendService.write(commandPacket);
						windowopener_int_flag = 1;

					} else if (recMessage.equals("OF")) {
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 2;
						commandPacket[3] = '-';
						commandPacket[4] = 0;

						mCmdSendService.write(commandPacket);

					} else if (recMessage.equals("OC")) {
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 2;
						commandPacket[3] = '-';
						commandPacket[4] = 11;

						mCmdSendService.write(commandPacket);
						windowopener_int_flag = 0;

					} else if (recMessage.equals("L1FL2F")) {
						// 조도 LED1 OFF를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 2;
						commandPacket[3] = 0;
						commandPacket[4] = 0;

						mCmdSendService.write(commandPacket); // 패킷 송신

						// 조도 LED2 OFF를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 5;
						commandPacket[3] = 0;
						commandPacket[4] = 0;

						mCmdSendService.write(commandPacket); // 패킷 송신

						lux_int_flag = 0; // 조도버튼을 OFF상태로

					} else if (recMessage.equals("L1OL2O")) {
						// 조도 LED1 ON를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 2;
						commandPacket[3] = 0;
						commandPacket[4] = 1;

						mCmdSendService.write(commandPacket); // 패킷 송신

						// 조도 LED2 OFF를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 5;
						commandPacket[3] = 0;
						commandPacket[4] = 1;

						mCmdSendService.write(commandPacket); // 패킷 송신

						lux_int_flag = 2; // 조도버튼을 2단계 상태로

					} else if (recMessage.equals("L1OL2F")) {
						// 조도 LED1 ON를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 2;
						commandPacket[3] = 0;
						commandPacket[4] = 1;

						mCmdSendService.write(commandPacket);// 패킷 송신

						// 조도 LED2 ON를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 5;
						commandPacket[3] = 0;
						commandPacket[4] = 0;

						mCmdSendService.write(commandPacket);// 패킷 송신

						lux_int_flag = 1; // 조도버튼을 1단계 상태로

					} else if (recMessage.equals("FF")) {
						// 펜 OFF를 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 3;
						commandPacket[3] = '-';
						commandPacket[4] = 0;

						mCmdSendService.write(commandPacket);// 패킷 송신

						fan_int_flag = 0; // 펜 버튼을 OFF 상태로

					} else if (recMessage.equals("F1")) {
						// 펜 LOW 출력을 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 3;
						commandPacket[3] = '+';
						commandPacket[4] = (byte) 255;

						mCmdSendService.write(commandPacket);// 패킷 송신

						fan_int_flag = 1; // 펜 버튼을 1단계 상태로
					} else if (recMessage.equals("F2")) {
						// 펜 HIGH 출력을 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 3;
						commandPacket[3] = '-';
						commandPacket[4] = (byte) 255;

						mCmdSendService.write(commandPacket);// 패킷 송신

						fan_int_flag = 2; // 펜 버튼을 2단계 상태로
					} else if (recMessage.equals("HF")) {
						// 히터 OFF를 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 4;
						commandPacket[3] = '-';
						commandPacket[4] = 0;

						mCmdSendService.write(commandPacket);// 패킷 송신

						heater_int_flag = 0; // 히터 버튼을 OFF 상태로
					}

					else if (recMessage.equals("H1")) {
						// 히터 LOW 출력을 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 4;
						commandPacket[3] = '+';
						commandPacket[4] = (byte) 50;

						mCmdSendService.write(commandPacket);// 패킷 송신

						heater_int_flag = 1; // 히터 버튼을 1단계 상태로

					} else if (recMessage.equals("H2")) {
						// 히터 HIGH 출력을 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 4;
						commandPacket[3] = '-';
						commandPacket[4] = (byte) 255;

						mCmdSendService.write(commandPacket);// 패킷 송신

						heater_int_flag = 2; // 히터 버튼을 2단계 상태로

					} else if (recMessage.equals("DO")) {
						// 자동문 OPEN을 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 5;
						commandPacket[3] = '+';
						commandPacket[4] = 22;

						mCmdSendService.write(commandPacket); // 패킷 송신

					} else if (recMessage.equals("DC")) {
						// 자동문 CLOSE를 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 5;
						commandPacket[3] = '-';
						commandPacket[4] = 22;

						mCmdSendService.write(commandPacket); // 패킷 송신
					}
				}
			}
		};

		new Thread() {
			@Override
			public void run() {
				try {
					System.out.println("Start Teleop Server : "
							+ InetAddress.getLocalHost() + "(" + port + ")");

					serverSocket = new ServerSocket(port);

					while (true) {

						Socket clientSocket = serverSocket.accept();

						Thread thread = new Thread(new rcvthread(clientSocket,
								clientSocket.getRemoteSocketAddress()));
						thread.start();

						if (clientSocket.isConnected()) {
							System.out.println("Connected Client IP : "
									+ clientSocket.getRemoteSocketAddress());
						}
					}
				} catch (IOException e) {
					System.out.println("Exception: " + e);
				}
			}
		}.start();

		/*********************************************/
		// 정의된 버튼을 객체로 변환 및 버튼 이벤트
		for (int btnId : BUTTONS) {
			Button btn = (Button) findViewById(btnId);
			btn.setOnClickListener(this);
		}

		// 정의된 텍스트뷰를 객체로 변환 및 버튼 이벤트
		for (int tvId : TEXTVIEW) {
			TextView tv = (TextView) findViewById(tvId);
			// 외부 폰트 적용
			tv.setTypeface(Typeface.createFromAsset(getAssets(), "DS-DIGI.TTF"));
			tv.setTextSize(30); // 글씨 크기 적용
		}

		// 정의된 레이아웃을 객체로 변환 및 버튼 이벤트
		linear_board = (LinearLayout) findViewById(R.id.layout_board);
		linear_board.setOnClickListener(this);

		// 정의된 토글버튼을 객체로 변환
		togbtnWindowsOpener = (ToggleButton) findViewById(R.id.windowsopener);
		togbtnSprinkler = (ToggleButton) findViewById(R.id.sprinkler);

		// 정의된 텍스트뷰를 객체로 변환
		tv_temp = (TextView) findViewById(R.id.tv_temp);
		tv_lux = (TextView) findViewById(R.id.tv_lux);
		key_tv = (TextView) findViewById(R.id.textView_key);
		smog_tv = (TextView) findViewById(R.id.textView_smog);

		// /*** 습도 출력 ****************************/
		// tv_hum.setTypeface(Typeface.createFromAsset(getAssets(),
		// "DS-DIGI.TTF")); // 외부폰트적용
		// tv_hum.setTextSize(60); // 글자크기 적용
		// tv_hum.setText("30"); // 습도값 출력

		// 개폐기 수동모드를 위한 토글버튼 이벤트
		togbtnWindowsOpener.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Perform action on clicks
				if (togbtnWindowsOpener.isChecked()) {
					Toast.makeText(MainActivity.this, "개폐기 OPEN",
							Toast.LENGTH_SHORT).show();

					// 개폐기 OPEN을 위한 패킷을 정의
					commandPacket[0] = 'm';
					commandPacket[1] = 0;
					commandPacket[2] = 2;
					commandPacket[3] = '+';
					commandPacket[4] = 11;

					mCmdSendService.write(commandPacket); // 패킷 송신

					// 클라이언트 메시지 송신(소켓)
					sndMessage = "OO";

					controlWindowsopener_flag = true; // 수동모드(우선순위)를 활성화

					windowopener_int_flag = 1;

					new DelayThread().start(); // 시간지연 시작

				} else {
					Toast.makeText(MainActivity.this, "개폐기 CLOSE",
							Toast.LENGTH_SHORT).show();

					// 개폐기 CLOSE을 위한 패킷을 정의
					commandPacket[0] = 'm';
					commandPacket[1] = 0;
					commandPacket[2] = 2;
					commandPacket[3] = '-';
					commandPacket[4] = 11;

					mCmdSendService.write(commandPacket); // 패킷 송신
					controlWindowsopener_flag = false; // 수동모드(우선순위)를 비활성화

					// 클라이언트 메시지 송신(소켓)
					sndMessage = "OC";

					windowopener_int_flag = 0;

					new DelayThread().start(); // 시간지연 시작
				}
			}
		});

		// 스프링쿨러 수동모드를 위한 토글버튼 이벤트
		togbtnSprinkler.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Perform action on clicks
				if (togbtnSprinkler.isChecked()) {
					Toast.makeText(MainActivity.this, "스프링쿨러 ON",
							Toast.LENGTH_SHORT).show();

					// 스프링쿨러 ON을 위한 패킷을 정의
					commandPacket[0] = 'm';
					commandPacket[1] = 0;
					commandPacket[2] = 1;
					commandPacket[3] = '+';
					commandPacket[4] = 11;

					mCmdSendService.write(commandPacket); // 패킷 송신

					// 클라이언트 메시지 송신(소켓)
					sndMessage = "SN";

					controlSprinkler_flag = true; // 수동모드(우선순위)를 활성화

					sprinkler_int_flag = 1;

				} else {
					Toast.makeText(MainActivity.this, "스프링쿨러 OFF",
							Toast.LENGTH_SHORT).show();

					// 스프링쿨러 OFF을 위한 패킷을 정의
					commandPacket[0] = 'm';
					commandPacket[1] = 0;
					commandPacket[2] = 1;
					commandPacket[3] = '-';
					commandPacket[4] = 0;

					mCmdSendService.write(commandPacket); // 패킷 송신

					// 클라이언트 메시지 송신(소켓)
					sndMessage = "SF";

					controlSprinkler_flag = false; // 수동모드(우선순위)를 비활성화

					sprinkler_int_flag = 0;
				}
			}
		});

		// 실시간 입력을 받기 위한 핸들러
		mTimerHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case 0:
					getGPIO(KEY_3_PORT); // IO 3번 포트 입력 함수
					break;
				case 1:
					getLux(); // 조도 입력 함수
					break;
				case 2:
					getGPIO(KEY_4_PORT); // IO 4번 포트 입력 함수
					break;
				case 3:
					getADC(); // ADC 입력 함수
					break;
				case 4:
					getGPIO(KEY_3_PORT);// IO 3번 포트 입력 함수
					break;
				case 5:
					requestTemperature(); // 온도 입력 함수
					break;
				case 6:
					getGPIO(KEY_4_PORT); // IO 4번 포트 입력 함수
					break;
				case 7:
					getADC(); // ADC 입력 함수
					break;
				case 8:
					getGPIO(KEY_3_PORT); // IO 3번 포트 입력 함수
					break;
				case 9:
					getADC(); // ADC 입력 함수
					break;
				case 10:
					getGPIO(KEY_4_PORT); // IO 4번 포트 입력 함수
					break;
				case 11:
					requestBattery(); // 배터리 입력 함수
					break;
				case 12:
					getGPIO(KEY_3_PORT); // IO 3번 포트 입력 함수
					break;
				case 13:
					break;
				case 14:
					getGPIO(KEY_4_PORT); // IO 4번 포트 입력 함수
					break;
				case 15:
					getADC(); // ADC 입력 함수
					break;
				case 16:
					getGPIO(KEY_3_PORT); // IO 3번 포트 입력 함수
					break;
				case 17:
					getLux(); // 조도 입력 함수
					break;
				case 18:
					getGPIO(KEY_4_PORT); // IO 4번 포트 입력 함수
					break;
				case 19:
					requestPressure(); // 고도 입력 함수
					break;
				case 20:
					getGPIO(KEY_3_PORT); // IO 3번 포트 입력 함수
					break;
				}
			}
		};

		// Change needed for the alarm.
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		mMACAddress = settings.getString(ROBOT_BT_ADDRESS, "");

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mPressureSensor = new BMP180();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		mmHandler.sendEmptyMessageDelayed(0, 500);
		
		///카메라
		mv = (MjpegView) findViewById(R.id.mv);
		String URL = "http://127.0.0.1:9000/?action=stream";
		new DoRead().execute(URL);
	} // OnCreate

	// 클릭 이벤트 발생 시 처리 함수
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
//		case R.id.cctv: // cctv 버튼 클릭 시 Cctv 액티비티로 이동
//			Intent i00 = new Intent(this, Cctv.class);
//			startActivity(i00);
//			break;

		case R.id.layout_board: // layout_board 버튼 클릭 시 Board 액티비티로 이동
			Intent i01 = new Intent(this, Board.class);
			startActivity(i01);
			break;

		case R.id.battery: // battery 버튼 클릭 시 Battery 액티비티로 이동
			Intent i02 = new Intent(this, Battery.class);
			startActivity(i02);
			break;

		case R.id.setting: // setting 버튼 클릭 시 Setting 액티비티로 이동
			Intent serverIntent = new Intent(getApplicationContext(),
					DeviceListActivity.class);
			startActivityForResult(serverIntent,
					MainActivity.REQUEST_CONNECT_DEVICE);
			break;

		case R.id.lux: // btn_lux 버튼 클릭 시

			luxAlertDialog(); // luxAlertDialog 함수 실행
			ad_lux.show(); // AlertDialog 출력
			break;

		case R.id.fan: // btn_fan 버튼 클릭 시

			fanAlertDialog(); // fanAlertDialog 함수 실행
			ad_fan.show(); // AlertDialog 출력
			break;

		case R.id.heater: // btn_heater 버튼 클릭 시

			heaterAlertDialog(); // heaterAlertDialog 함수 실행
			ad_heater.show(); // AlertDialog 출력
			break;

		}

	}

	/*** 수동모드 AlertDialog ****************************/
	// 조도 수동모드를 위한 AlertDialog 함수
	private void luxAlertDialog() {
		ad_lux = new AlertDialog.Builder(this)
				.setTitle("조도조절")
				.setIcon(R.drawable.ic_launcher)
				.setMessage("조도값을 조절해 주세요")
				.setPositiveButton("OFF",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								controlLight_flag = false; // 수동모드(우선순위)를 비활성화

								lux_int_flag = 0; // 화면을 OFF상태로

								Toast.makeText(MainActivity.this,
										"조도 수동모드 OFF", Toast.LENGTH_SHORT)
										.show();

								// 조도 LED1 OFF를 위한 패킷을 정의
								commandPacket[0] = 'o';
								commandPacket[1] = 0;
								commandPacket[2] = 2;
								commandPacket[3] = 0;
								commandPacket[4] = 0;

								mCmdSendService.write(commandPacket); // 패킷 송신

								// 조도 LED2 OFF를 위한 패킷을 정의
								commandPacket[0] = 'o';
								commandPacket[1] = 0;
								commandPacket[2] = 5;
								commandPacket[3] = 0;
								commandPacket[4] = 0;

								mCmdSendService.write(commandPacket); // 패킷 송신
								// 클라이언트 메시지 송신(소켓)
								sndMessage = "L1FL2F";
							}
						})
				.setNeutralButton("2단계", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						controlLight_flag = true; // 수동모드(우선순위)를 활성화

						lux_int_flag = 2; // 화면을 2단계 상태로

						Toast.makeText(MainActivity.this, "조도 수동모드 2단계",
								Toast.LENGTH_SHORT).show();

						// 조도 LED1 ON를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 2;
						commandPacket[3] = 0;
						commandPacket[4] = 1;

						mCmdSendService.write(commandPacket); // 패킷 송신

						// 조도 LED2 OFF를 위한 패킷을 정의
						commandPacket[0] = 'o';
						commandPacket[1] = 0;
						commandPacket[2] = 5;
						commandPacket[3] = 0;
						commandPacket[4] = 1;

						mCmdSendService.write(commandPacket); // 패킷 송신
						// 클라이언트 메시지 송신(소켓)
						sndMessage = "L1OL2O";
					}
				})
				.setNegativeButton("1단계",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								controlLight_flag = true; // 수동모드(우선순위)를 활성화

								lux_int_flag = 1; // 화면을 1단계 상태로

								Toast.makeText(MainActivity.this,
										"조도 수동모드 1단계", Toast.LENGTH_SHORT)
										.show();

								// 조도 LED1 ON를 위한 패킷을 정의
								commandPacket[0] = 'o';
								commandPacket[1] = 0;
								commandPacket[2] = 2;
								commandPacket[3] = 0;
								commandPacket[4] = 1;

								mCmdSendService.write(commandPacket);// 패킷 송신

								// 조도 LED2 ON를 위한 패킷을 정의
								commandPacket[0] = 'o';
								commandPacket[1] = 0;
								commandPacket[2] = 5;
								commandPacket[3] = 0;
								commandPacket[4] = 0;

								mCmdSendService.write(commandPacket);// 패킷 송신
								// 클라이언트 메시지 송신(소켓)
								sndMessage = "L1OL2F";

							}
						}).create();
	}

	// 펜 수동모드를 위한 AlertDialog 함수
	private void fanAlertDialog() {
		ad_fan = new AlertDialog.Builder(this)
				.setTitle("FAN 조절")
				.setIcon(R.drawable.ic_launcher)
				.setMessage("FAN 값을 조절해 주세요")
				.setPositiveButton("OFF",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								fan_int_flag = 0;

								Toast.makeText(MainActivity.this,
										"FAN 수동모드 OFF", Toast.LENGTH_SHORT)
										.show();

								// 펜 OFF를 위한 패킷을 정의
								commandPacket[0] = 'm';
								commandPacket[1] = 0;
								commandPacket[2] = 3;
								commandPacket[3] = '-';
								commandPacket[4] = 0;

								mCmdSendService.write(commandPacket);// 패킷 송신
								// 클라이언트 메시지 송신(소켓)
								sndMessage = "FF";
							}
						})
				.setNeutralButton("2단계", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						fan_int_flag = 2;

						Toast.makeText(MainActivity.this, "FAN 수동모드 2단계",
								Toast.LENGTH_SHORT).show();

						// 펜 HIGH 출력을 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 3;
						commandPacket[3] = '-';
						commandPacket[4] = (byte) 255;

						mCmdSendService.write(commandPacket);// 패킷 송신

						// 클라이언트 메시지 송신(소켓)
						sndMessage = "F2";
					}
				})
				.setNegativeButton("1단계",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								fan_int_flag = 1;

								Toast.makeText(MainActivity.this,
										"FAN 수동모드 1단계", Toast.LENGTH_SHORT)
										.show();

								// 펜 LOW 출력을 위한 패킷을 정의
								commandPacket[0] = 'm';
								commandPacket[1] = 0;
								commandPacket[2] = 3;
								commandPacket[3] = '+';
								commandPacket[4] = (byte) 255;

								mCmdSendService.write(commandPacket);// 패킷 송신

								// 클라이언트 메시지 송신(소켓)
								sndMessage = "F1";

							}
						}).create();
		
		///카메라
		mv = (MjpegView) findViewById(R.id.mv);
		String URL = "http://127.0.0.1:9000/?action=stream";
		new DoRead().execute(URL);
	}

	// 히터 수동모드를 위한 AlertDialog 함수
	private void heaterAlertDialog() {
		ad_heater = new AlertDialog.Builder(this)
				.setTitle("HEATER 조절")
				.setIcon(R.drawable.ic_launcher)
				.setMessage("HEATER 값을 조절해 주세요")
				.setPositiveButton("OFF",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								heater_int_flag = 0;

								Toast.makeText(MainActivity.this,
										"HEATER 수동모드 OFF", Toast.LENGTH_SHORT)
										.show();

								// 히터 OFF를 위한 패킷을 정의
								commandPacket[0] = 'm';
								commandPacket[1] = 0;
								commandPacket[2] = 4;
								commandPacket[3] = '-';
								commandPacket[4] = 0;

								mCmdSendService.write(commandPacket);// 패킷 송신

								// 클라이언트 메시지 송신(소켓)
								sndMessage = "HF";
							}
						})
				.setNeutralButton("2단계", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						heater_int_flag = 2;

						Toast.makeText(MainActivity.this, "HEATER 수동모드 2단계",
								Toast.LENGTH_SHORT).show();

						// 히터 HIGH 출력을 위한 패킷을 정의
						commandPacket[0] = 'm';
						commandPacket[1] = 0;
						commandPacket[2] = 4;
						commandPacket[3] = '-';
						commandPacket[4] = (byte) 255;

						mCmdSendService.write(commandPacket);// 패킷 송신

						// 클라이언트 메시지 송신(소켓)
						sndMessage = "H2";
					}
				})
				.setNegativeButton("1단계",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								heater_int_flag = 1;

								Toast.makeText(MainActivity.this,
										"HEATER 수동모드 1단계", Toast.LENGTH_SHORT)
										.show();

								// 히터 LOW 출력을 위한 패킷을 정의
								commandPacket[0] = 'm';
								commandPacket[1] = 0;
								commandPacket[2] = 4;
								commandPacket[3] = '+';
								commandPacket[4] = (byte) 50;

								mCmdSendService.write(commandPacket);// 패킷 송신

								// 클라이언트 메시지 송신(소켓)
								sndMessage = "H1";

							}
						}).create();
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			short port = 0;
			short value = 0;

			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothMotorControlService.STATE_CONNECTED:
					// mTitle.setText(R.string.title_connected_to);
					// mTitle.append(mConnectedDeviceName);
					mIndex = 0;
					initI2C();
					bluetoothPaired = true;
					break;

				case BluetoothMotorControlService.STATE_CONNECTING:
					// mTitle.setText(R.string.title_connecting);
					break;

				case BluetoothMotorControlService.STATE_LISTEN:
				case BluetoothMotorControlService.STATE_NONE:
					// mTitle.setText(R.string.title_not_connected);
					stopTimer();
					bluetoothPaired = false;
					break;
				}
				break;

			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(
						getApplicationContext(),
						getString(R.string.connected_to) + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;

			case MESSAGE_TOAST:
				if (msg.getData().getString(TOAST).equals("unable connect")) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.unable_connect),
							Toast.LENGTH_SHORT).show();
				} else if (msg.getData().getString(TOAST)
						.equals("connection lost")) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.connection_lost),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getApplicationContext(),
							msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
							.show();
				}
				break;

			case SEND_LETTER_GPIO_CONFIG:
				if (mCmdSendService.getState() != BluetoothMotorControlService.STATE_CONNECTED) {
					Toast.makeText(getBaseContext(), R.string.not_connected,
							Toast.LENGTH_SHORT).show();
					return;
				}

				if (commandPacket[0] != 0) {
					return;
				}

				commandPacket[0] = SEND_LETTER_GPIO_CONFIG;
				commandPacket[1] = 0;
				commandPacket[2] = (byte) msg.arg1;
				commandPacket[3] = 0;
				commandPacket[4] = (byte) msg.arg2;

				mCmdSendService.write(commandPacket);
				break;

			case SEND_LETTER_FIRMWARE_VERSION:
				if (mCmdSendService.getState() != BluetoothMotorControlService.STATE_CONNECTED) {
					return;
				}

				commandPacket[0] = SEND_LETTER_FIRMWARE_VERSION;
				commandPacket[1] = 0;
				commandPacket[2] = 0;
				commandPacket[3] = 0;
				commandPacket[4] = 0;

				mCmdSendService.write(commandPacket);
				break;

			case SEND_LETTER_GPIO_INPUT:
				if (mCmdSendService.getState() != BluetoothMotorControlService.STATE_CONNECTED) {
					return;
				}

				if (commandPacket[0] != 0) {
					// Log.e(TAG, "returned");
					return;
				}

				commandPacket[0] = SEND_LETTER_GPIO_INPUT;
				commandPacket[1] = 0;
				commandPacket[2] = (byte) msg.arg1;
				commandPacket[3] = 0;
				commandPacket[4] = 0;

				mCmdSendService.write(commandPacket);
				break;

			case SEND_LETTER_GPIO_OUTPUT:
				if (mCmdSendService.getState() != BluetoothMotorControlService.STATE_CONNECTED) {
					Toast.makeText(getBaseContext(), R.string.not_connected,
							Toast.LENGTH_SHORT).show();
					return;
				}

				commandPacket[0] = SEND_LETTER_GPIO_OUTPUT;
				commandPacket[1] = 0;
				commandPacket[2] = (byte) msg.arg1;
				commandPacket[3] = 0;
				if (msg.arg2 == HIGH)
					commandPacket[4] = HIGH;
				else
					commandPacket[4] = LOW;

				mCmdSendService.write(commandPacket);
				break;

			case SEND_LETTER_MOTOR:
				if (mCmdSendService.getState() != BluetoothMotorControlService.STATE_CONNECTED) {
					return;
				}

				value = (short) msg.arg2;
				commandPacket[0] = SEND_LETTER_MOTOR;
				commandPacket[1] = 0;
				commandPacket[2] = (byte) msg.arg1;
				if (msg.arg1 == 1)
					value *= -1;
				if (value > 0) {
					commandPacket[3] = '-';
					commandPacket[4] = (byte) value;
				} else {
					commandPacket[3] = '+';
					commandPacket[4] = (byte) (value * -1);
				}
				mCmdSendService.write(commandPacket);
				break;

			case SEND_LETTER_ADC_CONFIG:
				if (mCmdSendService.getState() != BluetoothMotorControlService.STATE_CONNECTED) {
					return;
				}

				commandPacket[0] = SEND_LETTER_ADC_CONFIG;
				commandPacket[1] = 0;
				commandPacket[2] = (byte) msg.arg1;
				commandPacket[3] = 0;
				commandPacket[4] = (byte) msg.arg2;

				mCmdSendService.write(commandPacket);
				break;

			case SEND_LETTER_BATT:
				if (commandPacket[0] != 0) {
					return;
				}

				commandPacket[0] = SEND_LETTER_BATT;

				mCmdSendService.write(commandPacket);
				break;

			case SEND_LETTER_I2C:

				if (msg.arg1 == SEND_LETTER_I2C) {
					commandPacket[0] = SEND_LETTER_I2C;
					commandPacket[1] = SEND_LETTER_I2C;
				}

				if (msg.arg1 == SEND_LETTER_BH1780) {
					commandPacket[0] = SEND_LETTER_I2C;
					commandPacket[1] = SEND_LETTER_BH1780;
				}

				if (msg.arg1 == SEND_LETTER_BMP180) {
					commandPacket[0] = SEND_LETTER_I2C;
					commandPacket[1] = SEND_LETTER_BMP180;
				}

				if (msg.arg1 == SEND_LETTER_CONSTANT) {
					commandPacket[0] = SEND_LETTER_I2C;
					commandPacket[1] = SEND_LETTER_CONSTANT;
					commandPacket[2] = 0x00;
					commandPacket[3] = (byte) msg.arg2;
				}
				if (msg.arg1 == SEND_LETTER_LUX) {
					if (commandPacket[0] != 0) {
						// Log.e(TAG, "returned");
						return;
					}
					commandPacket[0] = SEND_LETTER_I2C;
					commandPacket[1] = SEND_LETTER_LUX;
				}
				if (msg.arg1 == SEND_LETTER_TEMPERATURE) {
					if (commandPacket[0] != 0) {
						// Log.e(TAG, "returned");
						return;
					}
					commandPacket[0] = SEND_LETTER_I2C;
					commandPacket[1] = SEND_LETTER_TEMPERATURE;
				}

				if (msg.arg1 == SEND_LETTER_PRESSURE) {
					if (commandPacket[0] != 0) {
						// Log.e(TAG, "returned");
						return;
					}
					commandPacket[0] = SEND_LETTER_I2C;
					commandPacket[1] = SEND_LETTER_PRESSURE;
				}

				mCmdSendService.write(commandPacket);
				break;

			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;

				if (readBuf[0] == commandPacket[0] + ('A' - 'a')) {
					commandPacket[0] = 0;
				}

				switch (readBuf[0]) {
				case RECEIVED_LETTER_GPIO_INPUT:
					port = readBuf[2];
					value = readBuf[4];
					if (port == KEY_3_PORT) {
						if (value == HIGH) {
							inputPROX_flag = true;
						} else {
							inputPROX_flag = false;
						}
					}

					if (port == KEY_4_PORT) {
						if (value == HIGH) {
							inputSMOG_flag = true;
						} else {
							inputSMOG_flag = false;
						}
					}
					break;

				case RECEIVED_LETTER_GPIO_CONFIG:
					port = readBuf[2];
					if (port == LED_2_PORT) {
						initGPIO(LED_5_PORT, true);
					} else if (port == LED_5_PORT) {
						initGPIO(KEY_3_PORT, false);
					} else if (port == KEY_3_PORT) {
						initGPIO(KEY_4_PORT, false);
					} else if (port == KEY_4_PORT) {
						getCalibrationData(mPressureSensor.getRegisterAddress());
					}
					break;

				case RECEIVED_LETTER_ADC:
					value = (short) (readBuf[3] << 8);
					value |= (short) (readBuf[4] & 0x00ff);
					// updateADC(value);
					break;

				case RECEIVED_LETTER_BATT: {
					value = (short) (readBuf[3] << 8);
					value |= (short) (readBuf[4] & 0x00ff);
					Battery_value = value;
				}
					break;

				case RECEIVED_LETTER_FIRMWARE_VERSION: {
					value = (short) (readBuf[1] << 8);
					value |= readBuf[2];
					short major = value;
					value = (short) (readBuf[3] << 8);
					value |= readBuf[4];
					short minior = value;
					// updateVersion(major, minior);
				}
					break;

				case RECEIVED_LETTER_I2C:
					if (readBuf[1] == RECEIVED_LETTER_I2C) {
						initBH1780();
					} else if (readBuf[1] == RECEIVED_LETTER_BH1780) {
						initBMP180();
					} else if (readBuf[1] == RECEIVED_LETTER_BMP180) {
						getCalibrationData(mPressureSensor.getRegisterAddress());
					} else if (readBuf[1] == RECEIVED_LETTER_LUX) {
						lux = readBuf[2] << 8 & 0x0000ff00;
						lux |= readBuf[3] & 0x000000ff;
						// updateLux(lux);
					} else if (readBuf[1] == RECEIVED_LETTER_TEMPERATURE) {
						ByteBuffer bb = ByteBuffer.allocate(4);
						bb.order(ByteOrder.LITTLE_ENDIAN);
						bb.put(readBuf[2]);
						bb.put(readBuf[3]);
						bb.rewind();
						if (mPressureSensor.isAvailable()) {
							temperature = mPressureSensor
									.calculateTrueTemperature(bb.getInt());
							inputTEMP_flag = true;
							// updateTemperature(temperature);
						}
					} else if (readBuf[1] == RECEIVED_LETTER_PRESSURE) {
						ByteBuffer bb = ByteBuffer.allocate(4);
						bb.order(ByteOrder.LITTLE_ENDIAN);
						bb.put(readBuf[2]);
						bb.put(readBuf[3]);
						bb.rewind();
						long pressure = 0;
						if (mPressureSensor.isAvailable()) {
							pressure = mPressureSensor.calculateTruePressure(bb
									.getInt());
							// updatePressure(pressure);
						}
						long altitude = (long) mPressureSensor
								.calculateAltitude(pressure);
						// updateAltitude(altitude);
					} else if (readBuf[1] == RECEIVED_LETTER_CALIBRATIONDATA) {
						if (updateCalibrationData(readBuf)) {
							mPressureSensor.setNextRegisterAddress();
							getCalibrationData(mPressureSensor
									.getRegisterAddress());
						}
					}
					break;
				}
			default:
				break;
			}// switch
		} // handleMessage
	}; // handler
	private String out;

	private void initDevice() {
		Log.e(TAG, "initDevice");
		initI2C();
		initBH1780();
		initBMP180();
		initGPIO(LED_2_PORT, true);
		initGPIO(LED_5_PORT, true);
		initGPIO(KEY_3_PORT, false);
		initGPIO(KEY_4_PORT, false);
		getCalibrationData(mPressureSensor.getRegisterAddress());
	}

	private void getCalibrationData(byte addr) {
		Message msg = Message.obtain(mHandler, SEND_LETTER_I2C,
				SEND_LETTER_CONSTANT, addr);
		mHandler.sendMessage(msg);
	}

	private void initI2C() {
		Log.e(TAG, "initI2C");
		Message msg = Message.obtain(mHandler, SEND_LETTER_I2C,
				SEND_LETTER_I2C, 0);
		mHandler.sendMessage(msg);
	}

	private void initBH1780() {
		Log.e(TAG, "initBH1780");
		Message msg = Message.obtain(mHandler, SEND_LETTER_I2C,
				SEND_LETTER_BH1780, 0);
		mHandler.sendMessage(msg);
	}

	private void initBMP180() {
		Log.e(TAG, "initBMP180");
		Message msg = Message.obtain(mHandler, SEND_LETTER_I2C,
				SEND_LETTER_BMP180, 0);
		mHandler.sendMessage(msg);
	}

	private void initGPIO(int port, boolean input) {
		Log.e(TAG, "initGPIO(port:" + port + ")");
		Message msg = Message.obtain(mHandler, SEND_LETTER_GPIO_CONFIG, port,
				input ? HIGH : LOW);
		mHandler.sendMessage(msg);
	}

	private void requestVersion() {
		Log.e(TAG, "getVersion");
		Message msg = Message.obtain(mHandler, SEND_LETTER_FIRMWARE_VERSION, 0,
				0);
		mHandler.sendMessage(msg);
	}

	private void requestBattery() {
		// Log.e(TAG, "getBattery");
		Message msg = Message.obtain(mHandler, SEND_LETTER_BATT, 0, 0);
		mHandler.sendMessage(msg);
	}

	private void requestTemperature() {
		// Log.e(TAG, "getTemperature");
		Message msg = Message.obtain(mHandler, SEND_LETTER_I2C,
				SEND_LETTER_TEMPERATURE, 0);
		mHandler.sendMessage(msg);
	}

	private void requestPressure() {
		// Log.e(TAG, "getPressure");
		Message msg = Message.obtain(mHandler, SEND_LETTER_I2C,
				SEND_LETTER_PRESSURE, 0);
		mHandler.sendMessage(msg);
	}

	private void getGPIO(int index) {
		// Log.e(TAG, "getGPIO");
		Message msg = Message
				.obtain(mHandler, SEND_LETTER_GPIO_INPUT, index, 0);
		mHandler.sendMessage(msg);
	}

	private void getADC() {
		// Log.e(TAG, "getADC");
		Message msg = Message.obtain(mHandler, SEND_LETTER_ADC_CONFIG,
				ADC_PORT, 0);
		mHandler.sendMessage(msg);
	}

	private void getLux() {
		// Log.e(TAG, "getLux");
		Message msg = Message.obtain(mHandler, SEND_LETTER_I2C,
				SEND_LETTER_LUX, 0);
		mHandler.sendMessage(msg);
	}

	private boolean updateCalibrationData(byte[] value) {
		mBuf[mIndex++] = value[2];
		mBuf[mIndex++] = value[3];

		if (mIndex == 22) {
			fillCalibarationData(mBuf);
			requestVersion();
			startTimer();
			return false;
		} else
			return true;
	}

	// 온도에 따른 제어 함수
	private void updateTemp(int temp, int set_low_temp, int set_high_temp) {

		// 온도감지 & 현재온도 < 설정온도 일경우
		if (temp < set_low_temp && inputTEMP_flag) {

			// 히터 ON을 위한 패킷을 정의
			commandPacket[0] = 'm';
			commandPacket[1] = 0;
			commandPacket[2] = 4;
			commandPacket[3] = '-';
			commandPacket[4] = (byte) 255;

			mCmdSendService.write(commandPacket); // 패킷 송신

			if (!flag_heater_off)
				Toast.makeText(getApplicationContext(), "Heater가 작동됩니다.",
						Toast.LENGTH_SHORT).show();

			flag_heater_off = true; // 히터 비활성화

		}

		// 히터 비활성화 & 현재온도 >= 설정온도 일경우
		else if (temp >= set_low_temp && flag_heater_off) {

			// 히터 OFF을 위한 패킷을 정의
			commandPacket[0] = 'm';
			commandPacket[1] = 0;
			commandPacket[2] = 4;
			commandPacket[3] = '-';
			commandPacket[4] = 0;

			mCmdSendService.write(commandPacket); // 패킷 송신

			if (flag_heater_off)
				Toast.makeText(getApplicationContext(), "Heater가 OFF됩니다.",
						Toast.LENGTH_SHORT).show();

			flag_heater_off = false; // 히터 활성화
		}

		// 현재온도 > 설정온도 일경우
		else if (temp > set_high_temp) {

			// 펜 ON을 위한 패킷을 정의
			commandPacket[0] = 'm';
			commandPacket[1] = 0;
			commandPacket[2] = 3;
			commandPacket[3] = '-';
			commandPacket[4] = (byte) 255;

			mCmdSendService.write(commandPacket); // 패킷 송신

			if (!flag_fan_off)
				Toast.makeText(getApplicationContext(), "Fan이 작동됩니다.",
						Toast.LENGTH_SHORT).show();

			flag_fan_off = true; // 펜 비활성화
		}

		// 펜 비활성화 & 현재온도 <= 설정온도 일경우
		else if (temp <= set_high_temp && flag_fan_off) {

			// 히터 ON을 위한 패킷을 정의
			commandPacket[0] = 'm';
			commandPacket[1] = 0;
			commandPacket[2] = 3;
			commandPacket[3] = '-';
			commandPacket[4] = 0;

			mCmdSendService.write(commandPacket); // 패킷 송신
			if (flag_fan_off)
				Toast.makeText(getApplicationContext(), "Fan이 OFF됩니다.",
						Toast.LENGTH_SHORT).show();

			flag_fan_off = false; // 펜 활성화
		}

	}

	// 조도에 따른 제어 함수
	private void updateLux(int lux, int set_low_lux, int set_high_lux) {

		// 조도 < 설정 Low 조도일 경우
		if (lux < set_low_lux) {

			// 조도 LED1 ON을 위한 패킷을 정의
			commandPacket[0] = 'o';
			commandPacket[1] = 0;
			commandPacket[2] = 2;
			commandPacket[3] = 0;
			commandPacket[4] = 1;

			mCmdSendService.write(commandPacket); // 패킷 송신

			// 조도 LED2 ON을 위한 패킷을 정의
			commandPacket[0] = 'o';
			commandPacket[1] = 0;
			commandPacket[2] = 5;
			commandPacket[3] = 0;
			commandPacket[4] = 1;

			mCmdSendService.write(commandPacket); // 패킷 송신
		}

		// 설정 LOW < 조도 < 설정 HIGH 조도일 경우
		else if (lux > set_low_lux && lux < set_high_lux) {

			// 조도 LED1 ON을 위한 패킷을 정의
			commandPacket[0] = 'o';
			commandPacket[1] = 0;
			commandPacket[2] = 2;
			commandPacket[3] = 0;
			commandPacket[4] = 1;

			mCmdSendService.write(commandPacket); // 패킷 송신

			// 조도 LED2 OFF를 위한 패킷을 정의
			commandPacket[0] = 'o';
			commandPacket[1] = 0;
			commandPacket[2] = 5;
			commandPacket[3] = 0;
			commandPacket[4] = 0;

			mCmdSendService.write(commandPacket); // 패킷 송신
		}

		// 조도 > 설정 HIGH 조도일 경우
		else if (lux > set_high_lux) {

			// 조도 LED1 OFF를 위한 패킷을 정의
			commandPacket[0] = 'o';
			commandPacket[1] = 0;
			commandPacket[2] = 2;
			commandPacket[3] = 0;
			commandPacket[4] = 0;

			mCmdSendService.write(commandPacket); // 패킷 송신

			// 조도 LED2 OFF를 위한 패킷을 정의
			commandPacket[0] = 'o';
			commandPacket[1] = 0;
			commandPacket[2] = 5;
			commandPacket[3] = 0;
			commandPacket[4] = 0;

			mCmdSendService.write(commandPacket); // 패킷 송신
		}
	}

	// 근접센서에 따른 제어 함수
	private void updateKeyState(boolean on) {
		if (on) {
			key_tv.setText("접근이 감지되었습니다.");
			key_tv.setBackgroundColor(Color.RED);
			key_tv.setTextColor(Color.BLACK);

			// 자동문 OPEN을 위한 패킷을 정의
			commandPacket[0] = 'm';
			commandPacket[1] = 0;
			commandPacket[2] = 5;
			commandPacket[3] = '+';
			commandPacket[4] = 22;

			mCmdSendService.write(commandPacket); // 패킷 송신

		} else {
			key_tv.setText("안전합니다.");
			key_tv.setBackgroundColor(Color.rgb(165, 239, 58));
			key_tv.setTextColor(Color.BLACK);

			// 자동문 CLOSE를 위한 패킷을 정의
			commandPacket[0] = 'm';
			commandPacket[1] = 0;
			commandPacket[2] = 5;
			commandPacket[3] = '-';
			commandPacket[4] = 22;

			mCmdSendService.write(commandPacket); // 패킷 송신
		}
	}

	// 연기센서에 따른 제어 함수
	private void updateSmogState(boolean on) {
		if (on) {
			smog_tv.setText("연기가 감지되었습니다.");
			smog_tv.setBackgroundColor(Color.RED);
			smog_tv.setTextColor(Color.BLACK);

			sprinkler_flag = true;
			windowsopener_flag = true;
			// sprinkler_int_flag = 1;
			// windowopener_int_flag = 1;

		} else {
			smog_tv.setText("연기가 감지되지 않았습니다.");
			smog_tv.setBackgroundColor(Color.rgb(165, 239, 58));
			smog_tv.setTextColor(Color.BLACK);

			sprinkler_flag = false;
			windowsopener_flag = false;
			// sprinkler_int_flag = 0;
			// windowopener_int_flag = 0;
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mCmdSendService == null)
				setupBT();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mCmdSendService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mCmdSendService.getState() == BluetoothMotorControlService.STATE_NONE) {
				// Start the Bluetooth chat services
				mCmdSendService.start();
			}
		}
	}

	private void setupBT() {
		// Initialize the BluetoothChatService to perform bluetooth connections
		mCmdSendService = new BluetoothMotorControlService(this, mHandler);
		mCmdSendService.setIndexOfMessages(MESSAGE_STATE_CHANGE, MESSAGE_READ,
				MESSAGE_DEVICE_NAME, MESSAGE_TOAST);
		mCmdSendService.setDeviceNameString(DEVICE_NAME);
		mCmdSendService.setToastString(TOAST);

		if (mMACAddress.length() != 0) {
			Log.e(TAG, "MAC address " + mMACAddress);
			BluetoothDevice device = mBluetoothAdapter
					.getRemoteDevice(mMACAddress);
			mCmdSendService.connect(device);
		}
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		// Stop the Bluetooth chat services
		/*
		 * if (mCmdSendService != null) mCmdSendService.stop();
		 */
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mCmdSendService != null)
			mCmdSendService.stop();
		stopTimer();
	}

	private void stop() {
		// Log.e(TAG, "stop");
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mCmdSendService.connect(device);

				SharedPreferences.Editor edit = settings.edit();
				edit.putString(ROBOT_BT_ADDRESS, address);
				edit.commit();
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupBT();
			} else {
				// User did not enable Bluetooth or an error occured
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ITEM_CONNECT, 0, getString(R.string.connect)).setIcon(
				android.R.drawable.ic_menu_search);
		menu.add(0, MENU_ITEM_DISCONNECT, 0, getString(R.string.disconnect))
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0, MENU_IP_ADDRESS, 0, getString(R.string.ip_address))
				.setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, MENU_ITEM_EXIT, 0, getString(R.string.exit)).setIcon(
				R.drawable.exit);
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (bluetoothPaired) {
			menu.findItem(MENU_ITEM_DISCONNECT).setVisible(true);
		} else {
			menu.findItem(MENU_ITEM_DISCONNECT).setVisible(false);
		}
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_CONNECT:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case MENU_ITEM_DISCONNECT:
			// Stop the Bluetooth chat services
			if (mCmdSendService != null)
				mCmdSendService.stop();
			// mTitle.setText(R.string.title_not_connected);
			stopTimer();
			return true;

		case MENU_IP_ADDRESS:
			Toast.makeText(getBaseContext(), tryGetIpAddress(),
					Toast.LENGTH_SHORT).show();

			return true;

		case MENU_ITEM_EXIT:
			if (mCmdSendService != null)
				mCmdSendService.stop();
			finish();
			System.exit(0);
			return true;
		}
		return false;
	}

	private void fillCalibarationData(byte[] buf) {
		ByteBuffer bb2short = ByteBuffer.allocate(2);
		bb2short.order(ByteOrder.LITTLE_ENDIAN);
		bb2short.put(buf[0]);
		bb2short.put(buf[1]);
		short AC1 = bb2short.getShort(0);
		Log.e(TAG, String.format("AC1 = %d", AC1));

		bb2short.clear();
		bb2short.put(buf[2]);
		bb2short.put(buf[3]);
		short AC2 = bb2short.getShort(0);
		Log.e(TAG, String.format("AC2 = %d", AC2));

		bb2short.clear();
		bb2short.put(buf[4]);
		bb2short.put(buf[5]);
		short AC3 = bb2short.getShort(0);
		Log.e(TAG, String.format("AC3 = %d", AC3));

		ByteBuffer bb2int = ByteBuffer.allocate(4);
		bb2int.order(ByteOrder.LITTLE_ENDIAN);
		bb2int.put(buf[6]);
		bb2int.put(buf[7]);
		bb2int.rewind();
		int AC4 = bb2int.getInt();
		Log.e(TAG, String.format("AC4 = %d", AC4));

		bb2int.clear();
		bb2int.put(buf[8]);
		bb2int.put(buf[9]);
		bb2int.rewind();
		int AC5 = bb2int.getInt();
		Log.e(TAG, String.format("AC5 = %d", AC5));

		bb2int.clear();
		bb2int.put(buf[10]);
		bb2int.put(buf[11]);
		bb2int.rewind();
		int AC6 = bb2int.getInt();
		Log.e(TAG, String.format("AC6 = %d", AC6));

		bb2short.clear();
		bb2short.put(buf[12]);
		bb2short.put(buf[13]);
		short B1 = bb2short.getShort(0);
		Log.e(TAG, String.format("B1 = %d", +B1));

		bb2short.clear();
		bb2short.put(buf[14]);
		bb2short.put(buf[15]);
		short B2 = bb2short.getShort(0);
		Log.e(TAG, String.format("B2 = %d", B2));

		bb2short.clear();
		bb2short.put(buf[16]);
		bb2short.put(buf[17]);
		short MB = bb2short.getShort(0);
		Log.e(TAG, String.format("MB = %d", MB));

		bb2short.clear();
		bb2short.put(buf[18]);
		bb2short.put(buf[19]);
		short MC = bb2short.getShort(0);
		Log.e(TAG, String.format("MC = %d", MC));

		bb2short.clear();
		bb2short.put(buf[20]);
		bb2short.put(buf[21]);
		short MD = bb2short.getShort(0);
		Log.e(TAG, String.format("MD = %d", MD));

		mPressureSensor.setCalibrationData(AC1, AC2, AC3, AC4, AC5, AC6, B1,
				B2, MB, MC, MD);
	}

	private static String tryGetIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {

						if (inetAddress instanceof Inet4Address) {
							return inetAddress.getHostAddress().toString();
						}
					}

				}
			}
		} // try
		catch (final Exception e) {
			// Ignore
		} // for now eat exceptions
		return null;
	} // tryGetIpAddress()
	
	public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
		String TAG = "MJPEG";
		
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient(); 
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5*1000);
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());  
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            //if(result!=null) result.setSkip(1);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(false);
        }
    }
	
	void shellCommand(String cmd) throws IOException {
//        Runtime runtime = Runtime.getRuntime(); 
//        Process process; 
//        try { 
//        		Log.w("shell",cmd);
//
//                process = runtime.exec(cmd); 
//                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream())); 
//                String line ; 
//                while ((line = br.readLine()) != null) { 
//                	Log.w("shell",line);
//                } 
//
//        } catch (Exception e) { 
//                e.fillInStackTrace(); 
//                Log.e("Process Manager", "Unable to execute top command"); 
//        }

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
