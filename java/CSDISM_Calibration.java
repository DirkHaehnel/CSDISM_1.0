import ij.*;
import ij.gui.*;
import ij.io.OpenDialog;
import ij.plugin.*;

import java.io.IOException;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.io.ImgIOException;

public class CSDISM_Calibration implements PlugIn {

	@Override
	public void run(String arg) {
		int digits = 0;
		double prefDimensionX = 512;
		double prefDimensionY = 512;
		double prefDimX= 0;
		double prefDimY= 1;
		double prefDimZ= 2;
		double prefSequence = 378;
		double prefFirstFrame = 1;
		double prefNumOfSpots = 1024;
		double prefFwhm = 300;
		double prefLenseDistance = 10;
		double prefPixelSize = 60;
		boolean prefDebug = false;
		String cal_dir = null;
		String cal_file = null;
		String ref_dir = null;
		String ref_file = null;
		GenericDialog calibrationSettings = new GenericDialog("CSDISM Calibration Settings");
		calibrationSettings.addNumericField("Image width X:", prefDimensionX,
				digits);
		calibrationSettings.addNumericField("Image width Y:", prefDimensionY,
				digits);
		calibrationSettings.addNumericField("Number axis X:", prefDimX,
				digits);
		calibrationSettings.addNumericField("Number axis Y:", prefDimY,
				digits);
		calibrationSettings.addNumericField("Number axis Z:", prefDimZ,
				digits);
		calibrationSettings.addNumericField("Number Of Frames:", prefSequence,
				digits);
		calibrationSettings.addNumericField("Start Frame:", prefFirstFrame,
				digits);
		calibrationSettings.addNumericField("Number of Spots:", prefNumOfSpots,
				digits);
		calibrationSettings.addNumericField("FWHM (ym):", prefFwhm, digits);
		calibrationSettings.addNumericField("Lense Distance (ym) ", prefLenseDistance,
				digits);
		calibrationSettings.addNumericField("Pixelsize (ym):", prefPixelSize,
				digits);
		calibrationSettings.addCheckbox("Show Calibration Prozess (Slow!)",
				prefDebug);
		calibrationSettings.showDialog();

		if (calibrationSettings.wasCanceled())
			return;
		double dimensionX = calibrationSettings.getNextNumber();
		double dimensionY = calibrationSettings.getNextNumber();
		double dimX=calibrationSettings.getNextNumber();
		double dimY=calibrationSettings.getNextNumber();
		double dimZ=calibrationSettings.getNextNumber();
		long sequence = (long) calibrationSettings.getNextNumber();
		long firstFrame = (long) calibrationSettings.getNextNumber();
		int numOfSpots = (int) calibrationSettings.getNextNumber();
		int fwhm = (int) calibrationSettings.getNextNumber();
		int lenseDistance = (int) calibrationSettings.getNextNumber();
		int pixelSize = (int) calibrationSettings.getNextNumber();
		boolean debug = calibrationSettings.getNextBoolean();

		OpenDialog chooser_cal = new OpenDialog(
				"Choose Calibration Data File for CSDISM", cal_dir, cal_file);
		String calfile = chooser_cal.getDirectory() + chooser_cal.getFileName();

		OpenDialog chooser_ref = new OpenDialog(
				"Choose or Define a Reference File for CSDISM", ref_dir,
				ref_file);
		String reffile = chooser_ref.getDirectory() + chooser_ref.getFileName();

		try {

			new Calibration(reffile, calfile, dimensionX, dimensionY,dimX,dimY,dimZ, sequence,
					firstFrame, numOfSpots, fwhm, lenseDistance, pixelSize,
					debug);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new WaitForUserDialog("Calibration Finished!  -  Click OK, to continue")
				.show();

	}

	public static void main(String[] args) throws ImgIOException,
			IncompatibleTypeException, IOException {

		// set the plugins.dir property to make the plugin appear in the Plugins
		// menu
		Class<?> clazz = CSDISM_Calibration.class;
		String url = clazz.getResource(
				"/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length()
				- clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);
		// open an ImageJ window
		new ImageJ();
		// run the example
		IJ.runPlugIn(clazz.getName(), "");

	}

}
