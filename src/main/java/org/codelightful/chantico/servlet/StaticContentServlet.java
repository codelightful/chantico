package org.codelightful.chantico.servlet;

import org.codelightful.chantico.engine.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@WebServlet(name = "StaticContentServlet", urlPatterns = {"/*"})
public class StaticContentServlet extends AbstractServlet {
	private static final Logger logger = LoggerFactory.getLogger("servlet-static");
	/** Constant with the web context used for the static content servlet */
	public static final String SERVLET_CONTEXT = "/";

	@Override
	protected void doServe(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String uri = request.getRequestURI();
		if ("/".equals(uri)) {
			uri = "webapp/index.html";
		} else {
			uri = "webapp" + uri;
		}

		boolean isHtml = uri.endsWith(".html");
		if (isHtml) {
			if (!UserService.getInstance().hasUsers()) {
				uri = "internal/no-user.html";
			} else if (!isAuthenticated(request)) {
				uri = "internal/login.html";
			}
		}
		prepareResponse(uri, response);

		OutputStream out = response.getOutputStream();
		if (isHtml) {
			writeResource(out, "internal/common-header.html");
			writeJavaScriptRequestParameters(request.getParameterMap(), out);
		}
		if (!writeResource(out, uri)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (isHtml) {
			writeResource(out, "internal/common-footer.html");
		}
	}

	/** Internal method to prepare the response headers */
	private void prepareResponse(String uri, HttpServletResponse response) {
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setHeader(HttpHeaders.CONTENT_TYPE, getContentType(uri));
	}

	/** Internal method to consume request parameters and make it visible to the JavaScript content in the page */
	private void writeJavaScriptRequestParameters(Map<String, String[]> parameters, OutputStream out) throws Exception {
		if (parameters != null && !parameters.isEmpty()) {
			StringBuilder parameterContent = new StringBuilder();
			parameterContent.append("<script type=\"text/javascript\">");
			parameterContent.append("var requestParams = {};");
			for (String key : parameters.keySet()) {
				String[] values = parameters.get(key);
				if(values != null && values.length > 0) {
					parameterContent.append("requestParams['").append(key).append("'] = ");
					if(values.length == 1) {
						parameterContent.append("'").append(values[0]).append("';");
					} else {
						parameterContent.append("[");
						int valueCount = 0;
						for(String value : values) {
							if(valueCount > 0) {
								parameterContent.append(", ");
							}
							parameterContent.append("'").append(value).append("'");
							valueCount++;
						}
						parameterContent.append("];");
					}
				}
			}
			parameterContent.append("</script>");
			out.write(parameterContent.toString().getBytes());
		}
	}

	/**
	 * Reads the content from a resource and write it to an output stream
	 * @param out Output stream to write the content on it
	 * @param uri URI for the resource to read
	 * @return A boolean value to determine if the resource could be loaded
	 */
	private boolean writeResource(OutputStream out, String uri) throws Exception {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL resource = classLoader.getResource(uri);
		logger.info("resource={}", resource);
		if (resource == null) {
			logger.error("A static resource could not be found: {}", uri);
			return false;
		}

		try (InputStream input = classLoader.getResourceAsStream(uri)) {
			try {
				int readedByte;
				do {
					readedByte = input.read();
					if (readedByte >= 0) {
						out.write(readedByte);
					}
				} while (readedByte >= 0);
			} finally {
				out.flush();
			}
		}
		return true;
	}

	/**
	 * Internal method to get the content type for the requested URI
	 * @param uri Requested resource
	 */
	private String  getContentType(String uri) {
		if (uri.endsWith(".html") || uri.endsWith("htm")) {
			return MediaType.TEXT_HTML;
		} else if (uri.endsWith(".css")) {
			return "text/css";
		} else if (uri.endsWith(".js")) {
			return "text/javascript";
		} else if (uri.endsWith(".png")) {
			return "image/png";
		} else if (uri.endsWith(".woff")) {
			return "font/woff";
		} else if (uri.endsWith(".woff2")) {
			return "font/woff2";
		} else if (uri.endsWith(".ttf")) {
			return "font/truetype";
		} else {
			logger.error("A static resource has been denied because it is not listed on the accepted content types. uri={}", uri);
			throw new RuntimeException("Invalid content type");
		}
	}

	@Override
	protected void handleError(HttpServletRequest request, HttpServletResponse response) {
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
}
