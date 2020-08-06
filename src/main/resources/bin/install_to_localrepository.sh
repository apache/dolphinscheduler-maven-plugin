#!/bin/bash

basepath=$(cd `dirname $0`; pwd)

cd $basepath
cd ../lib

plugin_jar=`ls | grep dolphinscheduler-maven-plugin-*.jar`

#get the version
version_tmp=`echo $plugin_jar | awk '{split($0,b,"dolphinscheduler-maven-plugin-");print b[2]}'`
version=`echo $version_tmp | awk '{split($0,b,".jar");print b[1]}'`
mvn install:install-file -Dfile=$plugin_jar -DgroupId=org.apache.dolphinscheduler -DartifactId=dolphinscheduler-maven-plugin -Dversion=$version -Dpackaging=jar

