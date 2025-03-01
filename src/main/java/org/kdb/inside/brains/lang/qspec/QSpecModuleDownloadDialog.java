package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class QSpecModuleDownloadDialog extends DialogWrapper {
    private static final String GITHUB_URL = "https://github.com/nugend/qspec/archive/refs/heads/master.zip";
    private JPanel myComponent;
    private JButton downloadButton;
    private JTextArea progressArea;
    private TextFieldWithBrowseButton qSpecFolderField;
    private Future<?> activeFuture = null;

    public QSpecModuleDownloadDialog(Project project) {
        super(project);

        setTitle("Downloading QSpec Library");

        qSpecFolderField.setText(FilenameUtils.normalize(System.getProperty("user.home") + "/.qpackages"));

        progressArea.setEditable(false);
        ((DefaultCaret) progressArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        UIUtils.initializeFileChooser(project, qSpecFolderField,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("QSpec Downloading Folder")
                        .withDescription("Select folder where GitHub Nugend QSpec and QUtil files libs will be downloaded")
        );
        UIUtils.initializerTextBrowseValidator(qSpecFolderField, () -> "Folder can't be empty", () -> "Folder must exist");
        qSpecFolderField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                downloadButton.setEnabled(!qSpecFolderField.getText().trim().isEmpty());
                setOKActionEnabled(false);
            }
        });
        downloadButton.addActionListener(this::startDownloading);

        setOKActionEnabled(false);
        setOKButtonText("Use The Library");

        init();
        setSize(700, 500);
    }

    public Path getDownloadedPath() {
        return Path.of(qSpecFolderField.getText()).resolve("qspec");
    }

    private void startDownloading(ActionEvent e) {
        progressArea.setText("");
        downloadButton.setEnabled(false);

        final Path dest = getDownloadedPath();
        final Application application = ApplicationManager.getApplication();
        activeFuture = application.executeOnPooledThread(() -> {
            try {
                print("Downloading library from " + GITHUB_URL + " to " + dest);
                download(dest);
                print("Done");
                setOKActionEnabled(true);
            } catch (Exception ex) {
                error("File can't be downloaded", ex);
                downloadButton.setEnabled(true);
            }
        });
    }

    private void error(String text, Exception ex) {
        print("ERROR: " + text);
        print(ExceptionUtils.getStackTrace(ex));
    }

    private void print(String text) {
        progressArea.append(text + "\n");
    }

    @Override
    public void doCancelAction() {
        if (activeFuture != null) {
            activeFuture.cancel(true);
        }
        super.doCancelAction();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return myComponent;
    }

    private void download(Path dest) throws IOException {
        final Path tmp = Files.createTempFile("kdbinsidebrains", "qspec");
        try {
            FileUtils.copyURLToFile(new URL(GITHUB_URL), tmp.toFile());
            print("Downloaded. Extracting files...");
            extractFiles(tmp, dest);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void extractFiles(Path source, Path dest) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    name = name.substring(name.indexOf('/') + 1);
                    final Path outFile = dest.resolve(name).normalize();
                    if (!outFile.startsWith(dest.normalize())) {
                        throw new IOException("Bad zip entry: " + name);
                    }
                    print("Extracting '" + name + "' to " + outFile.toAbsolutePath());
                    copyEntryContent(zipIn, outFile);
                }
                zipIn.closeEntry();
            }
        }
    }

    private void copyEntryContent(ZipInputStream zipIn, Path outFile) throws IOException {
        int bytesRead;
        final byte[] buffer = new byte[4096];
        Files.createDirectories(outFile.getParent());
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {
            while ((bytesRead = zipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        }
    }
}