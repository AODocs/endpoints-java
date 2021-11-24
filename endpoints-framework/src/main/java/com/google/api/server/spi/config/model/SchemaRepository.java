package com.google.api.server.spi.config.model;

import com.google.api.client.util.Maps;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Description;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.jsonwriter.JacksonResourceSchemaProvider;
import com.google.api.server.spi.config.jsonwriter.ResourceSchemaProvider;
import com.google.api.server.spi.config.model.Schema.Builder;
import com.google.api.server.spi.config.model.Schema.Field;
import com.google.api.server.spi.config.model.Schema.SchemaReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A repository which creates and caches the compiled schemas for an API.
 */
public class SchemaRepository {
  private static final Schema PLACEHOLDER_SCHEMA = Schema.builder()
      .setName("_placeholder_")
      .setType("_placeholder_")
      .build();

  @VisibleForTesting
  static final Schema ANY_SCHEMA = Schema.builder()
      .setName("_any")
      .setType("any")
      .build();

  @VisibleForTesting
  static final Schema MAP_SCHEMA = Schema.builder()
      .setName("JsonMap")
      .setType("object")
      .build();

  private static final EnumSet<FieldType> SUPPORTED_MAP_KEY_TYPES = EnumSet.of(
      FieldType.STRING,
      FieldType.ENUM,
      FieldType.BOOLEAN,
      FieldType.INT8, FieldType.INT16, FieldType.INT32, FieldType.INT64,
      FieldType.FLOAT, FieldType.DOUBLE,
      FieldType.DATE, FieldType.DATE_TIME
  );

  @VisibleForTesting
  static final String ARRAY_UNUSED_MSG = "unused for array items";
  @VisibleForTesting
  static final String MAP_UNUSED_MSG = "unused for map values";

  private final Multimap<ApiKey, Schema> schemaByApiKeys = LinkedHashMultimap.create();
  private final Map<ApiSerializationConfig, Map<TypeToken<?>, Schema>> types 
      = Maps.newLinkedHashMap();
  private final ResourceSchemaProvider resourceSchemaProvider;

  private final TypeLoader typeLoader;

  public SchemaRepository(TypeLoader typeLoader) {
    this.typeLoader = typeLoader;
    this.resourceSchemaProvider = new JacksonResourceSchemaProvider(typeLoader);
  }

  /**
   * Gets a schema for a type and API config.
   *
   * @return a {@link Schema} if one has been created, or null otherwise.
   */
  public Schema get(TypeToken<?> type, ApiConfig config) {
    Map<TypeToken<?>, Schema> typesForConfig = getAllTypesForConfig(config);
    type = ApiAnnotationIntrospector.getSchemaType(type, config);
    Schema schema = typesForConfig.get(type);
    if (schema != null) {
      if (schema == PLACEHOLDER_SCHEMA) {
        throw new IllegalStateException("schema repository is in a bad state!");
      }
      return schema;
    }
    return null;
  }

  /**
   * Gets a schema for a type and API config, creating it if it doesn't already exist.
   *
   * @return a {@link Schema} for the requested type and API config.
   */
  public Schema getOrAdd(TypeToken<?> type, ApiConfig config) {
    Map<TypeToken<?>, Schema> typesForConfig = getAllTypesForConfig(config);
    Schema schema = getOrCreateTypeForConfig(type, typesForConfig, config);
    if (schema == PLACEHOLDER_SCHEMA) {
      throw new IllegalStateException("schema repository is in a bad state!");
    }
    return schema;
  }

  /**
   * Gets all schema for an API key.
   */
  public ImmutableList<Schema> getAllSchemaForApi(ApiKey apiKey) {
    return ImmutableList.copyOf(schemaByApiKeys.get(apiKey.withoutRoot()));
  }

  /**
   * Gets all schema for an API config.
   *
   * @return a {@link Map} from {@link TypeToken} to {@link Schema}. If there are no schema for this
   * config, an empty map is returned.
   */
  private Map<TypeToken<?>, Schema> getAllTypesForConfig(ApiConfig config) {
    Map<TypeToken<?>, Schema> typesForConfig = types.get(config.getSerializationConfig());
    if (typesForConfig == null) {
      typesForConfig = Maps.newLinkedHashMap();
      types.put(config.getSerializationConfig(), typesForConfig);
    }
    return typesForConfig;
  }

