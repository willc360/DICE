package remoting;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WrappingServletFilter implements Filter {

	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if(req instanceof HttpServletRequest) {
			chain.doFilter((HttpServletRequest) req, new CustomHttpServletResponseWrapper((HttpServletResponse) res));
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		
	}

}
