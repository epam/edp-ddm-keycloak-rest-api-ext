package com.epam.digital.data.platform.keycloak.rest.api.ext;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epam.digital.data.platform.keycloak.rest.api.ext.dto.SearchUserRequestDto;
import javax.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.Test;

class UserApiProviderTest extends KeycloakBaseTest {

  UserApiProvider userApiProvider = new UserApiProvider(session);

  @Test
  void searchUsersByAttributeNoHeaders() {
    SearchUserRequestDto searchUserRequestDto = new SearchUserRequestDto();
    assertThrows(NotAuthorizedException.class, ()-> userApiProvider.searchUsersByAttribute(request, searchUserRequestDto));
  }

}