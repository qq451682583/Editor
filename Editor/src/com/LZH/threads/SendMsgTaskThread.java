package com.LZH.threads;

import com.LZH.taskCore.queue.TaskQueue;
import com.LZH.taskCore.thread.TaskExecuteThread;

/**
 * 这个线程用来向Editor服务器发送报文。
 */
public class SendMsgTaskThread extends TaskExecuteThread {

	public SendMsgTaskThread(TaskQueue taskQueue){
		super(taskQueue);
	}
}
