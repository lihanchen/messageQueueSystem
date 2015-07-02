package com.nvidia.MessagingService;

/**
 * Created by hanchenl on 7/2/15.
 */
public class Enum {


	public enum MessageType {
		JSON, binary, string
	}

	public enum IPCmessageWhat {
		Send, ChangeConnectionPreferences, CloseConnection, Success, Failed
	}
}
