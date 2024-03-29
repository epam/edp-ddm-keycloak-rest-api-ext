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

package com.epam.digital.data.platform.keycloak.rest.api.ext;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

public class UserFilter {

  public static boolean isUserMatchesAttributesEquals(UserModel userModel,
      Map<String, List<String>> attributesEquals) {
    return attributesEquals.isEmpty() ||
        attributesEquals.entrySet().stream().allMatch(
            entry -> isListEmpty(entry.getValue()) ||
                userModel.getAttributeStream(entry.getKey()).anyMatch(entry.getValue()::contains));
  }

  public static boolean isUserMatchesAttributesStartsWith(UserModel userModel,
      Map<String, List<String>> attributesStartsWith) {
    return attributesStartsWith.isEmpty() ||
        attributesStartsWith.entrySet().stream().allMatch(entry -> isListEmpty(entry.getValue()) ||
            userModel.getAttributeStream(entry.getKey())
                .anyMatch(value -> entry.getValue().stream().anyMatch(value::startsWith)));
  }

  public static boolean isUserMatchesAttributesThatAreStartFor(UserModel userModel,
      Map<String, List<String>> attributesThatAreStartFor) {
    return attributesThatAreStartFor.isEmpty() ||
        attributesThatAreStartFor.entrySet().stream()
            .allMatch(entry -> isListEmpty(entry.getValue()) ||
                userModel.getAttributeStream(entry.getKey())
                    .anyMatch(value -> entry.getValue().stream()
                        .anyMatch(attr -> attr.startsWith(value))));
  }

  private static boolean isListEmpty(List<String> list) {
    return Objects.isNull(list) || list.isEmpty();
  }

  @Deprecated(forRemoval = true)
  public Stream<UserModel> filterUsersByAttributesEquals(KeycloakSession session,
      Map<String, String> attributes) {
    Stream<UserModel> userModels = Stream.empty();
    if (attributes == null || attributes.entrySet().isEmpty()) {
      return userModels;
    }
    int i = 0;
    for (Entry<String, String> attribute : attributes.entrySet()) {
      if (i == 0) {
        userModels = session.users()
            .searchForUserByUserAttributeStream(session.getContext().getRealm(), attribute.getKey(),
                attribute.getValue());
        i++;
      }
      userModels = userModels.filter(
          userModel -> Objects.equals(userModel.getFirstAttribute(attribute.getKey()),
              attribute.getValue()));
    }
    return userModels;
  }

  @Deprecated(forRemoval = true)
  public Stream<UserModel> filterUsersByAttributesInvertedStartsWith(Stream<UserModel> userModels,
      Map<String, List<String>> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return Stream.empty();
    }
    for (Entry<String, List<String>> attribute : attributes.entrySet()) {
      userModels = userModels.filter(userModel -> matchesAnyPrefix(attribute, userModel));
    }
    return userModels;
  }

  private boolean matchesAnyPrefix(Entry<String, List<String>> requestedAttributes,
      UserModel userModel) {
    return userModel.getAttributeStream(requestedAttributes.getKey()).anyMatch(
        usersAttribute -> requestedAttributes.getValue().stream()
            .anyMatch(requestedAttribute -> requestedAttribute.startsWith(usersAttribute)));
  }
}
