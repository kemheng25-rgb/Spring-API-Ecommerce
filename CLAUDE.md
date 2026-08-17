# java-api

Spring Boot 3.2.5 REST API, Java 21, Maven. Lombok, Spring Data JPA, Bean Validation.
H2 in dev, PostgreSQL in prod. Base package `com.example.ecommerce`.

## Commands

```bash
mvn spring-boot:run
```

```bash
mvn test
```

```bash
mvn -q -DskipTests package
```

Docker: `docker-compose up --build`.

## Layout

```
controller/   @RestController — HTTP only, no business logic
service/      business logic, @Transactional lives here
repository/   Spring Data interfaces, no @Query unless derived names can't express it
dto/          request/response records — never expose entities from controllers
model/        JPA entities
exception/    custom exceptions + @RestControllerAdvice handler
config/       beans, OpenAPI, CORS
```

## Rules

- Constructor injection only (`@RequiredArgsConstructor`), never `@Autowired` on fields.
- Controllers return DTOs. Entities never cross the controller boundary.
- Validate with `@Valid` + jakarta.validation annotations on DTOs.
- Throw domain exceptions; let the `@RestControllerAdvice` map them to status codes.
- Tests mirror the package under `src/test/java`: `@WebMvcTest` for controllers,
  `@DataJpaTest` for repositories, plain Mockito for services.
- Don't hand-edit anything in `target/`.
