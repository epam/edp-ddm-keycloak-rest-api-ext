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


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.v2.SearchUsersByAttributesRequestDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.v2.SearchUsersByAttributesResponseDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.v2.SearchUsersByAttributesResponseDto.Pagination;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jboss.resteasy.spi.HttpRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

class UserApiProviderSearchByAttributesV2Test {

  private static List<User> userStorage;
  private static Map<String, User> userStorageMap;

  HttpRequest request;
  UserApiProvider userApiProvider;

  ObjectMapper objectMapper;

  @BeforeAll
  static void setStorage() {
    userStorage = List.of(
        new User("user1", Map.of("attribute1", List.of("value1"), "hierarchy", List.of("100"))),
        new User("user2", Map.of("attribute1", List.of("value2"), "hierarchy", List.of("101"))),
        new User("user3", Map.of("attribute2", List.of("value1"), "hierarchy", List.of("100.200"))),
        new User("user4", Map.of("hierarchy", List.of("100.201"))),
        new User("user5", Map.of("hierarchy", List.of("100.201.300"))),
        new User("user6", Map.of("hierarchy", List.of("101.200.301"))),
        new User("user7", Map.of("hierarchy", List.of("100.200.301.400"))),
        new User("user8",
            Map.of("attribute1", List.of("value3"), "hierarchy", List.of("100.200.301.400.500")))
    );
    userStorageMap = userStorage.stream()
        .collect(Collectors.toMap(User::getUserName, Function.identity()));
  }

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    var session = Mockito.mock(KeycloakSession.class);
    var context = Mockito.mock(KeycloakContext.class);
    var realm = Mockito.mock(RealmModel.class);
    var userProvider = Mockito.mock(UserProvider.class);
    var userCredentialManager = Mockito.mock(UserCredentialManager.class);
    request = Mockito.mock(HttpRequest.class);
    userApiProvider = new UserApiProviderTestImpl(session, new UserFilter());
    when(session.getContext()).thenReturn(context);
    when(context.getRealm()).thenReturn(realm);
    when(realm.getName()).thenReturn("realmName");
    when(session.users()).thenReturn(userProvider);
    Mockito.doReturn(userCredentialManager).when(session).userCredentialManager();

