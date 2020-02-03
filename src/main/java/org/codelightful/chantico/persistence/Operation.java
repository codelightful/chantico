package org.codelightful.chantico.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface Operation {
    /** Represents a persistent operation to retrieve data */
    class Query implements Operation {
        private static Logger logger = LoggerFactory.getLogger("persistence");
        /** Contains the sentence to be executed */
        private String sentence;
        /** Parameters to bind in the sentence */
        private Object[] parameters;

        private Query() {}

        /**
         * Creates a query instance for a specific sentence
         * @param sentence String with the sentence to execute
         * @param parameters Parameters to bind in the sentence
         */
        public static Query from(String sentence, Object... parameters) {
            Query query = new Query();
            query.sentence = sentence;
            query.parameters = parameters;
            return query;
        }

        /**
         * Executes the query and process every record using a processor instance
         * @param processor Instance to process the extracted records
         */
        public void execute(ResultProcessor processor) {
            try(Connection connection = PersistenceManager.getInstance().createConnection()) {
                try(PreparedStatement stmt = connection.prepareStatement(this.sentence)) {
                    int parameterIndex = 0;
                    for (Object param : parameters) {
                        parameterIndex++;
                        stmt.setObject(parameterIndex, param);
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            if (processor != null) {
                                processor.process(rs);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("An error has occurred trying to execute a query sentence. query={} cause={}", this.sentence, ex.getMessage());
                throw new RuntimeException("Error executing a query sentence", ex);
            }
        }

        /** Functional interface that is received by a query to process the records retrieved */
        @FunctionalInterface
        public interface ResultProcessor {
            public void process(ResultSet rs) throws Exception;
        }
    }

    /** Represents a persistent operation to update data */
    class Update implements Operation {
        private static Logger logger = LoggerFactory.getLogger("persistence");
        /** Contains the sentence to be executed */
        private String sentence;
        /** Parameters to bind in the sentence */
        private Object[] parameters;

        private Update() {}

        /**
         * Creates an update instance for a specific sentence
         * @param sentence String with the sentence to execute
         * @param parameters Parameters to bind in the sentence
         */
        public static Update from(String sentence, Object... parameters) {
            Update update = new Update();
            update.sentence = sentence;
            update.parameters = parameters;
            return update;
        }

        public int execute() {
            try(Connection connection = PersistenceManager.getInstance().createConnection()) {
                PreparedStatement stmt = connection.prepareStatement(this.sentence);
                int parameterIndex = 0;
                for(Object param : parameters) {
                    parameterIndex++;
                    stmt.setObject(parameterIndex, param);
                }

                return stmt.executeUpdate();
            } catch (Exception ex) {
                logger.error("An error has occurred trying to execute an update sentence. query={} cause={}", this.sentence, ex.getMessage());
                throw new RuntimeException("Error executing an update sentence", ex);
            }
        }
    }
}
