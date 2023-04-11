/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.keycloak.rest.api.ext.dto.v2;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.jboss.resteasy.spi.HttpRequest;

/**
 * Representation of search users by attributes request body.
 * <p>
 * {@link SearchUsersByAttributesRequestDto#getAttributesEquals() getAttributesEquals()} contains a
 * map of attributes that user must have with <b>exact match</b> to be returned.
 * <p>
 * {@link SearchUsersByAttributesRequestDto#getAttributesStartsWith() getAttributesStartsWith()}
 * contains a map of attributes that user must have with <b>starts with match</b> to be returned.
 * <p>
 * {@link SearchUsersByAttributesRequestDto#getAttributesThatAreStartFor()
 * getAttributesThatAreStartFor()} contains a map of attributes that user must have <b>a start
 * for</b> to be returned.
 * <li>If there are filled several maps in request dto they processed by AND. For example if there
 * are filled {@code getAttributesEquals()} and {@code getAttributesStartsWith()} then user must
 * have attributes that are match both of these maps.
 * <li>If any of the map contains several attributes they processed by AND. For example there are
 * two attributes {@code attr1} and {@code attr 2} in map then user must have both of these
 * attribute matches to be returned.
 * <li>If any of the attribute of the maps contains several values they processed by OR. For
 * example {@code getAttributesEquals()} map contains attribute {@code attr1} with values
 * {@code ["value1", "value2"]} then user must have an attribute named {@code attr1} with any of the
 * values to be returned.
 * <p>
 * Pagination of the request implemented with {@code continuationToken}. It means that response will
 * return a token which must be used as anchor for the next page.
 *
 * @see com.epam.digital.data.platform.keycloak.rest.api.ext.UserApiProvider#searchUsersByAttributes(HttpRequest,
 * SearchUsersByAttributesRequestDto) request method itself
 * @see SearchUsersByAttributesResponseDto representation of the response body
 */
@Setter
public class SearchUsersByAttributesRequestDto {

  private Map<String, List<String>> attributesEquals;
  private Map<String, List<String>> attributesStartsWith;
  private Map<String, List<String>> attributesThatAreStartFor;

  @Getter
  private Pagination pagination = new Pagination();

  @Nonnull
  public Map<String, List<String>> getAttributesEquals() {
    return Map.copyOf(Objects.requireNonNullElse(attributesEquals, Map.of()));
  }

  @Nonnull
  public Map<String, List<String>> getAttributesStartsWith() {
    return Map.copyOf(Objects.requireNonNullElse(attributesStartsWith, Map.of()));
  }

  @Nonnull
  public Map<String, List<String>> getAttributesThatAreStartFor() {
    return Map.copyOf(Objects.requireNonNullElse(attributesThatAreStartFor, Map.of()));
  }

  @Setter
  @Getter
  public static class Pagination {

    private Integer limit;
    private Integer continueToken;
  }
}
