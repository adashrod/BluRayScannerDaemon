package com.aaron.scannerdaemon;

import com.aaron.scanner.CorruptBluRayStructureException;
import com.aaron.scanner.DemuxerException;
import com.aaron.scanner.Eac3toScanner;
import com.aaron.scanner.FileScanner;
import com.aaron.scanner.FormatConversionException;
import com.aaron.scanner.NotBluRayDirectoryException;
import com.aaron.scanner.UnreadableFileException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A daemon that runs until killed, periodically scanning the contents of a directory and attempts to demux them into
 * separate audio, video, and subtitle files.
 */
public class Daemon {
    private final Logger logger = Logger.getLogger(Daemon.class);

    private FileScanner fileScanner;
    private File dirToScan;
    private final Collection<String> languages = new HashSet<>();
    private final String scanRecordName = "scanRecord.txt";
    private ScanRecord scanRecord;
    private final File scanRecordFile = new File(dirToScan.getPath() + File.separator + scanRecordName);
    private long sleepTimeMs;
    private int maxRetries;

    public Daemon() {
        try {
            loadProperties();
        } catch (final FileNotFoundException fnf) {
            logger.fatal("Properties file not found.");
            final InputStream sampleStream = getClass().getResourceAsStream("/daemon.properties.sample");
            if (sampleStream == null) {
                logger.fatal("problem with jar; can't find daemon.properties.sample");
                System.exit(-1);
            }
            try {
                Files.copy(sampleStream, new File("daemon.properties").toPath());
            } catch (final IOException ioe) {
                logger.fatal("failed to create daemon.properties from sample file", ioe);
                System.exit(-1);
            }
            logger.info("Created sample properties file.");
            System.exit(1);
        } catch (final IOException ioe) {
            logger.fatal("Failed to load properties file or scanRecord file: " + ioe.getMessage());
            System.exit(-1);
        } catch (final IllegalArgumentException iae) {
            logger.fatal(iae.getMessage());
            System.exit(1);
        }
    }

    public void start() {
        logger.info("starting daemon");
        while (true) {
            final File[] files = dirToScan.listFiles();
            if (files != null) {
                boolean scannedAtLeastOneFile = false;

                for (final File file: files) {
                    // skip scanning the scan record file and any failed BD dir or failed/succeeded mkv file
                    if (isExemptFile(file) || isExemptFromScan(file, null)) { continue; }

                    if (file.isDirectory()) {
                        final Set<Integer> titleNumbers = scanBluRayDir(file);
                        if (titleNumbers == null) { continue; }
                        for (final int titleNumber: titleNumbers) {
                            if (isExemptFromScan(file, titleNumber)) { continue; }
                            scannedAtLeastOneFile = demuxTitle(file, titleNumber);
                        }
                    } else {
                        scannedAtLeastOneFile = demuxFile(file);
                    }
                }

                if (scannedAtLeastOneFile) { logger.info("finished scanning; sleeping"); }
                scanRecord.writeToFile();

                try {
                    Thread.sleep(sleepTimeMs);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.info("daemon sleep was interrupted");
                    break;
                }
            }
        }
    }

    private void loadProperties() throws IOException {
        final Properties properties = new Properties();
        final Consumer<String> check = (final String propName) -> {
            final String prop = properties.getProperty(propName);
            if (prop == null || prop.isEmpty()) {
                throw new IllegalArgumentException(String.format("invalid %s value in properties file", propName));
            }
        };
        properties.load(new FileReader(new File("daemon.properties")));

        final String dirProp = properties.getProperty("dirToScan");
        check.accept("dirToScan");
        dirToScan = new File(dirProp);

        final String execProp = properties.getProperty("eac3toExecutable");
        check.accept("eac3toExecutable");
        if (execProp.equals("mock")) {
            fileScanner = new MockEac3toScanner();
        } else {
            fileScanner = new Eac3toScanner(execProp, dirToScan);
        }

        final String maxRetriesProp = properties.getProperty("maxRetries");
        check.accept("maxRetries");
        maxRetries = Integer.parseInt(maxRetriesProp);

        final File scanRecordFile = new File(dirToScan.getPath() + File.separator + scanRecordName);
        scanRecordFile.createNewFile();
        scanRecord = new ScanRecord(scanRecordFile, maxRetries);


        final String languagesProp = properties.getProperty("languages");
        // todo: change this behavior to treat missing as "get all tracks"
        check.accept("languages");
        Collections.addAll(languages, languagesProp.split("\\s*,\\s*"));

        final String sleepTimeMinutesProp = properties.getProperty("sleepTimeMinutes");
        check.accept("sleepTimeMinutes");
        sleepTimeMs = 60 * 1000 * Integer.parseInt(sleepTimeMinutesProp); // m -> ms
    }

