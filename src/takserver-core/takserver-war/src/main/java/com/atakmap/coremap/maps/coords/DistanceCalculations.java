
package com.atakmap.coremap.maps.coords;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.hypot;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import tak.server.cot.CotElement;

/**
 * Utility functions to compute distances on the Earth.
 */
public class DistanceCalculations {

    private static final double EARTH_RADIUS = Ellipsoid.WGS_84
            .getSemiMajorAxis();
    private static final String TAG = "DistanceCalculations";
    private static final int MAX_CONVERGENCE_ITERATIONS = 12;

    /**
     * Static methods only.
     */
    private DistanceCalculations() {
        // static only
    }

    /**
     * Returns an angle between -{@linkplain Math#PI PI} and {@linkplain Math#PI PI} equivalent to
     * the specified angle in radians.
     *
     * @param alpha An angle value in radians.
     * @return The angle between between -{@linkplain Math#PI PI} and {@linkplain Math#PI PI}.
     */
    private static double castToAngleRange(final double alpha) {
        return alpha - (2 * PI) * floor(alpha / (2 * PI) + 0.5);
    }

    // /////////////////////////////////////////////////////////////
    // ////// ////////
    // ////// G E O D E T I C M E T H O D S ////////
    // ////// ////////
    // /////////////////////////////////////////////////////////////

    /**
     * Computes the destination point from the {@linkplain CotElement starting point}, given an
     * azimuth and distance in the direction of the destination point.
     * 
     * @param point Starting point of the calculation.
     * @param a Azimuth in degrees (True North)
     * @param distance Distance in meters
     * @return Destination point, if the point is antipodal from the starting point then the
     *         calculation could be off.
     */
    public static CotElement computeDestinationPoint(CotElement point, double a,
                                                   double distance) {
        // Protect internal variables from changes
        final double lat1 = toRadians(point.lat);
        final double long1 = toRadians(point.lon);
        final double azimuth = castToAngleRange(toRadians(a));
        /*
         * Solution of the geodetic direct problem after T.Vincenty. Modified Rainsford's method
         * with Helmert's elliptical terms. Effective in any azimuth and at any distance short of
         * antipodal. Latitudes and longitudes in radians positive North and East. Forward azimuths
         * at both points returned in radians from North. Programmed for CDC-6600 by LCDR L.Pfeifer
         * NGS ROCKVILLE MD 18FEB75 Modified for IBM SYSTEM 360 by John G.Gergen NGS ROCKVILLE MD
         * 7507 Ported from Fortran to Java by Daniele Franzoni. Source:
         * org.geotools.referencing.GeodeticCalcultor see http://geotools.org
         */
        double TU = fo * sin(lat1) / cos(lat1);
        double SF = sin(azimuth);
        double CF = cos(azimuth);
        double BAZ = (CF != 0) ? atan2(TU, CF) * 2.0 : 0;
        double CU = 1 / sqrt(TU * TU + 1.0);
        double SU = TU * CU;
        double SA = CU * SF;
        double C2A = 1.0 - SA * SA;
        double X = sqrt((1.0 / fo / fo - 1) * C2A + 1.0) + 1.0;
        X = (X - 2.0) / X;
        double C = 1.0 - X;
        C = (X * X / 4.0 + 1.0) / C;
        double D = (0.375 * X * X - 1.0) * X;
        TU = distance / fo / semiMajorAxis / C;
        double Y = TU;
        double SY, CY, CZ, E;
        do {
            SY = sin(Y);
            CY = cos(Y);
            CZ = cos(BAZ + Y);
            E = CZ * CZ * 2.0 - 1.0;
            C = Y;
            X = E * CY;
            Y = E + E - 1.0;
            Y = (((SY * SY * 4.0 - 3.0) * Y * CZ * D / 6.0 + X) * D / 4.0 - CZ)
                    * SY * D + TU;
        } while (abs(Y - C) > TOLERANCE_1);
        BAZ = CU * CY * CF - SU * SY;
        C = fo * hypot(SA, BAZ);
        D = SU * CY + CU * SY * CF;
        double lat2 = atan2(D, C);
        C = CU * CY - SU * SY * CF;
        X = atan2(SY * SF, C);
        C = ((-3.0 * C2A + 4.0) * f + 4.0) * C2A * f / 16.0;
        D = ((E * CY * C + CZ) * SY * C + Y) * SA;
        double long2 = long1 + X - (1.0 - C) * D * f;
        long2 = castToAngleRange(long2);

        CotElement point2 = new CotElement();
        point2.lat = toDegrees(lat2);
        point2.lon = toDegrees(long2);
        return point2;
    }

