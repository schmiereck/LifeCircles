package de.lifecircles.model.neural;

import java.io.Serializable;

/** Shared inert controller: no DNA, inputs, recurrent state or neural work. Never evolves. */
public final class RemainsBrain implements CellBrainInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public static final RemainsBrain INSTANCE = new RemainsBrain();
    private RemainsBrain() {}
    private Object readResolve() { return INSTANCE; }
    public int getSynapseCount() { return 0; }
    public boolean[] determineActiveHiddenLayers(int state) { return new boolean[3]; }
    public void setInputs(double[] inputs) {}
    public double[] process() { return new double[0]; }
    public int getInputCount() { return 0; }
    public long getProccessedSynapses() { return 0; }
    public NeuralNetwork mutate(double rate, double strength) { throw new IllegalStateException("Remains cannot reproduce"); }
    public double getOutputValue(int index) { return 0; }
    public double getInputValue(int index) { return 0; }
}
