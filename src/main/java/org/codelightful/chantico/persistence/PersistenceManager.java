package org.codelightful.chantico.persistence;

import org.codelightful.chantico.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

public class PersistenceManager {
    private static final Logger logger = LoggerFactory.getLogger("persistence");
    private static PersistenceManager instance;

    private PersistenceManager() {
    }

    /** Allows to obtain a singleton instance for the persistent manager */
    public static PersistenceManager getInstance() {
        if (instance == null) {
            synchronized (PersistenceManager.class) {
                if (instance == null) {
                    instance = new PersistenceManager();
                }
            }
        }
        return instance;
    }

    /** Allows to obtain the folder where the database will be created */
    private File getDataFolder() {
        return Configuration.getFileFromHome("database");
    }

    /**
     * Internal method to create a database connection. Allows to define if the database should exist or can be created
     * @param create Boolean flag to determine if the database is required to exist or if can be created
     */
    private Connection createConnection(boolean create) throws Exception {
        File dataFolder = getDataFolder();
        String url = "jdbc:h2:file:" + dataFolder.getAbsolutePath();
        if (!create) {
            url += ";IFEXISTS=TRUE";
        }
        return DriverManager.getConnection(url);
    }

    /** Creates a database connection to the data repository */
    public Connection createConnection() {
        try {
            return createConnection(false);
        } catch (Exception ex) {
            logger.error("An error has occurred trying to create the database connection: {}", ex.getMessage());
            throw new RuntimeException("Error opening the database connection", ex);
        }
    }

    /** Creates the database */
    public void createDatabase() {
        List<String> sentenceList = getModelSentences();
        if (sentenceList.isEmpty()) {
            throw new RuntimeException("Unable to create the database because there are not sentences produced");
        }

        logger.info("Creating database objects");
        try(Connection conn = createConnection(true)) {
            try(Statement stmt = conn.createStatement()) {
                for(String sentence : sentenceList) {
                    stmt.executeUpdate(sentence);
                }
            }
        } catch (Exception ex) {
            logger.error("An error has occurred trying to create the database objects: {}", ex.getMessage());
            throw new RuntimeException("Error creating the database objects", ex);
        }
    }

    /** Internal method to produce a XML document builder s*/
    private DocumentBuilder getDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder();
    }

    /** Extracts a model XML file from the resources */
    private Document loadXMLDocument(String name) {
        try {
            DocumentBuilder builder = getDocumentBuilder();
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            URL resourceUrl = classLoader.getResource("model/" + name + ".xml");
            if (resourceUrl == null) {
                logger.error("A xml specification was not found. name={}", name);
                return null;
            }
            try(InputStream stream = resourceUrl.openStream()) {
                return builder.parse(stream);
            }
        } catch (Exception ex) {
            logger.error("An error has occurred trying to load a XML specification. name={} cause={}", name, ex.getMessage());
            throw new RuntimeException("An error has occurred trying lo load a XML specification");
        }
    }

    /** Generates all the sentences to generate the data model based on the specification files */
    private List<String> getModelSentences() {
        Document dataModelSpec = loadXMLDocument("data-model");
        if (dataModelSpec == null) {
            throw new RuntimeException("Unable to generate a data model because the main specification file was not found");
        }

        NodeList dataObjectNodes = dataModelSpec.getElementsByTagName("object");
        int dataObjectCount = dataObjectNodes.getLength();
        if (dataObjectCount == 0) {
            throw new RuntimeException("Unable to generate a data model because the main specification has no database objects");
        }

        List<String> sentenceList = new LinkedList<>();
        for (int idx=0; idx < dataObjectCount; idx++) {
            Element element = (Element) dataObjectNodes.item(idx);
            String objectName = element.getAttribute("name");
            if (objectName == null || objectName.isEmpty()) {
                logger.error("The data model specification contains a database object without name");
            } else {
                String sentence = getObjectSentence(objectName);
                if (sentence != null) {
                    sentenceList.add(sentence);
                }
            }
        }
        return sentenceList;
    }

    /**
     * Generates the SQL sentence for a specific database object
     * @param objectName Name of the object to produce the SQL sentence for it
     * @return SQL sentence or null if could not be produced
     */
    private String getObjectSentence(String objectName) {
        Document objectSpec = loadXMLDocument(objectName);
        if (objectSpec == null) {
            logger.error("Unable to generate a database object because its specification file was not found. object={}", objectName);
            return null;
        }

        NodeList fieldNodes = objectSpec.getElementsByTagName("field");
        int fieldCount = fieldNodes.getLength();
        if (fieldCount == 0) {
            logger.error("Unable to generate a database object because it does not contain any field. object={}", objectName);
            return null;
        }

        StringBuilder sentence = new StringBuilder("CREATE TABLE ").append(objectName).append("(");
        for(int idx=0; idx < fieldCount; idx++) {
            Element element = (Element) fieldNodes.item(idx);
            if (idx > 0) {
                sentence.append(", ");
            }

            String fieldName = element.getAttribute("name");
            if (fieldName == null || fieldName.isEmpty()) {
                logger.error("Unable to create a database object because it does contain a field without name. object={}", objectName);
                return null;
            }
            sentence.append(fieldName).append(" ");

            String fieldType = element.getAttribute("type");
            if (fieldType == null || fieldType.isEmpty()) {
                logger.error("Unable to create a database object because it does contain a field without type. object={} field={}", objectName, fieldName);
                return null;
            }
            sentence.append(fieldType).append(" ");
            if ("IDENTITY".equals(fieldType)) {
                sentence.append(" NOT NULL PRIMARY KEY ");
            } else {
                String nullable = element.getAttribute("nullable");
                if ("true".equals(nullable)) {
                    sentence.append(" NOT NULL ");
                } else {
                    sentence.append(" NULL ");
                }
            }
        }
        sentence.append(")");
        return sentence.toString();
    }
}
