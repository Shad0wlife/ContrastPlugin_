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
	private int newMin = BLACK;
	private int newMax = WHITE;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		return DOES_8G;
	}

	@Override
	public void run(ImageProcessor ip) {
		//Teil 1: Nutzereinstellungen abfragen und verarbeiten
		getSettings(ip);
		
		//�berpr�fe ob der ermittelte Maximalwert kleiner oder gleich dem Minimalwert ist.
		//Wenn ja, beende, da sonst das Einfarbige (oder leere) Bild einfach nur schwarz wird.
		if(this.valueMax <= this.valueMin) {
			IJ.error("Bild ist einfarbig oder ein Fehler ist aufgetreten!");
			return;
		}
		
		//Teil 2: Bild verarbeiten
		modify(ip);
	}
	
	/**
	 * Verarbeitet die Abfrage der Plugin-Einstellungen
	 * @param ip Der ImageProcessor vom Bild
	 */
	private void getSettings(ImageProcessor ip) {
		double saturation = requestSaturation();
		
		//Pr�ft ob umg�ltiger S�ttigungswert (-1)
		if(saturation == -1) {
			requestLimits();
			findMinMax(ip);
		}else {
			findMinMaxSaturated(ip, saturation);
		}
	}
	
	/**
	 * F�hrt die eigentliche Kontrastver�nderung auf Basis der gew�hlten Einstellungen durch
	 * @param ip Der ImageProcessor vom Bild
	 */
	private void modify(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		double factor = (double)(newMax - newMin)/(double)(valueMax-valueMin);
		
		//Iterate over all pixels and modify value
		for(int cntWidth = 0; cntWidth < width; cntWidth++) {
			for(int cntHeight = 0; cntHeight < height; cntHeight++) {
				//Formel zur Berechnung des neuen Werts
				int pixelVal = (int)(newMin + (ip.get(cntWidth, cntHeight) - valueMin) * factor);
				ip.putPixel(cntWidth, cntHeight, clamp(pixelVal, 0, 255));
			}
		}
	}
	

	/**
	 * Die Nutzerabfrage f�r die neuen Grenzwerte des Bilds (falls kein S�ttigungswert angegeben wurde).
	 */
	private void requestLimits() {
		int newMin = (int)IJ.getNumber("Bitte einen neuen Minimalwert eingeben:", 0.0);
		int newMax = (int)IJ.getNumber("Bitte einen neuen Maximalwert eingeben:", 255.0);
		
		if(newMin < 0 || newMin > 255 || newMax < 0 || newMax > 255) {
			IJ.error("Falscher Grenzwert!", "Mindestens einer der eingegebenen Grenzwerte befindet sich au�erhalb des Intervalls [0,255].");
			return;
		}
		
		this.newMin = newMin;
		this.newMax = newMax;
	}
	
	
	/**
	 * Die Nutzerabfrage f�r den S�ttigungsanteil [0,50).
	 * @return
	 */
	private double requestSaturation() {
		double saturation = IJ.getNumber("Bitte einen Saettigungswert in Prozent eingeben:", 0.0);
		
		if(saturation < 0 || saturation >= 50) {
			IJ.error("Falscher Grenzwert!", "Der eingegebene Grenzwert befindet sich au�erhalb des Intervalls [0,50).");
			return -1;
		}
		return saturation;
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
