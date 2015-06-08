package com.nvidia.RabbitMQ;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.*;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;
public class MyActivity extends Activity {
	/**
	 * Called when the activity is first created.
	 */
	final public String serverURI="amqp://lhc:123@172.17.187.114:5672";
	final public String broadcastExchangeName="broadcast_group";

	enum messageWhat{
		ReceivedP2PMessage, ReceivedBroadCast, sendSucceed, sendFailed, setSucceed, setFailed, FatalErr
	}

	public String currentID;
	public ConnectionFactory factory;
	public Channel sendChannel;

	public Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if (msg.what==messageWhat.FatalErr.ordinal()){
					showMessageBox("Server Error");
			}
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		currentID=Integer.toString((int)(Math.random()*1000));
		((EditText)findViewById(R.id.editTextID)).setText(currentID);
		new Thread(){
			@Override
			public void run() {
				try {
					factory = new ConnectionFactory();
					factory.setUri(serverURI);
					factory.setConnectionTimeout(2000);
					Connection conn = factory.newConnection();
					sendChannel = conn.createChannel();
					sendChannel.queueDeclare(currentID, false, false, false, null);
					sendChannel.exchangeDeclare(broadcastExchangeName, "fanout");
					sendChannel.queueBind(currentID, broadcastExchangeName, "");
				} catch (Exception e) {
					handler.obtainMessage(messageWhat.FatalErr.ordinal()).sendToTarget();
					e.printStackTrace();
				}
			}
		}.start();


		(findViewById(R.id.buttonSetID)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showMessageBox("shit");
			}
		});

		(findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String message=((EditText)findViewById(R.id.editTextMsg)).getText().toString();
				String target=((EditText)findViewById(R.id.editTextTarget)).getText().toString();
				new Thread() {
					public void run(){
						try {
							sendChannel.basicPublish("", target, null, message.getBytes());
						} catch (Exception e){
							handler.obtainMessage(messageWhat.sendFailed.ordinal()).sendToTarget();
							e.printStackTrace();
						}
					}
				}.start();
			}
		});

		(findViewById(R.id.buttonSendGroup)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String message=((EditText)findViewById(R.id.editTextGroupMsg)).getText().toString();
				try {
					sendChannel.basicPublish(broadcastExchangeName, "", null, message.getBytes());
				}catch(Exception e){
					e.getStackTrace();
				}
			}
		});

	}

	public void showMessageBox(String msg){
		AlertDialog.Builder builder = new AlertDialog.Builder(MyActivity.this);
		builder.setMessage(msg).setTitle("Message");
		builder.setPositiveButton("OK",null);
		builder.create().show();
	}
}
