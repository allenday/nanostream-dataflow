package com.theappsolutions.nanostream.output;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;

/**
 * Data class for saving {@link japsa.seq.Sequence} into Firestore database
 */
@DefaultCoder(SerializableCoder.class)
public class SequenceBodyResult implements Serializable {

    private String sequenceBody;

    public SequenceBodyResult(String sequenceBody) {
        this.sequenceBody = sequenceBody;
    }

    public String getSequenceBody() {
        return sequenceBody;
    }
}
