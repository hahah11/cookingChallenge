package at.fraihs.cookoff.shared.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * JsonNullable has no fixed wrapped type at the class level, but at serialize time we always
 * have a concrete instance - delegate to whatever serializer the wrapped value's own runtime
 * type needs, same as Jackson does for a plain {@code Object}-typed field.
 */
public class JsonNullableSerializer extends ValueSerializer<JsonNullable<?>> {

    @Override
    public void serialize(JsonNullable<?> value, JsonGenerator gen, SerializationContext ctxt) {
        Object inner = value.isPresent() ? value.get() : null;
        if (inner == null) {
            gen.writeNull();
        } else {
            ctxt.writeValue(gen, inner);
        }
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, JsonNullable<?> value) {
        return value == null || !value.isPresent();
    }
}
