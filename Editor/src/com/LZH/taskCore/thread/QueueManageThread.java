package com.LZH.taskCore.thread;

import com.LZH.taskCore.queue.TaskQueue;

/**
 * 这个线程中管理一个任务队列。
 */
public abstract class QueueManageThread extends Thread {

	/** 线程管理的队列。 */
	private TaskQueue mTaskQueue = null;
	
	/**
	 * 构造方法。
	 * @param taskQueue 任务队列。
	 * @see #QueueManageThread()
	 */
	public QueueManageThread(TaskQueue taskQueue){
		this();
		mTaskQueue = taskQueue;
	}
	
	/**
	 * 构造方法。
	 * @see #QueueManageThread(TaskQueue)
	 */
	public QueueManageThread(){
		super();
	}
	
	/**
	 * 设置任务队列。
	 * @param taskQueue 任务队列对象。
	 */
	public void setTaskQueue(TaskQueue taskQueue){
		mTaskQueue = taskQueue;
	}
	
	/**
	 * 获得任务队列。
	 * @return 任务队列。
	 */
	public TaskQueue getTaskQueue(){
		return mTaskQueue;
	}
}
