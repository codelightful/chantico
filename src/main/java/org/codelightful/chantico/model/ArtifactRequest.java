package org.codelightful.chantico.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * DTO that represents a request to extract or to store an artifact
 */
public class ArtifactRequest {
	/** Constant with the URI part that contains the artifact filename */
	private static final String URI_PART_FILE_NAME = "fileName";
	/** Constant with the URI part that contains the artifact version */
	private static final String URI_PART_VERSION = "version";
	/** Constant with the URI part that contains the artifact name */
	private static final String URI_PART_ARTIFACT = "artifact";
	/** Constant with the URI part that contains the artifact group */
	private static final String URI_PART_GROUP = "group";
	/** Array with the order in which the atifact is described in a request URI */
	private static final String[] URI_PARTS = new String[] {
			URI_PART_GROUP,
			URI_PART_ARTIFACT,
			URI_PART_VERSION,
			URI_PART_FILE_NAME
	};

	/** Contains the entire group hierarchy (separated by dots) */
	public String group;
	/** Name of the artifact */
	public String artifact;
	/** Version requested for the artifact (it can be null for files that does not belong to a specific version) */
	public String version;
	/** Name of the file to extract */
	public String fileName;
	/** Array containing the separate hierarchy pieces of the group */
	public String[] groupParts;

	/**
	 * Creates an object representing the requests received to execute an artifact operation
 	 * @param path Resoyrce received in the request (is equivalent to the URI without the context part). For example:
	 *             if the URI is /artifact/group/version/artifact/file and the context for the servlet is /artifact
	 *             then the path for this method should be group/version/artifact/file
	 * @return Object that decomposes the request
	 */
	public static ArtifactRequest parse(String path) {
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("Unable to process an artifact request from a null or empty path");
		} else if (path.startsWith("/")) {
			path = path.substring(1);
		}
		String[] parsed = path.split("\\/");

		ArtifactRequest artifactRequest = new ArtifactRequest();
		int partIndex = URI_PARTS.length;
		for (int idx = parsed.length - 1; idx >= 0; idx--) {
			partIndex --;
			String partName = URI_PARTS[partIndex];
			if (URI_PART_FILE_NAME.equals(partName)) {
				artifactRequest.fileName = parsed[idx];
				// the maven metadata URI does not contain a version since is a generic file for the package
				if (artifactRequest.fileName.startsWith("maven-metadata")) {
					partIndex--;
				}
			} else if (URI_PART_VERSION.equals(partName)) {
				artifactRequest.version = parsed[idx];
			} else if (URI_PART_ARTIFACT.equals(partName)) {
				artifactRequest.artifact = parsed[idx];
			} else if (URI_PART_GROUP.equals(partName)) {
				artifactRequest.groupParts = new String[idx + 1];
				for (int gdx=0; gdx <= idx; gdx++) {
					artifactRequest.groupParts[gdx] = parsed[gdx];
				}
				artifactRequest.group = Arrays.stream(artifactRequest.groupParts).collect(Collectors.joining("."));
				break;
			}
		}
		return artifactRequest;
	}
}
