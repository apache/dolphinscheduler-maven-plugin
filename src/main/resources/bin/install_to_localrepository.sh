#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

basepath=$(cd `dirname $0`; pwd)

cd $basepath
cd ../lib

plugin_jar=`ls | grep dolphinscheduler-maven-plugin-*.jar`

#get the version
version_tmp=`echo $plugin_jar | awk '{split($0,b,"dolphinscheduler-maven-plugin-");print b[2]}'`
version=`echo $version_tmp | awk '{split($0,b,".jar");print b[1]}'`
mvn install:install-file -Dfile=$plugin_jar -DgroupId=org.apache.dolphinscheduler -DartifactId=dolphinscheduler-maven-plugin -Dversion=$version -Dpackaging=jar

