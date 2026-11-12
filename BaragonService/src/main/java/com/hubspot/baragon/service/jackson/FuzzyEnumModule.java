package com.hubspot.baragon.service.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * How the API reads the enumerated values of its entities: the text is matched against the names of the
 * values without regard to case, with the blanks in it left out and the hyphens and full stops in it read as
 * underscores; failing that, against what each value prints itself as. Text that names none of the values,
 * and anything that is not text at all, is an entity the API cannot read. An enumerated type that says how it
 * is read keeps on being read that way.
 */
public class FuzzyEnumModule extends Module {
  /** The annotations that say how a type is read, by the package they are declared in. */
  private static final String READS_ITSELF = "com.fasterxml.jackson.annotation";

  @Override
  public String getModuleName() {
    return "FuzzyEnumModule";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addDeserializers(new PermissiveEnumDeserializers());
  }

  /** Which enumerated types are read this way. */
  static class PermissiveEnumDeserializers extends Deserializers.Base {

    @Override
    @Nullable
    public JsonDeserializer<?> findEnumDeserializer(
      Class<?> type,
      DeserializationConfig config,
      BeanDescription description
    ) {
      if (
        config.hasDeserializationFeatures(
          DeserializationFeature.READ_ENUMS_USING_TO_STRING.getMask()
        )
      ) {
        return null;
      }
      final List<AnnotatedMethod> factories = description.getFactoryMethods();
      if (factories != null) {
        for (AnnotatedMethod factory : factories) {
          if (factory.hasAnnotation(JsonCreator.class)) {
            return null;
          }
        }
      }
      for (Field field : type.getFields()) {
        for (Annotation annotation : field.getAnnotations()) {
          if (READS_ITSELF.equals(annotation.annotationType().getPackage().getName())) {
            return null;
          }
        }
      }
      return new PermissiveEnumDeserializer(type);
    }
  }

  /** Reads one enumerated value of an entity. */
  static class PermissiveEnumDeserializer extends StdScalarDeserializer<Enum<?>> {
    private static final long serialVersionUID = 1L;

    private final Enum<?>[] constants;
    private final List<String> acceptedValues;

    @SuppressWarnings("unchecked")
    PermissiveEnumDeserializer(Class<?> type) {
      super(type);
      this.constants = (Enum<?>[]) type.getEnumConstants();
      this.acceptedValues = new ArrayList<>(constants.length);
      for (Enum<?> constant : constants) {
        acceptedValues.add(constant.name());
      }
    }

    @Override
    public Enum<?> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
      final String text = parser.getText();
      final Enum<?> value = fromStringFuzzy(text, constants);
      if (value != null) {
        return value;
      }
      throw context.weirdStringException(
        text,
        handledType(),
        text + " was not one of " + acceptedValues
      );
    }
  }

  /** The value of an enumerated type the text names, or nothing when it names none of them. */
  @Nullable
  static Enum<?> fromStringFuzzy(String text, Enum<?>[] constants) {
    final String sanitized = withoutBlanks(text).replace('-', '_').replace('.', '_');
    for (Enum<?> constant : constants) {
      if (constant.name().equalsIgnoreCase(sanitized)) {
        return constant;
      }
    }
    for (Enum<?> constant : constants) {
      if (constant.toString().equalsIgnoreCase(text)) {
        return constant;
      }
    }
    return null;
  }

  private static String withoutBlanks(String text) {
    final StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      if (!Character.isWhitespace(text.charAt(i))) {
        out.append(text.charAt(i));
      }
    }
    return out.toString();
  }

  /** The values of an enumerated type, for a caller that only needs to read one. */
  public static List<String> namesOf(Enum<?>[] constants) {
    final List<String> names = new ArrayList<>(constants.length);
    for (Enum<?> constant : Arrays.asList(constants)) {
      names.add(constant.name());
    }
    return names;
  }
}
