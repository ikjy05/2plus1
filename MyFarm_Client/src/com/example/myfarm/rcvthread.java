package com.example.myfarm;

import java.io.IOException;
import java.net.Socket;

public class rcvthread implements Runnable {

	private logger logger;
	private final int sizeBuf = 50;
	private int flag;
	private Socket socket;
	private String rcvFirStrData = "Error";
	private String rcvThirdStrData = "Error";
	private String rcvFifthStrData = "Error";

	private String rcvTempData = null;
	private String rcvLuxData1 = null;
	private String rcvLuxData2 = null;
	private String rcvLuxData3 = null;
	private String rcvLuxData4 = null;
	private String rcvProxData = null;
	private String rcvSmogData = null;

	public static String temperature = "25";
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

	@Override
	public void run() {
		while (flag == 1) {
			try {
				rcvBufSize = socket.getInputStream().read(rcvBuf);
				rcvFirStrData = new String(rcvBuf, 0, 1, "UTF-8");
				rcvProxData = new String(rcvBuf, 1, 1, "UTF-8");
				rcvTempData = new String(rcvBuf, 1, 3, "UTF-8");

				rcvThirdStrData = new String(rcvBuf, 2, 1, "UTF-8");
				rcvSmogData = new String(rcvBuf, 3, 1, "UTF-8");

				rcvFifthStrData = new String(rcvBuf, 4, 1, "UTF-8");

				rcvLuxData1 = new String(rcvBuf, 5, 1, "UTF-8");
				rcvLuxData2 = new String(rcvBuf, 5, 2, "UTF-8");
				rcvLuxData3 = new String(rcvBuf, 5, 3, "UTF-8");
				rcvLuxData4 = new String(rcvBuf, 5, 4, "UTF-8");

				if (rcvFirStrData.compareTo("T") == 0) {
					temperature = rcvTempData;
					socRec_flag = true;

				}

				if (rcvFirStrData.compareTo("G") == 0) {
					if (rcvProxData.compareTo("1") == 0)
						MainActivity.inputPROX_flag = true;
					else
						MainActivity.inputPROX_flag = false;

					socRec_flag = true;
				}

				if (rcvThirdStrData.compareTo("R") == 0) {
					if (rcvSmogData.compareTo("1") == 0)
						MainActivity.inputSMOG_flag = true;
					else
						MainActivity.inputSMOG_flag = false;

					socRec_flag = true;
				}

				if (rcvFifthStrData.compareTo("L") == 0) {
					if (rcvBufSize == 9)
						lux = rcvLuxData4;

					else if (rcvBufSize == 8)
						lux = rcvLuxData3;

					else if (rcvBufSize == 7)
						lux = rcvLuxData2;

					else if (rcvBufSize == 6)
						lux = rcvLuxData1;

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
