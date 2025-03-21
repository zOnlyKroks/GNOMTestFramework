package de.zonlykroks.algorithm.sin;

import de.zonlykroks.ApproximationAlgorithm;
import de.zonlykroks.ApproximationFunction;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

public class SinApproximationFunctions extends ApproximationFunction {
    private static final double MATH_PI = Math.PI;
    private static final double MATH_TWO_PI = 2.0 * Math.PI;
    private static final double MATH_HALF_PI = Math.PI / 2.0;

    private static final double[] CORDIC_TABLE = {
            0.78539816339744830961566084581988,  // atan(2^0)
            0.46364760900080611621425623146121,  // atan(2^-1)
            0.24497866312686415417208248121125,  // atan(2^-2)
            0.12435499454676143503135484916387,  // atan(2^-3)
            0.06241880999595735001266223708923,  // atan(2^-4)
            0.03123983343026827677213224609375,  // atan(2^-5)
            0.01562372862047683143278159022963,  // atan(2^-6)
            0.00781234106010111072490699797697,  // atan(2^-7)
            0.00390623013196697053054907127756,  // atan(2^-8)
            0.00195312251647881851173596536827,  // atan(2^-9)
            0.00097656218955931943040518985934,  // atan(2^-10)
            0.00048828121119489827547633981431,  // atan(2^-11)
            0.00024414062014936176401972135958,  // atan(2^-12)
            0.00012207031189367020424246244476,  // atan(2^-13)
            0.00006103515617420877374873989883,  // atan(2^-14)
            0.00003051757811552610187500593106   // atan(2^-15)
    };

    private static final double CORDIC_K = 0.6072529350088812561694;

    public SinApproximationFunctions() {
        super("Sin Approximations");
        initializeReferenceImplementations();
        initializeApproximationAlgorithms();
    }

    @Override
    public double getDefaultStartRange() {
        return -Math.PI;
    }

    @Override
    public double getDefaultEndRange() {
        return Math.PI;
    }

    @Override
    public List<ApproximationAlgorithm> getApproximationAlgorithms() {
        return List.of(
                new ApproximationAlgorithm("Piecewise 32-bit sine approximation") {
                    @Override
                    public DoubleUnaryOperator getFunction() {
                        return x -> {
                            final float TWO_PI = 6.28318530f;
                            final float PI = 3.14159265f;
                            final float HALF_PI = 1.57079632f;

                            float xFloat = (float) x;

                            float recipTwoPI = 1.0f / TWO_PI;
                            int n = (int)(xFloat * recipTwoPI + (xFloat >= 0 ? 0.5f : -0.5f));
                            float xNormalized = xFloat - n * TWO_PI;

                            if (Math.abs(xNormalized) < 1e-5f) {
                                return xNormalized;
                            }

                            boolean negate = false;
                            if (xNormalized < 0.0f) {
                                xNormalized = -xNormalized;
                                negate = true;
                            }

                            if (xNormalized > PI) {
                                xNormalized = TWO_PI - xNormalized;
                                negate = !negate;
                            }

                            if (xNormalized > HALF_PI) {
                                xNormalized = PI - xNormalized;
                            }

                            final float xSquared = xNormalized * xNormalized;

                            float result;
                            if (xNormalized < 0.5f) {
                                result = xNormalized * (
                                        1.0f - xSquared * (
                                                0.16666666f - xSquared * (
                                                        0.00833333f - xSquared * 0.00019841f
                                                )
                                        )
                                );
                            } else if (xNormalized < 1.3f) {
                                result = xNormalized * (
                                        1.0f - xSquared * (
                                                0.16666667f - xSquared * (
                                                        0.00833333f - xSquared * (
                                                                0.00019841f - xSquared * 0.00000276f
                                                        )
                                                )
                                        )
                                );
                            } else {
                                result = xNormalized * (
                                        1.0f - xSquared * (
                                                0.16666667f - xSquared * (
                                                        0.00833333f - xSquared * (
                                                                0.00019841f - xSquared * (
                                                                        0.00000276f - xSquared * 0.00000002f
                                                                )
                                                        )
                                                )
                                        )
                                );
                            }

                            return negate ? -result : result;
                        };
                    }

                    @Override
                    public String getName() {
                        return "Piecewise 32-bit sine approximation";
                    }
                },

                new ApproximationAlgorithm("CORDIC sine approximation") {
                    @Override
                    public DoubleUnaryOperator getFunction() {
                        return x -> {
                            double angle = x;

                            if (Math.abs(angle - MATH_PI) < 1e-14 || Math.abs(angle + MATH_PI) < 1e-14) {
                                return 0.0;
                            }

                            angle = angle % MATH_TWO_PI;

                            if (angle > MATH_PI) {
                                angle -= MATH_TWO_PI;
                            } else if (angle < -MATH_PI) {
                                angle += MATH_TWO_PI;
                            }

                            int quadrant;
                            if (angle >= 0 && angle <= MATH_HALF_PI) {
                                quadrant = 1;
                            } else if (angle > MATH_HALF_PI && angle <= MATH_PI) {
                                quadrant = 2;
                                angle = MATH_PI - angle;
                            } else if (angle >= -MATH_PI && angle < -MATH_HALF_PI) {
                                quadrant = 3;
                                angle = -MATH_PI - angle;
                            } else {
                                quadrant = 4;
                                angle = -angle;
                            }

                            double x0 = 1.0;
                            double y0 = 0.0;
                            double z = angle;

                            for (int i = 0; i < CORDIC_TABLE.length; i++) {
                                int sign = (z >= 0) ? 1 : -1;

                                double x_temp = x0;
                                double y_temp = y0;

                                double power = 1.0 / (1 << i);
                                x0 = x_temp - sign * y_temp * power;
                                y0 = y_temp + sign * x_temp * power;

                                z = z - sign * CORDIC_TABLE[i];
                            }

                            y0 *= CORDIC_K;

                            return switch (quadrant) {
                                case 1, 2, 3 -> y0;
                                case 4 -> -y0;
                                default -> 0.0;
                            };
                        };
                    }

                    @Override
                    public String getName() {
                        return "CORDIC sine approximation";
                    }
                },

                new ApproximationAlgorithm("Chebyshev polynomial sine approximation") {
                    @Override
                    public DoubleUnaryOperator getFunction() {
                        return x -> {
                            if (Math.abs(x - MATH_PI) < 1e-14 || Math.abs(x + MATH_PI) < 1e-14) {
                                return 0.0;
                            }

                            double xNormalized = x % MATH_TWO_PI;
                            if (xNormalized > MATH_PI) {
                                xNormalized -= MATH_TWO_PI;
                            } else if (xNormalized < -MATH_PI) {
                                xNormalized += MATH_TWO_PI;
                            }

                            double x2 = xNormalized * xNormalized;

                            return xNormalized * (1.0 - x2 * (1.0/6.0 - x2 * (1.0/120.0 - x2 * (1.0/5040.0 - x2/362880.0))));
                        };
                    }

                    @Override
                    public String getName() {
                        return "Chebyshev polynomial sine approximation";
                    }
                }
        );
    }

    @Override
    protected void initializeReferenceImplementations() {
        this.addReferenceImplementation(Math::sin);
    }
}