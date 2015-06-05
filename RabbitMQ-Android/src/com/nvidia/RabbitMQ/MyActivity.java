package com.nvidia.RabbitMQ;

import android.app.Activity;
import android.app.AlertDialog.*;
import android.content.DialogInterface;
import android.os.Bundle;
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
	final public String serverURI="amqp://172.17.187.114:5672";
	final public String broadcastExchangeName="broadcast_group";

	public String currentID;
	public ConnectionFactory factory;
	Channel sendChannel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		currentID=Integer.toString((int)(Math.random()*1000));
		((EditText)findViewById(R.id.editTextID)).setText(currentID);
		factory = new ConnectionFactory();
		try {
			factory.setUri(serverURI);
			Connection conn = factory.newConnection();
			sendChannel = conn.createChannel();
			sendChannel.queueDeclare(currentID, false, false, false, null);
			sendChannel.exchangeDeclare(broadcastExchangeName, "fanout");
			sendChannel.queueBind(currentID, broadcastExchangeName, "");
		} catch (Exception e) {
			Log.e("err","start here============");
			e.printStackTrace();
			Log.e("err","end here============");
		}
		(findViewById(R.id.buttonSetID)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.e("output", "buttonPressed");
			}
		});
		(findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String message=((EditText)findViewById(R.id.editTextMsg)).getText().toString();
				String target=((EditText)findViewById(R.id.editTextTarget)).getText().toString();
				try {
					sendChannel.basicPublish("", target, null, message.getBytes());
				}catch(Exception e){
					e.getStackTrace();

				}
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
}
