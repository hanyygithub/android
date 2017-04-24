package com.example.test.ComAssistant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android.util.Log;
import android_serialport_api.SerialPort;

import com.example.test.bean.ComBean;

/**
 * @author benjaminwan 串口辅助工具�?
 */
public abstract class SerialHelper {
	private SerialPort mSerialPort;
	private OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	private SendThread mSendThread;
	private String sPort = "/dev/ttyMT1";
	private int iBaudRate = 9600;
	private boolean _isOpen = false;
	private byte[] _bLoopData = new byte[] { 0x30 };
	private int iDelay = 500;
	private int currentLength;
	private byte[] buffers;
	public boolean ReadThread_Open = false;

	// ----------------------------------------------------
	public SerialHelper(String sPort, int iBaudRate) {
		this.sPort = sPort;
		this.iBaudRate = iBaudRate;
	}

	public SerialHelper() {
		this("/dev/ttyMT1", 9600);
	}

	public SerialHelper(String sPort) {
		this(sPort, 9600);
	}

	public SerialHelper(String sPort, String sBaudRate, boolean _ReadThread_Open) {
		this(sPort, Integer.parseInt(sBaudRate));
	}

	// ----------------------------------------------------
	public void open() throws SecurityException, IOException,
			InvalidParameterException {
		mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0);
		mOutputStream = mSerialPort.getOutputStream();
		mInputStream = mSerialPort.getInputStream();

		int CLEAR = mInputStream.available(); // 先清空缓存中的数�?
		byte[] clear = new byte[CLEAR];
		mInputStream.read(clear, 0, CLEAR);

		if (mReadThread != null) {
			ReadThread_Open = true;
		} else {
			mReadThread = new ReadThread();
			mReadThread.start();
			ReadThread_Open = true;
		}

