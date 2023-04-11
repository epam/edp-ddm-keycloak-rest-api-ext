# keycloak-rest-api-ext

### Overview

It's a Keycloak plugin that extends Keycloak REST API with new endpoints.

### Usage

* Add plugin to a Keycloak Dockerfile

```` dockerfile
## Works for Keycloak 15.1.1
ARG NEXUS_URL={{url-to-your-nexus}}
ARG NEXUS_REPO={{nexus-repo-name}}
ARG KEYCLOAK_REST_API_EXT_VERSION=keycloak-rest-api-ext-1.6.0.jar

ADD $NEXUS_URL/repository/$NEXUS_REPO/com/epam/digital/data/platform/saml-user-custom-attributes-mapper/KEYCLOAK_REST_API_EXT_VERSION /opt/jboss/keycloak/standalone/deployments/keycloak-rest-api-ext.jar
RUN chown jboss:jboss /opt/jboss/keycloak/standalone/deployments/keycloak-rest-api-ext.jar
````

* In any http client send request to any of the Keycloak endpoints.

### Added endpoints

* POST __/auth/realms/{realm}/users/search-by-attributes__ - search users by number of attributes.
  Request body structure
  see [here](src/main/java/com/epam/digital/data/platform/keycloak/rest/api/ext/dto/v2/SearchUsersByAttributesRequestDto.java)

### Local development

#### Running Keycloak

0. If you're using Mac then you should
   run [mac-local-keycloak-build.sh](src/local/mac-local-keycloak-build.sh). This script will
   clone [keycloak-containers](https://github.com/keycloak/keycloak-containers.git) checkout needed
   version of Keycloak ***(15.1.1)*** and build image of Keycloak same
   as [here](https://quay.io/repository/keycloak/keycloak) locally. For some reason if you
   pull `docker pull quay.io/keycloak/keycloak:15.1.1` it won't start properly on Mac, but it works
   if build it locally.
1. Build jar file. In terminal in project root directory run next.
   ```shell 
   mvn package
   ```
2. Build and run image of a Docker with this plugin
   ```shell 
   docker build -f src/local/Dockerfile-local -t keycloak-rest-api-ext .
   docker run -d -p 8080:8080 -p 8443:8443 -p 9990:9990 --name keycloak-rest-api-ext keycloak-rest-api-ext
   ```
3. Go to browser to [localhost:8080](http://localhost:8080) and login to Keycloak Administration
   console with credentials user - **admin** and password - **admin**
4. Remove _keycloak-rest-api-ext_ docker container and image when you need to rebuild it
   ```shell
   docker rm keycloak-rest-api-ext -f
   docker rmi keycloak-rest-api-ext
   ```

### License

The keycloak-rest-api-ext is Open Source software released under
the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).