package com.example.myfarmserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import android.os.Handler;
import android.os.Message;

public class rcvthread implements Runnable {

	private static final int sizeBuf = 50;
	private Socket clientSocket;
	private SocketAddress clientAddress;
	private int rcvBufSize;
	private byte[] rcvBuf = new byte[sizeBuf];
	String rcvData;

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

				if (rcvData.compareTo("Up") == 0)
					System.out.println("Go!");

				if (rcvData.compareTo("LeftTurn") == 0)
					System.out.println("LeftTurn!");

				if (rcvData.compareTo("RightTurn") == 0)
					System.out.println("RightTurn!");

				if (rcvData.compareTo("Down") == 0)
					System.out.println("Back!");

				if (rcvData.compareTo("Stop") == 0)
					System.out.println("Stop!");

				// System.out.println("Received data : " + rcvData + " ("
				// + clientAddress + ")");
				// outs.write(rcvBuf, 0, rcvBufSize);

				/******* 온도 송신 ******/
				String temp_str = "T";
				temp_str += Long.toString(MainActivity.temperature);

				outs.write(temp_str.getBytes());

				/******* 연기감지 신호 송신 ******/
				String smog_str = "R";

				if (MainActivity.inputSMOG_flag)
					smog_str += "1";

				else
					smog_str += "0";

				outs.write(smog_str.getBytes());

				/*********************/

				/******* 접근감지 신호 송신 ******/
				String prox_str = "G";

				if (MainActivity.inputPROX_flag)
					prox_str += "1";

				else
					prox_str += "0";

				outs.write(prox_str.getBytes());

				/*********************/

				/******* 조도 송신 ******/
				String lux_str = "L";
				lux_str += MainActivity.lux;

				outs.write(lux_str.getBytes());

				/*********************/
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

	// Handler handler = new Handler() {
	//
	// @Override
	// public void handleMessage(Message msg) {
	// // TODO Auto-generated method stub
	// super.handleMessage(msg);
	//
	// MainActivity.printToTextView(rcvData);
	// Log.i("in Handler", rcvData);
	// }
	//
	// };

}
