package com.adashrod.scannerdaemon;

import com.adashrod.mkvscanner.DemuxerException;
import com.adashrod.mkvscanner.FileScanner;
import com.adashrod.mkvscanner.UnreadableFileException;
import com.adashrod.mkvscanner.model.Iso639Language;
import com.adashrod.mkvscanner.model.Video;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A mock implementation of a FileScanner that returns static results and doesn't depend on a separate executable file.
 */
public class MockEac3toScanner implements FileScanner {
    private final Logger logger = Logger.getLogger(MockEac3toScanner.class);

    @Override
    public String exec(final File file, final String... strings) throws DemuxerException, IOException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public Video scanAndParseFile(final File file) throws DemuxerException, IOException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public Set<Integer> scanBluRayDir(final File file) throws DemuxerException, IOException {
        final Set<Integer> set = new HashSet<>();
        Collections.addAll(set, 1, 2, 3);
        return set;
    }

    @Override
    public Video scanAndParseBluRayTitle(final File file, final int i) throws DemuxerException, IOException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public Collection<String> demuxBluRayTitleByTracks(final File file, final int i, final Collection<Integer> integers) throws DemuxerException, IOException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public Collection<String> demuxBluRayTitleByLanguages(final File file, final int i, final Collection<Iso639Language> languages) throws DemuxerException, IOException {
        logger.info(String.format("Demuxing title by languages: %s", languages));
        final Collection<String> generatedFilenames = new HashSet<>();
        generatedFilenames.add(file.getName() + "_ti" + i + "_tr1_Chapters.txt");
        generatedFilenames.add(file.getName() + "_ti" + i + "_tr2_und.mkv");
        generatedFilenames.add(file.getName() + "_ti" + i + "_tr3_eng.dts");
        generatedFilenames.add(file.getName() + "_ti" + i + "_tr4_eng.sup");
        return generatedFilenames;
    }

    @Override
    public Collection<String> demuxFileByTracks(final File file, final Collection<Integer> integers) throws DemuxerException, IOException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public Collection<String> demuxFileByLanguages(final File file, final Collection<Iso639Language> languages) throws DemuxerException, IOException {
        logger.info(String.format("Demuxing file by languages: %s", languages));
        if (!file.getName().endsWith(".mkv")) { throw new UnreadableFileException("bleh", "blah", "bluh"); }
        final Collection<String> generatedFilenames = new HashSet<>();
        generatedFilenames.add(file.getName().substring(0, file.getName().indexOf(".mkv")) + "_tr1_und.mkv");
        generatedFilenames.add(file.getName().substring(0, file.getName().indexOf(".mkv")) + "_tr2_eng.dts");
        generatedFilenames.add(file.getName().substring(0, file.getName().indexOf(".mkv")) + "_tr3_eng.sup");
        return generatedFilenames;
    }
}
