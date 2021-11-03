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
package com.google.api.server.spi.testing;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;

@Api(name = "validation", version = "v1")
public class ValidationEndpoint {
  @ApiMethod(name = "create", path = "{pathParam}")
  public void create(
          @Named("pathParam") @Pattern(regexp = "^\\d+$") String pathParam, 
          @Named("queryParam") @Pattern(regexp = "^[a-z]{2}$") String queryParam,
          @Named("minMaxParam") @Min(10) @Max(20) Long minMaxParam,
          @Named("decimalMinMaxParam") @DecimalMin(value = "2.3", inclusive = false) @DecimalMax(value = "4") Double decimalMinMaxParam) {
  }
}
