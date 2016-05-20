package com.aaron.scannerdaemon.plugins;

import com.aaron.scannerdaemon.Plugin;
import com.aaron.scannerdaemon.PluginApi;

import java.io.File;
import java.util.Collection;

/**
 * Renames generated files after they've been generated. Specifically:
 * - gets rid of language on video since that's almost always "Undetermined" and not useful
 * - gets rid of language and track number on chapters and adds "Chapters" to the file name for the same reason above and
 *       for clarity
 * - gets rid of language and track number on log file since they come from chapters file and are not relevant
 */
public class FileRenamer extends Plugin {
    private PluginApi pluginApi;

    @Override
    public void onLoad(final PluginApi pluginApi) {
        this.pluginApi = pluginApi;
    }

    @Override
    public void afterScan(final File scannedFile, final Collection<File> generatedFiles) {
        final int lastDot = scannedFile.getName().lastIndexOf('.');
        final String scannedFileBase = scannedFile.getName().substring(0, lastDot != -1 ? lastDot : scannedFile.getName().length());
        for (final File file: generatedFiles) {
            final String absPath = file.getAbsolutePath();
            final String newName;
            if (absPath.matches(".*" + scannedFileBase + "(_ti\\d+)?_tr1_Undetermined.txt")) {
                // assumes that the chapters file is track 1 and language und, which so far has always been true
                newName = absPath.substring(0, absPath.indexOf("tr1_Undetermined")) + "Chapters.txt";
            } else if (absPath.matches(".*" + scannedFileBase + "(_ti\\d+)?_tr\\d+_Undetermined.mkv")) {
                // assumes that the video doesn't have a language, which so far has always been true for BDs
                newName = absPath.substring(0, absPath.indexOf("_Undetermined.mkv")) + ".mkv";
            } else {
                continue;
            }
            final File newFile = new File(newName);
            if (file.renameTo(newFile)) { pluginApi.addToScanRecord(newFile.getName()); }
        }
        final File[] logFiles = scannedFile.getParentFile().listFiles((final File pathname) -> {
            // assumes that the log file is named after the first track
            return pathname.getName().matches(".*" + scannedFileBase + "(_ti\\d+)?_tr\\d.+ - Log.txt");
        });
        for (final File logFile: logFiles) {
            final String absPath = logFile.getAbsolutePath();
            final String newName = absPath.replaceAll("_tr\\d+_Undetermined - Log", "_Log");
            final File newFile = new File(newName);
            if (logFile.renameTo(newFile)) { pluginApi.addToScanRecord(newFile.getName()); }
        }
    }
}
