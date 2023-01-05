package org.kdb.inside.brains.view.chart;

import org.jdom.Element;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ChartConfig {
    Element store();

    boolean isInvalid();

    ChartType getType();

    ChartConfig copy();

    String toHumanString();

    List<ColumnConfig> getColumns();

    default boolean isApplicable(ChartDataProvider dataProvider) {
        class C {
            final String name;
            final KdbType type;

            public C(ColumnConfig c) {
                name = c.getName();
                type = c.getType();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                C c = (C) o;
                return name.equals(c.name) && type == c.type;
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, type);
            }
        }

        final Set<C> required = getColumns().stream().filter(Objects::nonNull).map(C::new).collect(Collectors.toSet());
        final Set<C> exist = Stream.of(dataProvider.getColumns()).filter(Objects::nonNull).map(C::new).collect(Collectors.toSet());
        return exist.containsAll(required);
    }
}
