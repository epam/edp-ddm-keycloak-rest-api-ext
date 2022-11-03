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
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.keycloak.storage.adapter.InMemoryUserAdapter;

class UserFilterTest extends KeycloakBaseTest {

  UserFilter userFilter = new UserFilter();

  @Test
  void shouldReturnEmptyStreamWhenAttributeIsNull() {
    var users = userFilter.filterUsersByAttributesEquals(session, null);
    assertEquals(0, users.count());
  }

  @Test
  void shouldReturnEmptyStreamWhenZeroAttributes() {
    var users = userFilter.filterUsersByAttributesEquals(session, Map.of());
    assertEquals(0, users.count());
  }
  
  @Test
  void shouldReturnUserWithBothAttributes() {
    var userModel1 = new InMemoryUserAdapter(session, realm, "id");
    userModel1.setAttribute("fullName", List.of("Alex"));
    userModel1.setAttribute("drfo", List.of("11110000"));

    var userModel2 = new InMemoryUserAdapter(session, realm, "id");
    userModel2.setAttribute("fullName", List.of("Alex"));
    userModel2.setAttribute("drfo", List.of("22226666"));
    
    when(userProvider.searchForUserByUserAttributeStream(realm, "fullName", "Alex"))
        .thenReturn(Stream.of(userModel1, userModel2));
    when(userProvider.searchForUserByUserAttributeStream(realm, "drfo", "11110000"))
        .thenReturn(Stream.of(userModel1));
    
    var users = userFilter.filterUsersByAttributesEquals(session, 
        Map.of("fullName", "Alex", "drfo", "11110000"));
    
    var result = users.collect(Collectors.toList());
    assertEquals(1, result.size());
    assertEquals("Alex", result.get(0).getFirstAttribute("fullName"));
    assertEquals("11110000", result.get(0).getFirstAttribute("drfo"));
  }

  @Test
  void filterUsersByAttributesStartsWith() {
    var userModel1 = new InMemoryUserAdapter(session, realm, "id");
    userModel1.setAttribute("KATOTTG", List.of("UA0102"));

    var userModel2 = new InMemoryUserAdapter(session, realm, "id");
    userModel2.setAttribute("KATOTTG", List.of("UA0402003", "UA0505001002"));

    var requestedAttributes = Map.of("KATOTTG", List.of("UA05050010020412345"));

    var users = userFilter.filterUsersByAttributesInvertedStartsWith(
        Stream.of(userModel1, userModel2), requestedAttributes);

    var result = users.collect(Collectors.toList());
    assertEquals(1, result.size());
    assertEquals(2, result.get(0).getAttributeStream("KATOTTG").count());
  }
}
