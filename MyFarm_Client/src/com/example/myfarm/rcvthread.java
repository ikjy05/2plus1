package com.example.myfarm;

import java.io.IOException;
import java.net.Socket;

import android.util.Log;

public class rcvthread implements Runnable {

	private logger logger;
	private final int sizeBuf = 50;
	private int flag;
	private Socket socket;
	private String rcvFirStrData = "Error";
	private String rcvSecStrData = "Error";
	private String rcvTempData = null;
	private String rcvLuxData = null;

	public static String temperature;
	public static String lux;
	public static String Smog;
	public static String Prox;
	public static boolean socRec_flag;

	private byte[] rcvBuf = new byte[sizeBuf];
	private int rcvBufSize;

	public rcvthread(logger logger, Socket socket) {
		this.logger = logger;
		flag = 1;
		this.socket = socket;
	}

	public void setFlag(int setflag) {
		flag = setflag;
	}

	public void run() {
		while (flag == 1) {
			try {
				rcvBufSize = socket.getInputStream().read(rcvBuf);
				rcvFirStrData = new String(rcvBuf, 0, 1, "UTF-8");
				rcvSecStrData = new String(rcvBuf, 1, 1, "UTF-8");

				rcvTempData = new String(rcvBuf, 1, 3, "UTF-8");
				rcvLuxData = new String(rcvBuf, 1, rcvBufSize - 1, "UTF-8");

				if (rcvFirStrData.compareTo("T") == 0) {
					temperature = rcvTempData;
					socRec_flag = true;

				}

				if (rcvFirStrData.compareTo("L") == 0) {
					lux = rcvLuxData;
					socRec_flag = true;

				}

				if (rcvFirStrData.compareTo("G") == 0) {
					if (rcvSecStrData.compareTo("1") == 0)
						MainActivity.inputPROX_flag = true;
					else
						MainActivity.inputPROX_flag = false;

					socRec_flag = true;
				}

				if (rcvFirStrData.compareTo("R") == 0) {
					if (rcvSecStrData.compareTo("1") == 0)
						MainActivity.inputSMOG_flag = true;
					else
						MainActivity.inputSMOG_flag = false;

					socRec_flag = true;
				}

				logger.log("Recive Data : " + rcvFirStrData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.log("Exit loop");
	}
}
