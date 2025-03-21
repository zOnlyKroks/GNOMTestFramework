package de.zonlykroks;

import java.util.function.DoubleUnaryOperator;

public abstract class ApproximationAlgorithm {
    private final String name;

    public ApproximationAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract DoubleUnaryOperator getFunction();
}
