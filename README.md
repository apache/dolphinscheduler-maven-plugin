### This is a maven plugin for DolphinScheduler , It has three functions:

1. It is allowed to add \<packaging\>dolphinscheduler-plugin\</packaging\> to the pom file. If <packaging>dolphinscheduler-plugin</packaging> is added to the pom file, the DolphinScheduler service will load this model as a DolphinScheduler plugin.

2. Automatically check the model with <packaging>dolphinscheduler-plugin</packaging> added to the pom file, and will automatically generate META-INF/services/org.apache.dolphinscheduler.spi.DolphinScheduler file when compile.

3. Automatically check DolphinScheduler's maven dependency. Especially the dependencies used by plugins.

### Why we need this maven plugin?

If we are running on the server deployment. Because the plug-ins are in the corresponding plug-in directory, and the plug-in jar package has the corresponding META-INF/services, there is no problem. 

However, when we develop locally in IDE, we have no plug-ins dir and no plug-ins jar file . If we want to test and debug the plugin code, we need add the plugin module to the pom.xml of alert module ,But this violates the original intention of SPI. So we can`t add the plug-in module to the alert module as a dependency, So there will be a problem that the alert module cannot find the plug-in classes. 

With this maven plug-in, by scanning the pom file of the project ,The pluginloader can find the module identified by <package>dolphinscheduler-plugin</package>, then can load this module`s class files and its dependent third-party jars from the target/classes dir of the plug-in module. This will debug locally

### How to verify the release candidate

1. Download the apache-dolphinscheduler-maven-plugin-incubating-${RELEASE.VERSION}-bin.tar.gz file from `the release candidates` (it will be provided in the vote email).

2. Unzip the apache-dolphinscheduler-maven-plugin-incubating-${RELEASE.VERSION}-bin.tar.gz file

3. Go to apache-dolphinscheduler-maven-plugin-incubating-${RELEASE.VERSION}-bin/bin dir and run `chmod 755 install_to_localrepository.sh & sh install_to_localrepository.sh`. This will install apache-dolphinscheduler-maven-plugin to your local repository.

4. add this plugin to your project`s pom.xml file like this:

    ```
    <plugins>
    ...
        <plugin>
            <groupId>org.apache.dolphinscheduler</groupId>
            <artifactId>dolphinscheduler-maven-plugin</artifactId>
            <version>${RELEASE.VERSION}</version>
            <extensions>true</extensions>
        </plugin>
    ...
    </plugins>
    ```

5. run `mvn clean verify` test the plugin.