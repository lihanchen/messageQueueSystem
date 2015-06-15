package com.nvidia.MessgingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MyActivity extends Activity {
	/**
	 * Called when the activity is first created.
	 */

	public static Activity activity;

	//public MyHandler handler=new MyHandler();
	Messenger messenger=null;
	ServiceConnection connection=new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			messenger=new Messenger(service);
		}
		public void onServiceDisconnected(ComponentName name) {
			messenger = null;
		}
	};
	AlertDialog.Builder alertBuilder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		activity = this;
		EditText editTextID = (EditText) findViewById(R.id.editTextID);
		EditText editTextIP = (EditText) findViewById(R.id.editTextIP);
		editTextID.setText(Integer.toString((int) (Math.random() * 100)));
		editTextIP.setText("dhcp-172-17-185-197.nvidia.com");
		alertBuilder=new AlertDialog.Builder(MyActivity.this).setPositiveButton("OK", null).setTitle("Message");

		Intent intent = new Intent(MyActivity.this, MessagingServices.class);
		intent.putExtra("ID", editTextID.getText().toString());
		intent.putExtra("IP", editTextIP.getText().toString());
		startService(intent);
		bindService(new Intent(MyActivity.this, MessagingServices.class), connection, Context.BIND_AUTO_CREATE);

		(findViewById(R.id.buttonStartServer)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MyActivity.this, MessagingServices.class);
				intent.putExtra("ID", editTextID.getText().toString());
				intent.putExtra("IP", editTextIP.getText().toString());
				startService(intent);
			}
		});

		(findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (messenger==null){
					showMessageBox("Failed communicating with service");
					return;
				}

				((EditText) findViewById(R.id.editTextMsg)).selectAll();

				String message=((EditText)findViewById(R.id.editTextMsg)).getText().toString();
				String target=((EditText)findViewById(R.id.editTextTarget)).getText().toString();

				com.nvidia.MessgingService.Message msg
						=new com.nvidia.MessgingService.Message(editTextID.getText().toString(), target, message);
				try {
					messenger.send(Message.obtain(null, MessagingServices.messageWhat.Send.ordinal(), msg));
				}catch(Exception e){
					Log.e("ERROR","ERROR",e);
				}
			}
		});

		(findViewById(R.id.buttonSendGroup)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (messenger == null) {
					showMessageBox("Failed communicating with service");
					return;
				}

				((EditText) findViewById(R.id.editTextGroupMsg)).selectAll();
				String message = ((EditText) findViewById(R.id.editTextGroupMsg)).getText().toString();

				com.nvidia.MessgingService.Message msg
						=new com.nvidia.MessgingService.Message(editTextID.getText().toString(), null, message);
				try {
					messenger.send(Message.obtain(null, MessagingServices.messageWhat.Send.ordinal(), msg));
				}catch(Exception e){
					Log.e("ERROR","ERROR",e);
				}
			}
		});
	}

	public void showMessageBox(String msg){
		alertBuilder.setMessage(msg).create().show();
	}

}

//class MyHandler extends Handler {
//	@Override
//	public void handleMessage(Message msg) {
//
//		if (msg.what==MyActivity.messageWhat.ReceivedBroadCast.ordinal()){
//			showMessageBox("Received Broadcast:\n"+msg.obj);
//		}
//
//		if (msg.what==MyActivity.messageWhat.ReceivedP2PMessage.ordinal()){
//			showMessageBox("Received Message:\n"+msg.obj);
//		}
//
//		if (msg.what==MyActivity.messageWhat.FatalErr.ordinal()){
//			showMessageBox("Server Error");
//		}
//
//		if (msg.what==MyActivity.messageWhat.sendFailed.ordinal()){
//			showMessageBox("Send Failed");
//		}
//	}
//
//	public void showMessageBox(String msg){
//		new AlertDialog.Builder(MyActivity.activity)
//				.setPositiveButton("OK", null)
//				.setMessage(msg)
//				.setTitle("Message")
//				.create().show();
//	}
//}