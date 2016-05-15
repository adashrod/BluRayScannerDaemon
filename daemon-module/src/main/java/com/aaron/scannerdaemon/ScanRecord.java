package com.aaron.scannerdaemon;

import javafx.util.Pair;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A class that represents the scan record text file. Each line in the file is formatted "filename|titleNumber|attempts".
 * The filename is the name of the file or directory that was scanned. The titleNumber is the title that was scanned in
 * the case of a blu-ray directory (will be null or empty for mkv files). The attempts is the number of times that the
 * daemon tried (and failed) to scan the file; a special value of -1 means that the file was successfully scanned.
 */
public class ScanRecord {
    /**
     * In the log file, this value in the "tries" column represents a successful pass
     */
    private static final int SUCCESS_VALUE = -1;
    /**
     * In the log file, this value in the "tries" column represents a file that has failed the max number of times and
     * should be further ignored
     */
    private static final int ABANDONED_VALUE = -2;

    private final Logger logger = Logger.getLogger(ScanRecord.class);

    private final File file;
    private final int maxRetries;
    private final Map<LogLine, Integer> logs = new TreeMap<>();

    public ScanRecord(final File file, final int maxRetries) throws IOException {
        this.file = file;
        this.maxRetries = maxRetries;
        reload();
    }

    public void reload() throws IOException {
        file.createNewFile();
        logs.clear();
        final BufferedReader bufferedReader;
        try (FileReader fileReader = new FileReader(file)) {
            bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    final Pair<LogLine, Integer> pair = LogLine.parseLine(line.trim());
                    logs.put(pair.getKey(), pair.getValue());
                } catch (final IllegalArgumentException ignored) {}
            }
        }
    }

    /**
     * Returns true if the scan record contains a record of the file and title number having successfully completed
     * @param filename    which file to check status of
     * @param titleNumber title number, if any, to check status of. null for BD dirs or MKV files
     * @return true if success has been recorded
     */
    public boolean containsSuccess(final String filename, final Integer titleNumber) {
        final Integer attempts = logs.get(new LogLine(filename, titleNumber));
        return attempts != null && attempts == SUCCESS_VALUE;
    }

    /**
     * Returns true if the scan record contains a record of the file and title number having failed the max number of times
     * @param filename    which file to check status of
     * @param titleNumber title number, if any, to check status of. null for BD dirs or MKV files
     * @return true if complete failure has been recorded
     */
    public boolean containsAbandoned(final String filename, final Integer titleNumber) {
        final Integer attempts = logs.get(new LogLine(filename, titleNumber));
        return attempts != null && attempts == ABANDONED_VALUE;
    }

    public void addSuccess(final String filename) {
        logs.put(new LogLine(filename, null), SUCCESS_VALUE);
    }

    public void addSuccess(final String filename, final Integer titleNumber) {
        logs.put(new LogLine(filename, titleNumber), SUCCESS_VALUE);
    }

    public void addFailure(final String filename) {
        addFailure(filename, null);
    }

    public void addFailure(final String filename, final Integer titleNumber) {
        final LogLine logLine = new LogLine(filename, titleNumber);
        if (logs.containsKey(logLine)) {
            logs.put(logLine, logs.get(logLine) + 1);
        } else {
            logs.put(logLine, 1);
        }
        final int attempts = logs.get(logLine);
        if (attempts >= maxRetries) {
            logs.put(logLine, ABANDONED_VALUE);
        }
    }

    public void writeToFile() {
        try (FileWriter fileWriter = new FileWriter(file, false)) {
            for (final Map.Entry<LogLine, Integer> entry: logs.entrySet()) {
                fileWriter.write(LogLine.writeLine(entry.getKey(), entry.getValue()));
            }
            fileWriter.flush();
        } catch (final IOException e) {
            logger.warn(String.format("Failed to write scan record file: %s", e.getMessage()), e);
        }
    }

    public static class LogLine implements Comparable<LogLine> {
        private final String filename;
        private final Integer titleNumber;

        public LogLine(final String filename, final Integer titleNumber) {
            this.filename = filename;
            this.titleNumber = titleNumber;
        }

        public static Pair<LogLine, Integer> parseLine(final String str) {
            final String filename;
            final Integer titleNumber, attempts;
            final String[] parts = str.split("\\|");
            if (parts.length != 3) {
                throw new IllegalArgumentException(String.format("Can't parse %s. Format must be \"filename|titleNumber|attempts\"", str));
            }
            filename = parts[0];
            titleNumber = parts[1].isEmpty() || parts[1].equals("null") ? null : Integer.parseInt(parts[1]);
            attempts = parts[2].isEmpty() || parts[2].equals("null") ? 0 : Integer.parseInt(parts[2]);
            return new Pair<>(new LogLine(filename, titleNumber), attempts);
        }

        public static String writeLine(final LogLine logLine, final Integer attempts) {
            return String.format("%s|%d\n", logLine, attempts);
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof LogLine)) {
                return false;
            }
            final LogLine logLine = (LogLine) obj;
            return Objects.equals(filename, logLine.filename) && Objects.equals(titleNumber, logLine.titleNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filename, titleNumber);
        }

        @Override
        public String toString() {
            return String.format("%s|%d", filename, titleNumber);
        }

        @Override
        public int compareTo(final LogLine logLine) {
            final int f = filename.compareTo(logLine.filename);
            if (f != 0) { return f; }
            return Integer.compare(titleNumber != null ? titleNumber : 0, logLine.titleNumber != null ? logLine.titleNumber : 0);
        }
    }
}
