package com.nvidia.MessagingServiceTest;

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
