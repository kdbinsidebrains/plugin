package org.kdb.inside.brains.view.chart.types.line;

public enum Operation {
    SUM("Sum") {
        @Override
        public Number calc(Number current, Number added, int count) {
            return current.doubleValue() + added.doubleValue();
        }
    },
    COUNT("Count") {
        @Override
        public Number initialValue(Number current) {
            return 1;
        }

        @Override
        public Number calc(Number current, Number added, int count) {
            return current.intValue() + 1;
        }
    },
    AVG("Avg") {
        @Override
        public Number calc(Number current, Number added, int count) {
            return ((current.doubleValue() * count) + added.doubleValue()) / (count + 1);
        }
    },
    MIN("Min") {
        @Override
        public Number calc(Number current, Number added, int count) {
            return Math.min(current.doubleValue(), added.doubleValue());
        }
    },
    MAX("Max") {
        @Override
        public Number calc(Number current, Number added, int count) {
            return Math.max(current.doubleValue(), added.doubleValue());
        }
    };

    private final String label;

    Operation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Number initialValue(Number current) {
        return current;
    }

    public abstract Number calc(Number current, Number added, int count);
}