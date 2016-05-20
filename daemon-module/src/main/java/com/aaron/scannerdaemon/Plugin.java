package com.aaron.scannerdaemon;

import java.io.File;
import java.util.Collection;

/**
 * An interface for creating application plugins. Plugin authors can create implementations of this class, package them
 * in jar(s), and put those jars in a plugins directory where the app is. They will then automatically be loaded at
 * runtime and invoked at the appropriate times.
 */
public abstract class Plugin {

    /**
     * Implementations can define a priority value to determine what order plugins get run in. Lower values mean higher
     * priority, e.g. priority 0 runs before priority 10. Default value if not overridden is {@link Integer#MAX_VALUE}
     * @return how high of a priority the plugin should be
     */
    public int getPriority() { return Integer.MAX_VALUE; }

    /**
     * Gets called when the plugin is first instantiated. The one parameter is an API that can be used to interact with
     * the main program.
     * @param pluginApi the API to the program
     */
    public abstract void onLoad(PluginApi pluginApi);

    /**
     * Gets called after a successful scan of a BD dir/mkv file. I.e. it gets called once for an mkv file, and once for
     * a BD dir, no matter how many titles the BD dir has.
     * @param scannedFile    the file that was scanned (the mkv file or BD directory)
     * @param generatedFiles all video/audio/chapter/subtitle files generated by the demux
     */
    public abstract void afterScan(File scannedFile, Collection<File> generatedFiles);
}
