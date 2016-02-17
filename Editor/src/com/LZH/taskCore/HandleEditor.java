package com.LZH.taskCore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

import com.LZH.queue.SendMsgTaskQueue;
import com.LZH.threads.SendMsgTaskThread;
import com.LZH.threads.SocketTaskThread;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.TextUtils;
import android.webkit.URLUtil;


/**
 * 用来处理Editor功能的类，{@link EditorDebugManager}类对外提供一些接口，这里进行实质性的工作。
 */
public class HandleEditor {

	/** EmpEditor服务器地址设置有问题。 */
	private final String ADDRESS_ERROR = "EmpEditor服务器地址设置有问题。";
	
	/** 发送报文异常提示。 */
	private final String SEND_ERROR = "发送报文异常。";
	
	/** 当前的activity为空。 */
	private final String ACTIVITY_NULL = "当前的activity为空，无法弹出窗口。";
	
	/** 和服务器的连接异常。 */
	private final String CONNECTION_ERROR = "和EmpEditor服务器的连接异常，是否重连？";
	
	/** 上送报文任务名字。 */
	private final String SEND_TASK_NAME = "上送报文任务。";
	
	/** 上送日志任务名字。 */
	private final String SEND_LOG_TASK_NAME = "上送日志。";
	
	/** 主机地址。 */
	private String mHost = "10.0.2.2";
	
	/** 连接的端口号。 */
	private int mPort = 7003;
	
	/** 弹框提示socket重连。 */
	private Dialog mDialog = null;
	
	/** socket工作线程。 */
	private SocketTaskThread mSocketTaskThread = null;
	
	/** 上送报文工作线程。 */
	private SendMsgTaskThread mSendMsgTaskThread = null;
	
	/** 上送报文工作队列。 */
	private SendMsgTaskQueue mSendMsgTaskQueue = null;
	
	/**
	 * 构造方法。
	 */
	public HandleEditor(){
	}
	
	/**
	 * 构造方法，以给定的地址和端口号来初始化socket。
	 * @param host 地址。
	 * @param port 端口号。
	 */
	public HandleEditor(String host, int port){
		mHost = host;
		mPort = port;
	}
	
	/**
	 * 开始进行editor调试。
	 * @return 出错信息。
	 */
	public String startEditorDebug(){
		if(TextUtils.isEmpty(mHost)){
			return ADDRESS_ERROR;
		} else {
			String address = mHost.concat(":").concat(String.valueOf(mPort)); // 获得地址。
			if(!URLUtil.isValidUrl("http://".concat(address))){ // 如果地址无效，就返回出错信息。
				return ADDRESS_ERROR;
			}
		}
		mSendMsgTaskQueue = new SendMsgTaskQueue(); // 新建上送报文任务队列。
		
		mSocketTaskThread = new SocketTaskThread(mHost, mPort, mSendMsgTaskQueue); // 新建socket工作线程。
		mSendMsgTaskThread = new SendMsgTaskThread(mSendMsgTaskQueue); // 新建上送报文工作线程。
		
		mSocketTaskThread.start(); // 开始socket线程。
		mSendMsgTaskThread.start(); // 开始上送报文线程。
		return null;
	}
	
	/**
	 * 停止editor调试，关闭socket连接。
	 */
	public void stopEditorDebug(){
		Socket socket = null;
		if(mSocketTaskThread != null){
			socket = mSocketTaskThread.getSocket();
		}
		try {
			if(socket != null && socket.isConnected()){
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();
				if(is != null){
					is.close();
				}
				if(os != null){
					os.flush();
					os.close();
				}
				socket.close();
			}
			socket = null;
		} catch (IOException e) {
			Utils.printOutToConsole(e.getMessage());
		}
		if(mDialog != null){
			mDialog.dismiss();
			mDialog = null;
		}
		if(mSendMsgTaskQueue != null){
			mSendMsgTaskQueue.clear();
			mSendMsgTaskQueue.quit();
			mSendMsgTaskQueue = null;
		}
	}
	
	/* ---------- 以下是旧版上送报文和日志的方法，默认使用旧的数据传输协议。 ---------- */
	
	/**
	 * 上送报文到editor服务器。
	 * @param message 待处理的报文。
	 * @param scriptList 脚本列表。
	 */
	public void sendMessage(String message, Map<String, String> scriptList){
		if(TextUtils.isEmpty(message)){
			Utils.printOutToConsole(SEND_ERROR);
		} else {
			SendMsgTask newSendMsgTask = new SendMsgTask(mSocketTaskThread.getSocket(),
					Utils.formatSendingMessage(message, scriptList)); // 生成上送报文的任务。
			newSendMsgTask.setTaskName(SEND_TASK_NAME);
			mSendMsgTaskQueue.add(newSendMsgTask); // 将上送报文的任务加入任务队列，按顺序执行。
		}
	}
	
