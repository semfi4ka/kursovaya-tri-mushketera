package com.example.kursach.api;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,64}$");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request == null || isBlank(request.surname()) || isBlank(request.firstName())) {
            return error(HttpStatus.BAD_REQUEST, "Укажите фамилию и имя");
        }
        if (request.birthDate() == null || request.birthDate().isAfter(LocalDate.now())) {
            return error(HttpStatus.BAD_REQUEST, "Укажите корректную дату рождения");
        }

        String username = request.username() == null ? "" : request.username().trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return error(HttpStatus.BAD_REQUEST,
                    "Логин должен содержать 3–64 символа: латинские буквы, цифры, точку, дефис или подчёркивание");
        }
        if (isBlank(request.phone()) || request.phone().trim().length() > 32) {
            return error(HttpStatus.BAD_REQUEST, "Укажите корректный номер телефона");
        }
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            return error(HttpStatus.BAD_REQUEST, "Укажите корректный email");
        }
        if (request.password() == null || request.password().length() < 8) {
            return error(HttpStatus.BAD_REQUEST, "Пароль должен содержать не менее 8 символов");
        }

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)",
                Integer.class, username, email);
        if (existing != null && existing > 0) {
            return error(HttpStatus.CONFLICT, "Логин или email уже зарегистрированы");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO users " +
                "(surname, first_name, patronymic, birth_date, username, phone, email, password_hash) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.surname().trim());
            statement.setString(2, request.firstName().trim());
            statement.setString(3, isBlank(request.patronymic()) ? null : request.patronymic().trim());
            statement.setDate(4, Date.valueOf(request.birthDate()));
            statement.setString(5, username);
            statement.setString(6, request.phone().trim());
            statement.setString(7, email);
            statement.setString(8, passwordEncoder.encode(request.password()));
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(generatedId == null ? 0L : generatedId.longValue(), username));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.identifier()) || isBlank(request.password())) {
            return error(HttpStatus.BAD_REQUEST, "Введите логин/email и пароль");
        }
        String identifier = request.identifier().trim();
        List<StoredUser> matches = jdbcTemplate.query(
                "SELECT id, username, password_hash FROM users " +
                        "WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?) LIMIT 1",
                (resultSet, rowNum) -> new StoredUser(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash")),
                identifier, identifier);

        if (matches.isEmpty() || !passwordEncoder.matches(request.password(), matches.get(0).passwordHash())) {
            return error(HttpStatus.UNAUTHORIZED, "Неверный логин/email или пароль");
        }
        StoredUser user = matches.get(0);
        return ResponseEntity.ok(new AuthResponse(user.id(), user.username()));
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(message));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record RegisterRequest(
            String surname,
            String firstName,
            String patronymic,
            LocalDate birthDate,
            String username,
            String phone,
            String email,
            String password) { }

    public record LoginRequest(String identifier, String password) { }
    public record AuthResponse(long userId, String username) { }
    public record ApiError(String message) { }
    private record StoredUser(long id, String username, String passwordHash) { }
}
