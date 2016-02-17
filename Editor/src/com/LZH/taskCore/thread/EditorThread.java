package com.LZH.taskCore.thread;

import java.net.Socket;

import com.LZH.taskCore.queue.TaskQueue;

/**
 * Editor的线程，里面管理任务队列和socket对象。
 */
public class EditorThread extends QueueManageThread {

	/** 线程管理的socket。 */
	private Socket mSocket = null;
	
	/**
	 * 构造方法。
	 * @param socket socket对象。
	 * @param taskQueue 管理的任务队列。
	 * @see #EditorThread(Socket)
	 * @see #EditorThread()
	 */
	public EditorThread(Socket socket, TaskQueue taskQueue){
		this(socket);
		setTaskQueue(taskQueue);
	}
	
	/**
	 * 构造方法。
	 * @param socket socket对象。
	 * @see #EditorThread(Socket, TaskQueue)
	 * @see #EditorThread()
	 */
	public EditorThread(Socket socket){
		this();
		mSocket = socket;
	}
	
	/**
	 * 构造方法。
	 * @see #EditorThread(Socket, TaskQueue)
	 * @see #EditorThread(Socket)
	 */
	public EditorThread(){
		super();
	}
	
	/**
	 * 设置socket对象。
	 * @param socket socket对象。
	 */
	public void setSocket(Socket socket){
		mSocket = socket;
	}
	
	/**
	 * 获得socket对象。
	 * @return socket对象。
	 */
	public Socket getSocket(){
		return mSocket;
	}
}
