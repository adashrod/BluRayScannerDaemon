package com.aaron.scannerdaemon.plugins;

import com.aaron.scanner.util.StreamConsumer;
import com.aaron.scanner.util.StringLineIterator;
import com.aaron.scannerdaemon.Plugin;
import com.aaron.scannerdaemon.PluginApi;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tries to find existing instances of the DemuxerDaemon program and kills them
 */
public class ProcessKiller extends Plugin {
    private final int startTimeToleranceMs = 60000;

    @Override
    public void onLoad(final PluginApi pluginApi) {
        final boolean isWindows = System.getProperty("os.name").matches(".*[Ww]indows.*");
        try {
            final Integer pid = findPid(isWindows);
            if (pid != null) { kill(pid, isWindows); }
        } catch (final IOException | InterruptedException ignored) {}
    }

    @Override
    public void afterScan(final File scannedFile, final Collection<File> generatedFiles) {}

    private Integer findPid(final boolean isWindows) throws IOException, InterruptedException {
        if (isWindows) {
            final Process wmicProc = Runtime.getRuntime().exec(new String[]{"WMIC", "path", "win32_process", "get", "Processid,Commandline,CreationDate"});
            final StreamConsumer stdOutConsumer = new StreamConsumer(wmicProc.getInputStream()),
                stdErrConsumer = new StreamConsumer(wmicProc.getErrorStream());
            stdOutConsumer.start();
            stdErrConsumer.start();
            final int ret = wmicProc.waitFor();
            if (ret != 0) { return null; }
            return parseWindowsProcessList(stdOutConsumer.getStreamContent());
        } else {
            final Process psProc = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "ps -efo pid,etimes,args | grep \"DemuxerDaemon.jar\""});
            final StreamConsumer stdOutConsumer = new StreamConsumer(psProc.getInputStream()),
                stdErrConsumer = new StreamConsumer(psProc.getErrorStream());
            stdOutConsumer.start();
            stdErrConsumer.start();
            final int ret = psProc.waitFor();
            if (ret != 0) { return null; }
            return parseLinuxProcessList(stdOutConsumer.getStreamContent());
        }
    }

    private Integer parseWindowsProcessList(final String processList) {
        final Iterator<String> iterator = new StringLineIterator(processList);
        final String headerLine = iterator.next();

        final Function<Integer, Integer> findColumnLength = (final Integer start) -> {
            boolean inSpace = false;
            for (int i = start; i < headerLine.length(); i++) {
                final char c = headerLine.charAt(i);
                if (inSpace) {
                    if (!Character.isWhitespace(c)) { return i - start; }
                } else {
                    if (Character.isWhitespace(c)) { inSpace = true; }
                }
            }
            return headerLine.length() - start;
        };
        final int creationDateIndex = headerLine.indexOf("CreationDate");
        final int pidIndex = headerLine.indexOf("ProcessId");
        final int creationDateLength = findColumnLength.apply(creationDateIndex);
        final int pidLength = findColumnLength.apply(pidIndex);

        final Pattern pattern = Pattern.compile("^.*DemuxerDaemon.jar.*$");
        while (iterator.hasNext()) {
            final String line = iterator.next();
            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                final String creationDateString = line.substring(creationDateIndex, creationDateIndex + creationDateLength).trim();
                final String pidString = line.substring(pidIndex, pidIndex + pidLength).trim();
                final DateFormat wmicDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'.'SSS");
                final Date startTime;
                try {
                    startTime = wmicDateFormat.parse(creationDateString.substring(0, 19));
                } catch (final ParseException pe) {
                    return null;
                }
                if (startTime.before(new Date(System.currentTimeMillis() - startTimeToleranceMs))) {
                    return new Integer(pidString);
                }
            }
        }
        return null;
    }

    private Integer parseLinuxProcessList(final String processList) {
        for (final String line: new StringLineIterator(processList)) {
            if (line.matches(".*java\\s+-jar.*")) {
                final String[] parts = line.split("\\s+");
                final int pid = new Integer(parts[0]);
                final int elapsedS = new Integer(parts[1]);
                if (elapsedS * 1000 > startTimeToleranceMs) { return pid; }
            }
        }
        return null;
    }

    private void kill(final int pid, final boolean isWindows) throws IOException, InterruptedException {
        final String[] killCommand;
        System.out.println("trying to kill process " + pid);
        if (isWindows) {
            killCommand = new String[]{"Taskkill", "/PID", Integer.toString(pid), "/F"};
        } else {
            killCommand = new String[]{"kill", Integer.toString(pid)};
        }
        final Process process = Runtime.getRuntime().exec(killCommand);
        if (process.waitFor() == 0) {
            System.out.println("Successfully killed other daemon process, pid: " + pid);
        } else {
            System.out.println("Failed to kill other daemon process, pid: " + pid);
        }
    }
}
