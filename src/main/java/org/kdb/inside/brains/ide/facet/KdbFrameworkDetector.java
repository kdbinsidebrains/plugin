package org.kdb.inside.brains.ide.facet;

import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

public class KdbFrameworkDetector extends FacetBasedFrameworkDetector<KdbFacet, KdbFacetConfiguration> {
    protected KdbFrameworkDetector() {
        super("KDB+Q Framework");
    }

    @Override
    public @NotNull KdbFacetType getFacetType() {
        return KdbFacetType.getInstance();
    }

    @Override
    public @NotNull QFileType getFileType() {
        return QFileType.INSTANCE;
    }

    @Override
    public @NotNull ElementPattern<FileContent> createSuitableFilePattern() {
        return FileContentPattern.fileContent();
    }
}
