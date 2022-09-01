package com.epam.digital.data.platform.keycloak.rest.api.ext;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class UserApiProviderFactory implements RealmResourceProviderFactory {

  public static final String ID = "users";

  public RealmResourceProvider create(KeycloakSession session) {
    return new UserApiProvider(session);
  }

  public void init(Scope config) {
  }

  public void postInit(KeycloakSessionFactory factory) {
  }

  public void close() {
  }

  public String getId() {
    return ID;
  }

}
