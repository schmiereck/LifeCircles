package de.lifecircles.model.neural;

public enum ActivationFunction {
    Sigmoid {
        @Override
        public double apply(double x) {
            return 1.0 / (1.0 + Math.exp(-x));
        }

        @Override
        public double derivative(double x) {
            double sigmoid = apply(x);
            return sigmoid * (1 - sigmoid);
        }
    },
    ReLU {
        @Override
        public double apply(double x) {
            return Math.max(0, x);
        }

        @Override
        public double derivative(double x) {
            return x > 0 ? 1.0 : 0.0;
        }
    },
    Tanh {
        @Override
        public double apply(double x) {
            return Math.tanh(x);
        }

        @Override
        public double derivative(double x) {
            double tanh = Math.tanh(x);
            return 1 - (tanh * tanh);
        }
    },
    LeakyReLU {
        @Override
        public double apply(double x) {
            return (x > 0) ? x : 0.01 * x;
        }

        @Override
        public double derivative(double x) {
            return (x > 0) ? 1.0 : 0.01;
        }
    };

    public abstract double apply(double x);

    /**
     * Berechnet die Ableitung der Aktivierungsfunktion am gegebenen Punkt.
     * Wird für den Backpropagation-Algorithmus benötigt.
     *
     * @param x Der Eingabewert
     * @return Die Ableitung am Punkt x
     */
    public abstract double derivative(double x);
}