  private Schema getOrCreateTypeForConfig(
      TypeToken<?> type, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    type = ApiAnnotationIntrospector.getSchemaType(type, config);
    Schema schema = typesForConfig.get(type);
    ApiKey key = config.getApiKey().withoutRoot();
    if (schema != null) {
      // If the schema is a placeholder, it's currently being constructed and will be added when
      // the type construction is complete.
      if (schema != PLACEHOLDER_SCHEMA) {
        addSchemaToApi(key, schema);
      }
      return schema;
    }
    // We put a placeholder in because this is a recursive process that may result in circular
    // references. This should never be returned in the public interface.
    typesForConfig.put(type, PLACEHOLDER_SCHEMA);
    if (typeLoader.isSchemaType(type)) {
      throw new IllegalArgumentException("Can't use a primitive type as a resource" + getExceptionSuffix(type, config));
    } else {
      if (Types.isArrayType(type)) {
        schema = createArraySchema(type, typesForConfig, config);
      } else if (Types.isObject(type)) {
        schema = ANY_SCHEMA;
      } else if (Types.isMapType(type)) {
        schema = MAP_SCHEMA;
        final TypeToken<Map<?, ?>> mapSupertype = ((TypeToken) type).getSupertype(Map.class);
        final boolean hasConcreteKeyValue = Types.isConcreteType(mapSupertype.getType());
        boolean forceJsonMapSchema = EndpointsFlag.MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA.isEnabled();
        if (hasConcreteKeyValue && !forceJsonMapSchema) {
          schema = createMapSchema(mapSupertype, typesForConfig, config).or(schema);
        }
      } else if (Types.isEnumType(type)) {
        schema = createEnumSchema(type, config);
      } else if (Types.isOptional(type)) {
        schema = ANY_SCHEMA;
        if (Types.isConcreteType(type.getType())) {
          TypeToken<?> optionalType = Types.getTypeParameter(type, 0);
          if (Types.isOptional(optionalType)) {
            throw new IllegalArgumentException("Recursive Optional is not supported" + getExceptionSuffix(type, config));
          }
          if (Types.isArrayType(optionalType) || Types.isMapType(optionalType) || Types.isObject(optionalType)) {
            throw new IllegalArgumentException("Optional of array-like type, Map or Object is not supported" + getExceptionSuffix(type, config));
          }
          schema = Types.isEnumType(optionalType) 
                  ? createEnumSchema(optionalType, config) 
                  : createBeanSchema(optionalType, typesForConfig, config);
        }
      } else {
        schema = createBeanSchema(type, typesForConfig, config);
      }
      typesForConfig.put(type, schema);
      schemaByApiKeys.put(key, schema);
      return schema;
    }
  }

  private String getExceptionSuffix(TypeToken<?> type, ApiConfig config) {
    return ": '" + type + "' used in " + config.getApiKey();
  }

  private void addSchemaToApi(ApiKey key, Schema schema) {
    if (schemaByApiKeys.containsEntry(key, schema)) {
      return;
    }
    schemaByApiKeys.put(key, schema);
    for (Field f : schema.fields().values()) {
      while (f.type() == FieldType.ARRAY) {
        f = f.arrayItemSchema();
      }
      if (f.type() == FieldType.OBJECT || f.type() == FieldType.ENUM) {
        addSchemaToApi(key, f.schemaReference().get());
      }
    }
    Field mapValueSchema = schema.mapValueSchema();
    if (mapValueSchema != null) {
      while (mapValueSchema.type() == FieldType.ARRAY) {
        mapValueSchema = mapValueSchema.arrayItemSchema();
      }
      if (mapValueSchema.schemaReference() != null) {
        addSchemaToApi(key, mapValueSchema.schemaReference().get());
      }
    }
  }
  
  private Schema createArraySchema(
          TypeToken<?> type, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    TypeToken<?> arrayItemType = Types.getArrayItemType(type);
    String simpleName = Types.getSimpleName(type, config.getSerializationConfig());
    Field.Builder arrayItemSchema = Field.builder().setName(ARRAY_UNUSED_MSG);
    fillInFieldInformation(arrayItemSchema, arrayItemType, typesForConfig, config);
    Field arrayField = arrayItemSchema.build();
    Builder builder = Schema.builder()
            .setName(simpleName)
            .setType("object")
            .addField("items", Field.builder()
                    .setName("items")
                    .setType(FieldType.ARRAY)
                    .setArrayItemSchema(arrayField)
                    .build());
    SchemaReference itemSchema = arrayField.schemaReference();
    if (itemSchema != null) {
      builder.setDescription("An ordered list of " + itemSchema.get().name());
    }
    return builder.build();
  }

