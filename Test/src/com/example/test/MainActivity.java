package com.example.test;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android_serialport_api.SerialPortFinder;

import com.example.test.ComAssistant.Java_xor;
import com.example.test.ComAssistant.MyFunc;
import com.example.test.ComAssistant.SerialHelper;
import com.example.test.bean.ComBean;

public class MainActivity extends Activity implements OnClickListener {
	private Button hand_btn;
	private Button commit_btn;
	private Button auto_btn;
	private EditText angle_et;
	private TextView value1_hand_tv;
	private TextView value1_auto_tv;
	private TextView value2_hand_tv;
	private TextView value2_auto_tv;
	private TextView value3_hand_tv;
	private TextView value4_hand_tv;
	private TextView value3_auto_tv;
	private TextView value4_auto_tv;
	private TextView value5_tv;
	private float distance;
	private float angle;
	private float angle_hand;
	private float angle_auto;
	private float angle_forword;
	private float perimeter_hand;
	private float area_hand;
	private float perimeter_auto;
	private float area_auto;
	/**
	 * 串口部分
	 */
	private static SerialControl ComA;
	private SerialPortFinder mSerialPortFinder;// 串口设备搜索
	private DispQueueThread DispQueue;// 刷新显示线程
	private static boolean _DIS = true; // 是否中断刷新ui线程

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
	}

	private void initView() {
		hand_btn = (Button) findViewById(R.id.hand_btn);
		commit_btn = (Button) findViewById(R.id.commit_btn);
		auto_btn = (Button) findViewById(R.id.auto_btn);
		angle_et = (EditText) findViewById(R.id.angle_et);
		hand_btn.setOnClickListener(this);
		commit_btn.setOnClickListener(this);
		auto_btn.setOnClickListener(this);
		value1_hand_tv = (TextView) findViewById(R.id.value1_hand_tv);
		value1_auto_tv = (TextView) findViewById(R.id.value1_auto_tv);
		value2_hand_tv = (TextView) findViewById(R.id.value2_hand_tv);
		value2_auto_tv = (TextView) findViewById(R.id.value2_auto_tv);
		value3_hand_tv = (TextView) findViewById(R.id.value3_hand_tv);
		value4_hand_tv = (TextView) findViewById(R.id.value4_hand_tv);
		value3_auto_tv = (TextView) findViewById(R.id.value3_auto_tv);
		value4_auto_tv = (TextView) findViewById(R.id.value4_auto_tv);
		value5_tv = (TextView) findViewById(R.id.value5_tv);
		// 获得com对象
		ComA = new SerialControl();
		_DIS = true; // 刷新Ui循环代码块
		DispQueue = new DispQueueThread(); // 刷新Ui显示线程
		DispQueue.setName("刷新显示线程");
		DispQueue.start();
		setControls();

	}

	// ----------------------------------------------------设置串口
	private void setControls() {
		mSerialPortFinder = new SerialPortFinder();
		String[] entryValues = mSerialPortFinder.getAllDevicesPath(); // 获取所有的串口
		if (entryValues != null) {
			for (int i = 0; i < entryValues.length; i++) {
				System.out.println(entryValues[i]);
			}

			ComA.setPort(entryValues[6]);
			ComA.setBaudRate("9600");
			OpenComPort(ComA);
		}
	}

	// ----------------------------------------------------串口发送
	public void sendPortData(String sOut) {
		if (ComA != null && ComA.isOpen()) {
			ComA.sendHex(sOut);
		}
	}

	// ----------------------------------------------------关闭串口
	private static void CloseComPort(SerialHelper ComPort) {
		if (ComPort != null) {
			ComPort.setReadThread_Open();
			ComPort.stopSend();
			ComPort.close();
		}
	}

	// ----------------------------------------------------打开串口
	private void OpenComPort(SerialHelper ComPort) {
		try {
			ComPort.open();
		} catch (SecurityException e) {
			ShowMessage("打开串口失败:没有串口读/写权限!");
		} catch (IOException e) {
			ShowMessage("打开串口失败:未知错误!");
		} catch (InvalidParameterException e) {
			ShowMessage("打开串口失败:参数错误!");
		}
	}

	// ------------------------------------------显示消息
	private void ShowMessage(String sMsg) {
		Toast.makeText(MainActivity.this, sMsg, Toast.LENGTH_SHORT).show();
	}

	// ----------------------------------------------------刷新显示线程
	private class DispQueueThread extends Thread {
		private Queue<ComBean> QueueList = new LinkedList<ComBean>();

		@Override
		public void run() {
			super.run();
			while (_DIS) {
				runOnUiThread(new Runnable() {
					public void run() {
						final ComBean ComDatas;
						if ((ComDatas = QueueList.poll()) != null) {
							DispRecData(ComDatas);// 刷新Ui线程
						}
					}
				});
				try {
					Thread.sleep(50);// 显示性能高的话，可以把此数值调小。
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public synchronized void AddQueue(ComBean ComData) {
			if (_DIS) {
				QueueList.add(ComData);
			}

		}
	}

	// ----------------------------------------------------串口控制类
	private class SerialControl extends SerialHelper {

		public SerialControl() {
		}

		@Override
		protected void onDataReceived(final ComBean ComRecData) {
			// 数据接收量大或接收时弹出软键盘，界面会卡顿,可能和6410的显示性能有关
			// 直接刷新显示，接收数据量大时，卡顿明显，但接收与显示同步。
			// 用线程定时刷新显示可以获得较流畅的显示效果，但是接收数据速度快于显示速度时，显示会滞后。
			// 最终效果差不多-_-，线程定时刷新稍好一些。
			DispQueue.AddQueue(ComRecData);// 线程定时刷新显示(推荐)
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.auto_btn:
			// 一个完整的包
			String comString = "0901190001A801024d01";
			String xorString = Java_xor.checkcode_0007(comString);
			comString = "4B59" + comString + xorString;
			sendPortData(comString);
			break;

		case R.id.hand_btn:
			hand_btn.setVisibility(View.GONE);
			commit_btn.setVisibility(View.VISIBLE);
			angle_et.setVisibility(View.VISIBLE);
			// 一个完整的包
			String comString1 = "0901190001A801024d02";
			String xorString1 = Java_xor.checkcode_0007(comString1);
			comString1 = "4B59" + comString1 + xorString1;
			sendPortData(comString1);
			break;
		case R.id.commit_btn:
			hand_btn.setVisibility(View.VISIBLE);
			commit_btn.setVisibility(View.GONE);
			angle_et.setVisibility(View.GONE);
			if (angle_et.getText().toString() != null
					&& angle_et.getText().toString().length() > 0) {

				String ss = Integer.toHexString(Integer.valueOf(angle_et
						.getText().toString()));
				if (ss.length() == 1) {
					ss = "0" + ss + "00";
				} else if (ss.length() == 2) {
					ss = ss + "00";
				} else {
					ss = ss.substring(1) + "01";
				}
				// 一个完整的包
				String comString2 = "0A01190001A8010241" + ss;
				String xorString2 = Java_xor.checkcode_0007(comString2);
				comString2 = "4B59" + comString2 + xorString2;
				sendPortData(comString2);
			} else {
				Toast.makeText(MainActivity.this, "数据不全", Toast.LENGTH_LONG)
						.show();
			}
			break;
		}

	}

	/**
	 * 解析接收到的数据
	 * 
	 * @param ComRecData
	 */
	private void DispRecData(ComBean ComRecData) {
		int len = ComRecData.bRec.length;
		byte[] buffer = new byte[len];
		buffer = ComRecData.bRec;
		if (buffer != null && buffer.length > 0) {
			int type = buffer[7] & 0xff;
			value5_tv.setText("原始数据：" + MyFunc.ByteArrToHex(buffer) + "");
			// 接收到数据
			if (buffer[2] == 9) {// 电池低电压报警数据包

			} else if (buffer[2] == 14) {// 测量模式数据包
				if (buffer[3] == 1) {// 客户编码
					if (buffer[4] == 25) {// 仪器编码
						if (buffer[5] == 0) {// 生产批号
							if (buffer[6] == 1) {
								if (type == 168) {// 传感器类型
									if (buffer[8] == 1) {// 传感器编号
										if (buffer[9] == 1) {// 应用
											// 数据类型
											if (buffer[10] == 1) {// 自动
												int angle1 = buffer[12];
												angle1 = buffer[12] & 0xff;
												int angle2 = buffer[13];
												angle2 = buffer[13] & 0xff;
												angle = (float) (angle2 * 256 + angle1);
												if (angle > 32768) {
													angle = angle - 65535;
												}
												angle = angle / 10;

												int distance1 = buffer[14];
												distance1 = buffer[14] & 0xff;
												int distance2 = buffer[15];
												distance2 = buffer[15] & 0xff;
												int distance3 = buffer[16];
												distance3 = buffer[16] & 0xff;

												distance = (float) (distance3
														* 256 * 256 + distance2
														* 256 + distance1);
												if (distance > 65536 * 128) {
													distance = distance - 65536 * 256 - 1;
												}
												distance = distance / 10;

												value1_auto_tv
														.setVisibility(View.VISIBLE);
												value2_auto_tv
														.setVisibility(View.VISIBLE);
												value3_auto_tv
														.setVisibility(View.VISIBLE);
												value4_auto_tv
														.setVisibility(View.VISIBLE);
												value1_auto_tv.setText(angle
														+ "--自动");
												value2_auto_tv.setText(distance
														+ "--自动");
												value1_hand_tv
														.setVisibility(View.GONE);
												value2_hand_tv
														.setVisibility(View.GONE);
												value3_hand_tv
														.setVisibility(View.GONE);
												value4_hand_tv
														.setVisibility(View.GONE);
												angle_auto = angle
														- angle_forword;// 扇形角度
												angle_forword = angle;// 上一个角度
												perimeter_auto += Math
														.abs(distance
																* Math.sin(angle_auto));// 周长
												area_auto += Math.abs(distance
														* distance
														* Math.sin(angle_auto)
														/ 2);// 面积
												value3_auto_tv
														.setText(perimeter_auto
																+ "--自动");
												value4_auto_tv
														.setText(area_auto
																+ "--自动");
											}
											if (buffer[10] == 2) {// 手动
												int distance1 = buffer[14];
												distance1 = buffer[14] & 0xff;
												int distance2 = buffer[15];
												distance2 = buffer[15] & 0xff;
												int distance3 = buffer[16];
												distance3 = buffer[16] & 0xff;

												distance = (float) (distance3
														* 256 * 256 + distance2
														* 256 + distance1);
												if (distance > 65536 * 128) {
													distance = distance - 65536 * 256 - 1;
												}
												distance = distance / 10;
												value1_hand_tv
														.setVisibility(View.VISIBLE);
												value2_hand_tv
														.setVisibility(View.VISIBLE);
												value3_hand_tv
														.setVisibility(View.VISIBLE);
												value4_hand_tv
														.setVisibility(View.VISIBLE);

												value1_hand_tv.setText(angle_et
														.getText() + "--手动");
												value2_hand_tv.setText(distance
														+ "--手动");

												value1_auto_tv
														.setVisibility(View.GONE);
												value2_auto_tv
														.setVisibility(View.GONE);
												value3_auto_tv
														.setVisibility(View.GONE);
												value4_auto_tv
														.setVisibility(View.GONE);
												angle_hand -= Float
														.parseFloat(angle_et
																.getText() + "");
												perimeter_hand += Math
														.abs(distance
																* Math.sin(angle_hand));// 扇形角度为设置的角度
												area_hand += Math.abs(distance
														* distance
														* Math.sin(angle_hand)
														/ 2);
												value3_hand_tv
														.setText(perimeter_hand
																+ "--手动");
												value4_hand_tv
														.setText(area_hand
																+ "--手动");
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_MENU) {

		} else {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				CloseComPort(ComA);
				_DIS = false;
				MainActivity.this.finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
