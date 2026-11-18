package org.a2aproject.sdk.jsonrpc.common.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/** Binds converted A2A SDK records through their canonical constructors. @author 芋道源码 */
final class Java8RecordConstructorFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.getRawType().getName().startsWith("org.a2aproject.sdk.spec.")) {
            return null;
        }
        for (Constructor<?> constructor : type.getRawType().getConstructors()) {
            if (constructor.isAnnotationPresent(JsonCreator.class)) {
                return adapt(gson, type, constructor);
            }
        }
        return null;
    }

    private <T> TypeAdapter<T> adapt(Gson gson, TypeToken<T> type, Constructor<?> constructor) {
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        Parameter[] parameters = constructor.getParameters();
        Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < parameters.length; index++) {
            JsonProperty property = parameters[index].getAnnotation(JsonProperty.class);
            if (property == null || property.value().isEmpty()) {
                return delegate;
            }
            SerializedName jsonName;
            try {
                jsonName = type.getRawType().getDeclaredField(property.value()).getAnnotation(SerializedName.class);
            }
            catch (NoSuchFieldException error) {
                return delegate;
            }
            if (jsonName == null) {
                indexes.put(property.value(), index);
            }
            else {
                indexes.put(jsonName.value(), index);
                for (String alternate : jsonName.alternate()) {
                    indexes.put(alternate, index);
                }
            }
        }
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter writer, T value) throws IOException {
                delegate.write(writer, value);
            }

            @Override
            @SuppressWarnings("unchecked")
            public T read(JsonReader reader) {
                JsonElement tree = JsonParser.parseReader(reader);
                if (tree.isJsonNull()) {
                    return null;
                }
                JsonObject object = tree.getAsJsonObject();
                Object[] values = new Object[parameters.length];
                for (int index = 0; index < parameters.length; index++) {
                    Class<?> parameterType = parameters[index].getType();
                    if (parameterType.isPrimitive()) {
                        values[index] = Array.get(Array.newInstance(parameterType, 1), 0);
                    }
                }
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    Integer index = indexes.get(entry.getKey());
                    if (index == null) {
                        continue;
                    }
                    Parameter parameter = parameters[index];
                    JsonElement element = entry.getValue();
                    if (!element.isJsonNull() || !parameter.getType().isPrimitive()) {
                        values[index] = gson.fromJson(element, parameter.getParameterizedType());
                    }
                }
                try {
                    return (T) constructor.newInstance(values);
                }
                catch (InvocationTargetException error) {
                    throw new JsonParseException("Invalid A2A " + type, error.getCause());
                }
                catch (ReflectiveOperationException error) {
                    throw new JsonParseException("Cannot create A2A " + type, error);
                }
            }
        };
    }
}
