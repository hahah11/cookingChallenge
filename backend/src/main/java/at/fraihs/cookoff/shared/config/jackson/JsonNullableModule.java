package at.fraihs.cookoff.shared.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.module.SimpleModule;

/**
 * {@code org.openapitools:jackson-databind-nullable} only ships a Jackson 2
 * ({@code com.fasterxml.jackson.databind}) module; every generated request/response field
 * the spec marks {@code nullable: true} on an optional property comes back as
 * {@code org.openapitools.jackson.nullable.JsonNullable}, which the project's Jackson 3
 * ({@code tools.jackson.databind}) {@code ObjectMapper} has no built-in (de)serializer for.
 * Registered as a {@link tools.jackson.databind.JacksonModule} bean so Spring Boot 4's
 * Jackson autoconfiguration picks it up automatically - see
 * {@code at.fraihs.cookoff.shared.config.JacksonConfig}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JsonNullableModule extends SimpleModule {

    public JsonNullableModule() {
        super("JsonNullableModule");
        Class jsonNullableClass = JsonNullable.class;
        addSerializer(jsonNullableClass, new JsonNullableSerializer());
        addDeserializer(jsonNullableClass, new JsonNullableDeserializer());
    }
}
