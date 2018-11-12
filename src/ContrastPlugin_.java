import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
 
public class ContrastPlugin_  implements PlugInFilter {	
	/*Konstanten*/
	private final int BLACK = 0x00;
	private final int WHITE = 0xFF;
	
	/*Attribute*/
	private int valueMin = BLACK;
	private int valueMax = WHITE;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		return DOES_8G;
	}

	@Override
	public void run(ImageProcessor ip) {
		//Nutzereinstellungen abfragen und verarbeiten
		int newMin = BLACK;
		int newMax = WHITE;
		double saturation = requestSaturation();
		
		//Pr�ft ob ung�ltiger S�ttigungswert (nicht in [0,50])
		if(saturation < 0 || saturation >= 50) {
			if(saturation != IJ.CANCELED) {
				IJ.error("Falscher Grenzwert!", "Der eingegebene Grenzwert befindet sich au�erhalb des Intervalls [0,50).");
			}
			newMin = requestMinLimit();
			newMax = requestMaxLimit();
			
			//Pr�ft ob ung�ltiger Grenzwert (nicht in [0,255])
			if(newMin < 0 || newMin > 255 || newMax < 0 || newMax > 255 || newMin >= newMax) {
				IJ.error("Falscher Grenzwert!", "Mindestens einer der eingegebenen Grenzwerte befindet sich au�erhalb des Intervalls [0,255]"
						+ "oder das Minimum ist gr��er gleich dem Maximum");
				//Programmabbruch
				return;
			}
			
			findMinMax(ip);
		}else {
			findMinMaxSaturated(ip, saturation);
		}
		
		//�berpr�fe ob der ermittelte Maximalwert kleiner oder gleich dem Minimalwert ist.
		//Wenn ja, beende, da sonst das Einfarbige (oder leere) Bild einfach nur schwarz wird.
		if(this.valueMax <= this.valueMin) {
			IJ.error("Bild ist einfarbig oder ein Fehler ist aufgetreten!");
			//Programmabbruch
			return;
		}
		
		modifyImage(ip, newMin, newMax);
	}
	
	/**
	 * F�hrt die eigentliche Kontrastver�nderung auf Basis der gew�hlten Einstellungen durch
	 * @param ip Der ImageProcessor vom Bild
	 */
	private void modifyImage(ImageProcessor ip, int newMinValue, int newMaxValue) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		double factor = (double)(newMaxValue - newMinValue)/(double)(valueMax-valueMin);
		
		//Iterate over all pixels and modify value
		for(int cntWidth = 0; cntWidth < width; cntWidth++) {
			for(int cntHeight = 0; cntHeight < height; cntHeight++) {
				//Formel zur Berechnung des neuen Werts
				int pixelVal = (int)(newMinValue + (ip.get(cntWidth, cntHeight) - valueMin) * factor);
				ip.putPixel(cntWidth, cntHeight, clamp(pixelVal, 0, 255));
			}
		}
	}
	
	/**
	 * Die Nutzerabfrage f�r den Minimalwert
	 * @return Der eingegebene Wert
	 */
	private int requestMinLimit() {
		return (int)IJ.getNumber("Bitte einen neuen Minimalwert eingeben:", 0.0);
	}
	
	/**
	 * Die Nutzerabfrage f�r den Maximalwert
	 * @return Der eingegebene Wert
	 */
	private int requestMaxLimit() {
		return (int)IJ.getNumber("Bitte einen neuen Maximalwert eingeben:", 255.0);
	}
	
	/**
	 * Die Nutzerabfrage f�r den S�ttigungsanteil
	 * @return Der eingegebene Wert
	 */
	private double requestSaturation() {
		return IJ.getNumber("Bitte einen Saettigungswert in Prozent eingeben:", 0.0);
	}
	
	/**
	 * Sucht den Wert des minimalen und maximalen Pixelwerts im Bild.
	 * @param ip Der ImageProcessor vom Bild
	 */
	private void findMinMax(ImageProcessor ip) {
		findMinMaxSaturated(ip, 0.0);
	}
	
	/**
	 * Sucht den Wert des minimalen und maximalen Pixelwerts im Bild bei angegebener S�ttigung.
	 * Der Wert der S�ttigung gibt dabei den Anteil der Pixel des Bilds an,
	 * die nach der Vearbeitung den Minimal- bzw. Maximalwert annehmen.
	 * @param ip Der ImageProcessor vom Bild
	 * @param saturation Der S�ttigungsanteil in Prozent
	 */
	private void findMinMaxSaturated(ImageProcessor ip, double saturation) {
		int[] histogram = ip.getHistogram();
		int target = (int)(ip.getPixelCount() * (saturation * 0.01));
		int minCount = 0;
		int maxCount = 0;
		int idxMin = 0;
		int idxMax = histogram.length - 1;
		
		if(target == 0) {
			//Mindestens 1 pixel finden
			target = 1;
		}
		
		while(minCount < target && idxMin < 255) {
			minCount += histogram[idxMin];
			idxMin++;
		}
		
		while(maxCount < target && idxMax > 0) {
			maxCount += histogram[idxMax];
			idxMax--;
		}
		
		this.valueMin = idxMin;
		this.valueMax = idxMax;
	}
	
	/**
	 * Begrenzt einen Eingabewert auf einen Bereich.
	 * @param value Der Wert der auf den Bereich begrenzt werden soll.
	 * @param min Der minimal erlaubte Wert.
	 * @param max Der maximal erlaubte Wert.
	 * @return Der begrenzte Wert zwischen min und max.
	 */
	private int clamp(int value, int min, int max) {
		return Math.min(Math.max(min, value), max);
	}
	
}
