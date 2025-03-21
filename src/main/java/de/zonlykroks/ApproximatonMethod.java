package de.zonlykroks;

import java.util.function.DoubleUnaryOperator;

record ApproximationMethod(DoubleUnaryOperator function, String name) {
}
