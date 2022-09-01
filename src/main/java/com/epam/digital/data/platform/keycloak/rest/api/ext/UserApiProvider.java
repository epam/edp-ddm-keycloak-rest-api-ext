package com.epam.digital.data.platform.keycloak.rest.api.ext;

import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUserRequestDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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


  public UserApiProvider(KeycloakSession session) {
    this.session = session;
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
  public List<UserRepresentation> searchUsersByAttribute(@Context final HttpRequest request,
      SearchUserRequestDto searchUserRequestDto) {
    authenticateRealmAdminRequest(request.getHttpHeaders());
    validateRequestRealm(request, session.getContext().getRealm().getName());
    return filterUsersByAttributes(session, searchUserRequestDto.attributes);
  }

  private void validateRequestRealm(HttpRequest request, String realmName) {
    var realmNamePathOrder = 1;
    var pathSegment = request.getUri().getPathSegments().get(realmNamePathOrder);
    if (!Objects.equals(pathSegment.toString(), realmName)) {
      throw new ForbiddenException("Forbidden users search request for specified realm");
    }
  }

  private List<UserRepresentation> filterUsersByAttributes(KeycloakSession session,
      Map<String, String> attributes) {
    if (attributes == null || attributes.entrySet().isEmpty()) {
      return Collections.emptyList();
    }
    int i = 0;
    Stream<UserModel> userModelStream = Stream.empty();
    for (Entry<String, String> entry : attributes.entrySet()) {
      if (i == 0) {
        userModelStream = session.users().searchForUserByUserAttributeStream(
            session.getContext().getRealm(), entry.getKey(), entry.getValue());
        i++;
      }
      userModelStream = userModelStream.filter(
          userModel -> Objects.equals(userModel.getFirstAttribute(entry.getKey()),
              entry.getValue()));
    }
    return userModelStream.map(userModel -> ModelToRepresentation.toRepresentation(session,
        session.getContext().getRealm(), userModel)).collect(Collectors.toList());
  }

}
