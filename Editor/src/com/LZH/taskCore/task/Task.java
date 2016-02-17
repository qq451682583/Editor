package com.LZH.taskCore.task;

/**
 * 后台任务。<br />
 * 采取生成任务链的方式，执行任务前先设置下一个任务，执行任务后自动跳到下一个任务继续执行。
 * @param <CurrentTaskOption> 当前要执行的任务的参数。
 * @param <NextTaskOption> 下一个任务执行所需的参数。
 */
public abstract class Task<CurrentTaskOption, NextTaskOption> implements Runnable{
	
	/** 当前任务参数。 */
	private CurrentTaskOption mCurrentTaskOption = null;
	
	/** 下一个任务。 */
	private Task<NextTaskOption, ?> mNextTask = null;
	
	/** 任务的名字。 */
	private String mTaskName = null;
	
	/**
	 * 设置任务的名字。
	 * @param taskName 任务的名字。
	 */
	public void setTaskName(String taskName){
		mTaskName = taskName;
	}
	
	/**
	 * 获得任务的名字。
	 * @return 任务的名字。
	 */
	public String getTaskName(){
		return mTaskName;
	}
	
	/**
	 * 构造方法。
	 * @param currentTaskOption 当前任务参数。
	 */
	public Task(CurrentTaskOption currentTaskOption){
		mCurrentTaskOption = currentTaskOption; // 赋值当前的任务参数。
	}
	
	/**
	 * 设置当前的任务参数是什么。
	 * @param currentTaskOption 当前的任务参数。
	 */
	private void setCurrentTaskOption(CurrentTaskOption currentTaskOption){
		mCurrentTaskOption = currentTaskOption; // 设置当前的任务参数。
	}
	
	/**
	 * 设置下个任务是什么。
	 * @param nextTask 下个任务。
	 */
	public void addNextTask(Task<NextTaskOption, ?> nextTask){
		mNextTask = nextTask;
	}
	
	@Override
	public void run() {
		NextTaskOption nextTaskOption = execute(mCurrentTaskOption); // 执行当前任务，并返回下个任务所需的参数。
		if(mNextTask != null){ // 有后续任务需要执行。
			if(nextTaskOption != null){ // 如果有下个任务所需的参数，就把此参数赋值给mNextTask，没有的话也行，下个任务就无参执行了。
				/*
				 * 这里注意mNextTask的类型是Task<NextTaskOption, ?>，
				 * 而Task类的泛型为Task<CurrentTaskOption, NextTaskOption>，因此：
				 * mNextTask的类Task<NextTaskOption, ?>的第一个泛型NextTaskOption
				 * 对应Task类的第一个泛型CurrentTaskOption。
				 * Task<CurrentTaskOption, NextTaskOption>类的setCurrentTask方法本应传入一个CurrentTaskOption类型参数，
				 * 对应到mNextTask的Task<NextTaskOption, ?>类，则应传入其第一个泛型NextTaskOption类型的一个对象。
				 */
				mNextTask.setCurrentTaskOption(nextTaskOption);
			}
			mNextTask.run(); // 继续跑下一个任务的run方法，注意这里并不是开了一个线程，而是顺序执行的。
		}
	}
	
	/**
	 * 执行当前的任务，并返回下个任务对象所需的参数，以备继续执行。
	 * @param currentTaskOption 当前任务参数。
	 * @return 下个任务参数。
	 */
	public abstract NextTaskOption execute(CurrentTaskOption currentTaskOption);
}
