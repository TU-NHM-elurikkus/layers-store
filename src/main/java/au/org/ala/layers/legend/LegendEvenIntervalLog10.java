/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.ala.layers.legend;

/**
 * generates legend with even interval cutoff's.
 *
 * @author Adam
 */
public class LegendEvenIntervalLog10 extends Legend {

    @Override
    public void generate(float[] d, int divisions) {
        init(d, divisions);
        if (Float.isNaN(max)) {
            return;
        }

        //prevent negative number assignment
        float offset = (min < 1) ? 1 - min : 0;
        float tmax = max + offset;
        float tmin = min + offset;
        double lmin = Math.log10(tmin);
        double lmax = Math.log10(tmax);
        double lrange = lmax - lmin;

        cutoffs = new float[divisions];

        for (int i = 0; i < divisions; i++) {
            cutoffs[i] = (float) Math.pow(10, lmin + lrange * ((i + 1) / (double) (divisions)))
                    - offset;
        }

        //fix max
        cutoffs[divisions - 1] = max;
    }

    @Override
    public String getTypeName() {
        return "Even Interval Log 10";
    }
}