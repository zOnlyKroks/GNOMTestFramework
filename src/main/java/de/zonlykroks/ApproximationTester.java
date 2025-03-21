package de.zonlykroks;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public class ApproximationTester {
    private final String functionName;
    private DoubleUnaryOperator referenceFunction;
    private String referenceName;
    private final Map<String, DoubleUnaryOperator> approximations = new HashMap<>();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.########");

    public ApproximationTester(String functionName) {
        this.functionName = functionName;
    }

    public void setReferenceFunction(DoubleUnaryOperator function, String name) {
        this.referenceFunction = function;
        this.referenceName = name;
    }

    public void registerApproximation(DoubleUnaryOperator function, String name) {
        approximations.put(name, function);
    }

    public void testRange(double start, double end, int points, boolean reportWorst) {
        if (referenceFunction == null) {
            System.out.println("Error: Reference function not set");
            return;
        }

        if (approximations.isEmpty()) {
            System.out.println("Error: No approximation methods registered");
            return;
        }

        double step = (end - start) / points;

        System.out.println("========== ACCURACY TEST RESULTS ==========");
        System.out.println("Function: " + functionName);
        System.out.println("Reference: " + referenceName);
        System.out.println("Range: [" + start + ", " + end + "]");
        System.out.println("Test points: " + points);
        System.out.println("===========================================");

        for (Map.Entry<String, DoubleUnaryOperator> entry : approximations.entrySet()) {
            String approxName = entry.getKey();
            DoubleUnaryOperator approxFunction = entry.getValue();

            System.out.println("\nTesting: " + approxName);
            System.out.println("-------------------------------------");

            double totalError = 0.0;
            double maxError = 0.0;
            double maxErrorInput = 0.0;
            double maxRelativeError = 0.0;
            double maxRelativeErrorInput = 0.0;

            for (int i = 0; i < points; i++) {
                double x = start + i * step;
                double referenceValue = referenceFunction.applyAsDouble(x);
                double approximationValue = approxFunction.applyAsDouble(x);

                double absError = Math.abs(referenceValue - approximationValue);
                totalError += absError;

                if (absError > maxError) {
                    maxError = absError;
                    maxErrorInput = x;
                }

                if (Math.abs(referenceValue) > 1e-10) {
                    double relativeError = absError / Math.abs(referenceValue);
                    if (relativeError > maxRelativeError) {
                        maxRelativeError = relativeError;
                        maxRelativeErrorInput = x;
                    }
                }
            }

            double avgError = totalError / points;

            System.out.println("Average absolute error: " + DECIMAL_FORMAT.format(avgError));
            System.out.println("Maximum absolute error: " + DECIMAL_FORMAT.format(maxError));
            System.out.println("Maximum relative error: " + DECIMAL_FORMAT.format(maxRelativeError * 100) + "%");

            if (reportWorst) {
                System.out.println("\nWorst cases:");
                System.out.println("Max abs error at x = " + DECIMAL_FORMAT.format(maxErrorInput));
                System.out.println("  Reference: " + DECIMAL_FORMAT.format(referenceFunction.applyAsDouble(maxErrorInput)));
                System.out.println("  Approximation: " + DECIMAL_FORMAT.format(approxFunction.applyAsDouble(maxErrorInput)));

                System.out.println("Max rel error at x = " + DECIMAL_FORMAT.format(maxRelativeErrorInput));
                System.out.println("  Reference: " + DECIMAL_FORMAT.format(referenceFunction.applyAsDouble(maxRelativeErrorInput)));
                System.out.println("  Approximation: " + DECIMAL_FORMAT.format(approxFunction.applyAsDouble(maxRelativeErrorInput)));
            }
        }
    }

    public void testPerformance(int iterations) {
        if (referenceFunction == null) {
            System.out.println("Error: Reference function not set");
            return;
        }

        if (approximations.isEmpty()) {
            System.out.println("Error: No approximation methods registered");
            return;
        }

        System.out.println("========== PERFORMANCE TEST RESULTS ==========");
        System.out.println("Function: " + functionName);
        System.out.println("Reference: " + referenceName);
        System.out.println("Iterations: " + iterations);
        System.out.println("=============================================");

        double[] testValues = new double[1000];
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = Math.random() * 10;
        }

        long startTime = System.nanoTime();
        double refSum = 0;
        for (int i = 0; i < iterations; i++) {
            double x = testValues[i % testValues.length];
            refSum += referenceFunction.applyAsDouble(x);
        }
        long refTime = System.nanoTime() - startTime;

        System.out.println("\nReference implementation (" + referenceName + ")");
        System.out.println("Time: " + (refTime / 1_000_000.0) + " ms");
        System.out.println("Dummy sum: " + refSum + " (prevents optimization)");

        for (Map.Entry<String, DoubleUnaryOperator> entry : approximations.entrySet()) {
            String approxName = entry.getKey();
            DoubleUnaryOperator approxFunction = entry.getValue();

            System.out.println("\nTesting: " + approxName);
            System.out.println("-------------------------------------");

            startTime = System.nanoTime();
            double approxSum = 0;
            for (int i = 0; i < iterations; i++) {
                double x = testValues[i % testValues.length];
                approxSum += approxFunction.applyAsDouble(x);
            }
            long approxTime = System.nanoTime() - startTime;

            System.out.println("Time: " + (approxTime / 1_000_000.0) + " ms");
            System.out.println("Speedup factor: " + DECIMAL_FORMAT.format((double) refTime / approxTime) + "x");
            System.out.println("Dummy sum: " + approxSum + " (prevents optimization)");
        }
    }
}
