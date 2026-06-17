package com.adashrod.scannerdaemon;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;

public class JarLoader {
    private static final Collection<URL> addedJars = new HashSet<>();
    private static URLClassLoader pluginClassLoader;

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

        try {
            if (pluginClassLoader == null) {
                pluginClassLoader = new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
                Thread.currentThread().setContextClassLoader(pluginClassLoader);
            } else {
                final Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrlMethod.setAccessible(true);
                addUrlMethod.invoke(pluginClassLoader, url);
            }
            addedJars.add(url);
        } catch (final NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (final InvocationTargetException e) {
            if (!(e.getCause() instanceof RuntimeException)) {
                e.getCause().printStackTrace();
            } else {
                throw (RuntimeException) e.getCause();
            }
            return false;
        }
        return true;
    }
}
