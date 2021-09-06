package combination.adrienctx;

/**
 * @author acouetoux
 */
class Trajectory {

    public final boolean isFinal;

    public final IntArrayOfDoubleHashMap[] basisFunctionValues1;

    public final IntArrayOfDoubleHashMap[] basisFunctionValues2;

    public Trajectory(boolean _final, IntArrayOfDoubleHashMap[] _bf1, IntArrayOfDoubleHashMap[] _bf2) {
        isFinal = _final;
        basisFunctionValues1 = _bf1;
        basisFunctionValues2 = _bf2;
    }
}
