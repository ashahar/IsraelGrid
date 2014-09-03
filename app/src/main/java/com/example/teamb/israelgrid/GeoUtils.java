package com.example.teamb.israelgrid;

/**
 * Created by asafsh on 03/09/14.
 */
public class GeoUtils {
    /**
     * Convert gps coordinates to Israel new geographic system
     * @param lat location latitude in degrees
     * @param lng location longitude in degrees
     * @param out out an array of long put East in out[0] and
     *            North in out[1]
     */
    public static void GeoToITM (double lat, double lng, long[] out) {
        // scale factor
        double k0 = 1.0000067;

        // central meridian of ITM projection
        double P = Math.toRadians(lng)-Math.toRadians(35.2045169444444);

        // Ellipsoid constants WGS 80 datum
        double a = 6378137;  // equatorial radius
        double b = 6356752.3141; // polar radius
        double esq = (1 - (b/a)*(b/a));
        double e = Math.sqrt(esq);  // eccentricity
        double e1sq = e*e/(1-e*e);
        //double n = (a-b)/(a+b);

        double phi = Math.toRadians(lat);

        // Curvature at specified location
        double nu = a/Math.sqrt(1-Math.pow(e*Math.sin(phi),2));

        // Arc length along standard meridian
        double M =  phi*(1 - esq*(1.0/4.0 + esq*(3.0/64.0 + 5.0*esq/256.0)));
        M -= Math.sin(2*phi)*(esq*(3.0/8.0 + esq*(3.0/32.0 + 45.0*esq/1024.0)));
        M += Math.sin(4*phi)*(esq*esq*(15.0/256.0 + esq*45.0/1024.0));
        M -= Math.sin(6*phi)*(esq*esq*esq*(35.0/3072.0));
        M  = M*a;

        double T0 = Math.sin(phi);
        double T1 = Math.cos(phi);
        double T2 = T1*T1;
        double T3 = Math.pow(Math.tan(phi),2);

        double K1 = k0*M;
        double K2 = k0*nu*T0*T1/2;
        double K3 = k0*((nu*T0*T1*T2)/24)*(5-T3+9*e1sq*T2+4*e1sq*e1sq*T2*T2);
        double K4 = k0*nu*T1;
        double K5 = k0*T1*T2*(nu/6)*(1-T3+e1sq*T2);

        // Easting
        out[0] = Math.round(K4*P+K5*P*P*P + 219529.58);
        // Northing
        out[1] = Math.round(K1 + K2*P*P + K3*P*P*P*P - 3512424.41 + 626907.39);
    }

    public static double distance(double lat1, double lng1, double lat2, double lng2) {
        double l1 = Math.cos(Math.toRadians(lat1));
        double l2 = Math.cos(Math.toRadians(lat2));
        double d0 = Math.sin(Math.toRadians(lat2-lat1)/2);
        double d1 = Math.sin(Math.toRadians(lng2-lng1)/2);
        double a = d0*d0+l1*l2*d1*d1;
        double d = 2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
        return d*6378137;
    }
}