    Mockito.doAnswer(invocation -> {
      var firstResult = (int) invocation.getArgument(1);
      var maxResults = (int) invocation.getArgument(2);
      var stream = userStorage.stream();
      if (firstResult >= 0) {
        stream = stream.skip(firstResult);
      }
      if (maxResults > 0) {
        stream = stream.limit(maxResults);
      }

      return stream.map(this::mapToUserModelMock);
    }).when(userProvider).getUsersStream(any(), anyInt(), anyInt());
  }

  @ParameterizedTest
  @MethodSource("getRequestAndExpectedResponse")
  @SneakyThrows
  void searchUsersByAttributes(String requestString,
      SearchUsersByAttributesResponseDto expectedResponseDto) {
    var requestDto = objectMapper.readValue(requestString, SearchUsersByAttributesRequestDto.class);

    var actualResponseDto = userApiProvider.searchUsersByAttributes(request, requestDto);

    Assertions.assertEquals(expectedResponseDto.getUsers().size(),
        actualResponseDto.getUsers().size());
    Assertions.assertEquals(expectedResponseDto.getPagination().getContinueToken(),
        actualResponseDto.getPagination().getContinueToken());

    var expectedUsers = expectedResponseDto.getUsers().stream()
        .collect(Collectors.toMap(UserRepresentation::getUsername, Function.identity()));
    var actualUsers = actualResponseDto.getUsers().stream()
        .collect(Collectors.toMap(UserRepresentation::getUsername, Function.identity()));
    expectedUsers.forEach((userName, expectedUser) -> {
      var actualUser = actualUsers.get(userName);
      Assertions.assertNotNull(actualUser, "Not found user with name: " + userName);
      Assertions.assertEquals(expectedUser.getAttributes(), actualUser.getAttributes());
      Assertions.assertEquals(expectedUser.getUsername(), actualUser.getUsername());
    });
  }

  static Object[][] getRequestAndExpectedResponse() {
    return new Object[][]{{
        "{\"attributesEquals\":{\"attribute1\":[\"value1\",\"value2\"]}}",
        createResponseDto(List.of("user1", "user2"), Pagination.builder().continueToken(-1).build())
    }, {
        "{\"attributesEquals\":{\"attribute1\":[\"value3\"],\"attribute2\":[]}}",
        createResponseDto(List.of("user8"), Pagination.builder().continueToken(-1).build())
    }, {
        "{\"attributesStartsWith\":{\"hierarchy\":[\"100\",\"101.200\"]}}",
        createResponseDto(List.of("user1", "user3", "user4", "user5", "user6", "user7", "user8"),
            Pagination.builder().continueToken(-1).build())
    }, {
        "{\"attributesStartsWith\":{\"hierarchy\":[\"100\",\"101.200\"]},\"pagination\":{\"limit\":2}}",
        createResponseDto(List.of("user1", "user3"), Pagination.builder().continueToken(3).build())
    }, {
        "{\"attributesStartsWith\":{\"hierarchy\":[\"100\",\"101.200\"]},\"pagination\":{\"limit\":2,\"continueToken\":3}}",
        createResponseDto(List.of("user4", "user5"), Pagination.builder().continueToken(5).build())
    }, {
        "{\"attributesStartsWith\":{\"hierarchy\":[\"100\",\"101.200\"]},\"pagination\":{\"limit\":2,\"continueToken\":5}}",
        createResponseDto(List.of("user6", "user7"), Pagination.builder().continueToken(7).build())
    }, {
        "{\"attributesStartsWith\":{\"hierarchy\":[\"100\",\"101.200\"]},\"pagination\":{\"limit\":2,\"continueToken\":7}}",
        createResponseDto(List.of("user8"), Pagination.builder().continueToken(-1).build())
    }, {
        "{\"attributesEquals\":{},\"pagination\":{\"limit\":4,\"continueToken\":0}}",
        createResponseDto(List.of("user1", "user2", "user3", "user4"),
            Pagination.builder().continueToken(4).build())
    }, {
        "{\"attributesThatAreStartFor\":{},\"pagination\":{\"limit\":4,\"continueToken\":4}}",
        createResponseDto(List.of("user5", "user6", "user7", "user8"),
            Pagination.builder().continueToken(-1).build())
    }, {
        "{\"pagination\":{\"limit\":4,\"continueToken\":-1}}",
        createResponseDto(List.of(), Pagination.builder().continueToken(-1).build())
    }, {
        "{\"attributesThatAreStartFor\":{\"hierarchy\":[\"100.200\",\"101\"]},\"pagination\":{\"limit\":-2123,\"continueToken\":0}}",
        createResponseDto(List.of("user1", "user2", "user3"),
            Pagination.builder().continueToken(-1).build())
    }, {
        "{\"attributesStartsWith\":{\"hierarchy\":[\"100.200\"]},\"attributesThatAreStartFor\":{\"hierarchy\":[\"100.200.301.400.500\"]}}",
        createResponseDto(List.of("user3", "user7", "user8"),
            Pagination.builder().continueToken(-1).build())
    },
    };
  }

  static SearchUsersByAttributesResponseDto createResponseDto(List<String> userNames,
      SearchUsersByAttributesResponseDto.Pagination pagination) {
    var userRepresentations = userNames.stream()
        .map(userStorageMap::get)
        .map(user -> {
          var userRepresentation = new UserRepresentation();
          userRepresentation.setUsername(user.getUserName());
          userRepresentation.setAttributes(user.getAttributes());
          return userRepresentation;
        }).collect(Collectors.toList());

    return SearchUsersByAttributesResponseDto.builder()
        .users(userRepresentations)
        .pagination(pagination)
        .build();
  }

  private UserModel mapToUserModelMock(User user) {
    var userModel = Mockito.mock(UserModel.class);
    Mockito.doReturn(user.getUserName()).when(userModel).getUsername();
    Mockito.doAnswer(invocation -> {
      var attrName = (String) invocation.getArgument(0);
      return user.getAttributes().getOrDefault(attrName, List.of()).stream();
    }).when(userModel).getAttributeStream(anyString());
    Mockito.doReturn(user.getAttributes()).when(userModel).getAttributes();
    Mockito.doReturn("someId").when(userModel).getId();
    return userModel;
  }

  @AllArgsConstructor
  @Getter
  static class User {

    private String userName;
    private Map<String, List<String>> attributes;
  }
}
