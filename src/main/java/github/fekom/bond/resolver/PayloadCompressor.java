package github.fekom.bond.resolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PayloadCompressor {

	/**
	 * Comprime payload usando GZIP
	 */
	public static byte[] compress(String payload) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
			gzipStream.write(payload.getBytes("UTF-8"));
			gzipStream.finish();
		}

		return byteStream.toByteArray();
	}

	/**
	 * Comprime bytes usando GZIP
	 */
	public static byte[] compress(byte[] data) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
			gzipStream.write(data);
			gzipStream.finish();
		}

		return byteStream.toByteArray();
	}

	/**
	 * Descomprime payload GZIP
	 */
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

	/**
	 * Retorna estatísticas de compressão
	 */
	public static CompressionStats getStats(String original) throws IOException {
		byte[] compressed = compress(original);
		int originalSize = original.getBytes("UTF-8").length;
		int compressedSize = compressed.length;
		double ratio = (1.0 - (double) compressedSize / originalSize) * 100;

		return new CompressionStats(originalSize, compressedSize, ratio);
	}

	public record CompressionStats(
			int originalSizeBytes,
			int compressedSizeBytes,
			double compressionRatio) {
	}
}
