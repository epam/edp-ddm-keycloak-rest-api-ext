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

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;

public class UserApiProviderTestImpl extends UserApiProvider {

  public UserApiProviderTestImpl(KeycloakSession session, UserFilter userFilter) {
    super(session, userFilter);
  }

  @Override
  public void validateRequestRealm(HttpRequest request, String realmName) {
  }

  @Override
  protected AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
    return null;
  }

  @Override
  protected List<UserRepresentation> toRepresentation(Stream<UserModel> userModelStream) {
    return userModelStream.map(
            userModel -> {
              var representation = new UserRepresentation();
              representation.setAttributes(new HashMap<>(userModel.getAttributes()));
              return representation;
            })
        .collect(Collectors.toList());
  }
}
