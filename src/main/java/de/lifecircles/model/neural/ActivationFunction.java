package de.lifecircles.model.neural;

public enum ActivationFunction {
    Sigmoid {
        @Override
        public double apply(double x) {
            return 1.0 / (1.0 + Math.exp(-x));
        }
    },
    ReLU {
        @Override
        public double apply(double x) {
            return Math.max(0, x);
        }
    },
    Tanh {
        @Override
        public double apply(double x) {
            return Math.tanh(x);
        }
    },
    LeakyReLU {
        @Override
        public double apply(double x) {
            return (x > 0) ? x : 0.01 * x;
        }
    };

    public abstract double apply(double x);

}