		mSendThread = new SendThread();
		mSendThread.setSuspendFlag();
		mSendThread.start();
		_isOpen = true;
	}

	// ----------------------------------------------------
	public void close() {
		ReadThread_Open = false;
		if (mReadThread != null) {
			Log.i("Testtt", "读取线程消灭");
			ReadThread_Open = false;
		}
		if (mReadThread.isAlive()) {
			Log.i("Testtt", "读取线程依然存在");
			ReadThread_Open = false;
		}

		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}

		_isOpen = false;
	}

	// ----------------------------------------------------
	public void send(byte[] bOutArray) {
		try {
			mOutputStream.write(bOutArray);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ----------------------------------------------------
	public void sendHex(String sHex) {
		byte[] bOutArray = MyFunc.HexToByteArr(sHex);
		send(bOutArray);
	}

	// ----------------------------------------------------
	public void sendTxt(String sTxt) {
		byte[] bOutArray = sTxt.getBytes();
		send(bOutArray);
	}

	/**
	 * 读取终端设备数据
	 * 
	 * @author Administrator
	 */
	private class ReadThread extends Thread {
		private int contentLenght;
		private int cursor;

		@Override
		public void run() {
			Log.i("READTHREAD", "------------------------->" + "进入run方法");
			super.run();
			// 定义一个包的最大长度
			int maxLength = 2014;
			buffers = new byte[maxLength];
			// 每次收到实际长度
			int available = 0;
			currentLength = 0;
			// 协议头长度4个字节（开始符1，类型1，长度2）
			int headerLength = 4;

			while (ReadThread_Open) {
				Log.i("READTHREAD", "===========================>" + "读取串口循环");
				try {
					available = mInputStream.available();
					Log.i("READTHREAD", "------------------------->"
							+ "串口缓存区数据长度" + available);
					if (available > 0) {
						// 防止超出数组最大长度导致溢出
						if (available > maxLength - currentLength) {
							available = maxLength - currentLength;
						}
						mInputStream.read(buffers, currentLength, available);
						currentLength += available;
						Log.i("READTHREAD", "------------------------->"
								+ "读取的数据长�?");
						Log.i("READTHREAD", "------------------------->"
								+ "读取缓存区数据后缓存数组的数据长�?" + currentLength);
					}
					try {
						Thread.sleep(10);// 延时50ms
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				cursor = 0;
				// 如果当前收到包大于头的长度，则解析当前包
				while (currentLength >= headerLength
						&& cursor + 2 < currentLength) {
					// 取到头部第一个字节
					if (buffers[cursor] != 0x4B && buffers[cursor + 1] != 0x59) {
						--currentLength;
						++cursor;
						Log.i("READTHREAD", "------->" + "循环判断是否到包�?");
						Log.i("READTHREAD", "------->" + "currentLength"
								+ currentLength);
						Log.i("READTHREAD", "------->" + "cursor" + cursor);
						continue;
					} else {
						contentLenght = (int)buffers[cursor+2];
						Log.i("READTHREAD", "------->" + "取到包头"
								+ (int) buffers[cursor] + ""
								+ (int) buffers[cursor + 1] + "");
						Log.i("READTHREAD", "------->" + "包的长度"
								+ (int) (contentLenght + headerLength) + "");
					}
					// 如果内容包的长度大于最大内容长度或者小于等于0，则说明这个包有问题，丢弃
					if (contentLenght <= 0 || contentLenght > maxLength - 4) {
						currentLength = 0;
						break;
					}
					// 如果当前获取到长度小于整个包的长度，则跳出循环等待继续接收数据
					int factPackLen = contentLenght + 4;
					if (currentLength < contentLenght + 4) {
						Log.i("READTHREAD", "------->" + "数组长度不够�?个包长，跳出循环继续接收"
								+ currentLength);
						break;
					}
					// 一个完整包即产生
					ComBean ComRecData = new ComBean(sPort, buffers,
							factPackLen);
					onDataReceived(ComRecData);

					Log.i("READTHREAD", "------->" + "产生�?个完整包");
					Log.i("READTHREAD", "------->" + "产生�?个完整包时数组的长度"
							+ currentLength);
					currentLength -= factPackLen;
					Log.i("READTHREAD", "------->" + "产生�?个完整包后数组的长度"
							+ currentLength);
					Log.i("READTHREAD", "------->" + "产生�?个完整包时游标的长度" + cursor);
					cursor += factPackLen;
					Log.i("READTHREAD", "------->" + "产生�?个完整包后游标的长度" + cursor);
					// 残留字节移到缓冲区首
					if (currentLength > 0 && cursor > 0) {
						System.arraycopy(buffers, cursor, buffers, 0,
								currentLength);
						Log.i("READTHREAD", "------->" + "残留字节移到缓冲区首 ");
					}
				}
			}
		}
	}

	public void setReadThread_Open() {
		ReadThread_Open = false;
	}

	// ----------------------------------------------------
	private class SendThread extends Thread {
		public boolean suspendFlag = true;// 控制线程的执行

		@Override
		public void run() {
			super.run();
			while (!isInterrupted()) {
				synchronized (this) {
					while (suspendFlag) {
						try {
							wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				send(getbLoopData());
				try {
					Thread.sleep(iDelay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// 线程暂停
		public void setSuspendFlag() {
			this.suspendFlag = true;
		}

		// 唤醒线程
		public synchronized void setResume() {
			this.suspendFlag = false;
			notify();
		}
	}

	// ----------------------------------------------------
	public int getBaudRate() {
		return iBaudRate;
	}

	public boolean setBaudRate(int iBaud) {
		if (_isOpen) {
			return false;
		} else {
			iBaudRate = iBaud;
			return true;
		}
	}

	public boolean setBaudRate(String sBaud) {
		int iBaud = Integer.parseInt(sBaud);
		return setBaudRate(iBaud);
	}

	// ----------------------------------------------------
	public String getPort() {
		return sPort;
	}

	public boolean setPort(String sPort) {
		if (_isOpen) {
			return false;
		} else {
			this.sPort = sPort;
			return true;
		}
	}

	// ----------------------------------------------------
	public boolean isOpen() {
		return _isOpen;
	}

	// ----------------------------------------------------
	public byte[] getbLoopData() {
		return _bLoopData;
	}

	// ----------------------------------------------------
	public void setbLoopData(byte[] bLoopData) {
		this._bLoopData = bLoopData;
	}

	// ----------------------------------------------------
	public void setTxtLoopData(String sTxt) {
		this._bLoopData = sTxt.getBytes();
	}

	// ----------------------------------------------------
	public void setHexLoopData(String sHex) {
		this._bLoopData = MyFunc.HexToByteArr(sHex);
	}

	// ----------------------------------------------------
	public int getiDelay() {
		return iDelay;
	}

	// ----------------------------------------------------
	public void setiDelay(int iDelay) {
		this.iDelay = iDelay;
	}

	// ----------------------------------------------------
	public void startSend() {
		if (mSendThread != null) {
			mSendThread.setResume();
		}
	}

	// ----------------------------------------------------
	public void stopSend() {
		if (mSendThread != null) {
			mSendThread.setSuspendFlag();
		}
	}

	// ----------------------------------------------------
	protected abstract void onDataReceived(ComBean ComRecData);
}