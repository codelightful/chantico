package org.codelightful.chantico.servlet;

import org.codelightful.chantico.engine.ArtifactRepository;
import org.codelightful.chantico.model.ArtifactRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

public class ArtifactServlet extends AbstractServlet {
	private static final Logger logger = LoggerFactory.getLogger("artifact");
	/** Constant with the web context used for the artifact servlet */
	public static final String SERVLET_CONTEXT = "/artifact/*";

	@Override
	protected void doServe(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.trace("Request Received: method={} uri={}", request.getMethod(), request.getRequestURI());
		ArtifactRequest artifactRequest = getArtifactRequest(request);

		int responseStatus;
		if (HttpMethod.HEAD.equals(request.getMethod())) {
			responseStatus = retrieveArtifact(artifactRequest, null);
		} else if (HttpMethod.GET.equals(request.getMethod())) {
			responseStatus = retrieveArtifact(artifactRequest, response);
		} else if (HttpMethod.PUT.equals(request.getMethod())) {
			responseStatus = storeArtifact(artifactRequest, request);
		} else {
			logger.error("An invalid request method ({}) has been received. uri={}", request.getMethod(), request.getRequestURI());
			responseStatus = HttpServletResponse.SC_BAD_REQUEST;
		}
		response.setStatus(responseStatus);
	}

	/**
	 * Retrieves an artifact from the repository and writes its content to the response.  When the response is not
	 * provided then only verifies if the artifact is available
	 * @param artifactRequest Object representing the metadata for the requested artifact
	 * @param response HTTP response to write the content on it or null to do not write any content
	 * @return HTTP status code to set in the response
	 */
	private int retrieveArtifact(ArtifactRequest artifactRequest, HttpServletResponse response) throws Exception{
		OutputStream output = null;
		if (response != null) {
			output = response.getOutputStream();
		}
		if (ArtifactRepository.getInstance().retrieveArtifact(artifactRequest, output)) {
			return HttpServletResponse.SC_OK;
		}
		return HttpServletResponse.SC_NOT_FOUND;
	}

	/**
	 * Internal method to handle the request to store an artifact into the repository
	 * @param artifactRequest The object with the description of the artifact to store
	 * @param httpRequest The HTTP request containing the artifact data received from the client
	 * @return HTTP status code to set in the response
	 */
	private int storeArtifact(ArtifactRequest artifactRequest, HttpServletRequest httpRequest) throws Exception {
		String contentType = httpRequest.getHeader(HttpHeaders.CONTENT_TYPE);
		Integer contentLength = getIntegerHeader(httpRequest, HttpHeaders.CONTENT_LENGTH);
			if (!MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
			logger.error("An invalid content type ({}) has been received for a PUT request. uri={}", contentType, httpRequest.getRequestURI());
		} else if (contentLength == null || contentLength == 0) {
			logger.error("An request without content has been received for a PUT request. uri={}", httpRequest.getRequestURI());
		} else {
			ArtifactRepository.getInstance().storeArtifact(artifactRequest, httpRequest.getInputStream());
			return HttpServletResponse.SC_OK;
		}
		return HttpServletResponse.SC_BAD_REQUEST;
	}

	/**
	 * Generates an object representing the requested artifact for an HTTP request
 	 * @param request HTTP request
	 */
	public ArtifactRequest getArtifactRequest(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if ("/".equals(uri)) {
			logger.error("A request has been received to serve an artifact with no information. remoteIp={}", request.getRemoteAddr());
			throw new IllegalArgumentException("Unable to serve the requested artifact");
		}
		uri = uri.substring(SERVLET_CONTEXT.length() - 1);
		return ArtifactRequest.parse(uri);
	}

	@Override
	protected void handleError(HttpServletRequest request, HttpServletResponse response) {
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
}
