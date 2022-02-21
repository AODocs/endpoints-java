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
package com.google.api.server.spi.dispatcher;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.api.server.spi.dispatcher.PathTrie.Result;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

/**
 * Tests for {@link PathTrie}.
 */
@RunWith(JUnit4.class)
public class PathTrieTest {

  @Test
  public void simple() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "explorer", 1234)
        .build();

    assertSuccessfulGetResolution(trie, "explorer", 1234);
    assertFailedGetResolution(trie, HttpMethod.PUT, "explorer");
    assertFailedGetResolution(trie, "");
    assertFailedGetResolution(trie, "test");
  }

  @Test
  public void sharedPrefix() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/v1/rest", 1234)
        .add(HttpMethod.GET, "discovery/v2/rest", 4321)
        .build();

    assertSuccessfulGetResolution(trie, "discovery/v1/rest", 1234);
    assertSuccessfulGetResolution(trie, "discovery/v2/rest", 4321);
    assertFailedGetResolution(trie, "");
    assertFailedGetResolution(trie, "discovery");
    assertFailedGetResolution(trie, "discovery/v1");
  }

  @Test
  public void prefix() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery", 1234)
        .add(HttpMethod.GET, "discovery/v1", 4321)
        .build();

    assertSuccessfulGetResolution(trie, "discovery", 1234);
    assertSuccessfulGetResolution(trie, "discovery/v1", 4321);
    assertFailedGetResolution(trie, "");
  }

  @Test
  public void customMethod() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/rest:batchGet", 1234)
        .build();

    assertSuccessfulGetResolution(trie, "discovery/rest:batchGet", 1234);
    assertFailedGetResolution(trie, "discovery/rest");
  }

  @Test
  public void customMethodWithEquivalentStandardPath() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/rest:batchGet", 1234)
        .add(HttpMethod.GET, "discovery/rest/batchGet", 4321)
        .build();

    assertSuccessfulGetResolution(trie, "discovery/rest:batchGet", 1234);
    assertSuccessfulGetResolution(trie, "discovery/rest/batchGet", 4321);
  }

  @Test
  public void paramInCustomMethod() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/prefix:{version}", 1234)
        .build();

    assertSuccessfulGetResolution(trie, "discovery/prefix:v1", 1234,
        ImmutableMap.of("version", "v1"));
  }

  @Test
  public void paramsInLastSegmentAndCustomMethod() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{major}:{minor}", 1234)
        .build();

    assertSuccessfulGetResolution(trie, "discovery/1:2", 1234,
        ImmutableMap.of("major", "1", "minor", "2"));
  }

  @Test
  public void lastParameterHasUnescapedColon() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}", 1234)
        .add(HttpMethod.GET, "discovery/{version}/suffix", 1234)
        .add(HttpMethod.PUT, "discovery/{version}", 4321)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1:batchGet", 1234, ImmutableMap.of("version", "v1:batchGet"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1:first:second", 1234, ImmutableMap.of("version", "v1:first:second"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1:batchGet/suffix", 1234, ImmutableMap.of("version", "v1:batchGet"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1:first:second/suffix", 1234, ImmutableMap.of("version", "v1:first:second"));
    assertSuccessfulResolution(
        trie, HttpMethod.PUT, "discovery/v1:batchGet", 4321,
        ImmutableMap.of("version", "v1:batchGet"));
  }

  @Test
  public void lastParameterHasUnescapedColon_mixed() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}", 1234)
        .add(HttpMethod.GET, "discovery/{version}:batchGet", 4321)
        .add(HttpMethod.GET, "discovery/rest", 1)
        .add(HttpMethod.GET, "discovery/rest:batchGet", 2)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1:batchGet", 4321, ImmutableMap.of("version", "v1"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1:notMethod", 1234, ImmutableMap.of("version", "v1:notMethod"));
    assertSuccessfulGetResolution(trie, "discovery/rest:batchGet", 2);
    assertSuccessfulGetResolution(trie, "discovery/rest", 1);
    assertSuccessfulGetResolution(
        trie, "discovery/rest:notMethod", 1234, ImmutableMap.of("version", "rest:notMethod"));

  }

  @Test
  public void emptyCustomMethod() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest:", 1234)
        .build();

    assertFailedGetResolution(trie, "discovery/v1/rest");
    //this could be forbidden, but no harm in letting it work
    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest:", 1234, ImmutableMap.of("version", "v1"));
  }

  @Test
  public void twoCustomMethod() {
    assertThrows(IllegalArgumentException.class,
            () -> PathTrie.<Integer>builder()
                    .add(HttpMethod.GET, "discovery/{version}/rest:batchGet:batchGet", 1234)
                    .build()
    );
  }

  @Test
  public void intermediateCustomMethod() {
    assertThrows(IllegalArgumentException.class,
        () -> PathTrie.<Integer>builder()
            .add(HttpMethod.GET, "discovery/{version}:batchGet/rest", 1234)
            .build()
    );
  }

  @Test
  public void customMethodWithNumericPrefix() {
    //there's no specific restriction on the syntax of custom methods in Google's API guide
    // (https://cloud.google.com/apis/design/custom_methods) so we should accept this
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest:12invalidMethod", 1234)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest:12invalidMethod", 1234, ImmutableMap.of("version", "v1"));
  }

  @Test
  public void colonParameterAndCustomMethodInSamePath() {
    //This is a known and documented limitation (see Javadoc on PathTrie)
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest:batchGet", 1234)
        .build();

    assertFailedGetResolution(trie, "discovery/v1:suffix/rest:batchGet");
  }

  @Test
  public void customMethodParameter() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest:batchGet", 1234)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest:batchGet", 1234, ImmutableMap.of("version", "v1"));
  }

  @Test
  public void customMethodMultipleParameters() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{discovery_version}/apis/{api}/{format}:batchGet", 1234)
        .build();

    assertSuccessfulGetResolution(trie, "discovery/v1/apis/test/rest:batchGet", 1234,
        ImmutableMap.of("discovery_version", "v1", "api", "test", "format", "rest"));
    assertFailedGetResolution(trie, "discovery/v1/apis/test/rest");
  }

  @Test
  public void customMethodSharedParameters() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest:batchGet", 1234)
        .add(HttpMethod.GET, "discovery/{version}/rpc", 4321)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest:batchGet", 1234, ImmutableMap.of("version", "v1"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1/rpc", 4321, ImmutableMap.of("version", "v1"));
  }

  @Test
  public void parametersOnMixedMethods() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest:batchGet", 1234)
        .add(HttpMethod.GET, "discovery/{version}/rest", 4321)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest:batchGet", 1234, ImmutableMap.of("version", "v1"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest", 4321, ImmutableMap.of("version", "v1"));
    assertFailedGetResolution(trie, "discovery/v1/rest:unknownMethod");
  }

  @Test
  public void customMethodSamePathDifferentMethod() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest", 1324)
        .add(HttpMethod.PUT, "discovery/{version}/rest", 4123)
        .add(HttpMethod.GET, "discovery/{version}/rest:batchGet", 1234)
        .add(HttpMethod.PUT, "discovery/{version}/rest:batchGet", 2134)
        .add(HttpMethod.GET, "discovery/{version}/rest:customList", 4321)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest", 1324, ImmutableMap.of("version", "v1"));
    assertSuccessfulResolution(
        trie, HttpMethod.PUT, "discovery/v1/rest", 4123, ImmutableMap.of("version", "v1"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest:batchGet", 1234, ImmutableMap.of("version", "v1"));
    assertSuccessfulResolution(
        trie, HttpMethod.PUT, "discovery/v1/rest:batchGet", 2134, ImmutableMap.of("version", "v1"));
    assertSuccessfulGetResolution(
        trie, "discovery/v2/rest:customList", 4321, ImmutableMap.of("version", "v2"));
  }

  @Test
  public void parameter() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest", 1234)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest", 1234, ImmutableMap.of("version", "v1"));
  }

  @Test
  public void multipleParameters() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{discovery_version}/apis/{api}/{format}", 1234)
        .build();

    assertSuccessfulGetResolution(trie, "discovery/v1/apis/test/rest", 1234,
        ImmutableMap.of("discovery_version", "v1", "api", "test", "format", "rest"));
  }

  @Test
  public void sharedParameterPrefix() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "discovery/{version}/rest", 1234)
        .add(HttpMethod.GET, "discovery/{version}/rpc", 4321)
        .build();

    assertSuccessfulGetResolution(
        trie, "discovery/v1/rest", 1234, ImmutableMap.of("version", "v1"));
    assertSuccessfulGetResolution(
        trie, "discovery/v1/rpc", 4321, ImmutableMap.of("version", "v1"));
  }

  @Test
  public void encodedParameter() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "{value}", 1234)
        .build();

    assertSuccessfulGetResolution(
        trie, "%E4%B8%AD%E6%96%87", 1234, ImmutableMap.of("value", "中文"));
  }

  @Test
  public void testResolveParameterAfterLiteral() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "{one}/three", 1234)
        .add(HttpMethod.GET, "one/two", 4321)
        .build();

    assertSuccessfulGetResolution(trie, "one/three", 1234, ImmutableMap.of("one", "one"));
    assertSuccessfulGetResolution(trie, "one/two", 4321);
  }

  @Test
  public void testResolveBacktrack() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "{one}/{two}/three/{four}", 1234)
        .add(HttpMethod.GET, "one/two/{three}/four", 4321)
        .build();

    assertSuccessfulGetResolution(trie, "one/two/three/five", 1234,
        ImmutableMap.of("one", "one", "two", "two", "four", "five"));
    assertSuccessfulGetResolution(
        trie, "one/two/three/four", 4321, ImmutableMap.of("three", "three"));
  }

  @Test
  public void pathMethodsWithDifferentParameterNames() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder()
        .add(HttpMethod.GET, "test/{one}", 1234)
        .add(HttpMethod.PUT, "test/{two}", 4321)
        .build();

    assertSuccessfulResolution(
        trie, HttpMethod.GET, "test/foo", 1234, ImmutableMap.of("one", "foo"));
    assertSuccessfulResolution(
        trie, HttpMethod.PUT, "test/foo", 4321, ImmutableMap.of("two", "foo"));
  }

  @Test
  public void duplicatePath() {
    doStrictDuplicateTest("test/path", "test/path");
  }

  @Test
  public void duplicateParameterizedPath() {
    doStrictDuplicateTest("test/{param}/path", "test/{parameterized}/path");
  }

  @Test
  public void laxDuplicatePath() {
    PathTrie<Integer> trie = PathTrie.<Integer>builder(false)
        .add(HttpMethod.GET, "test/{one}", 1234)
        .add(HttpMethod.GET, "test/{two}", 4321)
        .build();

    Result<Integer> result = trie.resolve(HttpMethod.GET, "test/foo");
    // We don't care which result is returned as long as it is a valid one.
    if (result.getRawParameters().containsKey("one")) {
      assertThat(result.getResult()).isEqualTo(1234);
      assertThat(result.getRawParameters().get("one")).isEqualTo("foo");
    } else {
      assertThat(result.getResult()).isEqualTo(4321);
      assertThat(result.getRawParameters().get("two")).isEqualTo("foo");
    }
  }

  @Test
  public void builderNullPath() {
    try {
      PathTrie.<Integer>builder().add(HttpMethod.GET, null, 1234);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void builderNullValue() {
    try {
      PathTrie.<Integer>builder().add(HttpMethod.GET, "throws/an/exception", null);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void resolveNullPath() {
    try {
      PathTrie<Integer> trie = PathTrie.<Integer>builder().build();
      trie.resolve(HttpMethod.GET, null);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void invalidParameterName() {
    try {
      PathTrie.<Integer>builder().add(HttpMethod.GET, "bad/{[test}", 1234);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void invalidPathParameterSyntax() {
    try {
      PathTrie.<Integer>builder().add(HttpMethod.GET, "bad/{test", 1234);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void parameterPathWithUnexpectedPart() {
    try {
      PathTrie.<Integer>builder().add(HttpMethod.GET, "bad/{test}unexpected/afterBad", 1234);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void invalidParameterSegment() {
    String invalids = "?#[]{}";
    for (char c : invalids.toCharArray()) {
      try {
        PathTrie.<Integer>builder().add(HttpMethod.GET, "bad/" + c, 1234);
        fail("expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        // expected
      }
    }
  }

  private void doStrictDuplicateTest(String path, String duplicatePath) {
    try {
      PathTrie.<Integer>builder()
          .add(HttpMethod.GET, path, 1234)
          .add(HttpMethod.GET, duplicatePath, 4321);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  private void assertSuccessfulGetResolution(PathTrie<Integer> trie, String path, Integer value) {
    assertSuccessfulResolution(trie, HttpMethod.GET, path, value);
  }

  private void assertSuccessfulResolution(
      PathTrie<Integer> trie, HttpMethod method, String path, Integer value) {
    assertSuccessfulResolution(trie, method, path, value, ImmutableMap.<String, String>of());
  }

  private void assertSuccessfulGetResolution(PathTrie<Integer> trie, String path, Integer value,
      Map<String, String> rawParameters) {
    assertSuccessfulResolution(trie, HttpMethod.GET, path, value, rawParameters);
  }

  private void assertSuccessfulResolution(
      PathTrie<Integer> trie, HttpMethod method, String path, Integer value,
      Map<String, String> rawParameters) {
    Result<Integer> result = trie.resolve(method, path);
    assertThat(result).isNotNull();
    assertThat(result.getResult()).isEqualTo(value);
    assertThat(result.getRawParameters()).isEqualTo(rawParameters);
  }

  private void assertFailedGetResolution(PathTrie<Integer> trie, String path) {
    assertFailedGetResolution(trie, HttpMethod.GET, path);
  }

  private void assertFailedGetResolution(PathTrie<Integer> trie, HttpMethod method, String path) {
    assertThat(trie.resolve(method, path)).isNull();
  }
}
