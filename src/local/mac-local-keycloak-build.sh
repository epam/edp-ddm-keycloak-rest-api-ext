#!/bin/zsh

#
# Copyright 2023 EPAM Systems.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

VERSION=15.1.1

cd /tmp || exit
git clone https://github.com/keycloak/keycloak-containers.git
cd keycloak-containers/server || exit
git checkout $VERSION
docker build -t "jboss/keycloak:${VERSION}" .
docker build -t "quay.io/keycloak/keycloak:${VERSION}" .
cd /tmp || exit
rm -rf keycloak-containers