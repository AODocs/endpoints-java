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

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.common.collect.Table;
import com.google.common.flogger.FluentLogger;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple trie that maps pairs of HTTP methods and parameterized paths to arbitrary data. Each
 * node in the tree is a path segment. For example, given a path "discovery/v1/apis", the data would
 * be stored in the node path represented by "discovery" -&gt; "v1" -&gt; "apis". A path is
 * considered parameterized if one or more segments is of the form "{name}". When a parameterized
 * path is resolved, a map from parameter names to raw String values is returned as part of the
 * result. Null values are not acceptable values in this trie. Parameter names can only contain
 * alphanumeric characters or underscores, and cannot start with a numeric.
 * A path with a <a href="https://cloud.google.com/apis/design/custom_methods">custom method</a>
 * is also supported.
 */
public class PathTrie<T> {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final Splitter PATH_SPLITTER = Splitter.on('/');
  private static final String PARAMETER_PATH_SEGMENT = "{}";
  private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z_\\d]*");
  private static final Pattern CUSTOM_METHOD_NAME_PATTERN = PARAMETER_NAME_PATTERN;
  
  // General delimiters that must be URL encoded, as defined by RFC 3986.
  private static final CharMatcher RESERVED_URL_CHARS = CharMatcher.anyOf(":/?#[]{}");

  private final ImmutableMap<String, PathTrie<T>> subTries;
  private final ImmutableTable<HttpMethod, String, MethodInfo<T>> httpMethodTable;

  private PathTrie(Builder<T> builder) {
    this.httpMethodTable = ImmutableTable.copyOf(builder.httpMethodTable);
    ImmutableMap.Builder<String, PathTrie<T>> subTriesBuilder = ImmutableMap.builder();
    for (Entry<String, Builder<T>> entry : builder.subBuilders.entrySet()) {
      subTriesBuilder.put(entry.getKey(), new PathTrie<>(entry.getValue()));
    }
    this.subTries = subTriesBuilder.build();
  }
  
  /**
   * Attempts to resolve a path. Resolution prefers literal paths over path parameters. The result
   * includes the object to which the path mapped, as well a map from parameter names to
   * URL-decoded values. If the path cannot be resolved, null is returned.
   */
  public Result<T> resolve(HttpMethod method, String path) {
    Preconditions.checkNotNull(method, "method");
    Preconditions.checkNotNull(path, "path");
    List<String> pathSegments = getPathSegments(path);
    String customMethod = extractCustomMethodAndUpdatePathSegments(pathSegments);
    return resolve(method, customMethod, pathSegments, 0, new ArrayList<>());
  }

  private Result<T> resolve(
      HttpMethod httpMethod, String customMethod, List<String> pathSegments, int index, List<String> rawParameters) {
    if (index < pathSegments.size()) {
      String segment = pathSegments.get(index);
      PathTrie<T> subTrie = subTries.get(segment);
      if (subTrie != null) {
        Result<T> result = subTrie.resolve(httpMethod, customMethod, pathSegments, index + 1, rawParameters);
        if (result != null) {
          return result;
        }
      }
      subTrie = subTries.get(PARAMETER_PATH_SEGMENT);
      if (subTrie != null) {
        // TODO: We likely need to enforce non-empty values here.
        rawParameters.add(segment);
        Result<T> result = subTrie.resolve(httpMethod, customMethod, pathSegments, index + 1, rawParameters);
        if (result == null) {
          rawParameters.remove(rawParameters.size() - 1);
        }
        return result;
      }
      return null;
    } else {
      Optional<String> matchingMethod = findMatchingMethod(index - 1, httpMethod, customMethod);
      return matchingMethod.map(foundMatchingMethod -> {
        MethodInfo<T> methodInfo = httpMethodTable.get(httpMethod, foundMatchingMethod);
        ImmutableList<IndexedParameterName> parameterNames = methodInfo.parameterNames;
        Preconditions.checkState(rawParameters.size() == parameterNames.size());
        Map<String, String> rawParameterMap = Maps.newHashMap();
        for (int i = 0; i < parameterNames.size(); i++) {
          String parameterValue = decodeUri(rawParameters.get(i));
          if ((i == parameterNames.size() - 1) && (!customMethod.equals(foundMatchingMethod))) {
            parameterValue += ":" + decodeUri(customMethod);
          }
          rawParameterMap.put(parameterNames.get(i).parameterName, parameterValue);
        }
        return new Result<>(methodInfo.value, rawParameterMap);
      }).orElse(null);
    }
  }
  
  private Optional<String> findMatchingMethod(int pathSegmentsLastIndex, HttpMethod httpMethod, String customMethod) {
    if (httpMethodTable.contains(httpMethod, customMethod)) {
      return Optional.of(customMethod);
    }
    
    if (httpMethodTable.contains(httpMethod, "")) {
      MethodInfo<T> methodInfo = httpMethodTable.get(httpMethod, "");
      boolean lastSegmentHasParameter = !methodInfo.parameterNames.isEmpty()
              && methodInfo.parameterNames.get(methodInfo.parameterNames.size() - 1).index == pathSegmentsLastIndex;
      return lastSegmentHasParameter ? Optional.of("") : Optional.empty();
    }
    
    return Optional.empty();
  }

  
  /**
   * The resulting information for a successful path resolution, which includes the value to which
   * the path maps, as well as the raw (but URL decoded) string values of all path parameters.
   */
  public static class Result<T> {
    private final T result;
    private final Map<String, String> rawParameters;

    public Result(T result, Map<String, String> rawParameters) {
      this.result = result;
      this.rawParameters = rawParameters;
    }

    public T getResult() {
      return result;
    }

    public Map<String, String> getRawParameters() {
      return rawParameters;
    }
  }

  /**
   * Returns a new, path conflict validating {@link PathTrie.Builder}.
   *
   * @param <T> the type that the trie will be storing
   */
  public static <T> Builder<T> builder() {
    return new Builder<>(true);
  }

  /**
   * Returns a new {@link PathTrie.Builder}.
   *
   * @param throwOnConflict whether to throw an exception on path conflicts
   * @param <T> the type that the trie will be storing
   */
  public static <T> Builder<T> builder(boolean throwOnConflict) {
    return new Builder<>(throwOnConflict);
  }

  /**
   * A builder for creating a {@link PathTrie}, which is immutable.
   */
  public static class Builder<T> {
    private final Map<String, Builder<T>> subBuilders = Maps.newHashMap();
    private final Table<HttpMethod, String, MethodInfo<T>> httpMethodTable = HashBasedTable.create();
    private final boolean throwOnConflict;

    public Builder(boolean throwOnConflict) {
      this.throwOnConflict = throwOnConflict;
    }

    /**
     * Adds a path to the trie.
     *
     * @throws IllegalArgumentException if the path cannot be added to the trie
     * @throws NullPointerException if either path or value are null
     */
    public Builder<T> add(HttpMethod method, String path, T value) {
      Preconditions.checkNotNull(method, "method");
      Preconditions.checkNotNull(path, "path");
      Preconditions.checkNotNull(value, "value");
      // TODO: We likely want to do something about trailing slashes here (make configurable)
      List<String> pathSegments = getPathSegments(path);
      String customMethod = extractCustomMethodAndUpdatePathSegments(pathSegments);
      if (!customMethod.isEmpty()) {
        Matcher matcher = CUSTOM_METHOD_NAME_PATTERN.matcher(customMethod);
        if (!matcher.matches()) {
          throw new IllegalArgumentException(
                  String.format("'%s' not a valid custom method name", customMethod));
        }
      }
      add(method, customMethod, path, pathSegments, 0, value, new ArrayList<>());
      return this;
    }

    public PathTrie<T> build() {
      return new PathTrie<>(this);
    }

    private void add(HttpMethod httpMethod, String customMethod, String path, List<String> pathSegments,
            int segmentIndex, T value, List<IndexedParameterName> parameterNames) {
      if (segmentIndex < pathSegments.size()) {
        String segment = pathSegments.get(segmentIndex);
        if (segment.startsWith("{")) {
          if (segment.endsWith("}")) {
            parameterNames.add(new IndexedParameterName(segmentIndex, getAndCheckParameterName(segment)));
            getOrCreateSubBuilder(PARAMETER_PATH_SEGMENT)
                .add(httpMethod, customMethod, path, pathSegments, segmentIndex + 1, value, parameterNames);
          } else {
            throw new IllegalArgumentException(
                String.format("'%s' contains invalid parameter syntax: %s", path, segment));
          }
        } else {
          if (RESERVED_URL_CHARS.matchesAnyOf(segment)) {
            throw new IllegalArgumentException(
                String.format("'%s' contains invalid path segment: %s", path, segment));
          }
          getOrCreateSubBuilder(segment).add(httpMethod, customMethod, path, pathSegments,
                  segmentIndex + 1, value, parameterNames);
        }
      } else {
        boolean pathExists = httpMethodTable.contains(httpMethod, customMethod);
        if (pathExists && throwOnConflict) {
          throw new IllegalArgumentException(String.format("Path '%s' is already mapped", path));
        }
        if (pathExists) {
          log.atWarning().log("Path '%s' is already mapped, but overwriting it", path);
        }
        httpMethodTable.put(httpMethod, customMethod, new MethodInfo<>(parameterNames, value));
      }
    }

    private String getAndCheckParameterName(String segment) {
      String name = segment.substring(1, segment.length() - 1);
      Matcher matcher = PARAMETER_NAME_PATTERN.matcher(name);
      if (!matcher.matches()) {
        throw new IllegalArgumentException(
            String.format("'%s' not a valid path parameter name", name));
      }
      return name;
    }

    private Builder<T> getOrCreateSubBuilder(String segment) {
      Builder<T> subBuilder = subBuilders.get(segment);
      if (subBuilder == null) {
        subBuilder = builder(throwOnConflict);
        subBuilders.put(segment, subBuilder);
      }
      return subBuilder;
    }
  }

  private static List<String> getPathSegments(String path) {
    return Lists.newArrayList(PATH_SPLITTER.splitToList(path));
  }

  private static String decodeUri(String value) {
    try {
      return URLDecoder.decode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }
  
  private static String extractCustomMethodAndUpdatePathSegments(List<String> pathSegments) {
    String customMethod = "";
    if (!pathSegments.isEmpty()) {
      String lastSegment = pathSegments.get(pathSegments.size() - 1);
      String[] segmentParts = lastSegment.split(":");
      if (segmentParts.length >= 2) {
        customMethod = segmentParts[segmentParts.length - 1];
        String lastSegmentWithoutCustomMethod = lastSegment.substring(0, lastSegment.lastIndexOf(":"));
        pathSegments.set(pathSegments.size() - 1, lastSegmentWithoutCustomMethod);
      }
    }
    return customMethod;
  }
  
  private static class MethodInfo<T> {
    private final ImmutableList<IndexedParameterName> parameterNames;
    private final T value;

    MethodInfo(List<IndexedParameterName> parameterNames, T value) {
      this.parameterNames = ImmutableList.copyOf(parameterNames);
      this.value = value;
    }
  }
  
  private static class IndexedParameterName {
    private final int index;
    private final String parameterName;
    IndexedParameterName(int index, String parameterName) {
      this.index = index;
      this.parameterName = parameterName;
    }
  }
}
