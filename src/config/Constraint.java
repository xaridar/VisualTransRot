package config;

public class Constraint {

    private final DataType type;

    public enum DataType {
        INT, FLOAT, BOOLEAN
    }

    Constraint(DataType type) {
        this.type = type;
    }

    public Constraint.DataType getType() {
        return type;
    }

    public static class NumberConstraint<T extends Number> extends Constraint {

        private final T min, max;

        public NumberConstraint(DataType dataType, T min, T max) {
            super(dataType);
            this.min = min;
            this.max = max;
        }

        public T getMin() {
            return min;
        }
        
        public T getMax() {
            return max;
        }
    }

    public static class IntConstraint extends NumberConstraint<Integer> {

        public IntConstraint(Integer min, Integer max) {
            super(DataType.INT, min, max);
        }

        public IntConstraint() {
            this(null, null);
        }

        public IntConstraint(Integer min) {
            this(min, null);
        }
    }

    public static class FloatConstraint extends NumberConstraint<Double> {
        
        public FloatConstraint(Double min, Double max) {
            super(DataType.FLOAT, min, max);
        }

        public FloatConstraint() {
            this(null, null);
        }

        public FloatConstraint(Double min) {
            this(min, null);
        }
    }

    public static class BooleanConstraint extends Constraint {
        public BooleanConstraint() {
            super(DataType.BOOLEAN);
        }
    }
}