/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.keycloak.rest.api.ext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUserRequestDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUsersByEqualsAndStartsWithAttributesRequestDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jboss.resteasy.spi.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.adapter.InMemoryUserAdapter;

class UserApiProviderLogicTest {

  private UserApiProviderTestImpl userApiProviderTestImpl;

  private KeycloakSession session;
  private RealmModel realm;
  private UserProvider userProvider;

  @BeforeEach
  void beforeEach() {
    session = mock(KeycloakSession.class);
    KeycloakContext context = mock(KeycloakContext.class);
    realm = mock(RealmModel.class);
    userProvider = mock(UserProvider.class);
    userApiProviderTestImpl = new UserApiProviderTestImpl(session, new UserFilter());
    when(session.getContext()).thenReturn(context);
    when(context.getRealm()).thenReturn(realm);
    when(realm.getName()).thenReturn("realmName");
    when(session.users()).thenReturn(userProvider);
  }

  @Test
  void shouldReturnZeroUsersIfMatchesOnlyOneAttribute() {
    var requestDto = new SearchUserRequestDto();
    requestDto.attributes = Map.of("key1", "value1", "key2", "value2");

    var userModel = new InMemoryUserAdapter(session, realm, "id");
    userModel.setAttribute("key1", List.of("value1"));
    when(userProvider.searchForUserByUserAttributeStream(realm, "key1", "value1"))
        .thenReturn(Stream.of(userModel));

    var userRepresentations = userApiProviderTestImpl.searchUsersByAttributes(
        mock(HttpRequest.class), requestDto);

    assertEquals(0, userRepresentations.size());
  }

  @Test
  void shouldReturnUserIfMatchesAllAttributes() {
    var requestDto = new SearchUserRequestDto();
    requestDto.attributes = Map.of("key1", "value1", "key2", "value2");

    var userModel = new InMemoryUserAdapter(session, realm, "id");
    userModel.setAttribute("key1", List.of("value1"));
    userModel.setAttribute("key2", List.of("value2"));
    when(userProvider.searchForUserByUserAttributeStream(realm, "key1", "value1"))
        .thenReturn(Stream.of(userModel));
    when(userProvider.searchForUserByUserAttributeStream(realm, "key2", "value2"))
        .thenReturn(Stream.of(userModel));

    var userRepresentations = userApiProviderTestImpl.searchUsersByAttributes(
        mock(HttpRequest.class), requestDto);

    assertEquals(1, userRepresentations.size());
    assertEquals(2, userRepresentations.get(0).getAttributes().size());
    assertEquals(1, userRepresentations.get(0).getAttributes().get("key1").size());
    assertEquals("value1", userRepresentations.get(0).getAttributes().get("key1").get(0));
    assertEquals(1, userRepresentations.get(0).getAttributes().get("key2").size());
    assertEquals("value2", userRepresentations.get(0).getAttributes().get("key2").get(0));
  }

  @Test
  void shouldReturnEmptyListWhenBothMapsAreEmptyOrNull() {
    var requestDto = new SearchUsersByEqualsAndStartsWithAttributesRequestDto();
    requestDto.attributesEquals = Map.of();
    requestDto.attributesStartsWith = Map.of();

    var userRepresentations = userApiProviderTestImpl.searchUsersByAttributes(
        mock(HttpRequest.class), requestDto);

    assertEquals(0, userRepresentations.size());
  }

  @Test
  void shouldFilterUsersOnlyByAttributesStartsWithIfAttributesEqualsIsNullOrEmpty() {
    prepareKeycloakUsers();
    var requestDto = new SearchUsersByEqualsAndStartsWithAttributesRequestDto();
    requestDto.attributesEquals = Map.of();
    requestDto.attributesStartsWith = Map.of("KATOTTG", List.of("UA0102030405"));

    var userRepresentations =
        userApiProviderTestImpl.searchUsersByAttributes(mock(HttpRequest.class), requestDto);

    assertEquals(2, userRepresentations.size());
    assertEquals(1, userRepresentations.get(0).getAttributes().get("KATOTTG").size());
    assertEquals(1, userRepresentations.get(1).getAttributes().get("KATOTTG").size());
  }

  @Test
  void shouldFilterUsersOnlyByAttributesEqualsIfAttributesStartsWithIsNullOrEmpty() {
    prepareKeycloakUsers();
    var requestDto = new SearchUsersByEqualsAndStartsWithAttributesRequestDto();
    requestDto.attributesEquals = Map.of("key1", "value1");
    requestDto.attributesStartsWith = Map.of();

    var userRepresentations =
        userApiProviderTestImpl.searchUsersByAttributes(mock(HttpRequest.class), requestDto);

    assertEquals(2, userRepresentations.size());
    assertEquals(1, userRepresentations.get(0).getAttributes().get("key1").size());
    assertEquals(1, userRepresentations.get(1).getAttributes().get("key1").size());
  }

  @Test
  void shouldFilterUsersByBothMapsWhenTheyAreNotNullOrNotEmpty() {
    prepareKeycloakUsers();
    var requestDto = new SearchUsersByEqualsAndStartsWithAttributesRequestDto();
    requestDto.attributesEquals = Map.of("key1", "value1");
    requestDto.attributesStartsWith = Map.of("KATOTTG", List.of("UA0102030405"));

    var userRepresentations =
        userApiProviderTestImpl.searchUsersByAttributes(mock(HttpRequest.class), requestDto);

    assertEquals(1, userRepresentations.size());
    assertEquals(1, userRepresentations.get(0).getAttributes().get("key1").size());
    assertEquals(1, userRepresentations.get(0).getAttributes().get("KATOTTG").size());
  }

  private void prepareKeycloakUsers() {
    var userModel1 = new InMemoryUserAdapter(session, realm, "id");
    userModel1.setAttribute("key1", List.of("value1"));

    var userModel2 = new InMemoryUserAdapter(session, realm, "id");
    userModel2.setAttribute("key1", List.of("value1"));
    userModel2.setAttribute("KATOTTG", List.of("UA0102"));

    var userModel3 = new InMemoryUserAdapter(session, realm, "id");
    userModel3.setAttribute("KATOTTG", List.of("UA0102"));

    when(userProvider.searchForUserByUserAttributeStream(realm, "key1", "value1"))
        .thenReturn(Stream.of(userModel1, userModel2));
    when(userProvider.getUsersStream(realm))
        .thenReturn(Stream.of(userModel1, userModel2, userModel3));
  }
}
