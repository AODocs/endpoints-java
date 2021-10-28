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
package com.google.api.server.spi;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Tests for {@link ServletInitializationParameters}.
 */
@RunWith(JUnit4.class)
public class ServletInitializationParametersTest {

  @Test
  public void testBuilder_defaults() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .build();
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertThat(initParameters.isClientIdWhitelistEnabled()).isTrue();
    assertThat(initParameters.isIllegalArgumentBackendError()).isFalse();
    assertThat(initParameters.isExceptionCompatibilityEnabled()).isTrue();
    assertThat(initParameters.isPrettyPrintEnabled()).isTrue();
    assertThat(initParameters.isAddContentLength()).isFalse();
    assertThat(initParameters.getApiExplorerUrlTemplate()).isNull();
    assertThat(initParameters.isParameterValidationEnabled()).isTrue();
    verifyAsMap(initParameters, "", "true", "false", "true", "true", "false", null, "true");
  }

  @Test
  public void testBuilder_emptySetsAndTrue() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .setClientIdWhitelistEnabled(true)
        .addServiceClasses(ImmutableSet.<Class<?>>of())
        .setIllegalArgumentBackendError(true)
        .setExceptionCompatibilityEnabled(true)
        .setPrettyPrintEnabled(true)
        .setAddContentLength(true)
        .setApiExplorerUrlTemplate("apiExplorer")
        .setParameterValidationEnabled(true)
        .build();
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertThat(initParameters.isClientIdWhitelistEnabled()).isTrue();
    assertThat(initParameters.isIllegalArgumentBackendError()).isTrue();
    assertThat(initParameters.isExceptionCompatibilityEnabled()).isTrue();
    assertThat(initParameters.getApiExplorerUrlTemplate()).isEqualTo("apiExplorer");
    assertThat(initParameters.isParameterValidationEnabled()).isTrue();
    verifyAsMap(initParameters, "", "true", "true", "true", "true", "true", "apiExplorer", "true");
  }

  @Test
  public void testBuilder_oneEntrySetsAndFalse() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .addServiceClass(String.class)
        .setClientIdWhitelistEnabled(false)
        .setIllegalArgumentBackendError(false)
        .setExceptionCompatibilityEnabled(false)
        .setPrettyPrintEnabled(false)
        .setAddContentLength(false)
        .setParameterValidationEnabled(false)
        .build();
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class);
    assertThat(initParameters.isClientIdWhitelistEnabled()).isFalse();
    verifyAsMap(
        initParameters, String.class.getName(), "false", "false", "false", "false","false", null, "false");
  }

  @Test
  public void testBuilder_twoEntrySets() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .addServiceClasses(ImmutableSet.of(String.class, Integer.class))
        .build();
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class, Integer.class);
    verifyAsMap(initParameters, String.class.getName() + ',' + Integer.class.getName(), 
        "true", "false", "true", "true", "false", null, "true");
  }

  @Test
  public void testFromServletConfig_nullConfig() throws ServletException {
    ServletInitializationParameters initParameters =
        ServletInitializationParameters.fromServletConfig(null, getClass().getClassLoader());
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertThat(initParameters.isClientIdWhitelistEnabled()).isTrue();
  }

  @Test
  public void testFromServletConfig_nullValues() throws ServletException {
    ServletInitializationParameters initParameters =
        fromServletConfig(null, null, null, null, null, null, null, null);
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertThat(initParameters.isClientIdWhitelistEnabled()).isTrue();
    assertThat(initParameters.isIllegalArgumentBackendError()).isFalse();
    assertThat(initParameters.isExceptionCompatibilityEnabled()).isTrue();
    assertThat(initParameters.isPrettyPrintEnabled()).isTrue();
    assertThat(initParameters.isParameterValidationEnabled()).isTrue();
    assertThat(initParameters.getApiExplorerUrlTemplate()).isNull();
  }

  @Test
  public void testFromServletConfig_emptySetsAndFalse() throws ServletException {
    ServletInitializationParameters initParameters =
        fromServletConfig("", "false", "false", "false", "false", "false", null, "false");
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertThat(initParameters.isClientIdWhitelistEnabled()).isFalse();
    assertThat(initParameters.isIllegalArgumentBackendError()).isFalse();
    assertThat(initParameters.isExceptionCompatibilityEnabled()).isFalse();
    assertThat(initParameters.isPrettyPrintEnabled()).isFalse();
    assertThat(initParameters.isParameterValidationEnabled()).isFalse();
    assertThat(initParameters.getApiExplorerUrlTemplate()).isNull();
  }

  @Test
  public void testFromServletConfig_oneEntrySetsAndTrue() throws ServletException {
    ServletInitializationParameters initParameters =
        fromServletConfig(String.class.getName(), "true", "true", "true", "true", "true", null, "true");
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class);
    assertThat(initParameters.isClientIdWhitelistEnabled()).isTrue();
    assertThat(initParameters.isIllegalArgumentBackendError()).isTrue();
    assertThat(initParameters.isExceptionCompatibilityEnabled()).isTrue();
    assertThat(initParameters.isPrettyPrintEnabled()).isTrue();
    assertThat(initParameters.isParameterValidationEnabled()).isTrue();
    assertThat(initParameters.getApiExplorerUrlTemplate()).isNull();
  }

  @Test
  public void testFromServletConfig_twoEntrySets() throws ServletException {
    ServletInitializationParameters initParameters = fromServletConfig(
        String.class.getName() + ',' + Integer.class.getName(), null, null, null, null, null, null, null);
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class, Integer.class);
  }

  @Test
  public void testFromServletConfig_skipsEmptyElements() throws ServletException {
    ServletInitializationParameters initParameters = fromServletConfig(
        ",," + String.class.getName() + ",,," + Integer.class.getName() + ",", null, null,
        null, null, null, null, null);
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class, Integer.class);
  }

  @Test
  public void testFromServletConfig_invalidBooleanThrows() throws ServletException {
    try {
      fromServletConfig(null, "yes", null, null, null, null, null, null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  private void verifyAsMap(
      ServletInitializationParameters initParameters, String serviceClasses,
      String isClientIdWhitelistEnabled,
      String isIllegalArgumentBackendError, String isExceptionCompatibilityEnabled,
      String isPrettyPrintEnabled, String isAddContentLength, String apiExplorerUrlTemplate,
      String isParameterValidationEnabled) {
    Map<String, String> map = initParameters.asMap();
    assertThat(map).hasSize(8);
    assertThat(map.get("services")).isEqualTo(serviceClasses);
    assertThat(map.get("clientIdWhitelistEnabled")).isEqualTo(isClientIdWhitelistEnabled);
    assertThat(map.get("illegalArgumentIsBackendError")).isEqualTo(isIllegalArgumentBackendError);
    assertThat(map.get("enableExceptionCompatibility")).isEqualTo(isExceptionCompatibilityEnabled);
    assertThat(map.get("prettyPrint")).isEqualTo(isPrettyPrintEnabled);
    assertThat(map.get("addContentLength")).isEqualTo(isAddContentLength);
    assertThat(map.get("enableParameterValidation")).isEqualTo(isParameterValidationEnabled);
    assertThat(map.get("apiExplorerUrlTemplate")).isEqualTo(apiExplorerUrlTemplate);
  }

  private ServletInitializationParameters fromServletConfig(
      String serviceClasses,
      String isClientIdWhitelistEnabled, String isIllegalArgumentBackendError,
      String isExceptionCompatibilityEnabled, String isPrettyPrintEnabled,
      String isAddContentLength, String apiExplorerUrlTemplate,
      String isParameterValidationEnabled)
      throws ServletException {
    ServletConfig servletConfig = new StubServletConfig(serviceClasses,
        isClientIdWhitelistEnabled, isIllegalArgumentBackendError,
        isExceptionCompatibilityEnabled, isPrettyPrintEnabled, isAddContentLength, 
        apiExplorerUrlTemplate, isParameterValidationEnabled);
    return ServletInitializationParameters.fromServletConfig(
            servletConfig, getClass().getClassLoader());
  }

  private static class StubServletConfig implements ServletConfig {
    private final Map<String, String> initParameters;

    public StubServletConfig(
        String serviceClasses, String isClientIdWhitelistEnabled,
        String isIllegalArgumentBackendError, String isExceptionCompatibilityEnabled,
        String isPrettyPrintEnabled, String isAddContentLength, String apiExplorerUrlTemplate,
        String isParameterValidationEnabled) {
      initParameters = Maps.newHashMap();
      initParameters.put("services", serviceClasses);
      initParameters.put("clientIdWhitelistEnabled", isClientIdWhitelistEnabled);
      initParameters.put("illegalArgumentIsBackendError", isIllegalArgumentBackendError);
      initParameters.put("enableExceptionCompatibility", isExceptionCompatibilityEnabled);
      initParameters.put("prettyPrint", isPrettyPrintEnabled);
      initParameters.put("addContentLength", isAddContentLength);
      initParameters.put("apiExplorerUrlTemplate", apiExplorerUrlTemplate);
      initParameters.put("enableParameterValidation", isParameterValidationEnabled);
    }

    @Override
    public String getServletName() {
      return null;
    }

    @Override
    public ServletContext getServletContext() {
      return null;
    }

    @Override
    public String getInitParameter(String name) {
      return initParameters.get(name);
    }

    @Override
    public Enumeration<?> getInitParameterNames() {
      return null;
    }
  }
}
