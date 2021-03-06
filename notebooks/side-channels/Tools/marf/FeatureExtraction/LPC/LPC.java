package marf.FeatureExtraction.LPC;

import java.util.Vector;

import marf.MARF;
import marf.FeatureExtraction.FeatureExtraction;
import marf.FeatureExtraction.FeatureExtractionException;
import marf.Preprocessing.IPreprocessing;
import marf.Storage.ModuleParams;
import marf.gui.Spectrogram;
import marf.math.Algorithms;
import marf.util.Debug;

/**
 * <p>Class LPC implements Linear Predictive Coding.</p>
 *
 * <p>$Id: LPC.java,v 1.40 2005/12/30 05:54:25 mokhov Exp $</p>
 *
 * @author Ian Clement
 * @author Serguei Mokhov
 *
 * @version $Revision: 1.40 $
 * @since 0.0.1
 */
public class LPC
        extends FeatureExtraction {

    /**
     * Default window length of 128 elements.
     * @since 0.3.0
     */
    public static final int DEFAULT_WINDOW_LENGTH = 128;
    /**
     * Default number of poles, 20.
     * @since 0.3.0
     */
    public static final int DEFAULT_POLES = 10;
    /**
     * Number of poles.
     * <p>A pole is a root of the denominator in the Laplace transform of the
     * input-to-output representation of the speech signal.</p>
     */
    private int iPoles;
    /**
     * Window length.
     */
    private int iWindowLen;
    /**
     * For serialization versioning.
     * When adding new members or make other structural
     * changes regenerate this number with the
     * <code>serialver</code> tool that comes with JDK.
     * @since 0.3.0.4
     */
    private static final long serialVersionUID = 7960314467497310447L;

    /**
     * LPC Constructor.
     * @param poPreprocessing Preprocessing module reference
     */
    public LPC(IPreprocessing poPreprocessing) {
        super(poPreprocessing);
        setDefaults();

        // LPC-specific parameters, if any
        ModuleParams oModuleParams = MARF.getModuleParams();

        if (oModuleParams != null) {
            Vector oParams = oModuleParams.getFeatureExtractionParams();

            if (oParams.size() > 0) {
                this.iPoles = ((Integer) oParams.elementAt(0)).intValue();
                this.iWindowLen = ((Integer) oParams.elementAt(1)).intValue();
            }
        }
    }

    /**
     * Sets the default values of poles and window length if none
     * were supplied by an application.
     */
    private void setDefaults() {
        this.iPoles = DEFAULT_POLES;
        this.iWindowLen = DEFAULT_WINDOW_LENGTH;
    }

    /**
     * LPC Implementation of <code>extractFeatures()</code>.
     * @return <code>true</code> if features were extracted, <code>false</code> otherwise
     * @throws FeatureExtractionException
     */
    public final boolean extractFeatures()
            throws FeatureExtractionException {
        try {
//            System.out.println("LPC.extractFeatures() has begun...");

            double[] adSample = this.oPreprocessing.getSample().getSampleArray();

//            System.out.println("sample length: " + adSample.length);
//            System.out.println("poles: " + this.iPoles);
//            System.out.println("window length: " + this.iWindowLen);

            Spectrogram oSpectrogram = null;

            // For the case when we want intermediate spectrogram
            if (MARF.getDumpSpectrogram() == true) {
                oSpectrogram = new Spectrogram("lpc");
            }

            this.adFeatures = new double[this.iPoles];

            double[] adWindowed = new double[this.iWindowLen];
            double[] adLPCCoeffs = new double[this.iPoles];
            double[] adLPCError = new double[this.iPoles];

            // Number of windows
            int iWindowsNum = 1;

            int iHalfWindow = this.iWindowLen / 2;


//            this.coefMean = new double[this.iPoles];
            this.coefVariance = new double[this.iPoles];

            for (int i = 0; i < this.iPoles; i++) {
                this.adFeatures[i] = 0;
                this.coefVariance[i] = 0;
            }

            for (int iCount = iHalfWindow; (iCount + iHalfWindow) <= adSample.length; iCount += iHalfWindow) {
                // Window the input.
                for (int j = 0; j < this.iWindowLen; j++) {
                    adWindowed[j] = adSample[iCount - iHalfWindow + j];
//                    windowed[j] = adSample[count - iHalfWindow + j] * hamming(j, this.windowLen);
//                    System.out.println("window: " + windowed[j]);
                }

                Algorithms.Hamming.hamming(adWindowed);
                Algorithms.LPC.doLPC(adWindowed, adLPCCoeffs, adLPCError, this.iPoles);

                if (MARF.getDumpSpectrogram() == true) {
                    oSpectrogram.addLPC(adLPCCoeffs, this.iPoles, iHalfWindow);
                }


                // Collect features
                double[] mean0 = new double[this.iPoles];
                int i = (int) Math.floor((iCount - iHalfWindow) / iHalfWindow);

//                System.out.println("");

                for (int j = 0; j < this.iPoles; j++) {
                    mean0[j] = this.adFeatures[j];
                    this.adFeatures[j] = (i * mean0[j] + adLPCCoeffs[j]) / (i + 1); // u_(N+1) = (N*u_N + x_(N+1))/N+1
                    this.coefVariance[j] = (i * this.coefVariance[j] + i * Math.pow((mean0[j] - this.adFeatures[j]), 2) + Math.pow((adLPCCoeffs[j] - this.adFeatures[j]), 2)) / (i + 1);

//                    System.out.print(Double.toString(adLPCCoeffs[j]).replaceAll("\\.", ",") + "\t");

//                    adFeatures[j] += adLPCCoeffs[j];
                    //System.out.println("lpc_coeffs[" + j + "]"  + lpc_coeffs[j]);
                }

                iWindowsNum++;
            }



//                System.out.println("");

            // Smoothing
//            if (iWindowsNum > 1) {
//                for (int j = 0; j < this.iPoles; j++) {
//                    adFeatures[j] /= iWindowsNum;
//                }
//            }


//            System.out.println("LPC.extractFeatures() - number of windows = " + iWindowsNum);

            // For the case when we want intermediate spectrogram
            if (MARF.getDumpSpectrogram() == true) {
                oSpectrogram.dump();
            }

//            System.out.println("LPC.extractFeatures() has finished.");

            return (this.adFeatures.length > 0);
        } catch (Exception e) {
            throw new FeatureExtractionException(e);
        }
    }

    /**
     * Retrieves the number of poles.
     * @return the number of poles
     * @since 0.3.0.4
     */
    public int getPoles() {
        return this.iPoles;
    }

    /**
     * Allows setting the number of poles.
     * @param piPoles new number of poles
     * @since 0.3.0.4
     */
    public void setPoles(int piPoles) {
        this.iPoles = piPoles;
    }

    /**
     * Retrieves the window length.
     * @return the window length
     * @since 0.3.0.4
     */
    public int getWindowLength() {
        return this.iWindowLen;
    }

    /**
     * Allows setting the window length.
     * @param piWindowLen the window length to set
     * @since 0.3.0.4
     */
    public void setWindowLength(int piWindowLen) {
        this.iWindowLen = piWindowLen;
    }

    /**
     * Returns source code revision information.
     * @return revision string
     * @since 0.3.0.2
     */
    public static String getMARFSourceCodeRevision() {
        return "$Revision: 1.40 $";
    }
}
// EOF

