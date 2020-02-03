package org.codelightful.chantico.servlet;

import org.codelightful.chantico.Chantico;
import org.codelightful.chantico.Configuration;
import org.codelightful.chantico.engine.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestApiServlet extends AbstractServlet {
	private static final Logger logger = LoggerFactory.getLogger("servlet-api");
	/** Constant with the web context used for the API servlet */
	public static final String SERVLET_CONTEXT = "/api/";

	@Override
	protected void doServe(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String uri = request.getRequestURI().substring(SERVLET_CONTEXT.length());
		logger.trace("API request - method={} uri={}", request.getMethod(), uri);
		if (uri.equals("initialize")) {
			serveInitialize(request, response);
		} else if(uri.equals("login")) {
			serveLogin(request, response);
		} else {
			throw new IllegalArgumentException("Invalid API operation: " + uri);
		}
	}

	@Override
	protected void handleError(HttpServletRequest request, HttpServletResponse response) {
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	/** Initializes the server for its first usage */
	private void serveInitialize(HttpServletRequest request, HttpServletResponse response) throws Exception {
		UserService userService = UserService.getInstance();
		if (userService.hasUsers()) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("already-initialized");
			return;
		}

		String serverKey = request.getParameter("serverKey");
		if (serverKey == null || serverKey.isEmpty()) {
			throw new IllegalArgumentException("No server key provided");
		} else if (!serverKey.equals(Configuration.getInstance().getString("server.key", null))) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("invalid-server-key");
		} else {
			String email = request.getParameter("email");
			String password = request.getParameter("password");
			String another = Chantico.encrypt(password);
			password = Chantico.encrypt(password);
			userService.createUser(email, null, password);
		}
	}

	/** Executes a login operation */
	private void serveLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String login = request.getParameter("usr");
		String password = request.getParameter("pwd");
		String userName = UserService.getInstance().authenticate(login, password);
		if(userName == null) {
			response.getWriter().write("unauthorized");
		} else {
			request.getSession(true).setAttribute(AUTH_SESSION_ATTRIBUTE, Chantico.encrypt(userName));
			response.getWriter().write("granted");
		}
	}
}
