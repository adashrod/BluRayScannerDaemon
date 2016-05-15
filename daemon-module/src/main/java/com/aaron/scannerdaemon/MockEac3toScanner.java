package com.aaron.scannerdaemon;

import com.aaron.scanner.DemuxerException;
import com.aaron.scanner.FileScanner;
import com.aaron.scanner.model.Video;

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
        Collections.addAll(set, 1, 2, 3, 4);
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
    public Collection<String> demuxBluRayTitleByLanguages(final File file, final int i, final Collection<String> collection) throws DemuxerException, IOException {
        final Collection<String> generatedFilenames = new HashSet<>();
        generatedFilenames.add(file.getName() + "_ti_" + i + "_video");
        generatedFilenames.add(file.getName() + "_ti_" + i + "_audio");
        generatedFilenames.add(file.getName() + "_ti_" + i + "_subtitle");
        return generatedFilenames;
    }

    @Override
    public Collection<String> demuxFileByTracks(final File file, final Collection<Integer> integers) throws DemuxerException, IOException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public Collection<String> demuxFileByLanguages(final File file, final Collection<String> collection) throws DemuxerException, IOException {
        final Collection<String> generatedFilenames = new HashSet<>();
        generatedFilenames.add(file.getName() + "_video");
        generatedFilenames.add(file.getName() + "_audio");
        generatedFilenames.add(file.getName() + "_subtitle");
        return generatedFilenames;
    }
}
