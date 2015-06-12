package com.nvidia.MessgingService;

/**
 * Created by hanchenl on 6/12/15.
 */
public class Message {
	public String from;
	public String to;
	public String Content;

	public Message(String from, String to, String content) {
		this.from = from;
		this.to = to;
		Content = content;
	}
}