    /**
     * Scans a BD dir to find the title numbers contained in the BD.
     * @param bluRayDir the directory containing the BD
     * @return a set of title numbers available on the BD. Returns null if anything went wrong in the scanning, e.g. if
     * the directory is not a BD dir or is corrupt.
     */
    private Set<Integer> scanBluRayDir(final File bluRayDir) {
        try {
            return fileScanner.scanBluRayDir(bluRayDir);
        } catch (final NotBluRayDirectoryException nbde) {
            // might or might not be an error (corrupt BD dir or just any dir that shouldn't be scanned)
            logger.warn(String.format("tried to scan %s as BD dir, demuxer doesn't recognize it as such", bluRayDir.getName()));
        } catch (final DemuxerException de) {
            logger.error(String.format("failed to scan %s directory as BD dir, demuxer output=%s", bluRayDir.getName(), de.getDemuxerOutput()));
        } catch (final IOException ioe) {
            logger.error(String.format("failed to scan %s directory: IOException: %s", bluRayDir.getName(), ioe.getMessage()));
        }
        scanRecord.addFailure(bluRayDir.getName());
        if (scanRecord.containsAbandoned(bluRayDir.getName(), null)) {
            logger.error(String.format("Failed to scan %s directory the max number of times", bluRayDir.getName()));
        }
        return null;
    }

    /**
     * Demuxes a specific title on a BD directory. Returns false if anything went wrong.
     * @param bluRayDir   the directory containing the BD
     * @param titleNumber which title to demux
     * @return success
     */
    private boolean demuxTitle(final File bluRayDir, final int titleNumber) {
        try {
            final Collection<String> generatedFilenames =
                fileScanner.demuxBluRayTitleByLanguages(bluRayDir, titleNumber, languages);
            scanRecord.addSuccess(bluRayDir.getName(), titleNumber);
            generatedFilenames.forEach(scanRecord::addSuccess);
            return true;
        } catch (final CorruptBluRayStructureException cbse) {
            logger.error(String.format("was able to scan %s dir, but unable to scan title %d: %s",
                bluRayDir.getName(), titleNumber, cbse.getDemuxerOutput()));
        } catch (final FormatConversionException fce) {
            // shouldn't happen unless there's a bug in MkvScannerDemuxer
            logger.error(String.format("possible bug: demuxer attempted a bad format conversion or something else went wrong. arguments=%s\noutput=%s", fce.getArguments(), fce.getDemuxerOutput()));
        } catch (final DemuxerException de) {
            logger.error(String.format("failed to demux BD title, arguments=%s\noutput=%s", de.getArguments(), de.getDemuxerOutput()));
        } catch (final IOException ioe) {
            logger.error(String.format("failed to demux BD title, dir=%s, title=%d: IOException: %s", bluRayDir.getName(), titleNumber, ioe.getMessage()));
        }
        scanRecord.addFailure(bluRayDir.getName(), titleNumber);
        if (scanRecord.containsAbandoned(bluRayDir.getName(), titleNumber)) {
            logger.error(String.format("Failed to demux %s title %d the max number of times", bluRayDir.getName(), titleNumber));
        }
        return false;
    }

    /**
     * Demuxes a file, such as an MKV. Returns false if anything went wrong.
     * @param containerFile the file to demux
     * @return success
     */
    private boolean demuxFile(final File containerFile) {
        try {
            final Collection<String> generatedFilenames = fileScanner.demuxFileByLanguages(containerFile, languages);
            scanRecord.addSuccess(containerFile.getName());
            generatedFilenames.forEach(scanRecord::addSuccess);
            return true;
        } catch (final UnreadableFileException ufe) {
            logger.warn(String.format("failed to scan %s as video container file\n\t%s", containerFile.getName(), ufe.getDemuxerOutput()));
        } catch (final FormatConversionException fce) {
            // shouldn't happen unless there's a bug in MkvScannerDemuxer
            logger.error(String.format("possible bug: daemon attempted a bad format conversion or something else went wrong. arguments=%s\noutput=%s", fce.getArguments(), fce.getDemuxerOutput()));
        } catch (final DemuxerException de) {
            logger.error(String.format("failed to demux file, file=%s, arguments=%s\noutput=%s",
                containerFile.getName(), de.getArguments(), de.getDemuxerOutput()));
        } catch (final IOException ioe) {
            logger.error(String.format("failed to demux file, file=%s, IOException: %s", containerFile.getName(), ioe.getMessage()));
        }
        scanRecord.addFailure(containerFile.getName());
        if (scanRecord.containsAbandoned(containerFile.getName(), null)) {
            logger.error(String.format("Failed to demux %s the max number of times", containerFile.getName()));
        }
        return false;
    }

    /**
     * Tests if the file is one that shouldn't be scanned at all, such as the scan record file or a scan log from the
     * demuxer
     * @param file the file to possibly be scanned
     * @return true if file shouldn't be scanned
     */
    private boolean isExemptFile(final File file) {
        return file.equals(scanRecordFile) || file.getName().endsWith(" - Log.txt");
    }

    /**
     * Returns true for files/dirs/titles that are exempt from scanning because they've already been scanned or have
     * failed too many times.
     * @param file        the file to possibly be scanned
     * @param titleNumber the title number if checking a BD title, null otherwise
     * @return true if the file should not be scanned: this happens if the file has been successfully scanned or if it
     * has failed to be scanned the max number of times
     */
    private boolean isExemptFromScan(final File file, final Integer titleNumber) {
        return scanRecord.containsSuccess(file.getName(), titleNumber) || scanRecord.containsAbandoned(file.getName(), titleNumber);
    }

    public static void main(final String[] args) {
        new Daemon().start();
    }
}
