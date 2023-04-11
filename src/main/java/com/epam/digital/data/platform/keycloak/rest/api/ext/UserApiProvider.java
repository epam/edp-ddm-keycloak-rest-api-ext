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

import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUserRequestDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUsersByEqualsAndStartsWithAttributesRequestDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.v2.SearchUsersByAttributesRequestDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.v2.SearchUsersByAttributesResponseDto;
import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.v2.SearchUsersByAttributesResponseDto.Pagination;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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

  /**
   * @deprecated Use
   * {@link UserApiProvider#searchUsersByAttributes(HttpRequest, SearchUsersByAttributesRequestDto)}
   * instead
   */
  @Deprecated(forRemoval = true)
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

  /**
   * @deprecated Use
   * {@link UserApiProvider#searchUsersByAttributes(HttpRequest, SearchUsersByAttributesRequestDto)}
   * instead
   */
  @Deprecated(forRemoval = true)
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

    var userModels = userFilter.filterUsersByAttributesEquals(session, requestDto.attributesEquals);
    userModels = userFilter.filterUsersByAttributesInvertedStartsWith(userModels,
        requestDto.attributesStartsWith);
    return toRepresentation(userModels);
  }

  /**
   * API for searching requests by attributes.
   * <p>
   * Pagination implemented with using of {@code limit} as a page size and {@code continueToken}.
   * Any response will provide a continue token that must be used for the next page. Returns -1 on
   * the last page. If -1 was passed to a request as continue token it will return empty list of
   * users with {@code continueToken=-1}. If 0 or {@code null} was passed as continue token it will
   * return first page. If 0 or {@code null} was passed as {@code limit} then pagination is disabled
   * and request will return all found users.
   *
   * @param request    The http request itself
   * @param requestDto {@link SearchUsersByAttributesRequestDto} representation of request body
   * @return For example there is a list of users with their attributes:<br/>
   * {@code user1 - attribute1=value1,hierarchyCode=100}<br/>
   * {@code user2 - attribute1=value2,hierarchyCode=101.200}<br/>
   * {@code user3 - attribute1=value3,hierarchyCode=101.201.300}<br/>
   * {@code user4 - attribute1=value1,hierarchyCode=100.200.300}<br/>
   * {@code user5 - hierarchyCode=100.200.300.400}
   * <p>
   * <li>Request <pre>{@code {
   *   "attributesEquals":{
   *     "attribute1":["value1", "value2"]
   *   }
   * }}</pre> will return
   * {@code user1}, {@code user2} and {@code user4} as they have attribute with name
   * {@code attribute1} that matches the {@code attributesEquals} request.
   * <p>
   * <li>Request
   * <pre>{@code {
   *   "attributesEquals":{
   *     "attribute1":["value1", "value2"],
   *     "hierarchyCode":["100"]
   *   }
   * }}</pre> will return only {@code user1} as only this user have both attributes that match the
   * request.
   * <p>
   * <li>Request <pre>{@code {
   *   "attributesStartsWith":{
   *     "hierarchyCode":["100", "101.201"]
   *   }
   * }}</pre> will return {@code user1},{@code user3}, {@code user4} and {@code user5} as they all
   * have attribute with name {@code hierarchyCode} that matches the {@code attributesStartsWith}
   * request.
   * <p>
   * <li>Request <pre>{@code {
   *   "attributesEquals":{
   *     "attribute1":["value1", "value2"]
   *   },
   *   "attributesStartsWith":{
   *     "hierarchyCode":["100", "101.201"]
   *   }
   * }}</pre> will return {@code user1} and {@code user4}.
   * <p>
   * <li>Request <pre>{@code {
   *   "attributesThatAreStartFor":{
   *     "hierarchyCode":["100.200.300", "101"]
   *   }
   * }}</pre> will return {@code user1} and {@code user4} because these both users have attribute
   * {@code hierarchyCode} that are start for {@code 100.200.300}
   * <p>
   * <li>Request <pre>{@code {
   *   "attributesStartsWith":{
   *     "hierarchyCode":["100.200"]
   *   },
   *   "attributesThatAreStartFor":{
   *     "hierarchyCode":["100.200.300.400"]
   *   }
   * }}</pre> will return {@code user4} and {@code user5}
   */
  @POST
  @Path("v2/search-by-attributes")
  @NoCache
  @Produces({MediaType.APPLICATION_JSON})
  @Encoded
  public SearchUsersByAttributesResponseDto searchUsersByAttributes(
      @Context final HttpRequest request, SearchUsersByAttributesRequestDto requestDto) {
    final var realm = session.getContext().getRealm();
    authenticateRealmAdminRequest(request.getHttpHeaders());
    validateRequestRealm(request, realm.getName());

    final var continueToken = new AtomicInteger(
        Objects.requireNonNullElse(requestDto.getPagination().getContinueToken(), 0));
    if (continueToken.get() < 0) {
      // continue token shows that all pages were selected
      return SearchUsersByAttributesResponseDto.builder().users(List.of())
          .pagination(Pagination.builder().continueToken(-1).build()).build();
    }

    final var limit = new AtomicInteger(
        Objects.requireNonNullElse(requestDto.getPagination().getLimit(), 0));
    if (limit.get() <= 0) {
      // limit set to null of less than or equal 0 shows that pagination is disabled
      limit.set(-1);
    }

    final var foundUsers = new ArrayList<UserRepresentation>();

    final var oldContinueToken = new AtomicInteger(continueToken.get());
    do {
      oldContinueToken.set(continueToken.get());
      session.users().getUsersStream(realm, continueToken.get(), limit.get())
          // skip all remaining users if list is filled
          .filter(userModel -> limit.get() < 0 || foundUsers.size() < (limit.get() + 1))
          // set count of all processed users as continue token
          .peek(userModel -> continueToken.incrementAndGet())
          // filter users by attributesEquals
          .filter(userModel -> UserFilter.isUserMatchesAttributesEquals(userModel,
              requestDto.getAttributesEquals()))
          // filter users by attributesStartWith
          .filter(userModel -> UserFilter.isUserMatchesAttributesStartsWith(userModel,
              requestDto.getAttributesStartsWith()))
          // filter users by attributesThatAreStartFor
          .filter(userModel -> UserFilter.isUserMatchesAttributesThatAreStartFor(userModel,
              requestDto.getAttributesThatAreStartFor()))
          // map to UserRepresentation
          .map(userModel -> ModelToRepresentation.toRepresentation(session, realm, userModel))
          // add to list of found users
          .forEach(foundUsers::add);
    } while (limit.get() > 0 // if limit<=0 then we need only 1 iteration
        // if continueToken hasn't changed then user stream was empty, so end loop
        && oldContinueToken.get() != continueToken.get()
        // if found enough users end the loop
        && foundUsers.size() < (limit.get() + 1));

    if (limit.get() < 0 || foundUsers.size() <= limit.get()) {
      // if there were found users only for this page then it's last page
      continueToken.set(-1);
    } else {
      // remove last found user as they're from next page
      foundUsers.remove(limit.get());
      continueToken.decrementAndGet();
    }

    return SearchUsersByAttributesResponseDto.builder().users(foundUsers)
        .pagination(Pagination.builder().continueToken(continueToken.get()).build())
        .build();
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
