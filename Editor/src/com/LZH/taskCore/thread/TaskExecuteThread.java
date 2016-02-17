package com.LZH.taskCore.thread;


import com.LZH.Tools.Utils;
import com.LZH.taskCore.queue.TaskQueue;
import com.LZH.taskCore.task.Task;

import android.os.Looper;

/**
 * 执行任务队列所用的循环。
 */
public class TaskExecuteThread extends Thread {

	/** 待执行的任务队列。 */
	private TaskQueue mTaskQueue = null;
	
	/** 任务队列处理线程出现了问题。 */
	private final String ERROR = "任务队列处理线程出现了问题。";
	
	/**
	 * 设置任务队列。
	 * @param taskQueue 任务队列。
	 */
	public void setTaskQueue(TaskQueue taskQueue){
		mTaskQueue = taskQueue;
	}
	
	/**
	 * 默认的构造方法。
	 */
	public TaskExecuteThread(){
		super();
	}
	
	/**
	 * 构造方法，将线程使用到的任务队列一并传进来。
	 * @param taskQueue 本线程需要使用的任务队列。
	 */
	public TaskExecuteThread(TaskQueue taskQueue){
		this();
		setTaskQueue(taskQueue);
	}
	
	@Override
	public void run() {
		if (Looper.myLooper() == null) {
			Looper.prepare();
		}
		try {
			for (;;) { // 采用循环的方式，无任务就阻塞，有任务就执行，任务执行完毕抛弃当前的线程，开启一个新的线程。
				if (mTaskQueue.mQuiting) {
					mTaskQueue.clear();
					mTaskQueue = null;
					return;
				}
				Task<?, ?> task = mTaskQueue.next(); // 拿到下个任务。
				if (task == null) {
					synchronized (mTaskQueue) {
						try {
							mTaskQueue.wait(); // 任务队列中没有任务，等待。如果队列被执行唤醒方法，则继续。
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (task == null) { // 即使任务队列被唤醒，这个值还是有可能为null，则继续for循环，再拿一个有效的任务去执行。
						continue;
					}
				}
				task.run();
			}
		} catch (Exception e) {
			Utils.printOutToConsole(ERROR);
			Utils.printOutToConsole(e.getMessage());
		} finally {
			if (mTaskQueue != null && !mTaskQueue.mQuiting) {
				new TaskExecuteThread().start();
			}
		}
	}
}
