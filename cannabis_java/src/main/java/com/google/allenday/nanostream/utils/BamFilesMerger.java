/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.google.allenday.nanostream.utils;

import htsjdk.samtools.*;
import htsjdk.samtools.util.*;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BamFilesMerger implements Serializable {
    private static final Log log = Log.getInstance(BamFilesMerger.class);

    private SAMFileHeader.SortOrder SORT_ORDER = SAMFileHeader.SortOrder.coordinate;
    private boolean ASSUME_SORTED = false;
    private boolean MERGE_SEQUENCE_DICTIONARIES = false;
    private boolean USE_THREADING = false;
    private List<String> COMMENT = new ArrayList<>();
    private File INTERVALS = null;

    private static final int PROGRESS_INTERVAL = 1000000;

    public int merge(List<Path> inputPaths, String outputFileName) {
        boolean matchedSortOrders = true;

        // read interval list if it is defined
        final List<Interval> intervalList = (INTERVALS == null ? null : IntervalList.fromFile(INTERVALS).uniqued().getIntervals());
        // map reader->iterator used if INTERVALS is defined
        final Map<SamReader, CloseableIterator<SAMRecord>> samReaderToIterator = new HashMap<>(inputPaths.size());

        // Open the files for reading and writing
        final List<SamReader> readers = new ArrayList<>();
        final List<SAMFileHeader> headers = new ArrayList<>();
        {
            SAMSequenceDictionary dict = null; // Used to try and reduce redundant SDs in memory

            for (final Path inFile : inputPaths) {
                IOUtil.assertFileIsReadable(inFile);
                final SamReader in = SamReaderFactory.makeDefault().referenceSequence(Defaults.REFERENCE_FASTA).open(inFile);
                if (INTERVALS != null) {
                    if (!in.hasIndex()) {
                        throw new RuntimeException("Merging with interval but BAM file is not indexed: " + inFile);
                    }
                    final CloseableIterator<SAMRecord> samIterator = new SamRecordIntervalIteratorFactory().makeSamRecordIntervalIterator(in, intervalList, true);
                    samReaderToIterator.put(in, samIterator);
                }

                readers.add(in);
                headers.add(in.getFileHeader());

                // A slightly hackish attempt to keep memory consumption down when merging multiple files with
                // large sequence dictionaries (10,000s of sequences). If the dictionaries are identical, then
                // replace the duplicate copies with a single dictionary to reduce the memory footprint. 
                if (dict == null) {
                    dict = in.getFileHeader().getSequenceDictionary();
                } else if (dict.equals(in.getFileHeader().getSequenceDictionary())) {
                    in.getFileHeader().setSequenceDictionary(dict);
                }

                matchedSortOrders = matchedSortOrders && in.getFileHeader().getSortOrder() == SORT_ORDER;
            }
        }

        // If all the input sort orders match the output sort order then just merge them and
        // write on the fly, otherwise setup to merge and sort before writing out the final file
        File outputFile = new File(outputFileName);
        IOUtil.assertFileIsWritable(new File(outputFileName));
        final boolean presorted;
        final SAMFileHeader.SortOrder headerMergerSortOrder;
        final boolean mergingSamRecordIteratorAssumeSorted;

        if (matchedSortOrders || SORT_ORDER == SAMFileHeader.SortOrder.unsorted || ASSUME_SORTED || INTERVALS != null) {
            log.info("Input files are in same order as output so sorting to temp directory is not needed.");
            headerMergerSortOrder = SORT_ORDER;
            mergingSamRecordIteratorAssumeSorted = ASSUME_SORTED;
            presorted = true;
        } else {
            log.info("Sorting input files using temp directory ");
            headerMergerSortOrder = SAMFileHeader.SortOrder.unsorted;
            mergingSamRecordIteratorAssumeSorted = false;
            presorted = false;
        }
        final SamFileHeaderMerger headerMerger = new SamFileHeaderMerger(headerMergerSortOrder, headers, MERGE_SEQUENCE_DICTIONARIES);
        final MergingSamRecordIterator iterator;
        // no interval defined, get an iterator for the whole bam
        if (intervalList == null) {
            iterator = new MergingSamRecordIterator(headerMerger, readers, mergingSamRecordIteratorAssumeSorted);
        } else {
            // show warning related to https://github.com/broadinstitute/picard/pull/314/files
            log.info("Warning: merged bams from different interval lists may contain the same read in both files");
            iterator = new MergingSamRecordIterator(headerMerger, samReaderToIterator, true);
        }
        final SAMFileHeader header = headerMerger.getMergedHeader();
        for (final String comment : COMMENT) {
            header.addComment(comment);
        }
        header.setSortOrder(SORT_ORDER);
        final SAMFileWriterFactory samFileWriterFactory = new SAMFileWriterFactory();
        if (USE_THREADING) {
            samFileWriterFactory.setUseAsyncIo(true);
        }
        final SAMFileWriter out = samFileWriterFactory.makeSAMOrBAMWriter(header, presorted, outputFile);

        // Lastly loop through and write out the records
        final ProgressLogger progress = new ProgressLogger(log, PROGRESS_INTERVAL);
        while (iterator.hasNext()) {
            final SAMRecord record = iterator.next();
            out.addAlignment(record);
            progress.record(record);
        }

        log.info("Finished reading inputs.");
        for (final CloseableIterator<SAMRecord> iter : samReaderToIterator.values()) CloserUtil.close(iter);
        CloserUtil.close(readers);
        out.close();
        return 0;
    }
}
