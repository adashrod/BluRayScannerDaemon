package com.aaron.scannerdaemon;

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
        final URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        final Class<?> sysClass = URLClassLoader.class;

        try {
            final Method method = sysClass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysLoader, url);
            addedJars.add(url);
        } catch (final NoSuchMethodException | IllegalAccessException e) {
            // not possible unless the name of URLClassloader#addURL changes; IllegalAccessException can't happen because of the call to setAccessible
        } catch (final InvocationTargetException e) {
            if (!(e.getCause() instanceof RuntimeException)) {
                e.getCause().printStackTrace();
            } else {
                throw (RuntimeException) e.getCause();
            }
        }
        return true;
    }
}
