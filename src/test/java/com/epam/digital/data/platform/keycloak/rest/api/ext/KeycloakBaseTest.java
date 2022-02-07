package com.epam.digital.data.platform.keycloak.rest.api.ext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserProvider;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;

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
    when(session.userCredentialManager()).thenReturn(userCredentialManager);
    when(session.getContext()).thenReturn(context);
    when(request.getHttpHeaders()).thenReturn(headers);
    when(request.getHttpHeaders().getRequestHeaders()).thenReturn(mock(MultivaluedMap.class));
    when(context.getRealm()).thenReturn(realm);
  }

}
