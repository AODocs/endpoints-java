/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.server.spi.config;

import com.google.api.server.spi.config.model.ApiValidationConstraints;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * A schema definition for an individual property on a resource.
 *
 * @see ResourceSchema
 */
public class ResourcePropertySchema {
  private final TypeToken<?> type;
  private String description;
  private Boolean required;
  private ApiValidationConstraints validationConstraints;

  private ResourcePropertySchema(TypeToken<?> type) {
    this.type = type;
  }

  /**
   * Gets the type of the property. This is used to determine how it should be serialized, as well
   * as what it's type and format should be in the schema.
   *
   * @return the property's type
   */
  public Type getJavaType() {
    return type.getType();
  }

  public TypeToken<?> getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getRequired() {
    return required;
  }

  public ResourcePropertySchema setRequired(Boolean required) {
    this.required = required;
    return this;
  }

  public ApiValidationConstraints getValidationConstraints() {
    return validationConstraints;
  }

  public void setValidationConstraints(ApiValidationConstraints validationConstraints) {
    this.validationConstraints = validationConstraints;
  }

  /**
   * Returns a default resource property schema for a given type.
   *
   * @param type the property type
   * @return a default schema for this type
   */
  public static ResourcePropertySchema of(TypeToken<?> type) {
    return new ResourcePropertySchema(Preconditions.checkNotNull(type));
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourcePropertySchema that = (ResourcePropertySchema) o;
    return type.equals(that.type) && Objects.equals(description, that.description) && Objects.equals(required, that.required) && Objects.equals(validationConstraints, that.validationConstraints);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(type, description, required, validationConstraints);
  }
  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("type", type)
            .add("description", description)
            .add("required", required)
            .add("validationConstraints", validationConstraints)
            .toString();
  }
}
