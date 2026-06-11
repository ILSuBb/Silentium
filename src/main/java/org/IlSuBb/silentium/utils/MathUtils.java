package org.IlSuBb.silentium.utils;

import java.util.List;

public final class MathUtils {

    private MathUtils() {}

    public static double gcd(double a, double b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > 1e-10) {
            double t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    public static double gcdList(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double result = Math.abs(values.get(0));
        for (int i = 1; i < values.size(); i++) {
            result = gcd(result, Math.abs(values.get(i)));
            if (result < 1e-10) return result;
        }
        return result;
    }

    /** Angle in degrees between two normalised 3D vectors. */
    public static double angleBetween(org.bukkit.util.Vector a, org.bukkit.util.Vector b) {
        double dot = a.dot(b);
        double mags = a.length() * b.length();
        if (mags < 1e-10) return 0.0;
        return Math.toDegrees(Math.acos(clamp(dot / mags, -1.0, 1.0)));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double horizontalDistance(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }
}
