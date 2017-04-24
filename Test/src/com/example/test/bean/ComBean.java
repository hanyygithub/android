package com.example.test.bean;

import java.text.SimpleDateFormat;

/**
 * @author benjaminwan 串口数据
 */
public class ComBean {
	public byte[] bRec = null;
	public String sRecTime = "";
	public String sComPort = "";
	int writeIndex = 0;
	int ReadIndex = 0;
	int CurSor = 0;

	public ComBean(String sPort, byte[] buffer, int size) {
		if (size > 0) {
			sComPort = sPort;
			bRec = new byte[size];
			for (int i = 0; i < size; i++) {
				bRec[i] = buffer[i];
			}
			SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
			sRecTime = sDateFormat.format(new java.util.Date());
		}

	}
}