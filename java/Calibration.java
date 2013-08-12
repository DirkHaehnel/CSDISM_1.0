import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.Roi;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localization.EllipticGaussianOrtho;
import net.imglib2.algorithm.localization.Gaussian;
import net.imglib2.algorithm.localization.LevenbergMarquardtSolver;
import net.imglib2.algorithm.localization.MLEllipticGaussianEstimator;
import net.imglib2.algorithm.localization.MLGaussianEstimator;
import net.imglib2.algorithm.localization.PeakFitter;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.HyperSliceImgPlus;
import net.imglib2.view.Views;

/**
 * Calibration of csdism data
 */
public class Calibration {

	@SuppressWarnings("unused")
	public <T extends RealType<T> & NativeType<T>> Calibration(String reffile,
			String calfile, double dimensionX, double dimensionY, double dimX,
			double dimY, double dimZ, long sequence, long firstFrame,
			int numOfSpots, int fwhm, int lenseDistance, int pixelSize,
			boolean debug) throws ImgIOException, IncompatibleTypeException,
			IOException {

		ImagePlus showPeakImage = NewImage.createByteImage("Peak Preview",
				(int) dimensionX, (int) dimensionY, 1, NewImage.FILL_BLACK);

		if (debug) {
			showPeakImage.show();
			showPeakImage.resetDisplayRange();
			showPeakImage.updateAndDraw();

		}

		double softFwhm = fwhm / pixelSize;
		int radius = (int) Math.round(softFwhm);
		int border = (int) Math.round(softFwhm) * (-1);

		Img<FloatType> image = new ImgOpener().openImg(calfile,
				new ArrayImgFactory<FloatType>(), new FloatType());

		if (debug) {
		ImageJFunctions.show(image).setTitle("Calibration Raw-Data ");
		}
		File fileTmp = new File(reffile);

		if (!fileTmp.exists())
			try {
				fileTmp.createNewFile();
			} catch (IOException e2) {
				e2.printStackTrace();
			}

		FileOutputStream fosTmp = null;

		try {
			fosTmp = new FileOutputStream(reffile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(fosTmp);
		} catch (Exception e) {
		}

		try {
			dos.writeDouble(dimensionX);
			dos.writeDouble(dimensionY);
			dos.writeDouble(sequence);
			dos.writeDouble(dimX);
			dos.writeDouble(dimY);
			dos.writeDouble(dimZ);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		File file = new File(calfile);
		ImgPlus<FloatType> img;
		try {
			img = ImgOpener.open(file.getAbsolutePath());
		} catch (ImgIOException e) {
			System.err.println("Could not open image " + file);
			System.err.println(e.getLocalizedMessage());
			return;
		}

		for (long frame = firstFrame; (frame < sequence); ++frame) {

			IJ.showProgress((int) frame, (int) sequence);

			Collection<Localizable> peaks = new HashSet<Localizable>(numOfSpots);

			RandomAccessibleInterval<FloatType> view = Views.hyperSlice(image,
					2, frame);

			Img<FloatType> display = spotEstimator(view,
					new ArrayImgFactory<FloatType>(), new FloatType(), peaks,
					radius, border);

			System.out.println(display);
			ImgPlus<FloatType> currentSlice = new HyperSliceImgPlus<FloatType>(
					img, 2, frame);

			PeakFitter<FloatType> fitter = new PeakFitter<FloatType>(
					(Img<FloatType>) currentSlice, peaks,
					new LevenbergMarquardtSolver(),
					new EllipticGaussianOrtho(),
					new MLEllipticGaussianEstimator(new double[] { 2d, 2d }));

			System.out.println(fitter);
			if (!fitter.checkInput() || !fitter.process()) {
				System.err.println("Problem with peak fitting: "
						+ fitter.getErrorMessage());
				return;
			}

			Map<Localizable, double[]> results = fitter.getResult();

			final Overlay overlayPeaks = new Overlay();
			showPeakImage.setOverlay(overlayPeaks);

			for (Localizable peak : peaks) {

				double[] params = results.get(peak);
				double x = params[0];
				double y = params[1];
				if (debug) {

					overlayPeaks.add(new Roi(x, y, 1, 1));
					showPeakImage.updateAndDraw();
					

				}
				try {
					dos.writeDouble(x);
					dos.writeDouble(y);
					dos.writeDouble(frame);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Checks all pixels in the image if they are a local maxima and draws a
	 * circle into the output if they are
	 * 
	 * @param source
	 *            - the image data to work on
	 * @param imageFactory
	 *            - the factory for the output img
	 * @param outputType
	 *            - the output type
	 * @return - an Img with circles on locations of a local minimum
	 */
	public static <T extends Comparable<T>, U extends RealType<U>> Img<U> spotEstimator(
			RandomAccessibleInterval<T> source, ImgFactory<U> imageFactory,
			U outputType, Collection<Localizable> peaks, int radius, int border) {

		Img<U> output = imageFactory.create(source, outputType);

		Interval interval = Intervals.expand(source, border);

		source = Views.interval(source, interval);

		final Cursor<T> center = Views.iterable(source).localizingCursor();

		final RectangleShape shape = new RectangleShape(radius, true);

		for (final Neighborhood<T> localNeighborhood : shape
				.neighborhoods(source)) {

			try {
				final T centerValue = center.next();

				boolean isMaximum = true;

				for (final T value : localNeighborhood) {

					if (centerValue.compareTo(value) <= 0) {
						isMaximum = false;
						break;
					}
				}

				if (isMaximum) {

					HyperSphere<U> hyperSphere = new HyperSphere<U>(output,
							center, 1);

					int posX = 0;
					int posY = 1;

					Localizable coordinate = new Point(
							center.getLongPosition(posX),
							center.getLongPosition(posY));

					peaks.add(coordinate);

					for (U value : hyperSphere)
						value.setOne();

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return output;
	}

}