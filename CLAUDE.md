# java-api

Spring Boot 3.2.5 REST API, Java 21, Maven. Lombok, Spring Data JPA, Bean Validation.
PostgreSQL by default (`prod` profile - despite the name, this is also the everyday local
dev target now; needs `docker-compose up db` and `sql/schema.sql` applied first, since
`ddl-auto: validate` won't create the schema for you). H2 in-memory is still available as
the `dev` profile for a quick throwaway run with no local Postgres - pass
`-Dspring-boot.run.profiles=dev`. Base package `com.example.ecommerce`.

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
