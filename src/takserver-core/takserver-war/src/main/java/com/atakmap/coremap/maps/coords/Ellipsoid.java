
package com.atakmap.coremap.maps.coords;

public class Ellipsoid {

    private final String _name;
    private double _semiMajorAxis; // equatorial radius (a)
    private double _semiMinorAxis; // polar radius (b)
    private double _flattening; // (f) = (a-b)/a
    private double _inverseFlattening; // (f^-1) = 1.0/f
    private double _firstEccentricity; // (e) = sqrt(1-(b^2/a^2))
    private double _firstEccentricitySquared; // (e^2) = (a^2-b^2)/a^2
    private double _secondEccentricity; // (e`) = sqrt((a^2/b^2)-1)
    private double _secondEccentricitySquared; // (e`^2) = (a^2-b^2)/b^2

    /**
     * World Geodetic System 1984 (WGS84)
     * <P>
     * The Global Positioning System (GPS) uses the World Geodetic System 1984 (WGS84) to determine
     * the location of a point near the surface of the Earth.
     */
    public static final Ellipsoid WGS_84 = new Ellipsoid("WGS 84", 6378137.0d,
            6356752.3142);

    public Ellipsoid(String name, double semiMajorAxis, double semiMinorAxis) {
        _name = name;
        _calcFromAB(semiMajorAxis, semiMinorAxis);
    }

    public String getName() {
        return _name;
    }

    public double getSemiMajorAxis() {
        return _semiMajorAxis;
    }

    public double getSemiMinorAxis() {
        return _semiMinorAxis;
    }

    public double getFirstEccentricitySquared() {
        return _firstEccentricitySquared;
    }

    public double getFlattening() {
        return _flattening;
    }

    public double getSecondEccentricity() {
        return _secondEccentricity;// Math.sqrt(_secondEccentricitySquared);
    }

    public double getSecondEccentricitySquared() {
        return _secondEccentricitySquared;
    }

    public double getInverseFlattening() {
        return _inverseFlattening;
    }

    public double getFirstEccentricity() {
        return _firstEccentricity;
    }

    private void _calcFromAB(double a, double b) {
        _semiMajorAxis = a;
        _semiMinorAxis = b;
        _flattening = (a - b) / a;
        _inverseFlattening = 1d / _flattening;
        double aSquared = Math.pow(a, 2d);
        double bSquared = Math.pow(b, 2d);
        _firstEccentricity = Math.sqrt(1d - (bSquared / aSquared));
        _firstEccentricitySquared = (aSquared - bSquared) / aSquared;
        _secondEccentricity = Math.sqrt((aSquared / bSquared) - 1);
        _secondEccentricitySquared = (aSquared - bSquared) / bSquared;
    }

}
