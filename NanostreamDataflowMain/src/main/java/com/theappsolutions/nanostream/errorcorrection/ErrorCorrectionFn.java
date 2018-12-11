package com.theappsolutions.nanostream.errorcorrection;

import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import japsa.seq.SequenceBuilder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Provides sequence merging operation that creates one final sequence from multiple parts
 */
public class ErrorCorrectionFn extends DoFn<KV<String, Iterable<Sequence>>, KV<String, Sequence>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        List<Sequence> seqList = StreamSupport.stream(c.element().getValue().spliterator(), false)
                .collect(Collectors.toList());

        if (seqList.size() == 1) {
            c.output(KV.of(c.element().getKey(), seqList.get(0)));
            return;
        }

        int[] seqCoeficients = new int[seqList.size()];
        Arrays.fill(seqCoeficients, 1);


        SequenceBuilder sb = new SequenceBuilder(Alphabet.DNA(), seqList.get(0).length());
        for (int baseIndex = 0; baseIndex < seqList.get(0).length(); baseIndex++) {
            Rating rating = Rating.init();
            for (int seqIndex = 0; seqIndex < seqList.size(); seqIndex++) {
                byte base = seqList.get(seqIndex).getBase(baseIndex);
                rating.count(base, seqCoeficients[seqIndex]);
            }
            int maxIdx = rating.getMax();
            if (maxIdx < Alphabet.DNA.GAP) {
                sb.append((byte) maxIdx);
            }
        }
        c.output(KV.of(c.element().getKey(), sb.toSequence()));
    }

    private static class Rating {

        private final static int ALPHABET_TOP_LIMIT = 6;

        private Map<Byte, Integer> ratingMap = new HashMap<>();
        private byte N_SYMBOL = (byte) Alphabet.DNA().char2int('N');

        private Rating() {
            for (byte i = 0; i < ALPHABET_TOP_LIMIT; i++) {
                ratingMap.put(i, 0);
            }
        }

        static Rating init() {
            return new Rating();
        }

        void count(byte base, int coefficient) {
            incrementElement(base >= ALPHABET_TOP_LIMIT ? N_SYMBOL : base, coefficient);
        }

        private void incrementElement(byte base, int coefficient) {
            ratingMap.put(base, ratingMap.get(base) + coefficient);
        }

        byte getMax() {
            Map.Entry<Byte, Integer> maxEntry = null;
            for (Map.Entry<Byte, Integer> entry : ratingMap.entrySet()) {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                    maxEntry = entry;
                }
            }
            assert maxEntry != null;
            return maxEntry.getKey();
        }
    }
}