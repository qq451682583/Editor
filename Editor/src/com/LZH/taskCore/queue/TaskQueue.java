package com.LZH.taskCore.queue;

import java.util.LinkedList;
import java.util.Queue;

import com.LZH.Tools.Utils;
import com.LZH.taskCore.task.Task;


/**
 * 任务队列。
 */
public class TaskQueue{

	/** 任务数量超过上限时候的警告语。 */
	private final String TASK_COUNT_EXPANDED_LIMIT = "向任务队列中加入的任务数量超过了上限";

	/** 限制加入任务队列中的任务个数。 */
	private final int TASK_COUNT_LIMIT = 500;
	
	/**
	 * 替补任务队列。<br />
	 * 有时候在socket没有建立好之前，任务要排列在替补队列上。等socket建立后，会唤醒TaskQueue，先执行替补队列上的任务。
	 * @see #mTaskQueue
	 */
	private Queue<Task<?, ?>> mBackupQueue = null;

	/**
	 * 内部管理的任务队列。<br />
	 * @see #mBackupQueue
	 */
	private Queue<Task<?, ?>> mTaskQueue = null;
	
	/** 是否退出队列循环。 */
	public boolean mQuiting = false;
	
	/** 是否要等待socket建立。 */
	private boolean mWaitForSocket = true; // 默认要等待socket的建立。
	
	/**
	 *  设置等待socket建立的标志位。
	 * @param waitForSocket 等待socket建立的标志位。
	 */
	public void setWaitForSocket(boolean waitForSocket){
		mWaitForSocket = waitForSocket;
	}
	
	/**
	 * 构造方法。
	 */
	public TaskQueue() {
		mQuiting = false;
		mBackupQueue = new LinkedList<Task<?,?>>();
		mTaskQueue = new LinkedList<Task<?,?>>();
	}

	/**
	 * 向队列中加入一条任务。
	 * @param task 待添加的任务。
	 */
	public void add(Task<?, ?> task) {
		if(mTaskQueue.size() + mBackupQueue.size() > TASK_COUNT_LIMIT){
			Utils.printOutToConsole(TASK_COUNT_EXPANDED_LIMIT);
			return;
		}
		if (task == null) {
			return;
		}
		synchronized (this) {
			if (mQuiting) {
				return;
			}
			if(mWaitForSocket == true){
				mBackupQueue.add(task); // 等待socket建立的这段时间中，将任务放到后备队列中。
			} else {
				moveTask(); // socket已经建立，那么先把后备队列上的任务移动到正式的任务队列上。
				mTaskQueue.add(task);
				notifyAll();
			}
		}
	}
	
	/**
	 * 将后备队列上的任务移动到正式队列上。
	 * @return 被移动的任务数量。
	 */
	public int moveTask(){
		int actionCount = 0; // 移动任务的个数。
		if(mBackupQueue != null && mTaskQueue != null){
			while(!mBackupQueue.isEmpty()){
				mTaskQueue.add(mBackupQueue.poll());
				actionCount++;
			}
		}
		return actionCount;
	}

	/**
	 * 获得下个任务。<br />
	 * 注：此处只从正式队列上拿任务，在socket还没有建立好之前，不拿后备队列上的任务出来。
	 * @return 下个任务。
	 */
	public Task<?, ?> next() {
		synchronized (this) {
			if (mTaskQueue.isEmpty()) {
				return null;
			}
			return mTaskQueue.poll();
		}
	}

	/**
	 * 清空任务队列。
	 */
	public void clear() {
		synchronized (this) {
			mBackupQueue.clear();
			mTaskQueue.clear();
		}
	}

	/**
	 * 退出队列循环。
	 */
	public void quit() {
		synchronized (this) {
			if (mQuiting) {
				return;
			}
			mQuiting = true;
			notifyAll();
		}
	}
}
