package org.mcv.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CharsetDetector {

	private static int utf8State;
	private static int bufsize = 1024;
	
	public static void setBufsize(int size) {
		bufsize = size;
	}
	
	public static String determineCharset(File src) {
		try {
			String charset = null;
			utf8State = 0;
			@Cleanup
			BufferedInputStream imp = new BufferedInputStream(
					new FileInputStream(src));
			int size = bufsize == -1 ? (int)src.length() : bufsize;
			byte[] buf = new byte[size];
			int len = imp.read(buf, 0, buf.length);
			if (len == -1)
				return null;
			charset = CharsetDetector.checkBOM(buf);
			boolean eightbit = false;
			if (charset == null) {
				do {
					if (!isAscii(buf)) {
						eightbit = true;
						if (isUtf8(buf)) {
							charset = "utf-8";
							break;
						} else if (isCp1252(buf)) {
							charset = "cp1252";
							break;
						}
					}
				} while ((imp.read(buf, 0, buf.length)) != -1);
			}
			if(charset == null) {
				if(eightbit) {
					charset = "iso-8859-1";
				} else {
					charset = "us-ascii";
				}
			}
			return charset;
		} catch (IOException e) {
			return "us-ascii";
		}
	}

	private static int unsigned(byte b) {
		return (b & 0xFF);
	}

	public static String checkBOM(byte[] input) {
		if (input.length < 4)
			return null;
		if (unsigned(input[0]) == 0xEF && unsigned(input[1]) == 0xBB
				&& unsigned(input[2]) == 0xBF)
			return "UTF-8";
		if (unsigned(input[0]) == 0xFE && unsigned(input[1]) == 0xFF)
			return "UTF-16BE";
		if (unsigned(input[0]) == 0xFF && unsigned(input[1]) == 0xFE)
			return "UTF-16LE";
		if (unsigned(input[0]) == 0x00 && unsigned(input[1]) == 0x00
				&& unsigned(input[2]) == 0xFE && unsigned(input[3]) == 0xFF)
			return "UTF-32BE";
		if (unsigned(input[0]) == 0xFF && unsigned(input[1]) == 0xFE
				&& unsigned(input[2]) == 0x00 && unsigned(input[3]) == 0x00)
			return "UTF-32LE";
		return null;
	}

	public static boolean isAscii(byte[] input) {
		for (int i = 0; i < input.length; i++) {
			if (unsigned(input[i]) >= 0x80)
				return false;
		}
		return true;
	}

	public static boolean isUtf8(byte[] input) {
		int i = 0;
		for (; utf8State > 0 && i < input.length; utf8State--, ++i) {
			log.debug("initial utf8state = " + utf8State);
			if (unsigned(input[i]) >= 0x80 && unsigned(input[i]) < 0xc0) {
				if (utf8State == 1) {
					utf8State = 0;
					return true;
				}
			}
		}
		for (; i < input.length; i++) {
			// get next byte unsigned
			int b = unsigned(input[i]);
			switch (b >>> 5) {
			case 6:
				if (i == input.length - 1) {
					log.debug("set utf8state to 1");
					utf8State = 1;
				} else {
					// two byte encoding
					// 110yyyyy 10xxxxxx
					if (unsigned(input[i + 1]) >= 0x80
							&& unsigned(input[i + 1]) < 0xc0) {
						return true;
					}
				}
				break;
			case 7:
				// three byte encoding
				// 1110zzzz 10yyyyyy 10xxxxxx
				if (i == input.length - 1) {
					log.debug("set utf8state to 2");
					utf8State = 2;
				} else {
					if (unsigned(input[i + 1]) >= 0x80
							&& unsigned(input[i + 1]) < 0xc0) {
						if (i == input.length - 2) {
							log.debug("set utf8state to 1 (3byte)");
							utf8State = 1;
						} else {
							if (unsigned(input[i + 2]) >= 0x80
									&& unsigned(input[i + 2]) < 0xc0) {
								return true;
							}
						}
					}
				}
				break;
			}
		}
		return false;
	}

	public static boolean isCp1252(byte[] input) {
		for (int i = 0; i < input.length; i++) {
			if (unsigned(input[i]) >= 0x80 && unsigned(input[i]) < 0xA0)
				return true;
		}
		return false;
	}

}
