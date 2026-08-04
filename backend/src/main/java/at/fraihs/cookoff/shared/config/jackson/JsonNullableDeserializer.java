package at.fraihs.cookoff.shared.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

/**
 * Unlike serialization, deserializing a bare JSON value into {@code JsonNullable<T>} needs to
 * know T - resolved once per property from the declaring field's generic parameter
 * (e.g. {@code JsonNullable<String>} -> {@code String}), then cached on a per-property
 * instance via {@link #createContextual}. {@link #deserialize} is never invoked for an
 * explicit JSON {@code null} (Jackson routes that to {@link #getNullValue} instead); an
 * absent field is never routed here either, it just leaves the generated model's own
 * {@code JsonNullable.undefined()} field initializer in place.
 */
public class JsonNullableDeserializer extends ValueDeserializer<JsonNullable<?>> {

    private final ValueDeserializer<?> delegate;

    public JsonNullableDeserializer() {
        this(null);
    }

    private JsonNullableDeserializer(ValueDeserializer<?> delegate) {
        this.delegate = delegate;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }
        JavaType wrapperType = property.getType();
        JavaType wrappedType = wrapperType.containedTypeCount() > 0
                ? wrapperType.containedType(0)
                : ctxt.constructType(Object.class);
        return new JsonNullableDeserializer(ctxt.findContextualValueDeserializer(wrappedType, property));
    }

    @Override
    public JsonNullable<?> deserialize(JsonParser p, DeserializationContext ctxt) {
        Object value = delegate != null ? delegate.deserialize(p, ctxt) : ctxt.readValue(p, Object.class);
        return JsonNullable.of(value);
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) {
        return JsonNullable.of(null);
    }
}
