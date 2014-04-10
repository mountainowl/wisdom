/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.maven.mojos;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Before;
import org.junit.Test;
import org.wisdom.maven.WatchingException;
import org.wisdom.maven.node.NodeManager;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the Less Compiler Mojo.
 */
public class LessCompilerMojoTest {

    public static final String FAKE_PROJECT = "target/test-classes/fake-project";
    public static final String FAKE_PROJECT_TARGET = "target/test-classes/fake-project/target";
    File nodeDirectory;
    private LessCompilerMojo mojo;


    @Before
    public void setUp() throws IOException {
        nodeDirectory = new File("target/test/node");
        nodeDirectory.mkdirs();
        Log log = new SystemStreamLog();
        NodeManager manager = new NodeManager(log, nodeDirectory);
        manager.installIfNotInstalled();
        mojo = new LessCompilerMojo();
        mojo.basedir = new File(FAKE_PROJECT);
        mojo.buildDirectory = new File(FAKE_PROJECT_TARGET);
        mojo.buildDirectory.mkdirs();
        mojo.lessVersion = "1.7.0";
        cleanup();
    }

    @Test
    public void testProcessingOfLessFiles() throws MojoFailureException, MojoExecutionException, IOException {
        cleanup();
        mojo.execute();

        File style = new File(FAKE_PROJECT_TARGET, "classes/assets/less/style.css");
        assertThat(style).isFile();
        assertThat(FileUtils.readFileToString(style))
                .contains("-webkit-box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("border-color: #fdcdea;")
                .contains("color: #fe33ac;");

        style = new File(FAKE_PROJECT_TARGET, "wisdom/assets/style.css");
        assertThat(style).isFile();
        assertThat(FileUtils.readFileToString(style))
                .contains("-webkit-box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("border-color: #fdcdea;")
                .contains("color: #fe33ac;");
    }

    @Test
    public void testWatching() throws MojoFailureException, MojoExecutionException, IOException, WatchingException, InterruptedException {
        cleanup();

        // Copy style to style2 (do not modify script as it is used by other tests).
        final File originalInternalStyle = new File(FAKE_PROJECT, "src/main/resources/assets/less/style.less");
        final File newInternalStyle = new File(FAKE_PROJECT, "src/main/resources/assets/less/style2.less");
        final File originalExternalStyle = new File(FAKE_PROJECT, "src/main/assets/style.less");

        String originalStyleContent = FileUtils.readFileToString(originalInternalStyle);
        FileUtils.copyFile(originalInternalStyle, newInternalStyle);

        mojo.execute();

        File style = new File(FAKE_PROJECT_TARGET, "classes/assets/less/style2.css");
        assertThat(style).isFile();
        assertThat(FileUtils.readFileToString(style))
                .contains("-webkit-box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("border-color: #fdcdea;")
                .contains("color: #fe33ac;");

        File ext = new File(FAKE_PROJECT_TARGET, "wisdom/assets/style.css");
        assertThat(ext).isFile();
        assertThat(FileUtils.readFileToString(style))
                .contains("-webkit-box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("border-color: #fdcdea;")
                .contains("color: #fe33ac;");

        // Delete script 2
        newInternalStyle.delete();
        mojo.fileDeleted(newInternalStyle);

        assertThat(new File(FAKE_PROJECT_TARGET, "classes/assets/less/style2.css").isFile()).isFalse();

        // Recreate the file with another name (same content)
        File newFile = new File(FAKE_PROJECT, "src/main/resources/assets/style3.less");
        FileUtils.write(newFile, originalStyleContent);
        mojo.fileCreated(newFile);
        File style3 = new File(FAKE_PROJECT_TARGET, "classes/assets/style3.css");
        assertThat(FileUtils.readFileToString(style3))
                .contains("-webkit-box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);")
                .contains("border-color: #fdcdea;")
                .contains("color: #fe33ac;");

        // Update link
        long originalLastModified = ext.lastModified();
        FileUtils.touch(originalExternalStyle);
        mojo.fileUpdated(originalExternalStyle);
        // The file should have been updated
        assertThat(ext.lastModified()).isGreaterThanOrEqualTo(originalLastModified);
    }

    private void cleanup() {
        FileUtils.deleteQuietly(mojo.buildDirectory);
    }


}