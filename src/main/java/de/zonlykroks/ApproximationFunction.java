package de.zonlykroks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public abstract class ApproximationFunction {
    private final String name;
    private final Map<String, DoubleUnaryOperator> referenceImplementations = new HashMap<>();

    public ApproximationFunction(String name) {
        this.name = name;
        initializeReferenceImplementations();
        initializeApproximationAlgorithms();
    }

    public String getName() {
        return name;
    }

    public Map<String, DoubleUnaryOperator> getReferenceImplementations() {
        return referenceImplementations;
    }

    public abstract double getDefaultStartRange();

    public abstract double getDefaultEndRange();

    public abstract List<ApproximationAlgorithm> getApproximationAlgorithms();

    protected abstract void initializeReferenceImplementations();

    protected void initializeApproximationAlgorithms() {}

    protected void addReferenceImplementation(DoubleUnaryOperator function) {
        referenceImplementations.put("sin", function);
    }
}
