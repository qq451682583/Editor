package com.LZH.Tools;

import java.io.ByteArrayOutputStream;

/**
 * <p>
 * Base64编码解码。
 * </p>
 */
public final class Base64 {

	private static final char[] charTab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	/**
	 * <p>
	 * Create a base64 encoded string.
	 * </p>
	 * 
	 * @param data source data.
	 * @return encoded stringbuffer.
	 * @see #encode(byte[], int, int, StringBuffer)
	 */
	public static String encode(byte[] data) {
		return encode(data, 0, data.length, null).toString();
	}

	/**
	 * <p>
	 * Encodes the part of the given byte array denoted by start and len to the Base64 format. The encoded data is appended to the given StringBuffer. If no StringBuffer is given, a new one is created
	 * automatically. The StringBuffer is the return value of this method.
	 * </p>
	 * 
	 * @param data source data.
	 * @param start start position of source data to encode.
	 * @param len length from start position of source data to encode.
	 * @param buf a stringbuffer to catch result.
	 * @return Encoded stringbuffer.
	 * @see #encode(byte[])
	 */
	public static StringBuffer encode(byte[] data, int start, int len, StringBuffer buf) {

		if (buf == null)
			buf = new StringBuffer(data.length * 3 / 2);

		int end = len - 3;
		int i = start;

		while (i <= end) {
			int d = (((data[i]) & 0x0ff) << 16) | (((data[i + 1]) & 0x0ff) << 8) | ((data[i + 2]) & 0x0ff);

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append(charTab[(d >> 6) & 63]);
			buf.append(charTab[d & 63]);

			i += 3;

			// if (n++ >= 14) {
			// n = 0;
			// buf.append("\r\n");
			// }
		}

		if (i == start + len - 2) {
			int d = (((data[i]) & 0x0ff) << 16) | (((data[i + 1]) & 255) << 8);

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append(charTab[(d >> 6) & 63]);
			buf.append("=");
		} else if (i == start + len - 1) {
			int d = ((data[i]) & 0x0ff) << 16;

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append("==");
		}

		return buf;
	}

	private static int decode(char c) {
		if (c >= 'A' && c <= 'Z')
			return (c) - 65;
		else if (c >= 'a' && c <= 'z')
			return (c) - 97 + 26;
		else if (c >= '0' && c <= '9')
			return (c) - 48 + 26 + 26;
		else
			switch (c) {
			case '+':
				return 62;
			case '/':
				return 63;
			case '=':
				return 0;
			default:
				throw new RuntimeException(new StringBuffer("错误的数据格式引起decode出错: ").append(c).toString());
			}
	}

	/**
	 * <p>
	 * Decodes the given Base64 encoded String to a new byte array. The byte array holding the decoded data is returned.
	 * </p>
	 * 
	 * @param s a given Base64 encoded String.
	 * @return The decoded data.
	 */
	private static ByteArrayOutputStream decodeToStream(String s) {
		int i = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int len = s.length();
		while (true) {
			while (i < len && s.charAt(i) <= ' ')
				i++;
			if (i == len)
				break;

			char ch0 = s.charAt(i);
			char ch1 = s.charAt(i + 1);
			char ch2 = s.charAt(i + 2);
			char ch3 = s.charAt(i + 3);
			int tri = (decode(ch0) << 18) + (decode(ch1) << 12) + (decode(ch2) << 6) + (decode(ch3));

			bos.write((tri >> 16) & 255);
			if (s.charAt(i + 2) == '=')
				break;
			bos.write((tri >> 8) & 255);
			if (s.charAt(i + 3) == '=')
				break;
			bos.write(tri & 255);

			i += 4;
		}
		return bos;
	}

	/**
	 * <p>
	 * 将字符串解码成比特数组。
	 * </p>
	 * 
	 * @param s 原始字符串。
	 * @return 解码后的比特数组。
	 */
	public static byte[] decodeToBytes(String s) {
		return (s == null) ? null : decodeToStream(s).toByteArray();
	}

	/**
	 * <p>
	 * 将字符串解码为字符串。
	 * </p>
	 * 
	 * @param s 原始字符串。
	 * @return 解码后的字符串。
	 */
	public static String decodeToString(String s) {
		return (s == null) ? null : decodeToStream(s).toString();
	}

}
