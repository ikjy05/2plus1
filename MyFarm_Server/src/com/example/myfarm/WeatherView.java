package com.example.myfarm;
/**
 * keti Jeong-youn 2013.07.28
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class WeatherView extends ImageView{
	Context context;
	private LocationManager locationManager;
	private Location location;
	String mCountry;

	public WeatherView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub


		Log.i("Constructor", "1");
	}
	public WeatherView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.i("Constructor", "2");

		this.context = context;

		checkMyLocation();
		new WeatherThread().start();

	}

	public WeatherView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		Log.i("Constructor", "3");

	}

	public void checkMyLocation(){
		Log.i("fcall", "checkMyLocation");
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();


		String provider = locationManager.getBestProvider(criteria, true);

		//		locationManager.requestLocationUpdates(provider, 10000, 100, new LocationListener() {


		if(provider == null){ //gps off이면 network통해서 받아오도록..
			Toast.makeText(context.getApplicationContext(), "no GPS Provider", Toast.LENGTH_SHORT).show();
			provider = LocationManager.NETWORK_PROVIDER;
			location = locationManager.getLastKnownLocation(provider);
		}

		location = locationManager.getLastKnownLocation(provider);

		if(location == null){
			try{
				Thread.sleep(10000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			location = locationManager.getLastKnownLocation(provider);    
		}
		else {
			try {
				getAddress(location.getLatitude(), location.getLongitude());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

		}

	}

	public void getAddress(double lat, double lng) throws InterruptedException {
		Geocoder geoCoder = new Geocoder(context);

		List<Address> addresses = null;

		//		mTextView.setText(lat + " " + lng);

		try {
			addresses = geoCoder.getFromLocation(lat, lng, 5);
			Thread.sleep(500);

			if(addresses.size()>0){
				Address mAddress = addresses.get(0);
				//String Area = mAddress.getCountryName();
				String mAddressStr = 
						//				+mAddress.getCountryName()+" "
						//						+mAddress.getPostalCode()+" "
						mAddress.getLocality()+" "
						+mAddress.getSubLocality()+" "
						+mAddress.getThoroughfare()+" "
						+mAddress.getFeatureName();
				Log.i("Address", mAddressStr);
				mCountry = mAddress.getLocality();
				//				Toast.makeText(getBaseContext(), mAddressStr, Toast.LENGTH_LONG).show();

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			Locale systemLocale = context.getResources().getConfiguration().locale;
			mCountry = systemLocale.getCountry();
		}

	}

	class WeatherThread extends Thread{
		String currentWeather;
		String skyCode;
		String systemCountry;
		@Override
		public void run() {   
			try{
				//검색할 도시 얻어오기
				String city = mCountry;

				Log.i("country", mCountry);
				//city가 한글이면 글자가 깨지지 않게 인코딩 설정
				city=URLEncoder.encode(city,"UTF-8");
				String surl="http://weather.service.msn.com/data.aspx?weadergreetype=C&culture=ko-KR&weasearchstr="
						+city;    
				URL url=new URL(surl);
				//url로부터 읽어오기 위한 스트림 객체 얻어오기
				InputStream is=url.openStream();
				//xml을 파싱하기 위한 객체를 생성하기 위한
				//XmlPullParserFactory 객체 얻어오기
				XmlPullParserFactory factory=
						XmlPullParserFactory.newInstance();
				//xml을 파싱하기 위한 객체 얻어오기
				XmlPullParser parser=factory.newPullParser();
				//is로 부터 읽어오도록 설정
				parser.setInput(is,"utf-8");

				//이벤트 얻어오기(문서 끝, 문서 시작, 시작 태그, 끝태그, ..)
				int event=parser.getEventType();

				//요일별 날씨 정보를 저장하기 위한 ArrayList 객체 생성
				//		    ArrayList<WeatherInfo> list = new ArrayList<WeatherInfo>();
				while(event!=XmlPullParser.END_DOCUMENT){ //문서 끝이 아닐때까지 루프돌기
					event=parser.next(); //다음 요소(태그)로 이동하기
					if(event==XmlPullParser.START_TAG){ //시작태그이면
						String name=parser.getName(); //태그명 얻어오기
						if(name!=null && name.equals("current")){
							//날씨 정보를 저장하기 위한 객체 생성
							//		       WeatherInfo info=new WeatherInfo();
							//forcast 태그의 속성값을 얻기 위해 루프 돌기
							for(int i=0;i<parser.getAttributeCount();i++){
								// i 번째 속성명 얻어오기
								String attr=parser.getAttributeName(i);
								if(attr!=null && attr.equals("skycode")){
									//i번째 속성값 객체에 저장하기
									//		         info.setDate(parser.getAttributeValue(i));
									skyCode = parser.getAttributeValue(i);

								}
								if(attr!=null && attr.equals("temperature")){
									//i번째 속성값 객체에 저장하기
									//		         info.setHigh(parser.getAttributeValue(i));
									Log.i("temperature", parser.getAttributeValue(i));
								}
								if(attr!=null && attr.equals("observationpoint")){
									//i번째 속성값 객체에 저장하기
									//		         info.setLow(parser.getAttributeValue(i));
									//									Toast.makeText(getBaseContext(), parser.getAttributeValue(i), Toast.LENGTH_LONG).show();
									Log.i("point", parser.getAttributeValue(i));

								}
								if(attr!=null && attr.equals("skytext")){
									//i번째 속성값 객체에 저장하기
									//		         info.setSkytextday(parser.getAttributeValue(i));
									//									skyText.setText(parser.getAttributeValue(i));
									//								skyText.setText(currentWeather);
									Log.i("skytext", parser.getAttributeValue(i));
								}
							}

							//		       list.add(info); //요일에 대한 날씨 정보 ArrayList에 담기
						}
					}
				}


				Log.i("skycode", skyCode);
				Message msg=Message.obtain();
				msg.what=0;
				//		    msg.obj=list;
				handler.sendMessage(msg); //핸들러에 메시지 보내기
			}catch(Exception e){
				Log.i("msg", e.getMessage());
				//				Toast.makeText(getBaseContext(),"error", Toast.LENGTH_LONG).show();

			}


		}

		Handler handler=new Handler(){
			@Override
			public void handleMessage(Message msg){

				//		skyText.setText(currentWeather);
				//		Toast.makeText(getBaseContext(),"error", Toast.LENGTH_LONG).show();	
				setWeatherImage(skyCode);

			}
		};

		void setWeatherImage(String skycode) {

			int skyImageId = getResources().getIdentifier("w"+skycode, "drawable", context.getPackageName());
			setImageResource(skyImageId);

		}

	}

}
