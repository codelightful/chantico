package org.codelightful.chantico.engine;

import org.codelightful.chantico.Configuration;
import org.codelightful.chantico.model.ArtifactRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

public class ArtifactRepository {
	private static final Logger logger = LoggerFactory.getLogger("artifact");
	public static ArtifactRepository repository = new ArtifactRepository();

	private ArtifactRepository() {
	}

	public static ArtifactRepository getInstance() {
		return repository;
	}

	/**
	 * Allows to obtain the path and file for a specific artifact request
	 * @param request Object with the details for the artifact to obtain the file for it
	 * @return File representing the path and filename for the requested artifact
	 */
	private File getArtifactFile(ArtifactRequest request) {
		String repoFolder = Configuration.getFileFromHome("repository").getAbsolutePath();
		String baseFolder = Paths.get(repoFolder, request.groupParts).toString();
		if (request.version == null) {
			return Paths.get(baseFolder, request.artifact, request.fileName).toFile();
		} else {
			return Paths.get(baseFolder, request.artifact, request.version, request.fileName).toFile();
		}
	}

	public boolean retrieveArtifact(ArtifactRequest request, OutputStream output) {
		File sourceFile = getArtifactFile(request);
		if (!sourceFile.exists()) {
			if (!Configuration.getInstance().getBoolean("proxy", false)) {
				logger.error("An artifact could not be found in the local repository. group={} artifact={} version={} file={}",
						request.group, request.artifact, request.version, request.fileName);
			} else {
				//https://mvnrepository.com/artifact/
			}
		} else {
			if (output != null) {
				try (InputStream input = new FileInputStream(sourceFile)) {
					int readByte;
					do {
						readByte = input.read();
						if (readByte >= 0) {
							output.write(readByte);
						}
					} while (readByte >= 0);
				} catch (Exception ex) {
					logger.error("An error has occurred trying to load an artifact file. group={} artifact={} version={} file={}: {}",
							request.group, request.artifact, request.version, request.fileName, ex.getMessage());
					throw new RuntimeException("Error reading artifact", ex);
				}
			}
			return true;
		}
		return false;
	}

	public void storeArtifact(ArtifactRequest request, InputStream input) {
		File targetFile = getArtifactFile(request);
		targetFile.getParentFile().mkdirs();

		try (FileOutputStream output = new FileOutputStream(targetFile)) {
			int readByte;
			do {
				readByte = input.read();
				if (readByte >= 0) {
					output.write(readByte);
				}
			} while (readByte >= 0);
		} catch (Exception ex) {
			logger.error("An error has occurred trying to store an artifact. group={} id={} version={} file={} targetFile={}: {}",
					request.group, request.artifact, request.version, request.fileName, targetFile, ex.getMessage());
			throw new RuntimeException("Artifact store error", ex);
		}
	}
}
