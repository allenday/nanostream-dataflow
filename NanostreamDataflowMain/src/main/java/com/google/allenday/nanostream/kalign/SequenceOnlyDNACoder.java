package com.google.allenday.nanostream.kalign;

import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.coders.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Special {@link Coder} for {@link Sequence} objects
 */
public class SequenceOnlyDNACoder extends CustomCoder<Sequence> {

    private static final Coder<byte[]> BYTE_SEQ_CODER = ByteArrayCoder.of();
    private static final Coder<String> NAME_CODER = StringUtf8Coder.of();
    private static final Coder<String> DESC_CODER = StringUtf8Coder.of();

    @Override
    public void encode(Sequence value, OutputStream outStream) throws IOException {
        BYTE_SEQ_CODER.encode(value.toBytes(), outStream);
        NAME_CODER.encode(value.getName(), outStream);
        DESC_CODER.encode(value.getDesc(), outStream);
        if (!(value.alphabet() instanceof Alphabet.DNA16)) {
            throw new IOException("Alphabet must implement DNA16 for this Coder");
        }
    }

    @Override
    public Sequence decode(InputStream inStream) throws IOException {
        byte[] byteSeq = BYTE_SEQ_CODER.decode(inStream);
        Sequence sequence = new Sequence(Alphabet.DNA(), byteSeq);
        sequence.setName(NAME_CODER.decode(inStream));
        sequence.setDesc(DESC_CODER.decode(inStream));
        return sequence;
    }
}