  private Optional<Schema> createMapSchema(
      TypeToken<Map<?, ?>> mapType, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    FieldType keyFieldType = FieldType.fromType(Types.getTypeParameter(mapType, 0));
    boolean supportedKeyType = SUPPORTED_MAP_KEY_TYPES.contains(keyFieldType);
    if (!supportedKeyType) {
      String message = "Map field type '" + mapType + "' has a key type not serializable to String";
      if (EndpointsFlag.MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES.isEnabled()) {
        System.err.println(message + ", its schema will be JsonMap");
      } else {
        throw new IllegalArgumentException(message);
      }
    }
    TypeToken<?> valueTypeToken = Types.getTypeParameter(mapType, 1);
    FieldType valueFieldType = FieldType.fromType(valueTypeToken);
    boolean supportArrayValues = EndpointsFlag.MAP_SCHEMA_SUPPORT_ARRAYS_VALUES.isEnabled();
    boolean supportedValueType = supportArrayValues || valueFieldType != FieldType.ARRAY;
    if (!supportedValueType) {
      System.err.println("Map field type '" + mapType + "' "
          + "has an array-like value type, its schema will be JsonMap");
    }
    if (!supportedKeyType || !supportedValueType) {
      return Optional.absent();
    }
    TypeToken<?> valueSchemaType = ApiAnnotationIntrospector.getSchemaType(valueTypeToken, config);
    Schema.Builder builder = Schema.builder()
        .setName(Types.getSimpleName(mapType, config.getSerializationConfig()))
        .setType("object");
    Field.Builder fieldBuilder = Field.builder().setName(MAP_UNUSED_MSG);
    fillInFieldInformation(fieldBuilder, valueSchemaType, typesForConfig, config);
    Field mapValueField = fieldBuilder.build();
    SchemaReference valueSchema = mapValueField.schemaReference();
    if (valueSchema != null) {
      builder.setDescription(
          String.format("A collection of name / %s pairs", valueSchema.get().name()));
    }
    return Optional.of(builder.setMapValueSchema(mapValueField).build());
  }

  private Schema createBeanSchema(
      TypeToken<?> type, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    Schema.Builder builder = Schema.builder()
        .setName(Types.getSimpleName(type, config.getSerializationConfig()))
        .setType("object");
    setSchemaDescription(type, builder);
    ResourceSchema schema = resourceSchemaProvider.getResourceSchema(type, config);
    for (Entry<String, ResourcePropertySchema> entry : schema.getProperties().entrySet()) {
      String propertyName = entry.getKey();
      ResourcePropertySchema propertySchema = entry.getValue();
      TypeToken<?> propertyType = propertySchema.getType();
      if (propertyType != null) {
        String description = propertySchema.getDescription();
        Field.Builder fieldBuilder = Field.builder()
            .setName(propertyName)
            .setDescription(Strings.isNullOrEmpty(description) ? null : description)
            .setRequired(propertySchema.getRequired())
            .setConstraints(toFieldConstraints(propertySchema.getValidationConstraints()));
        fillInFieldInformation(fieldBuilder, propertyType, typesForConfig, config);
        builder.addField(propertyName, fieldBuilder.build());
      }
    }
    return builder.build();
  }
  
  private Schema.FieldConstraints toFieldConstraints(ApiValidationConstraints validationConstraints) {
    Schema.FieldConstraints constraints = null;
    if (validationConstraints != null && !validationConstraints.isEmpty()) {
       constraints = Schema.FieldConstraints.builder()
              .setPattern(validationConstraints.getPattern())
              .setMin(validationConstraints.getMin())
              .setMax(validationConstraints.getMax())
              .setDecimalMin(validationConstraints.getDecimalMin())
              .setDecimalMax(validationConstraints.getDecimalMax())
              .setDecimalMinInclusive(validationConstraints.getDecimalMinInclusive())
              .setDecimalMaxInclusive(validationConstraints.getDecimalMaxInclusive())
              .setMinSize(validationConstraints.getMinSize())
              .setMaxSize(validationConstraints.getMaxSize())
              .build();
      }
    return constraints;
  }
  
  private Schema createEnumSchema(TypeToken<?> type, ApiConfig config) {
    Map<String, String> valuesAndDescriptions
            = Types.getEnumValuesAndDescriptions((TypeToken<Enum<?>>) type);
    Builder builder = Schema.builder()
            .setName(Types.getSimpleName(type, config.getSerializationConfig()))
            .setType("string");
    setSchemaDescription(type, builder);
    for (Entry<String, String> entry : valuesAndDescriptions.entrySet()) {
      builder.addEnumValue(entry.getKey());
      builder.addEnumDescription(entry.getValue());
    }
    return builder.build();
  }
  
  private void fillInFieldInformation(Field.Builder builder, TypeToken<?> fieldType,
      Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    FieldType ft = FieldType.fromType(fieldType);
    builder.setType(ft);
    if (ft == FieldType.OBJECT || ft == FieldType.ENUM) {
      getOrCreateTypeForConfig(fieldType, typesForConfig, config);
      builder.setSchemaReference(SchemaReference.create(this, config, fieldType));
    } else if (ft == FieldType.ARRAY) {
      Field.Builder arrayItemBuilder = Field.builder().setName(ARRAY_UNUSED_MSG);
      fillInFieldInformation(
          arrayItemBuilder,
          ApiAnnotationIntrospector.getSchemaType(Types.getArrayItemType(fieldType), config),
          typesForConfig,
          config);
      builder.setArrayItemSchema(arrayItemBuilder.build());
    }
  }

  private void setSchemaDescription(TypeToken<?> type, Builder builder) {
    Description description = type.getRawType().getAnnotation(Description.class);
    if (description != null && !Strings.isNullOrEmpty(description.value())) {
      builder.setDescription(description.value());
    }
  }
  
  public static boolean isJsonMapSchema(Schema schema) {
    return schema == MAP_SCHEMA;
  }
  
}
