package org.kdb.inside.brains.view.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JsonExportAction extends AnExportAction<VirtualFileWrapper> {
    private static final c.Flip EMPTY_FLIP = new c.Flip(new c.Dict(new String[0], new Object[0]));
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonNodeFactory factory = mapper.getNodeFactory();

    public JsonExportAction(String text, ExportingType type, ExportDataProvider dataProvider, String description) {
        super(text, type, dataProvider, description);
    }

    private static <T> T[] concat(T[] first, T[] second, Class<T> type) {
        @SuppressWarnings("unchecked") final T[] result = (T[]) Array.newInstance(type, first.length + second.length);
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @Override
    protected VirtualFileWrapper getExportConfig(Project project, ExportDataProvider view) {
        final FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Export to JSON", "Exporting data into JSON file format", "json");
        return FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, project).save(view.getExportName());
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, VirtualFileWrapper file, ExportDataProvider dataProvider, @NotNull ProgressIndicator indicator) throws Exception {
        final JTable table = dataProvider.getTable();

        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        int count = 0;
        double totalCount = ri.count() * ci.count();
        indicator.setIndeterminate(false);
        final KdbOutputFormatter formatter = KdbOutputFormatter.getDefault();

        final String[] names = new String[table.getColumnCount()];
        for (int c = ci.reset(); c != -1; c = ci.next()) {
            names[c] = table.getColumnName(c);
        }

        final ArrayNode tbl = factory.arrayNode();
        for (int r = ri.reset(); r != -1 && !indicator.isCanceled(); r = ri.next()) {
            final ObjectNode row = factory.objectNode();
            for (int c = ci.reset(); c != -1 && !indicator.isCanceled(); c = ci.next()) {
                row.set(names[c], getValueAt(table, formatter, r, c));
                indicator.setFraction(count++ / totalCount);
            }
            tbl.add(row);
        }

        if (indicator.isCanceled()) {
            return;
        }

        final String str = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(tbl.size() == 1 ? tbl.get(0) : tbl);
        Files.writeString(file.getFile().toPath(), str, StandardCharsets.UTF_8);
    }

    private JsonNode getValueAt(JTable table, KdbOutputFormatter formatter, int r, int c) {
        final Object valueAt = table.getValueAt(r, c);
        if (valueAt == null) {
            return factory.nullNode();
        }
        return convertValue(formatter, valueAt);
    }

    private JsonNode convertValue(KdbOutputFormatter formatter, Object value) {
        if (value == null || KdbType.isNull(value)) {
            return factory.nullNode();
        } else if (value instanceof char[] ch) {
            return factory.textNode(new String(ch));
        } else if (value instanceof c.Flip f) {
            return convertFlip(formatter, f, EMPTY_FLIP);
        } else if (value instanceof c.Dict d) {
            return convertDict(formatter, d);
        } else if (value.getClass().isArray()) {
            return convertArrayValue(formatter, value);
        }
        return convertSimpleValue(formatter, value);
    }

    private JsonNode convertFlip(KdbOutputFormatter formatter, c.Flip f1, c.Flip f2) {
        final String[] names = concat(f1.x, f2.x, String.class);
        final Object[] values = concat(f1.y, f2.y, Object.class);

        final int size = Array.getLength(values[0]);
        final ArrayNode node = factory.arrayNode();
        for (int i = 0; i < size; i++) {
            final ObjectNode row = factory.objectNode();
            for (int j = 0; j < names.length; j++) {
                row.set(names[j], convertValue(formatter, Array.get(values[j], i)));
            }
            node.add(row);
        }
        return node;
    }

    private JsonNode convertDict(KdbOutputFormatter formatter, c.Dict d) {
        if (d.x instanceof c.Flip keys && d.y instanceof c.Flip values) {
            convertFlip(formatter, keys, values);
        }

        final int length = Array.getLength(d.x);
        final ObjectNode node = factory.objectNode();
        for (int i = 0; i < length; i++) {
            final Object name = Array.get(d.x, i);
            final Object value = Array.get(d.y, i);
            node.set(String.valueOf(convertValue(formatter, name)), convertValue(formatter, value));
        }
        return node;
    }

    private JsonNode convertArrayValue(KdbOutputFormatter formatter, Object value) {
        final ArrayNode node = factory.arrayNode();
        if (value instanceof boolean[] a) {
            for (boolean v : a) {
                node.add(v);
            }
        } else if (value instanceof byte[] a) {
            for (byte v : a) {
                node.add(v);
            }
        } else if (value instanceof short[] a) {
            for (short v : a) {
                node.add(v);
            }
        } else if (value instanceof int[] a) {
            for (int v : a) {
                node.add(v);
            }
        } else if (value instanceof long[] a) {
            for (long v : a) {
                node.add(v);
            }
        } else if (value instanceof float[] a) {
            for (float v : a) {
                node.add(v);
            }
        } else if (value instanceof double[] a) {
            for (double v : a) {
                node.add(v);
            }
        } else {
            final int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                node.add(convertValue(formatter, Array.get(value, i)));
            }
        }
        return node;
    }

    private JsonNode convertSimpleValue(KdbOutputFormatter formatter, Object value) {
        if (value instanceof Byte v) {
            return factory.numberNode(v);
        }
        if (value instanceof Short v) {
            return factory.numberNode(v);
        }
        if (value instanceof Integer v) {
            return factory.numberNode(v);
        }
        if (value instanceof Long v) {
            return factory.numberNode(v);
        }
        if (value instanceof Float v) {
            return factory.numberNode(v);
        }
        if (value instanceof Double v) {
            return factory.numberNode(v);
        }
        return factory.textNode(formatter.objectToString(value, false, false));
    }
}