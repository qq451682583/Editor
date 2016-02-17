package com.LZH.Tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rytong.emp.tool.EMPEditorHandler.LogType;
import com.rytong.empeditor.EditorDebugManager;

import android.text.TextUtils;

/** 存放一些常用的工具。 */
public class Utils {
	
	/** 控制台输出debug字样的前缀。 */
	private static final String DEBUG_MARK = "EditorDebug = ";
	
	/** 禁止上传前缀。 */
	private static final String DO_NOT_SEND = "donotsend&";
	
	/* ---------- 旧版数据格式化协议用到的常量。 ---------- */
	
	/** 信息之间的分隔符。 */
	private static final String MESSAGE_PADDING = "#EditorMessage#";
	
	/** 信息开始标志。 */
	private static final String MESSAGE_START = "EditorMessageStart".concat(MESSAGE_PADDING);
	
	/** 信息结束标志。 */
	private static final String MESSAGE_END = MESSAGE_PADDING.concat("EditorMessageEnd");
	
	/** 报文之间的分隔符。 */
	private static final String CONTENT_PADDING = "#EditorContent#";
	
	/** 后台传过来的报文开始的标志。 */
	private static final String CONTENT_START = "EditorContentStart".concat(CONTENT_PADDING);
	
	/** 后台传过来的报文结束的标志。 */
	private static final String CONTENT_END = CONTENT_PADDING.concat("EditorContentEnd");
	
	/** 脚本之间的分割符。 */
	private static final String SCRIPT_PADDING = "#EditorScript#";
	
	/** 脚本开始标记。 */
	private static final String SCRIPT_START = "EditorScriptStart".concat(SCRIPT_PADDING);
	
	/** 脚本结束标记。 */
	private static final String SCRIPT_END = SCRIPT_PADDING.concat("EditorScriptEnd");
	
	/** 日志之间的分隔符。 */
	private static final String LOG_PADDING = "#EditorLog#";
	
	/** 日志开始的标志。 */
	private static final String LOG_START = "EditorLogStart".concat(LOG_PADDING);
	
	/** 日志结束的标志。 */
	private static final String LOG_END = LOG_PADDING.concat("EditorLogEnd");
	
	/** 脚本文件名分隔符。 */
	private static final String SCRIPT_FILENAME_PADDING = "#fileName#";
	
	/** 刷新页面用报文的前缀。 */
	private static final String SERVER_TO_CLIENT = "s2bContent&$";
	
	/** 刷新页面用报文的分隔符，用来分割报文和外联lua脚本。 */
	private static final String REFLASH_PADDING = "#&#";
	
	/** 刷新页面用报文的外联lua脚本文件名。 */
	private static final String OUTLINK_FILENAME = "#fileName#";
	
	/** 刷新页面用报文的后缀。 */
	private static final String SERVER_TO_CLIENT_END = "$&end";
	
	/* ---------- 新版数据格式化协议用到的常量。 ---------- */
	
	/** json字符串开始的标志。 */
	private static final String JSON_START = "#s#";
	
	/** json字符串结束的标志。 */
	private static final String JSON_END = "#e#";
	
	/** 原始报文。 */
	private static final String ORIGIN_MESSAGE = "originMessage";
	
	/** 经过slt脚本转化后的报文。 */
	private static final String EXPANDED_MESSAGE = "expandedMessage";
	
	/** 静态页面内容。 */
	private static final String STATIC_CONTENT = "staticContent";
	
	/** 单独的Lua脚本。 */
	private static final String LUA_CONSOLE = "lua_console";
	
	/** 外联样式文件。 */
	private static final String LINK_CSS = "css";
	
	/** 外联脚本文件。 */
	private static final String LINK_SCRIPT = "script";
	
	/** 文件名。 */
	private static final String LINK_FILE_NAME = "name";
	
	/** 文件内容。 */
	private static final String LINK_FILE_CONTENT = "content";
	
	/** 上送的报文的级别。 */
	private static final String LOG_LEVEL = "level";
	
	/** 上送的报文的内容。 */
	private static final String LOG_MESSAGE = "message";
	
