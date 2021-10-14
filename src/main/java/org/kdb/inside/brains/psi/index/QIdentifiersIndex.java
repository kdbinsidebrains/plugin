package org.kdb.inside.brains.psi.index;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QIdentifiersIndex extends FileBasedIndexExtension<String, List<IdentifierDescriptor>> {
    protected static final int VERSION = 1 + QDataIndexer3.VERSION * 1000;

    public static final ID<String, List<IdentifierDescriptor>> INDEX_ID = ID.create("qIdentifiers");

    @Override
    public @NotNull ID<String, List<IdentifierDescriptor>> getName() {
        return INDEX_ID;
    }

    @Override
    public @NotNull DataIndexer<String, List<IdentifierDescriptor>, FileContent> getIndexer() {
        return new QDataIndexer3();
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return new EnumeratorStringDescriptor();
    }

    @Override
    public @NotNull DataExternalizer<List<IdentifierDescriptor>> getValueExternalizer() {
        return new DataExternalizer<>() {
            @Override
            public void save(@NotNull DataOutput out, List<IdentifierDescriptor> value) throws IOException {
                out.writeInt(value.size());
                for (IdentifierDescriptor d : value) {
                    final TextRange range = d.getRange();
                    out.writeUTF(d.getType().name());
                    out.writeInt(range.getStartOffset());
                    out.writeInt(range.getEndOffset());

                    final List<String> params = d.getParams();
                    out.writeInt(params.size());
                    for (String param : params) {
                        out.writeUTF(param);
                    }
                }
            }

            @Override
            public List<IdentifierDescriptor> read(@NotNull DataInput in) throws IOException {
                int size = in.readInt();

                ArrayList<IdentifierDescriptor> infos = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    final IdentifierType type = IdentifierType.parseFrom(in.readUTF());
                    final TextRange range = new TextRange(in.readInt(), in.readInt());

                    final int p = in.readInt();
                    final List<String> params = new ArrayList<>(p);
                    for (int j = 0; j < p; j++) {
                        params.add(in.readUTF());
                    }
                    if (type != null) {
                        infos.add(new IdentifierDescriptor(type, params, range));
                    }
                }
                return infos;
            }
        };
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(QFileType.INSTANCE) {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                return file.isInLocalFileSystem();
            }
        };
    }
}
