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

import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUserRequestDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUsersByEqualsAndStartsWithAttributesRequestDto;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.Encoded;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.utils.MediaType;

public class UserApiProvider extends AdminRoot implements RealmResourceProvider {

  private final UserFilter userFilter;

  public UserApiProvider(KeycloakSession session, UserFilter userFilter) {
    this.session = session;
    this.userFilter = userFilter;
  }

  public void close() {
  }

  public Object getResource() {
    return this;
  }

  @POST
  @Path("search")
  @NoCache
  @Produces({MediaType.APPLICATION_JSON})
  @Encoded
  public List<UserRepresentation> searchUsersByAttributes(@Context final HttpRequest request,
      SearchUserRequestDto requestDto) {
    authenticateRealmAdminRequest(request.getHttpHeaders());
    validateRequestRealm(request, session.getContext().getRealm().getName());
    return toRepresentation(
        userFilter.filterUsersByAttributesEquals(session, requestDto.attributes));
  }

  @POST
  @Path("search-by-attributes")
  @NoCache
  @Produces({MediaType.APPLICATION_JSON})
  @Encoded
  public List<UserRepresentation> searchUsersByAttributes(@Context final HttpRequest request,
      SearchUsersByEqualsAndStartsWithAttributesRequestDto requestDto) {
    authenticateRealmAdminRequest(request.getHttpHeaders());
    validateRequestRealm(request, session.getContext().getRealm().getName());
    if (requestDto.attributesEquals == null || requestDto.attributesEquals.isEmpty()) {
      var userModels = userFilter.filterUsersByAttributesInvertedStartsWith(
          session.users().getUsersStream(session.getContext().getRealm()),
          requestDto.attributesStartsWith);
      return toRepresentation(userModels);
    } 
    if (requestDto.attributesStartsWith == null || requestDto.attributesStartsWith.isEmpty()) {
      return toRepresentation(
          userFilter.filterUsersByAttributesEquals(session, requestDto.attributesEquals));
    }

    var userModels = userFilter.filterUsersByAttributesEquals(
        session, requestDto.attributesEquals);
    userModels = userFilter.filterUsersByAttributesInvertedStartsWith(userModels,
        requestDto.attributesStartsWith);
    return toRepresentation(userModels);
  }

  protected void validateRequestRealm(HttpRequest request, String realmName) {
    var realmNamePathOrder = 1;
    var pathSegment = request.getUri().getPathSegments().get(realmNamePathOrder);
    if (!Objects.equals(pathSegment.toString(), realmName)) {
      throw new ForbiddenException("Forbidden users search request for specified realm");
    }
  }

  protected List<UserRepresentation> toRepresentation(Stream<UserModel> userModelStream) {
    return userModelStream.map(userModel -> ModelToRepresentation.toRepresentation(session,
        session.getContext().getRealm(), userModel)).collect(Collectors.toList());
  }
}
