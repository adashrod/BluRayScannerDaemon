package com.aaron.scannerdaemon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Responsible for loading jars at runtime to load plugins and their dependencies.
 */
public class PluginManager {
    private final PluginApi pluginApi;
    private final Collection<String> jarClassNames = new HashSet<>();
    private final List<Plugin> plugins = new ArrayList<>();

    public PluginManager(final PluginApi pluginApi) {
        this.pluginApi = pluginApi;
    }

    /**
     * Loads the jars into the runtime, then searches for classes implementing {@link com.aaron.scannerdaemon.Plugin},
     * instantiates each of them and calls {@link com.aaron.scannerdaemon.Plugin#onLoad(PluginApi)}
     * @param jars Files that refer to jar files to be loaded
     * @return a map of class name to exception for any class that couldn't be instantiated for any reason
     * @throws IOException problem loading jar file
     */
    public Map<String, Throwable> addPluginsFromJars(final File... jars) throws IOException {
        final Map<String, Throwable> errors = new HashMap<>();

        for (final File file: jars) {
            if (!JarLoader.addJarFile(file)) { continue; }
            final JarFile jarFile = new JarFile(file);
            for (final String className: findClassNames(jarFile)) { jarClassNames.add(className); }
        }
        // separate loop because all jars must be loaded before we can try to instantiate a class in case one a class in
        // one jar depends on one in another
        for (final String className: jarClassNames) {
            try {
                instantiate(className);
            } catch (final ClassNotFoundException | IllegalAccessException | InstantiationException | NoClassDefFoundError e) {
                // NCDFE happens if any class in one of the runtime-loaded jars references a class in a jar that wasn't loaded,
                // e.g. user didn't include a 3rd-party jar needed by their plugin
                errors.put(className, e);
            }
        }

        return errors;
    }

    public List<Plugin> getPlugins() {
        plugins.sort((final Plugin o1, final Plugin o2) -> { return Integer.compare(o1.getPriority(), o2.getPriority()); });
        return plugins;
    }

    private Iterable<String> findClassNames(final JarFile jarFile) {
        final Enumeration<JarEntry> jarEntries = jarFile.entries();
        final Collection<String> classNames = new HashSet<>();
        while (jarEntries.hasMoreElements()) {
            final JarEntry jarEntry = jarEntries.nextElement();
            final String entryName = jarEntry.getName();
            if (entryName.endsWith(".class")) {
                classNames.add(entryName.substring(0, entryName.length() - 6).replaceAll("/", "."));
            }
        }
        return classNames;
    }

    /**
     * Instantiates className iff it is a class that implements {@link com.aaron.scannerdaemon.Plugin}, adds it to the
     * list of plugins, and calls {@link com.aaron.scannerdaemon.Plugin#onLoad(PluginApi)}
     * @param className FQ class name
     * @throws ClassNotFoundException shouldn't happen
     * @throws InstantiationException if there's anything wrong with the Plugin implementation
     * @throws IllegalAccessException if the Plugin's default constructor is private
     */
    private void instantiate(final String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        final Class potentialPluginClass;
        potentialPluginClass = Class.forName(className);
        if (potentialPluginClass.getSuperclass().equals(Plugin.class)) {
            final Plugin plugin = (Plugin) potentialPluginClass.newInstance();
            for (final Plugin addedPlugin: plugins) {
                if (addedPlugin.getClass().isAssignableFrom(potentialPluginClass)) { return; }
            }
            plugins.add(plugin);
            plugin.onLoad(pluginApi);
        }
    }
 }
