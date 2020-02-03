package org.codelightful.chantico;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.codelightful.chantico.servlet.ArtifactServlet;
import org.codelightful.chantico.servlet.RestApiServlet;
import org.codelightful.chantico.servlet.StaticContentServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

/** Wraps the embedded application server */
public class EmbeddedServer {
	private static final Logger logger = LoggerFactory.getLogger("server");

	/*
	Notes on TOMCAT:
	----------------
	Architecture Hierarchy:
	Server(1) -> Service(*) -> Engine(1) -> Host(*) -> Context(*) -> Web Application(1)
	One ore more Connectors to the Service
	Server: represents the entire Catalina servlet engine and is used as a top-level element for a single Tomcat instance
	Service: holds a collection of one or more <Connector> elements that share a single <Engine> element
	Connector:  the class that does the actual handling requests and responses to and from a calling client application
	Engine: handles all requests received by all of the defined <Connector> components defined by a parent service
	Host: defines the virtual hosts that are contained in each instance of a Catalina <Engine>
	Context: most commonly used container in a Tomcat instance. Represents an individual web application that is running
	*/
	private Tomcat tomcat = new Tomcat();

	public EmbeddedServer() {
		configurePort();
		configureContext();
	}

	/** Starts the application server */
	public void start() throws Exception {
		java.util.logging.Logger.getLogger("org.apache").setLevel(java.util.logging.Level.WARNING);
		tomcat.start();
		tomcat.getServer().await();
	}

	/** Stops the application server */
	public void stop() throws Exception {
		tomcat.stop();
	}

	private void configurePort() {
		int port = Configuration.getInstance().getInt("port", 8080);
		logger.info("Starting server at port {}", port);
		tomcat.setPort(port);
	}

	private void configureContext() {
		String contextDir = Configuration.getFileFromHome("server").toString();
		logger.debug("Deploying server context on: {}", contextDir);
		tomcat.setBaseDir(contextDir);
		Context context = tomcat.addContext("", contextDir);

		tomcat.addServlet(context, "StaticContextServlet", new StaticContentServlet());
		context.addServletMappingDecoded(StaticContentServlet.SERVLET_CONTEXT, "StaticContextServlet");

		tomcat.addServlet(context, "RestApiServlet", new RestApiServlet());
		context.addServletMappingDecoded(RestApiServlet.SERVLET_CONTEXT + "*", "RestApiServlet");

		tomcat.addServlet(context, "ArtifactServlet", new ArtifactServlet());
		context.addServletMappingDecoded(ArtifactServlet.SERVLET_CONTEXT, "ArtifactServlet");
	}
}