	/**
	 * 将信息打印到控制台。
	 * @param s 需要打印的信息。
	 */
	public static final void printOutToConsole(String s) {
		if (!TextUtils.isEmpty(s)) {
			int length = s.length();
			int offset = 3000;
			if (length > offset) { // 解决报文过长，打印不全的问题。
				int n = 0;
				int i = 0;
				for (; i < length; i += offset) {
					n += offset;
					if (n > length) {
						n = length;
					}
					System.out.println(DEBUG_MARK.concat(s.substring(i, n)));
				}
			} else {
				System.out.println(DEBUG_MARK.concat(s));
			}
		}
	}
	
	/**
	 * 从后台获得的报文一般是不符合editor的报文格式的，这里将报文格式化后返回。<br />
	 * 注：通讯协议如下：
	 * <pre>
	 * 【客户端->服务端】
	 * EditorMessageStart#EditorMessage#
	 * EditorContentStart#EditorContent#报文内容（全部）#EditorContent#EditorContentEnd
	 * EditorScriptStart#EditorScript#脚本1名称（外联）#fileName#脚本1内容（外联）#EditorScript#EditorScriptEnd
	 * #EditorScript#
	 * EditorScriptStart#EditorScript#脚本2名称（外联）#fileName#脚本2内容（外联）#EditorScript#EditorScriptEnd
	 * #EditorScript#
	 * #EditorMessage#EditorMessageEnd
	 * </pre>
	 * @param source 待处理的报文。
	 * @param scriptList 外联脚本的队列，这个队列是框架解析出来的，此处不再重复获取。
	 * @return 符合editor通讯协议的报文。
	 */
	public static String formatSendingMessage(String source, Map<String, String> scriptList){
		String outLinkScriptContent = getOutLinkScriptContent(scriptList); // 外联脚本内容。
		String target =
				MESSAGE_START
				.concat(CONTENT_START)
				.concat(source == null ? "" : source)
				.concat(CONTENT_END)
				.concat(outLinkScriptContent)
				.concat(MESSAGE_END);
		return target;
	}
	
