package com.lifehub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for full-stack API integration tests: real Spring context, real Spring Security
 * filter chain, real PostgreSQL (via Flyway-migrated schema), dispatched through {@link MockMvc}.
 * <p>
 * DB isolation: each test method runs inside one Spring-managed transaction (via {@code @Transactional}
 * on the test class) that is rolled back afterwards, so tests never see each other's data even
 * though they share one Spring context / one PostgreSQL instance for the whole run.
 * <p>
 * DB source: by default a Testcontainers-managed PostgreSQL, started once for the whole JVM
 * (the "singleton container" pattern — reused across all test classes instead of one per class).
 * If {@code LIFEHUB_TEST_DB_URL} is set, that database is used directly instead and no container
 * is started at all. This is primarily meant for CI runners that already provide a Postgres
 * service container, but it is also what lets tests run in environments where Docker itself is
 * unavailable (see TROUBLESHOOTING.md) by pointing it at an already-running PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    private static final String ENV_JDBC_URL = "LIFEHUB_TEST_DB_URL";
    private static final String ENV_USERNAME = "LIFEHUB_TEST_DB_USERNAME";
    private static final String ENV_PASSWORD = "LIFEHUB_TEST_DB_PASSWORD";

    private static final PostgreSQLContainer<?> POSTGRES = System.getenv(ENV_JDBC_URL) == null
            ? startContainer()
            : null;

    private static PostgreSQLContainer<?> startContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("lifehub_test")
                .withUsername("lifehub_test")
                .withPassword("lifehub_test");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        String envUrl = System.getenv(ENV_JDBC_URL);
        if (envUrl != null) {
            registry.add("spring.datasource.url", () -> envUrl);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault(ENV_USERNAME, "lifehub"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault(ENV_PASSWORD, "lifehub"));
        } else {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected JsonNode toJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected MockHttpServletRequestBuilder postJson(String url, Object body) {
        return MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }

    protected MockHttpServletRequestBuilder putJson(String url, Object body) {
        return MockMvcRequestBuilders.put(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }

    /** POSTs {@code body} to {@code url}, asserts 201, and returns the created resource's "id". */
    protected long postAndGetId(String url, Object body) throws Exception {
        MvcResult result = mockMvc.perform(postJson(url, body))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        return toJsonNode(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
