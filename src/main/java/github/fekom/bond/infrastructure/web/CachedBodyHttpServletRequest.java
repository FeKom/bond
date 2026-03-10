package github.fekom.bond.infrastructure.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

	private final byte[] cachedBody;

	public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
		super(request);
		this.cachedBody = request.getInputStream().readAllBytes();
	}

	public byte[] getCachedBody() {
		return cachedBody;
	}

	@Override
	public ServletInputStream getInputStream() {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
		return new ServletInputStream() {
			@Override
			public boolean isFinished() {
				return byteStream.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
				// not needed
			}

			@Override
			public int read() {
				return byteStream.read();
			}
		};
	}
}
