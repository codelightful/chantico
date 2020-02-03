package org.codelightful.chantico.servlet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codelightful.chantico.Chantico;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public abstract class AbstractServlet extends HttpServlet {
	/** Constant with the name of the session attribute to store the authentication key */
	protected static final String AUTH_SESSION_ATTRIBUTE = "chantico-auth";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		serveRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		serveRequest(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		serveRequest(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		serveRequest(req, resp);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	/** Internal method with a common impelementation to serve all the requests */
	private void serveRequest(HttpServletRequest request, HttpServletResponse response) {
		try {
			doServe(request, response);
		} catch (Exception ex) {
			Logger logger = getLogger();
			if (logger != null) {
				logger.error("An error has occurred trying to handle a server request", ex);
			}
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			handleError(request, response);
		}
	}

	protected abstract void doServe(HttpServletRequest request, HttpServletResponse response) throws Exception;

	protected abstract void handleError(HttpServletRequest request, HttpServletResponse response);

	protected abstract Logger getLogger();

	/**
	 * Parses a JSON content received in the body
	 * @param request HTTP rewquest
	 * @param targetClass Target class to deserialize the content as it
	 * @param <T> Data type with the class for the object to return
	 */
	protected <T> T parseBody(HttpServletRequest request, Class<T> targetClass) {
		try {
			InputStream stream = request.getInputStream();
			if (stream != null) {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.readValue(stream, targetClass);
			}
		} catch (Exception ex) {
			throw new RuntimeException("An error has occurred trying to parse the body request", ex);
		}
		return null;
	}

	/**
	 * Obtains a header value as an Integer
	 * @param request HTTP request to obtain the header from it
	 * @param headerName Header name to obtain
	 */
	protected Integer getIntegerHeader(HttpServletRequest request, String headerName) {
		String headerValue = request.getHeader(headerName);
		if (headerValue != null && !headerValue.isEmpty()) {
			try {
				return new Integer(headerValue);
			} catch (Exception ex) {
				getLogger().error("An error has occurred trying to parse a header as an integer. header={} value={}", headerName, headerValue);
			}
		}
		return null;
	}

	/**
	 * Extracts the user from the authorization mechanism for a specific HTTP request
	 * @param request HTTP request
	 */
	protected Authentication getUser(HttpServletRequest request) {
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth != null) {
			int separator = auth.indexOf(" ");
			if (separator <= 0) {
				getLogger().error("A malformed authorization request header has been received. header={} uri={} remoteIp={}",
						auth, request.getRequestURI(), request.getRemoteAddr());
				throw new RuntimeException("Malformed authorization");
			}
			String authMethod = auth.substring(0, separator).trim().toLowerCase();
			auth = auth.substring(separator);

			if ("basic".equals(authMethod)) {
				byte[] decodedAuth = Base64.getDecoder().decode(auth);
				auth = new String(decodedAuth, StandardCharsets.UTF_8);
				String[] authValues = auth.split("\\:");
				Authentication authentication = new Authentication();
				authentication.user = authValues[0];
				authentication.password = authValues[1];
				return authentication;
			} else {
				throw new RuntimeException("Invalid authentication method: " + authMethod);
			}
		}
		return null;
	}

	/** Class representing an authentication information obtained from a request */
	public static class Authentication {
		/** User name received in the request */
		public String user;
		/** User password */
		public String password;
	}

	/** Allows to obtain the current logged in user */
	public String getCurrentUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if(session != null) {
			String sessionAuth = (String) session.getAttribute(AUTH_SESSION_ATTRIBUTE);
			if(sessionAuth != null) {
				return Chantico.decrypt(sessionAuth);
			}
		}
		return null;
	}

	/** Allows to determine if there is an user authenticated */
	public boolean isAuthenticated(HttpServletRequest request) {
		return getCurrentUser(request) != null;
	}
}
