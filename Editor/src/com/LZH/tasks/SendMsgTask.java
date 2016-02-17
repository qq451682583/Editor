package com.LZH.tasks;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.http.protocol.HTTP;

import com.LZH.Tools.Utils;
import com.LZH.taskCore.task.Task;

import android.webkit.HttpAuthHandler;


/**
 * 向Editor服务器发送请求的任务。
 */
public class SendMsgTask extends Task<String, Void> {

	/** 用于上送报文的socket连接。 */
	private Socket mSocket = null;
	
	/** 上送报文时候出错。 */
	private final String UPLOAD_ERROR = "上送报文时候出错。";
	
	/** 刷新流的时候出错。 */
	private final String CLOSE_STREAM_ERROR = "刷新流的时候出错。";
	
	/**
	 * 构造方法。
	 * @param socket socket连接对象。
	 * @param source 需要上送的报文。
	 */
	public SendMsgTask(Socket socket, String source) {
		super(source);
		mSocket = socket;
	}

	@Override
	public Void execute(String source) {
		if(mSocket != null){
			OutputStream os = null;
			try {
				os = mSocket.getOutputStream(); // 输出流只是打开，但不关闭，到程序退出的时候自动关闭掉。
				os.write(source.getBytes(HTTP.UTF-8));
			} catch (IOException e) {
				Utils.printOutToConsole(UPLOAD_ERROR);
				Utils.printOutToConsole(e.getMessage());
			} finally {
				try {
					os.flush();
				} catch (IOException e) {
					Utils.printOutToConsole(CLOSE_STREAM_ERROR);
					Utils.printOutToConsole(e.getMessage());
				}
			}
		}
		return null;
	}
}
