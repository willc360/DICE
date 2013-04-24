package remoting;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class CustomHttpServletResponseWrapper extends HttpServletResponseWrapper {

	HttpServletResponse response;
	
	public CustomHttpServletResponseWrapper(HttpServletResponse response) {
		super(response);
		this.response = response;
	}
	
	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriterWrapper(super.getWriter());
	}

}
