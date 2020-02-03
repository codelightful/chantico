package org.codelightful.chantico;

import org.codelightful.chantico.persistence.PersistenceManager;
import org.codelightful.harpo.RSAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

public class Chantico {
	private static final Logger logger = LoggerFactory.getLogger("server");
	/** Contains the instance for the current running process */
	private static Chantico chantico;
	/** Reference to the embedded application server */
	private EmbeddedServer server;
	/** Contains the reference to the key pair used to execute encryption operations */
	private KeyPair keyPair;

	/** Entry point to start the artifact server */
	public static void main(String[] args) throws Exception {
		System.out.print("\033[H\033[2J");
		System.out.flush();
		System.out.println("--------------------------------------------------------------------------------");
		System.out.println(" _______ _     _ _______ __   _ _______ _____ _______  _____ ");
		System.out.println(" |       |_____| |_____| | \\  |    |      |   |       |     |");
		System.out.println(" |_____  |     | |     | |  \\_|    |    __|__ |_____  |_____|");
		System.out.println("\r\n Artifact Server v1.0.0"); // TODO: set the version dynamically
		System.out.println("--------------------------------------------------------------------------------");
		try {
			chantico = new Chantico();
			chantico.start();
		} catch (Exception ex) {
			logger.error("An error has occurred trying to start the server", ex);
		} finally {
			System.out.println("--------------------------------------------------------------------------------");
		}
	}

	/** Allows to obtain the current running instance */
	public static Chantico current() {
		return chantico;
	}

	/**
	 * Starts the Chantico server
	 */
	public void start() throws Exception {
		if (canStart()) {
			configure();
			server = new EmbeddedServer();
			server.start();
		}
	}

	/** Obtains the reference to the control file used to prevent multiple instances */
	private File getProcessControlFile() {
		return Configuration.getFileFromHome("chantico.pid");
	}

	/** Executes a minimum set of system validations to determine if the application can start */
	private boolean canStart() {
		File processControl = getProcessControlFile();
		if (processControl.exists()) {
			logger.error("A previous server instance seems to be running. Please check the running processes " +
					"in your operating system. If you consider this could be an error remove the chantico.pid file " +
					"from the chantico home folder");
			return false;
		}

		try {
			if(!processControl.getParentFile().mkdirs()) {
				logger.error("Unable to create the folder to cont");

			} else if(!processControl.createNewFile()) {

			} else {

			}
		} catch (Exception ex) {
			logger.error("An error has occurred trying to create the process control file. location={} cause={}", processControl.getAbsolutePath(), ex.getMessage());
			throw new RuntimeException("An error has occurred trying to create the process control file", ex);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				File processControl = getProcessControlFile();
				if (processControl.exists() && !processControl.delete()) {
					logger.error("The process control file located on {} could not be removed.  Please try " +
							"to remove the file manually. Otherwise the server could not be able to start in " +
							"a future execution", processControl.getAbsolutePath());
				}
				System.out.println("--------------------------------------------------------------------------------");
				System.out.println("The Chantico server has been terminated");
			}
		});
		return true;
	}

	/**
	 * Determines if the server is being started by first time and creates the default configuration file and any
	 * other activity required to initialize the application
	 */
	private void configure() {
		Configuration configuration = Configuration.getInstance();
		String serverKey = configuration.getString("server.key", null);
		if (serverKey == null) {
			try {
				logger.info("Starting the server for the first time");
				configuration.setValue("server.key", UUID.randomUUID().toString());
				configuration.setValue("port", "8080");

				PersistenceManager.getInstance().createDatabase();
				RSAUtil.getInstance().createKeyPair(1024, getRSAKeyStorage());

				configuration.store();
			} catch (Exception ex) {
				throw new RuntimeException("An error has occurred trying to configure the application", ex);
			}
		}
	}

	/** Returns the storage used to save and retrieve the RSA keys used in cipher processes */
	public RSAUtil.FileSystemStorage getRSAKeyStorage() {
		return new RSAUtil.FileSystemStorage(Configuration.getHome());
	}

	/** Obtains the singleton key pair instance used by cipher processes */
	private KeyPair getKeyPair() {
		if(keyPair == null) {
			synchronized (this) {
				if(keyPair == null) {
					keyPair = RSAUtil.getInstance().getKeyPair(getRSAKeyStorage());
				}
			}
		}
		return keyPair;
	}

	/** Gets the public key used by cipher processes */
	private PublicKey getPublicKey() {
		return getKeyPair().getPublic();
	}

	/** Gets the private key used by cipher processes */
	private PrivateKey getPrivateKey() {
		return getKeyPair().getPrivate();
	}

	/** Encrypt a string using the default cipher method */
	public static String encrypt(String toEncrypt) {
		return RSAUtil.getInstance().encrypt(Chantico.current().getPublicKey(), toEncrypt);
	}

	/** Decrypt a string using the default cipher method */
	public static String decrypt(String toDecrypt) {
		return RSAUtil.getInstance().decrypt(Chantico.current().getPrivateKey(), toDecrypt);
	}
}
