package github.fekom.bond.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP compression utilities for calculating request payload size.
 * <p>
 * Bond uses compressed size (not raw size) for rate limiting,
 * so clients sending highly compressible data pay less from their quota.
 */
public class PayloadCompressor {

	public static byte[] compress(String payload) throws IOException {
		return compress(payload.getBytes("UTF-8"));
	}

	public static byte[] compress(byte[] data) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
			gzipStream.write(data);
			gzipStream.finish();
		}
		return byteStream.toByteArray();
	}

	public static String decompress(byte[] compressedData) throws IOException {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
		StringBuilder result = new StringBuilder();
		try (GZIPInputStream gzipStream = new GZIPInputStream(byteStream)) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = gzipStream.read(buffer)) != -1) {
				result.append(new String(buffer, 0, len, "UTF-8"));
			}
		}
		return result.toString();
	}

	public static long compressedSize(byte[] data) throws IOException {
		return compress(data).length;
	}
}