	/**
	 * 上传log信息到EmpEditor服务器。
	 * @param logInfo log信息。
	 */
	public void sendLog(String logInfo){
		if(TextUtils.isEmpty(logInfo)){
			Utils.printOutToConsole(SEND_ERROR);
		} else {
			SendMsgTask newSendlogTask = new SendMsgTask(mSocketTaskThread.getSocket(),
					Utils.formatSenddingLog(logInfo)); // 生成上送报文的任务。
			newSendlogTask.setTaskName(SEND_LOG_TASK_NAME);
			mSendMsgTaskQueue.add(newSendlogTask); // 将上送log的任务加入任务队列，按顺序执行。
		}
	}
	
	/* ---------- 以下是新版上送报文和日志的方法，使用新的数据传输协议。 ---------- */
	
	/**
	 * <h1>[使用新数据传输协议]</h1>
	 * 上送报文到editor服务器。
	 * @param originContent 原始静态页面报文。
	 * @param expandedContent 经过slt脚本转化后的静态页面报文。
	 * @param originCSS 原始外联样式文件。
	 * @param expandedCSS 经过slt脚本转化后的外联样式文件。
	 * @param originScript 原始外联Lua脚本文件。
	 * @param expandedScript 经过slt脚本转化后的外联Lua脚本文件。
	 */
	public void sendMessage(String originContent, String expandedContent,
			Map<String, String> originCSS, Map<String, String> expandedCSS,
			Map<String, String> originScript, Map<String, String> expandedScript){
		if(TextUtils.isEmpty(originContent) || TextUtils.isEmpty(expandedContent)){
			Utils.printOutToConsole(SEND_ERROR);
		} else {
			String content = Utils.formatSendingMessage(originContent, expandedContent,
					originCSS, expandedCSS,
					originScript, expandedScript);
			SendMsgTask newSendMsgTask = new SendMsgTask(mSocketTaskThread.getSocket(),
					content); // 生成上送报文的任务。
			newSendMsgTask.setTaskName(SEND_TASK_NAME);
			mSendMsgTaskQueue.add(newSendMsgTask); // 将上送报文的任务加入任务队列，按顺序执行。
		}
	}
	
	/**
	 * <h1>[使用新数据传输协议]</h1>
	 * 上传log信息到EmpEditor服务器。
	 * @param logType log信息级别。
	 * @param logContent log信息。
	 */
	public void sendLog(LogType logType, String logContent){
		if(TextUtils.isEmpty(logContent)){
			Utils.printOutToConsole(SEND_ERROR);
		} else {
			SendMsgTask newSendlogTask = new SendMsgTask(mSocketTaskThread.getSocket(),
					Utils.formatSenddingLog(logType, logContent)); // 生成上送报文的任务。
			newSendlogTask.setTaskName(SEND_LOG_TASK_NAME);
			mSendMsgTaskQueue.add(newSendlogTask); // 将上送log的任务加入任务队列，按顺序执行。
		}
	}
	
	/**
	 * socket重连。
	 */
	public void resetSocket(){
		EMPRender empRender = AndroidResources.getInstance().getEMPRender();
		Activity activity = AndroidEMPBuilder.getActivity(empRender); // 获得当前的activity。
		if(activity != null){
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showDialog();
				}
			});
		} else {
			Utils.printOutToConsole(ACTIVITY_NULL);
		}
	}
	
	/**
	 * 弹出对话框。
	 */
	private void showDialog(){
		EMPRender empRender = AndroidResources.getInstance().getEMPRender();
		Activity activity = AndroidEMPBuilder.getActivity(empRender); // 获得当前的activity。
		if(mDialog == null && activity != null){
			Builder builder = new Builder(activity);
			builder.setCancelable(true);
			builder.setTitle("EmpEditor：");
			builder.setMessage(CONNECTION_ERROR);
			builder.setPositiveButton("是", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					restartSocketThread();
				}
			});
			builder.setNegativeButton("否", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			mDialog = builder.create();
		}
		mDialog.show();
	}
	
	/**
	 * 重新开始socket线程。
	 */
	private void restartSocketThread(){
		if(mSocketTaskThread != null){
			mSocketTaskThread.markThreadEnd(true);
			mSocketTaskThread.interrupt();
		}
		mSocketTaskThread = new SocketTaskThread(mHost, mPort, mSendMsgTaskQueue); // 新建socket工作线程。
		mSocketTaskThread.start();
	}
}
