package br.com.digitalstore.api;

import br.com.digitalstore.api.model.User;
import br.com.digitalstore.api.repository.UserRepository;
import br.com.digitalstore.api.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    protected static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    protected static final String JPEG_BASE64 = Base64.getEncoder().encodeToString(
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00});

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE product_categories, product_options, product_images, products, "
                + "categories, users RESTART IDENTITY CASCADE");
    }

    protected User createUser(String email, String rawPassword) {
        User user = new User();
        user.setFirstname("Test");
        user.setSurname("User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    protected String bearerToken() {
        User user = createUser("token-" + System.nanoTime() + "@mail.com", "123@123");
        return "Bearer " + jwtService.generate(user);
    }
}
