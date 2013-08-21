package com.example.myfarmserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class rcvthread implements Runnable {

	private static final int sizeBuf = 50;
	private Socket clientSocket;
	private SocketAddress clientAddress;
	private int rcvBufSize;
	private byte[] rcvBuf = new byte[sizeBuf];
	String rcvData;
	private String rcvFirStrData = "Error";
	public static String rcvLowSetTemp = "10";
	public static String rcvHighSetTemp = "30";
	public static String rcvLowSetLux = "300";
	public static String rcvHighSetLux = "400";

	public static boolean socRec_flag;

	public rcvthread(Socket clientSocket, SocketAddress clientAddress) {
		this.clientSocket = clientSocket;
		this.clientAddress = clientAddress;
	}

	@Override
	public void run() {
		try {
			InputStream ins = clientSocket.getInputStream();
			OutputStream outs = clientSocket.getOutputStream();
			Handler handler = MainActivity.handler;

			while ((rcvBufSize = ins.read(rcvBuf)) != -1) {

				rcvData = new String(rcvBuf, 0, rcvBufSize, "UTF-8");
				rcvFirStrData = new String(rcvBuf, 0, 1, "UTF-8");

				if (rcvFirStrData.compareTo("T") == 0) {

					int Lindex, Hindex = 0;

					Lindex = rcvData.indexOf('L');
					Hindex = rcvData.indexOf('H');

					String Lstr = rcvData.substring(Lindex + 1, Hindex);
					String Hstr = rcvData.substring(Hindex + 1);

					if (Lstr.length() < 3 && Hstr.length() < 3) {

						rcvLowSetTemp = Lstr;
						rcvHighSetTemp = Hstr;
					}

					// if (rcvBufSize == 6) {
					// rcvLowSetTemp = new String(rcvBuf, 2, 1, "UTF-8");
					// rcvHighSetTemp = new String(rcvBuf, 4, 1, "UTF-8");
					// } else if (rcvBufSize == 7) {
					// rcvLowSetTemp = new String(rcvBuf, 2, 1, "UTF-8");
					// rcvHighSetTemp = new String(rcvBuf, 4, 2, "UTF-8");
					// } else if (rcvBufSize == 8) {
					// rcvLowSetTemp = new String(rcvBuf, 2, 2, "UTF-8");
					// rcvHighSetTemp = new String(rcvBuf, 5, 2, "UTF-8");
					// }

					socRec_flag = true;
				}

				if (rcvFirStrData.compareTo("B") == 0) {

					int Lindex, Hindex, Eindex = 0;

					Lindex = rcvData.indexOf('l');
					Hindex = rcvData.indexOf('h');
					Eindex = rcvData.indexOf('e');

					String Lstr = rcvData.substring(Lindex + 1, Hindex);
					String Hstr = rcvData.substring(Hindex + 1, Eindex);

					if (Lstr.length() < 5 && Hstr.length() < 5) {

						rcvLowSetLux = Lstr;
						rcvHighSetLux = Hstr;

						Log.e("****str", Lstr);
						Log.e("****str", Hstr);

					}

					socRec_flag = true;
				}

				/******* 온도 송신 ******/
				String temp_str = "T";
				temp_str += Long.toString(MainActivity.temperature);

				outs.write(temp_str.getBytes("UTF-8"));
				outs.flush();

				/******* 접근감지 신호 송신 ******/
				String prox_str = "G";

				if (MainActivity.inputPROX_flag)
					prox_str += "1";

				else
					prox_str += "0";

				outs.write(prox_str.getBytes("UTF-8"));
				outs.flush();

				/*********************/

				/******* 연기감지 신호 송신 ******/
				String smog_str = "R";

				if (MainActivity.inputSMOG_flag)
					smog_str += "1";

				else
					smog_str += "0";

				outs.write(smog_str.getBytes("UTF-8"));
				outs.flush();

				/*********************/

				/******* 조도 송신 ******/
				String lux_str = "U";
				lux_str += MainActivity.lux + "E";

				outs.write(lux_str.getBytes("UTF-8"));
				outs.flush();

				/*********************/

				try {
					outs.write(MainActivity.sndMessage.getBytes("UTF-8"));
					outs.flush();
				} catch (IOException e) {
					logger.log("Fail to send");
					e.printStackTrace();
				}

				Message message = handler.obtainMessage(1, rcvData); // mstrdata는
																		// 받은
																		// 데이터
																		// 스트링
				handler.sendMessage(message);
				// MainActivity.printToTextView(rcvData);
			}
			System.out.println(clientSocket.getRemoteSocketAddress()
					+ " Closed");

		} catch (IOException e) {
			System.out.println("Exception: " + e);
		} finally {
			try {
				clientSocket.close();
				System.out
						.println("Disconnected! Client IP : " + clientAddress);
			} catch (IOException e) {
				System.out.println("Exception: " + e);
			}
		}

	}
}
