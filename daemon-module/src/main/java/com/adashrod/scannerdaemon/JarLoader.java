package com.adashrod.scannerdaemon;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;

public class JarLoader {
    private static final Collection<URL> addedJars = new HashSet<>();
    private static PluginClassLoader pluginClassLoader;

    /**
     * Loads the jar into the runtime, making its classes available for use
     * @param file jar file to load
     * @return true if the jar was loaded; false if it had been loaded previously
     */
    public static boolean addJarFile(final File file) throws MalformedURLException {
        return addJarUrl(file.toURI().toURL());
    }

    /**
     * Loads the jar into the runtime, making its classes available for use
     * @param url url of jar file to load
     * @return true if the jar was loaded; false if it had been loaded previously
     */
    public static boolean addJarUrl(final URL url) {
        if (addedJars.contains(url)) { return false; }

        if (pluginClassLoader == null) {
            pluginClassLoader = new PluginClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
        } else {
            pluginClassLoader.addURL(url);
        }
        addedJars.add(url);
        return true;
    }

    private static class PluginClassLoader extends URLClassLoader {
        PluginClassLoader(final URL[] urls, final ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public void addURL(final URL url) {
            super.addURL(url);
        }
    }
}
