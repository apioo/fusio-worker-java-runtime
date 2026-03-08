package org.fusioproject.worker.runtime;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.fusioproject.worker.runtime.exception.ConnectionException;
import org.fusioproject.worker.runtime.exception.ConnectionNotFoundException;
import org.fusioproject.worker.runtime.exception.InvalidConnectionTypeException;
import org.fusioproject.worker.runtime.exception.RuntimeException;
import org.fusioproject.worker.runtime.generated.ExecuteConnection;
import redis.clients.jedis.RedisClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Connector {
    private final Map<String, ExecuteConnection> connections;
    private final HashMap<String, Object> instances;
    private final ObjectMapper objectMapper;

    public Connector(Map<String, ExecuteConnection> connections) {
        this.connections = connections;
        this.instances = new HashMap<>();
        this.objectMapper = new ObjectMapper();
    }

    public Object getConnection(String name) throws RuntimeException {
        if (this.instances.containsKey(name)) {
            return this.instances.get(name);
        }

        if (!this.connections.containsKey(name)) {
            throw new ConnectionNotFoundException("Provided connection is not configured");
        }

        ExecuteConnection connection = this.connections.get(name);
        HashMap<String, String> config = this.parseConfig(connection.getConfig());

        if (connection.getType().equals("Fusio.Adapter.Sql.Connection.Sql")) {
            java.sql.Connection con;
            if (config.get("type").equals("pdo_mysql")) {
                con = this.newSqlConnection("mysql://" + config.get("host") + ":3306/" + config.get("database") + "?user=" + config.get("username") + "&password=" + config.get("password"));
            } else if (config.get("type").equals("pdo_pgsql")) {
                con = this.newSqlConnection("postgresql://" + config.get("host") + ":5432/" + config.get("database") + "?user=" + config.get("username") + "&password=" + config.get("password"));
            } else {
                throw new ConnectionException("SQL type is not supported");
            }

            this.instances.put(name, con);

            return con;
        } else if (connection.getType().equals("Fusio.Adapter.Sql.Connection.SqlAdvanced")) {
            var con = this.newSqlConnection(config.get("url"));

            this.instances.put(name, con);

            return con;
        } else if (connection.getType().equals("Fusio.Adapter.Http.Connection.Http")) {
            HttpClientBuilder builder = HttpClientBuilder.create();
            CloseableHttpClient client = builder.build();

            // @TODO configure a base url so that the action can only make requests against this base url
            //config.get("url");

            // @TODO configure proxy for http client
            //config.get("username");
            //config.get("password");
            //config.get("proxy");

            this.instances.put(name, client);

            return client;
        } else if (connection.getType().equals("Fusio.Adapter.Mongodb.Connection.MongoDB")) {
            var client = MongoClients.create(config.get("url"));
            var database = client.getDatabase(config.get("database"));

            this.instances.put(name, database);

            return database;
        } else if (connection.getType().equals("Fusio.Adapter.Elasticsearch.Connection.Elasticsearch")) {
            var client = ElasticsearchClient.of(builder -> builder
                .host(config.get("host"))
                .apiKey(config.get("password"))
            );

            this.instances.put(name, client);

            return client;
        } else if (connection.getType().equals("Fusio.Adapter.File.Connection.Filesystem")) {
            var filesystem = FileSystems.getFileSystem(URI.create(config.get("config")));

            this.instances.put(name, filesystem);

            return filesystem;
        } else if (connection.getType().equals("Fusio.Adapter.Redis.Connection.Redis")) {
            int port = Integer.parseInt(config.get("port"));
            var client = RedisClient.builder().hostAndPort(config.get("host"), port).build();

            this.instances.put(name, client);

            return client;
        } else {
            throw new InvalidConnectionTypeException("Provided a not supported connection type");
        }
    }

    private java.sql.Connection newSqlConnection(String url) throws ConnectionException {
        try {
            return DriverManager.getConnection("jdbc:" + url);
        } catch (SQLException e) {
            throw new ConnectionException("Could not obtain connection", e);
        }
    }

    private HashMap<String, String> parseConfig(String rawConfig) throws ConnectionException {
        TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};

        try {
            return this.objectMapper.readValue(Base64.getDecoder().decode(rawConfig), typeRef);
        } catch (IOException e) {
            throw new ConnectionException("Could not parse connection config");
        }
    }
}
