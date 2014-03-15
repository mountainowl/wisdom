package org.wisdom.maven.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.slf4j.LoggerFactory;
import org.wisdom.maven.mojos.AbstractWisdomMojo;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.ow2.chameleon.core.utils.BundleHelper.isBundle;

/**
 * Copy all compile dependencies from the project (excluding transitive) to the expected directories.
 */
public class DependencyCopy {

    /**
     * Copies dependencies, that are bundles, to the application directory.
     * If the bundle is already in core or runtime, the bundle is not copied.
     *
     * @param mojo       the mojo
     * @param transitive whether or not we include the transitive dependencies.
     * @throws IOException when a bundle cannot be copied
     */
    public static void copyBundles(AbstractWisdomMojo mojo, boolean transitive) throws IOException {
        File applicationDirectory = new File(mojo.getWisdomRootDirectory(), "application");
        File runtimeDirectory = new File(mojo.getWisdomRootDirectory(), "runtime");
        File coreDirectory = new File(mojo.getWisdomRootDirectory(), "core");

        // No transitive.
        Set<Artifact> artifacts;
        if (!transitive) {
            // Direct dependencies that the current project has (no transitive)
            artifacts = mojo.project.getDependencyArtifacts();
        } else {
            // All dependencies that the current project has, including transitive ones. Contents are lazily
            // populated, so depending on what phases have run dependencies in some scopes won't be
            // included.
            artifacts = mojo.project.getArtifacts();
        }
        for (Artifact artifact : artifacts) {
            if ("compile".equalsIgnoreCase(artifact.getScope())) {
                File file = artifact.getFile();

                // Check it's a 'jar file'
                if (!file.getName().endsWith(".jar")) {
                    mojo.getLog().info("Dependency " + file.getName() + " not copied - it does not look like a jar " +
                            "file");
                    continue;
                }

                // Do we already have this file in core or runtime ?
                File test = new File(coreDirectory, file.getName());
                if (test.exists()) {
                    mojo.getLog().info("Dependency " + file.getName() + " not copied - already existing in `core`");
                    continue;
                }
                test = new File(runtimeDirectory, file.getName());
                if (test.exists()) {
                    mojo.getLog().info("Dependency " + file.getName() + " not copied - already existing in `runtime`");
                    continue;
                }
                // Check that it's a bundle.
                if (!isBundle(file)) {
                    mojo.getLog().info("Dependency " + file.getName() + " not copied to 'application' - it's not a " +
                            "bundle");
                    continue;
                }

                // All check done, let's copy !
                FileUtils.copyFileToDirectory(file, applicationDirectory);
            }
        }
    }

    /**
     * Extracts dependencies, that are webjars, to the 'assets/libs' directory.
     * Only the 'webjar' part of the jar file is unpacked.
     *
     * @param mojo       the mojo
     * @param transitive whether or not we include the transitive dependencies.
     * @throws IOException when a jar cannot be copied
     */
    public static void extractWebJars(AbstractWisdomMojo mojo, boolean transitive) throws IOException {
        File webjars = new File(mojo.getWisdomRootDirectory(), "assets/libs");


        // No transitive.
        Set<Artifact> artifacts;
        if (!transitive) {
            // Direct dependencies that the current project has (no transitive)
            artifacts = mojo.project.getDependencyArtifacts();
        } else {
            // All dependencies that the current project has, including transitive ones. Contents are lazily
            // populated, so depending on what phases have run dependencies in some scopes won't be
            // included.
            artifacts = mojo.project.getArtifacts();
        }
        for (Artifact artifact : artifacts) {
            if ("compile".equalsIgnoreCase(artifact.getScope())) {
                File file = artifact.getFile();

                // Check it's a 'jar file'
                if (!file.getName().endsWith(".jar")) {
                    mojo.getLog().debug("Dependency " + file.getName() + " not copied - it does not look like a jar " +
                            "file");
                    continue;
                }

                // Check that it's a bundle.
                if (!isWebJar(file)) {
                    mojo.getLog().debug("Dependency " + file.getName() + " not copied to 'libs' - it's not a webjar");
                    continue;
                }

                // All check done, unpack.
                extract(mojo, file, webjars);
            }
        }
    }

    /**
     * A regex extracting the library name and version from Zip Entry names.
     */
    public static final Pattern WEBJAR_REGEX = Pattern.compile(".*META-INF/resources/webjars/([^/]+)/([^/]+)/.*");

    public static final String WEBJAR_LOCATION = "META-INF/resources/webjars/";

    private static void extract(final AbstractWisdomMojo mojo, File in, File out) throws IOException {
        ZipFile file = new ZipFile(in);
        try {
            Enumeration<? extends ZipEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith(WEBJAR_LOCATION) && !entry.isDirectory()) {
                    // Compute destination.
                    File output = new File(out,
                            entry.getName().substring(WEBJAR_LOCATION.length()));
                    InputStream stream = null;
                    try {
                        stream = file.getInputStream(entry);
                        output.getParentFile().mkdirs();
                        FileUtils.copyInputStreamToFile(stream, output);
                    } catch (IOException e) {
                        mojo.getLog().error("Cannot unpack " + entry.getName() + " from " + file.getName(), e);
                        throw e;
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(file);
        }
    }

    /**
     * Checks whether the given file is a WebJar or not (http://www.webjars.org/documentation).
     * The check is based on the presence of {@literal META-INF/resources/webjars/} directory in the jar file.
     *
     * @param file the file.
     * @return {@literal true} if it's a bundle, {@literal false} otherwise.
     */
    public static boolean isWebJar(File file) {
        Set<String> found = new LinkedHashSet<>();
        if (file.isFile() && file.getName().endsWith(".jar")) {
            JarFile jar = null;
            try {
                jar = new JarFile(file);

                // Fast return if the base structure is not there
                if (jar.getEntry(WEBJAR_LOCATION) == null) {
                    return false;
                }

                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    Matcher matcher = WEBJAR_REGEX.matcher(entry.getName());
                    if (matcher.matches()) {
                        found.add(matcher.group(1) + "-" + matcher.group(2));
                    }
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(DependencyCopy.class).error("Cannot check if the file {} is a webjar, " +
                        "cannot open it", file.getName(), e);
                return false;
            } finally {
                final JarFile finalJar = jar;
                IOUtils.closeQuietly(new Closeable() {
                    @Override
                    public void close() throws IOException {
                        if (finalJar != null) {
                            finalJar.close();
                        }
                    }
                });
            }

            for (String lib : found) {
                LoggerFactory.getLogger(DependencyCopy.class).info("Web Library found in {} : {}",
                        file.getName(), lib);
            }

            return !found.isEmpty();
        }

        return false;
    }
}
