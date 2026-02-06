package ru.lifevaluable.brewflow.user.integration.common;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.lifevaluable.brewflow.user.entity.Role;
import ru.lifevaluable.brewflow.user.entity.User;
import ru.lifevaluable.brewflow.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("brewflow_user")
            .withUsername("test_user")
            .withPassword("test_password");

    // один контейнер для всех тестов
    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",           postgres::getJdbcUrl);
        registry.add("spring.datasource.username",      postgres::getUsername);
        registry.add("spring.datasource.password",      postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto",   () -> "validate");
    }

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    protected User createUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        return user;
    }
}
