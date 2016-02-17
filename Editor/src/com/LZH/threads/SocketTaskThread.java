package com.LZH.threads;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.protocol.HTTP;

import com.LZH.Tools.Utils;
import com.LZH.taskCore.queue.TaskQueue;
import com.LZH.taskCore.thread.EditorThread;

import android.os.Looper;
import android.text.TextUtils;


/**
 * 这个线程用来处理与Editor服务器建立连接、接收报文的任务。
 */
public class SocketTaskThread extends EditorThread {
	
	/** 未连接上服务器异常。 */
	private final String HOST_ERROR = "未连接上服务器。";
	
	/** I/O处理出现异常。 */
	private final String IO_ERROR = "I/O处理出现异常。";
	
	/** socket地址。 */
	private String mHost = null;
	
	/** socket端口。 */
	private int mPort = -1;
	
	/** 每次读取的比特长度。 */
	public int mByteLimit = 1024;

	/** 服务器响应信息结束标志。 */
	private final String RESPONSE_END_MARK = "$&end";
	
	/** 服务器响应信息结束标志(使用新的数据传输协议)。 */
	private final String RESPONSE_END_MARK_NEW = "#e#";
	
	/** read服务器响应的时候出现了异常。 */
	private final String READ_ERROR = "read服务器响应的时候出现了异常。";
	
	/** 标记线程已经结束。 */
	private boolean mIsThreadEnd = false;
	
	/**
	 * 设置线程已经结束的标记。
	 * @param isThreadEnd 线程已经结束的标记。
	 */
	public synchronized void markThreadEnd(boolean isThreadEnd){
		mIsThreadEnd = isThreadEnd;
	}

	public SocketTaskThread(String host, int port, TaskQueue taskQueue){
		mHost = host;
		mPort = port;
		setTaskQueue(taskQueue);
	}
	
	@Override
	public void run() {
		if (Looper.myLooper() == null) {
			Looper.prepare();
		}
		InputStream inputStream = null;
		try {
			Socket socket = initSocket();
			setSocket(socket);
			inputStream = getSocket().getInputStream(); // 获得输入流。
			StringBuffer result = new StringBuffer(); // 获得服务器返回的报文。
			byte[] data = new byte[mByteLimit]; // 数据暂存。
			int length = -1; // 每次读取的比特位数。
			String responseEndMark = EditorDebugManager.getInstance().mUseNewProtocol ? RESPONSE_END_MARK_NEW : RESPONSE_END_MARK; // 报文结束符。
			try {
				while(mIsThreadEnd == false && (length = inputStream.read(data)) != -1){ // read方法在读不到信息的时候会阻塞住。
					String dataFragment = new String(data, 0, length, HTTP.UTF_8);
					result.append(dataFragment);
					if(result.indexOf(responseEndMark) != -1){ // 遇到了报文结束标志。
						reflashUI(result.toString()); // 获得新的报文去刷新页面。
						result.delete(0, result.length());
						result.setLength(0);
					}
				}
				setQueueStartWaitting(); // socket断连，设置任务队列等待socket重连。
				EditorDebugManager.getInstance().resetSocket(); // socket重连。
			} catch (IOException e) {
				Utils.printOutToConsole(READ_ERROR);
				Utils.printOutToConsole(e.getMessage());
			}
			// 清空资源。
			result.setLength(0);
			result = null;
		} catch (UnknownHostException e) {
			Utils.printOutToConsole(HOST_ERROR);
			Utils.printOutToConsole(e.getMessage());
		} catch (IOException e) {
			Utils.printOutToConsole(IO_ERROR);
			Utils.printOutToConsole(e.getMessage());
		} catch (Exception e) {
			Utils.printOutToConsole(e.getMessage());
		} finally {
			try {
				if(inputStream != null){
					inputStream.close();
				}
				if(getSocket() != null && getSocket().isConnected()){
					getSocket().close();
				}
			} catch (IOException e) {
				Utils.printOutToConsole(e.getMessage());
			}
		}
	}
	
	/**
	 * 以给定的地址和端口来初始化socket。<br />
	 * 如果初始化失败，则令任务队列进入等待socket连接的状态，并提示socket重连；
	 * 如果初始化成功，则令任务队列进入准备状态。
	 * @return 初始化好了的socket。
	 */
	private Socket initSocket(){
		boolean errorFlag = false;
		Socket socket = null;
		try {
			socket = new Socket(mHost, mPort);
			socket.setKeepAlive(true);
		} catch (UnknownHostException e) {
			errorFlag = true;
			Utils.printOutToConsole(HOST_ERROR);
			Utils.printOutToConsole(e.getMessage());
		} catch (IOException e) {
			errorFlag = true;
			Utils.printOutToConsole(IO_ERROR);
			Utils.printOutToConsole(e.getMessage());
		}
		if(errorFlag){ // 初始化socket失败。
			socket = null;
			setQueueStartWaitting(); // 令任务队列等待socket建立。
			EditorDebugManager.getInstance().resetSocket();
		} else {
			setQueueStopWaitting();
		}
		return socket;
	}
	
	/**
	 * 设置任务队列停止对socket的等待，并马上被唤醒。
	 */
	private void setQueueStopWaitting(){
		TaskQueue taskQueue = getTaskQueue();
		if(taskQueue != null){
			synchronized (taskQueue) {
				taskQueue.setWaitForSocket(false);
				taskQueue.moveTask();
				taskQueue.notifyAll();
			}
		}
	}
	
	/**
	 * 设置任务队列开始等待socket的建立。
	 */
	private void setQueueStartWaitting(){
		TaskQueue taskQueue = getTaskQueue();
		if(taskQueue != null){
			synchronized (taskQueue) {
				taskQueue.setWaitForSocket(true);
			}
		}
	}
	
	/**
	 * 用新的报文去刷新页面。
	 * @param response 新报文。
	 */
	private void reflashUI(String response){
		EMPRender empRender = AndroidResources.getInstance().getEMPRender();
		if(!EditorDebugManager.getInstance().mUseNewProtocol){ // 用老的数据传输协议。
			empRender.load(Utils.formatReceivingMessage(response), Utils.formatReceivingLua(response));
		} else { // 使用新的数据传输协议。
			String luaConsole = Utils.formReceivingLuaConsole(response);
			if (!TextUtils.isEmpty(luaConsole)) {
				empRender.getEMPLua().loadString(luaConsole);
			} else {
				empRender.load(Utils.formatReceivingMessage(response),
						Utils.formatReceivingCSS(response), Utils.formatReceivingLua(response));
			}
		}
	}
}
