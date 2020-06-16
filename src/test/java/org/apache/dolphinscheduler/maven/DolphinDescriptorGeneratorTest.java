/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.maven;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;

import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.junit.Test;
import org.junit.runner.RunWith;
import static java.nio.file.Files.readAllLines;
import static org.junit.Assert.assertEquals;
import static java.util.Collections.singletonList;
import java.io.File;
import java.util.List;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.3.9", "3.5.4", "3.6.2"})
@SuppressWarnings({"JUnitTestNG", "PublicField"})
public class DolphinDescriptorGeneratorTest {

    private static final String DESCRIPTOR = "META-INF/services/org.apache.dolphinscheduler.spi.DolphinSchedulerPlugin";

    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime maven;

    public DolphinDescriptorGeneratorTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder)
            throws Exception
    {
        this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void testSimplest() throws Exception
    {
        testProjectPackaging("simplest", "its.SimplestPlugin");
    }

    @Test
    public void testAbstractPluginClass() throws Exception
    {
        testProjectPackaging("abstract-plugin-class", "its.TestPluginImpl");
    }

    @Test
    public void testInterfacePluginClass() throws Exception
    {
        testProjectPackaging("interface-plugin-class", "its.TestPluginImpl");
    }

    protected void testProjectPackaging(String projectId, String expectedPluginClass)
            throws Exception
    {
        File basedir = resources.getBasedir(projectId);
        maven.forProject(basedir)
                .execute("package")
                .assertErrorFreeLog();

        File output = new File(basedir, "target/classes/" + DESCRIPTOR);

        List<String> lines = readAllLines(output.toPath(), UTF_8);
        assertEquals(singletonList(expectedPluginClass), lines);
    }
}
