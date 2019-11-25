/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
package com.google.api.server.spi;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.config.model.EndpointsFlag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

import java.util.Objects;
import java.util.Optional;

/**
 * Tests for {@link ObjectMapperUtil}
 */
public class ObjectMapperUtilTest {
  
  @Test
  public void createStandardObjectMapper_base64Variant() throws Exception {
    byte[] bytes = new byte[] {(byte) 0xff, (byte) 0xef};
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    assertThat(mapper.writeValueAsString(bytes)).isEqualTo("\"_-8\"");
    assertThat(mapper.readValue("\"_-8\"", byte[].class)).isEqualTo(bytes);
  }

  @Test
  public void createStandardObjectMapper_useJacksonAnnotations() throws Exception {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    assertThat(mapper.writeValueAsString(new TestObject())).contains("test");
  }

  @Test
  public void createStandardObjectMapper_disableJacksonAnnotations() throws Exception {
    System.setProperty(EndpointsFlag.JSON_USE_JACKSON_ANNOTATIONS.systemPropertyName, "false");
    try {
      ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
      assertThat(mapper.writeValueAsString(new TestObject())).contains("TEST");
    } finally {
      System.clearProperty(EndpointsFlag.JSON_USE_JACKSON_ANNOTATIONS.systemPropertyName);
    }
  }

  @Test
  public void createStandardObjectMapper_optional() throws Exception {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    assertThat(mapper.writeValueAsString(new TestJava8Types(Optional.of("value")))).isEqualTo("{\"optional\":\"value\"}");
    assertThat(mapper.writeValueAsString(new TestJava8Types(Optional.of("")))).isEqualTo("{\"optional\":\"\"}");
    assertThat(mapper.writeValueAsString(new TestJava8Types(Optional.empty()))).isEqualTo("{\"optional\":null}");
    assertThat(mapper.writeValueAsString(new TestJava8Types(null))).isEqualTo("{\"optional\":null}");

    ObjectReader reader = mapper.readerFor(TestJava8Types.class);
    assertThat(reader.<TestJava8Types>readValue("{\"optional\":\"value\"}").getOptional()).isEqualTo(Optional.of("value"));
    assertThat(reader.<TestJava8Types>readValue("{\"optional\":\"\"}").getOptional()).isEqualTo(Optional.of(""));
    assertThat(reader.<TestJava8Types>readValue("{\"optional\":null}").getOptional()).isEqualTo(Optional.empty());
    assertThat(reader.<TestJava8Types>readValue("{}").getOptional()).isEqualTo(null);
  }

  @Test
  public void createStandardObjectMapper_parameterNames() throws Exception {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    TestParameterNames result = mapper.readerFor(TestParameterNames.class)
        .readValue("{\"name\":\"Jerry\",\"surname\":\"Smith\"}");
    assertThat(result).isEqualTo(new TestParameterNames("Jerry", "Smith"));
  }


  private enum TestEnum {
    @JsonProperty("test") TEST
  }

  private class TestObject {
    TestEnum test;

    TestObject() {
      this.test = TestEnum.TEST;
    }

    public void setTest(TestEnum test) {
      this.test = test;
    }

    public TestEnum getTest() {
      return test;
    }
  }
  
  private static class TestJava8Types {
    Optional<String> optional;

    public TestJava8Types() {
    }

    public TestJava8Types(Optional<String> optional) {
      this.optional = optional;
    }

    public Optional<String> getOptional() {
      return optional;
    }

    public void setOptional(Optional<String> optional) {
      this.optional = optional;
    }
  }

  private static class TestParameterNames {

    private final String name;
    private final String surname;

    private String nickname;

    //can't be deserialized if not build with -parameters compiler option
    public TestParameterNames(String name, String surname) {
      this.name = name;
      this.surname = surname;
    }

    public String getName() {
      return name;
    }

    public String getSurname() {
      return surname;
    }

    public String getNickname() {
      return nickname;
    }

    public void setNickname(String nickname) {
      this.nickname = nickname;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestParameterNames that = (TestParameterNames) o;
      return Objects.equals(name, that.name) &&
          Objects.equals(surname, that.surname) &&
          Objects.equals(nickname, that.nickname);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, surname, nickname);
    }

    @Override
    public String toString() {
      return "TestParameterNames{" +
          "name='" + name + '\'' +
          ", surname='" + surname + '\'' +
          ", nickname='" + nickname + '\'' +
          '}';
    }
  }
  
}