    /**
     * Tolerance factors from the strictest (<code>TOLERANCE_0</CODE>) to the most relax one (
     * <code>TOLERANCE_3</CODE>).
     */
    private static final double TOLERANCE_0 = 5.0e-15, // tol0
            TOLERANCE_1 = 5.0e-14, // tol1
            TOLERANCE_2 = 5.0e-13, // tt
            TOLERANCE_3 = 7.0e-3; // tol2

    /**
     * Tolerance factor for assertions. It has no impact on computed values.
     */
    private static final double TOLERANCE_CHECK = 1E-8;

    /**
     * The encapsulated ellipsoid.
     */
    private final static Ellipsoid ellipsoid;

    /*
     * The semi major axis of the refereced ellipsoid.
     */
    private static final double semiMajorAxis;

    /*
     * The semi minor axis of the refereced ellipsoid.
     */
    private final static double semiMinorAxis;

    /*
     * The eccenticity squared of the refereced ellipsoid.
     */
    private final static double eccentricitySquared;

    /*
     * The maximum orthodromic distance that could be calculated onto the referenced ellipsoid.
     */
    private final static double maxOrthodromicDistance;

    /**
     * GPNARC parameters computed from the ellipsoid.
     */
    private final static double A, B, C, D, E, F;

    /**
     * GPNHRI parameters computed from the ellipsoid. {@code f} if the flattening of the referenced
     * ellipsoid. {@code f2}, {@code f3} and {@code f4} are <var>f<sup>2</sup></var>,
     * <var>f<sup>3</sup></var> and <var>f<sup>4</sup></var> respectively.
     */
    private final static double fo, f, f2, f3, f4;

    /**
     * Parameters computed from the ellipsoid.
     */
    private final static double T1, T2, T4, T6;

    /**
     * Parameters computed from the ellipsoid.
     */
    private static final double a01, a02, a03, a21, a22, a23, a42, a43, a63;

    /**
     * Statically compute all of the associated values from the WGS 84 Ellipsoid
     */
    static {
        ellipsoid = Ellipsoid.WGS_84;
        semiMajorAxis = ellipsoid.getSemiMajorAxis();
        semiMinorAxis = ellipsoid.getSemiMinorAxis();

        /* calculation of GPNHRI parameters */
        f = (semiMajorAxis - semiMinorAxis) / semiMajorAxis;
        fo = 1.0 - f;
        f2 = f * f;
        f3 = f * f2;
        f4 = f * f3;
        eccentricitySquared = f * (2.0 - f);

        /* Calculation of GNPARC parameters */
        final double E2 = eccentricitySquared;
        final double E4 = E2 * E2;
        final double E6 = E4 * E2;
        final double E8 = E6 * E2;
        final double EX = E8 * E2;

        A = 1.0 + 0.75 * E2 + 0.703125 * E4 + 0.68359375 * E6
                + 0.67291259765625 * E8 + 0.6661834716796875 * EX;
        B = 0.75 * E2 + 0.9375 * E4 + 1.025390625 * E6 + 1.07666015625 * E8
                + 1.1103057861328125 * EX;
        C = 0.234375 * E4 + 0.41015625 * E6 + 0.538330078125 * E8
                + 0.63446044921875 * EX;
        D = 0.068359375 * E6 + 0.15380859375 * E8 + 0.23792266845703125 * EX;
        E = 0.01922607421875 * E8 + 0.0528717041015625 * EX;
        F = 0.00528717041015625 * EX;

        maxOrthodromicDistance = semiMajorAxis * (1.0 - E2) * PI * A - 1.0;

        T1 = 1.0;
        T2 = -0.25 * f * (1.0 + f + f2);
        T4 = 0.1875 * f2 * (1.0 + 2.25 * f);
        T6 = 0.1953125 * f3;

        final double a = f3 * (1.0 + 2.25 * f);
        a01 = -f2 * (1.0 + f + f2) / 4.0;
        a02 = 0.1875 * a;
        a03 = -0.1953125 * f4;
        a21 = -a01;
        a22 = -0.25 * a;
        a23 = 0.29296875 * f4;
        a42 = 0.03125 * a;
        a43 = 0.05859375 * f4;
        a63 = 5.0 * f4 / 768.0;
    }
}
