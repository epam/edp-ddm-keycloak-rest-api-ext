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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserProvider;

public abstract class KeycloakBaseTest {

  protected final KeycloakSession session = mock(KeycloakSession.class);
  protected final HttpRequest request = mock(HttpRequest.class);
  protected final HttpHeaders headers = mock(HttpHeaders.class);
  protected final RealmModel realm = mock(RealmModel.class);
  protected final UserProvider userProvider = mock(UserProvider.class);
  protected final UserCredentialManager userCredentialManager = mock(UserCredentialManager.class);
  protected final KeycloakContext context = mock(KeycloakContext.class);

  protected KeycloakBaseTest() {
    when(session.userLocalStorage()).thenReturn(userProvider);
    when(session.users()).thenReturn(userProvider);
    when(session.userCredentialManager()).thenReturn(userCredentialManager);
    when(session.getContext()).thenReturn(context);
    when(request.getHttpHeaders()).thenReturn(headers);
    when(request.getHttpHeaders().getRequestHeaders()).thenReturn(mock(MultivaluedMap.class));
    when(context.getRealm()).thenReturn(realm);
  }

}
