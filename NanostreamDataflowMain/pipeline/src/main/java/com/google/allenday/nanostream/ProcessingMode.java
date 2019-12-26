package com.google.allenday.nanostream;

import java.util.stream.Stream;

public enum ProcessingMode {
    SPECIES("species"),
    RESISTANT_GENES("resistance_genes");

    public final String label;

    ProcessingMode(String label) {
        this.label = label;
    }

    public static ProcessingMode findByLabel(String label) {
        return Stream.of(ProcessingMode.values()).filter(
                processingMode -> processingMode.label.equals(label))
                .findFirst().orElse(SPECIES);
    }
}