	/**
	 * <h1>[使用新数据传输协议]</h1>
	 * 从后台获得的报文一般是不符合editor的报文格式的，这里将报文格式化后返回，此处使用新的通讯协议。<br />
	 * 注：通讯协议如下：
	 * <pre>
	 * #s#{
	 *     "originMessage": {
	 *         "staticContent": "报文内容(经过Base64编码)",
	 *         "css": [
	 *             {
	 *                 "name": "外联样式1名称",
	 *                 "content": "外联样式1内容(经过Base64编码)"
	 *             },
	 *             {
	 *                 "name": "外联样式2名称",
	 *                 "content": "外联样式2内容(经过Base64编码)"
	 *             }
	 *         ],
	 *         "script": [
	 *             {
	 *                 "name": "外联脚本1名称",
	 *                 "content": "外联脚本1内容(经过Base64编码)"
	 *             },
	 *             {
	 *                 "name": "外联脚本2名称",
	 *                 "content": "外联脚本2内容(经过Base64编码)"
	 *             }
	 *         ]
	 *     },
	 *     "expandedMessage": {
	 *         "staticContent": "报文内容(经过Base64编码)",
	 *         "css": [
	 *             {
	 *                 "name": "外联样式1名称",
	 *                 "content": "外联样式1内容(经过Base64编码)"
	 *             },
	 *             {
	 *                 "name": "外联样式2名称",
	 *                 "content": "外联样式2内容(经过Base64编码)"
	 *             }
	 *         ],
	 *         "script": [
	 *             {
	 *                 "name": "外联脚本1名称",
	 *                 "content": "外联脚本1内容(经过Base64编码)"
	 *             },
	 *             {
	 *                 "name": "外联脚本2名称",
	 *                 "content": "外联脚本2内容(经过Base64编码)"
	 *             }
	 *         ]
	 *     }
	 * }#e#
	 * </pre>
	 * @param originContent 原始静态页面报文。
	 * @param expandedContent 经过slt脚本转化后的静态页面报文。
	 * @param originCSS 原始外联样式文件。
	 * @param expandedCSS 经过slt脚本转化后的外联样式文件。
	 * @param originScript 原始外联Lua脚本文件。
	 * @param expandedScript 经过slt脚本转化后的外联Lua脚本文件。
	 * @return 符合editor通讯协议的报文。
	 */
	public static String formatSendingMessage(String originContent, String expandedContent,
			Map<String, String> originCSS, Map<String, String> expandedCSS,
			Map<String, String> originScript, Map<String, String> expandedScript){
		JSONObject resultJson = new JSONObject(); // 存储最终结果。
		try {
			resultJson.put(ORIGIN_MESSAGE, getNetworkMessage(originContent, originCSS, originScript));
			resultJson.put(EXPANDED_MESSAGE, getNetworkMessage(expandedContent, expandedCSS, expandedScript));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return JSON_START.concat(resultJson.toString()).concat(JSON_END);
	}
	
	/**
	 * 刷新页面用的报文不是符合框架规定的格式，这里将其转化成符合框架规定格式的报文后返回，供刷新页面用。<br />
	 * 注：旧通讯协议如下（此处使用s2bContent&$报文部分）：
	 * <pre>
	 * 【服务端->客户端】
	 * s2bContent&$报文
	 * #&#脚本1（外联）名称#fileName#脚本1（外联）内容
	 * #&#脚本2(外联)名称#fileName#脚本2（外联）内容
	 * $&end
	 * </pre>
	 * 新的数据协议如下（此处使用staticContent部分）：
	 * <pre>
	 * #s#{
	 *     "staticContent": "报文内容(经过Base64编码)",
	 *     "css": [
	 *         {
	 *             "name": "外联样式1名称",
	 *             "content": "外联样式1内容(经过Base64编码)"
	 *         },
	 *         {
	 *             "name": "外联样式2名称",
	 *             "content": "外联样式2内容(经过Base64编码)"
	 *         }
	 *     ],
	 *     "script": [
	 *         {
	 *             "name": "外联脚本1名称",
	 *             "content": "外联脚本1内容(经过Base64编码)"
	 *         },
	 *         {
	 *             "name": "外联脚本2名称",
	 *             "content": "外联脚本2内容(经过Base64编码)"
	 *         }
	 *     ]
	 * }#e#
	 * </pre>
	 * @param source 原始报文。
	 * @return 符合框架规定格式的报文。
	 */
	public static String formatReceivingMessage(String source){
		String content = ""; // 页面内容。
		if(!TextUtils.isEmpty(source)){
			if(!EditorDebugManager.getInstance().mUseNewProtocol){ // 使用旧的数据传输协议。
				if(source.indexOf(REFLASH_PADDING) != -1){ // 含有外联脚本。
					content = source.split(REFLASH_PADDING)[0].replace(SERVER_TO_CLIENT, ""); // 去掉前缀。
				} else {
					content = source.replace(SERVER_TO_CLIENT, "").replace(SERVER_TO_CLIENT_END, "");
				}
			} else if (source.startsWith(JSON_START) && source.endsWith(JSON_END)){ // 使用新的数据传输协议。
				String jsonStr = source.substring(JSON_START.length(), source.lastIndexOf(JSON_END)); // 去掉头尾。
				try {
					JSONObject jsonObj = new JSONObject(jsonStr);
					content = Base64.decodeToString(jsonObj.getString(STATIC_CONTENT));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			content = DO_NOT_SEND.concat(content); // 加入禁止上传的标志。
		}
		return content;
	}
	
	/**
	 * <h1>[使用新数据传输协议]</h1>
	 * 刷新页面的溶蚀替换外联的css样式文件，此处使用新的数据通信协议进行格式化。<br />
	 * 注：新的数据通信协议见{@link #formatReceivingMessage(String)}，此处使用css部分。
	 * @param source 原始报文。
	 * @return 外联css文件集合。
	 */
	public static Map<String, String> formatReceivingCSS(String source){
		Map<String, String> result = new HashMap<String, String>();
		if (!TextUtils.isEmpty(source) && source.startsWith(JSON_START) && source.endsWith(JSON_END)){ // 使用新的数据传输协议。
			String jsonStr = source.substring(JSON_START.length(), source.lastIndexOf(JSON_END)); // 去掉头尾。
			try {
				JSONObject jsonObj = new JSONObject(jsonStr);
				JSONArray cssArray = jsonObj.getJSONArray(LINK_CSS);
				result = getFileInfo(cssArray);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * 刷新页面的同时替换外联的lua脚本，这里将按照给定的通讯协议进行格式化。<br />
	 * 注：旧通讯协议如下（此处使用脚本部分）：
	 * <pre>
	 * 【服务端->客户端】
	 * s2bContent&$报文
	 * #&#脚本1（外联）名称#fileName#脚本1（外联）内容
	 * #&#脚本2(外联)名称#fileName#脚本2（外联）内容
	 * $&end
	 * </pre>
	 * 新的数据通信协议见{@link #formatReceivingMessage(String)}，此处使用script部分。
	 * @param source 原始报文。
	 * @return 外联Lua脚本集合。
	 */
	public static Map<String, String> formatReceivingLua(String source){
		Map<String, String> result = new HashMap<String, String>(); // 待替换的外联lua脚本。
		if(!TextUtils.isEmpty(source)){
			if(!EditorDebugManager.getInstance().mUseNewProtocol && source.indexOf(REFLASH_PADDING) != -1){ // 使用旧的通讯协议，含有外联脚本，可以处理。
				String[] data = source.split(REFLASH_PADDING);
				int i = 0;
				for (; i < data.length; i++) {
					if(!TextUtils.isEmpty(data[i]) && data[i].indexOf(OUTLINK_FILENAME) != -1){ // 含有文件名分割。
						String content = data[i].replace(SERVER_TO_CLIENT_END, ""); // 去掉结尾符号。
						result.put(content.split(OUTLINK_FILENAME)[0], content.split(OUTLINK_FILENAME)[1]);
					}
				}
			} else if(source.startsWith(JSON_START) && source.endsWith(JSON_END)){ // 使用新的数据传输协议。
				String jsonStr = source.substring(JSON_START.length(), source.lastIndexOf(JSON_END)); // 去掉头尾。
				try {
					JSONObject jsonObj = new JSONObject(jsonStr);
					JSONArray cssArray = jsonObj.getJSONArray(LINK_SCRIPT);
					result = getFileInfo(cssArray);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	public static String formReceivingLuaConsole(String source){
		String result="";
		if(source.startsWith(JSON_START) && source.endsWith(JSON_END)){ // 使用新的数据传输协议。
				String jsonStr = source.substring(JSON_START.length(), source.lastIndexOf(JSON_END)); // 去掉头尾。
				try {
					JSONObject jsonObj = new JSONObject(jsonStr);
					result = Base64.decodeToString(jsonObj.getString(LUA_CONSOLE));					
				} catch (JSONException e) {
					e.printStackTrace();
				}
		 }
		return result;
	}
	
	/**
	 * 格式化log信息，按照协议格式化信息后上传给EmpEditor服务器。<br />
	 * 注：通讯协议如下：
	 * <pre>
	 * 【客户端->服务端】
	 * EditorMessageStart#EditorMessage#
	 * EditorLogStart#EditorLog#Log内容（全部）#EditorLog#EditorLogEnd
	 * #EditorMessage#EditorMessageEnd
	 * </pre>
	 * @param source 原始的log信息。
	 * @return 格式化之后的log信息。
	 */
	public static String formatSenddingLog(String source){
		String target = MESSAGE_START
				.concat(LOG_START).concat(source).concat(LOG_END)
				.concat(MESSAGE_END);
		return target;
	}
	
	/**
	 * <h1>[使用新数据传输协议]</h1>
	 * 格式化log信息，按照协议格式化信息后上传给EmpEditor服务器此处使用新的数据通信协议。<br />
	 * 注：通讯协议如下：
	 * <pre>
	 * #s#{
	 *     "level": "日志级别(普通i|警告w|错误e)",
	 *     "message": "日志内容(经过Base64编码)"
	 * }#e#
	 * </pre>
	 * @param logType 日志类型。
	 * @param logInfo 日志信息。
	 * @return 格式化之后的log信息。
	 */
	public static String formatSenddingLog(LogType logType, String logInfo){
		JSONObject resultJson = new JSONObject(); // 存储最终结果。
		try {
			resultJson.put(LOG_LEVEL, logType.toString());
			resultJson.put(LOG_MESSAGE, Base64.encode(logInfo.getBytes()));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return JSON_START.concat(resultJson.toString()).concat(JSON_END);
	}
	
	/* ---------- 旧版数据传输协议拼接报文的方法。 ---------- */
	
	/***
	 * 获得外联的脚本文件的内容。
	 * @param scriptList 脚本列表。
	 * @return 脚本内容字符串。
	 */
	private static String getOutLinkScriptContent(Map<String, String> scriptList){
		StringBuffer outLinkScriptContent = new StringBuffer(); // 外联脚本内容。
		if(scriptList != null){
			Set<String> keySet = scriptList.keySet();
			Iterator<String> it = keySet.iterator();
			while(it.hasNext()){
				String fileName = it.next(); // 获得lua脚本的文件名。
				String content = scriptList.get(fileName); // 获得对应的lua脚本内容。
				outLinkScriptContent.append(SCRIPT_START)
				.append(fileName).append(SCRIPT_FILENAME_PADDING)
				.append(content).append(SCRIPT_END)
				.append(SCRIPT_PADDING);
			}
			scriptList.clear(); // 清空list。
		}
		return outLinkScriptContent.toString();
	}
	
	/* ---------- 新版数据传输协议构造json的方法及获取文件信息的方法。 ---------- */
	
	/**
	 * 返回一个给定格式的json对象。包括静态报文、外联样式、外联脚本三项。
	 * @param staticContent 静态页面报文。
	 * @param css 外联样式文件集合，每一项包括文件名和文件内容。
	 * @param script 外联脚本文件集合，每一项包括文件名和文件内容。
	 * @return 给定格式的json对象。
	 * @throws JSONException json操作异常。
	 */
	private static JSONObject getNetworkMessage(String staticContent, Map<String, String> css, Map<String, String> script) throws JSONException{
		JSONObject resultJson = new JSONObject();
		if(staticContent != null){
			resultJson.put(STATIC_CONTENT, Base64.encode(staticContent.getBytes()));
		}
		JSONArray cssArray = getArrayValueObj(css);
		if(cssArray.length() > 0){
			resultJson.put(LINK_CSS, getArrayValueObj(css));
		}
		JSONArray scriptArray = getArrayValueObj(script);
		if(scriptArray.length() > 0){
			resultJson.put(LINK_SCRIPT, getArrayValueObj(script));
		}
		return resultJson;
	}
	
	/**
	 * 拼接一个指定格式的json数组，数组中每一项都由文件名和文件内容(经过BASE64加密)组成。
	 * @param valueResource 制作数组的材料。
	 * @return 指定格式的json数组。
	 * @throws JSONException json操作异常。
	 */
	private static JSONArray getArrayValueObj(Map<String, String> valueResource) throws JSONException{
		JSONArray valueArray = new JSONArray();
		if(valueResource == null){
			return valueArray;
		}
		Set<String> fileNameSet = valueResource.keySet();
		Iterator<String> fileNameIt = fileNameSet.iterator();
		while(fileNameIt.hasNext()){
			String fileName = fileNameIt.next(); // 获得文件名字。
			String fileContent = valueResource.get(fileName); // 获得文件内容。
			JSONObject fileItem = new JSONObject();
			fileItem.put(LINK_FILE_NAME, fileName);
			fileItem.put(LINK_FILE_CONTENT, Base64.encode(fileContent.getBytes())); // 文件内容使用Base64进行加密。
			valueArray.put(fileItem); // 存入数组。
		}
		return valueArray;
	}
	
	/**
	 * 从给定的json数组中拿出文件名和文件内容的集合。
	 * @param resource 给定的json数组。
	 * @return 一个map含有json数组中描述的每个项目。
	 * @throws JSONException json操作异常。
	 */
	private static Map<String, String> getFileInfo(JSONArray resource) throws JSONException{
		Map<String, String> result = new HashMap<String, String>();
		if(resource == null){
			return result;
		}
		int fileCount = resource.length();
		int fileIndex = 0;
		for (; fileIndex < fileCount; fileIndex++) {
			JSONObject fileItem = resource.getJSONObject(fileIndex); // 获得一个文件的信息。
			String fileName = fileItem.getString(LINK_FILE_NAME); // 获得文件名。
			String fileContent = Base64.decodeToString(fileItem.getString(LINK_FILE_CONTENT)); // 获得文件内容。
			result.put(fileName, fileContent); // 加入map。
		}
		return result;
	}
}
