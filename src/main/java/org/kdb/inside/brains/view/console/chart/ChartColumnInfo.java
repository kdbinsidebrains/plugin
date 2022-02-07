package org.kdb.inside.brains.view.console.chart;

import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ChartColumnInfo<T, V> extends ColumnInfo<T, V> {
    private final String maxStringValue;
    private final Function<T, V> getter;
    private final BiConsumer<T, V> setter;

    public ChartColumnInfo(String name, Function<T, V> getter, BiConsumer<T, V> setter) {
        this(name, getter, setter, null);
    }

    public ChartColumnInfo(String name, Function<T, V> getter, BiConsumer<T, V> setter, String maxStringValue) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.maxStringValue = maxStringValue;
    }

    @Override
    public @Nullable V valueOf(T cc) {
        return getter.apply(cc);
    }

    @Override
    public void setValue(T config, V value) {
        if (setter != null) {
            setter.accept(config, value);
        }
    }

    @Override
    public boolean isCellEditable(T cc) {
        return setter != null;
    }

    @Override
    public @Nullable String getMaxStringValue() {
        return maxStringValue;
    }
}
