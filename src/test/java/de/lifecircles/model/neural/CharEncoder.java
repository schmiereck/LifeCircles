package de.lifecircles.model.neural;

/**
 * Klasse zur Kodierung von Zeichen in binäre Arrays mit 8 Bits (UTF-8)
 */
public class CharEncoder {

    /**
     * Kodiert ein Zeichen in eine binäre Repräsentation (8 Bits).
     *
     * @param ch Das zu kodierende Zeichen
     * @return Ein Byte-Array mit 8 Elementen (0 oder 1)
     */
    public static double[] encode(char ch) {
        // Array mit 8 Elementen für 8-Bit UTF-8 Repräsentation
        double[] encoded = new double[8];

        // Umwandeln des Zeichens in seinen ASCII/UTF-8 Code
        byte charByte = (byte) ch;

        // Extrahieren der Bits und Umwandlung in double-Werte (0.0 oder 1.0)
        for (int i = 0; i < 8; i++) {
            encoded[7 - i] = ((charByte >> i) & 1) == 1 ? 1.0D : 0.0D;
        }

        return encoded;
    }

    /**
     * Dekodiert eine binäre Repräsentation zurück zu einem Zeichen.
     *
     * @param encoded Das binäre Array (8 Elemente)
     * @return Das dekodierte Zeichen
     */
    public static char decode(double[] encoded) {
        if (encoded.length != 8) {
            throw new IllegalArgumentException("Encoded array must have exactly 8 elements");
        }

        byte charByte = 0;

        // Umwandlung der binären Darstellung zurück in ein Byte
        for (int i = 0; i < 8; i++) {
            if (encoded[7 - i] >= 0.5D) { // Wenn der Wert näher an 1 als an 0 ist
                charByte |= (1 << i);
            }
        }

        return (char) charByte;
    }

    /**
     * Findet den Index des höchsten Wertes im Array
     * und gibt die entsprechende binäre Kodierung zurück.
     *
     * @param output Das Ausgabe-Array des neuronalen Netzes
     * @return Ein binäres Array mit 8 Elementen
     */
    public static double[] argmax(double[] output) {
        double[] result = new double[8];
        for (int i = 0; i < 8; i++) {
            result[i] = output[i] >= 0.5D ? 1.0D : 0.0D;
        }
        return result;
    }
}
