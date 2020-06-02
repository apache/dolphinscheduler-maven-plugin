This is a maven plugin for DolphinScheduler , It has tow functions:

1. It is allowed to add <packaging>dolphinscheduler-plugin</packaging> to the pom file. If <packaging>dolphinscheduler-plugin</packaging> is added to the pom file, the DolphinScheduler service will load this model as a DolphinScheduler plugin.

2. Automatically check the model with <packaging>dolphinscheduler-plugin</packaging> added to the pom file, and will automatically generate META-INF/services/org.apache.dolphinscheduler.spi.DolphinScheduler file when compile.

3. Automatically check DolphinScheduler's maven dependency. Especially the dependencies used by plugins.

