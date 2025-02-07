package org.kdb.inside.brains.ide.sdk;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import icons.KdbIcons;
import org.apache.commons.lang3.SystemUtils;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class KdbSdkType extends SdkType {
    private static final Logger log = Logger.getInstance(KdbSdkType.class);

    public KdbSdkType() {
        super("KDB SDK");
    }

    private static boolean isUnix() {
        return SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_MAC;
    }

    @Override
    public Icon getIcon() {
        return KdbIcons.Main.Application;
    }


    @Override
    public @NotNull String suggestSdkName(@Nullable String currentSdkName, @NotNull String sdkHome) {
        final String kdbVersion = getKdbVersion(sdkHome);
        if (kdbVersion == null) {
            return "Unknown KDB";
        }
        return "KDB " + kdbVersion.substring(0, kdbVersion.indexOf(' '));
    }

    public boolean isRootTypeApplicable(@NotNull OrderRootType type) {
        return false;
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        return "KDB SDK";
    }

    protected static File findExecutable(@NotNull File path) {
        final File[] folders = path.listFiles(File::isDirectory);
        if (folders == null) {
            return null;
        }
        for (File folder : folders) {
            final File[] files = folder.listFiles(f -> f.isFile() && f.canExecute());
            if (files == null) {
                return null;
            }
            for (File file : files) {
                if (isWindows() && file.getName().equals("q.exe")) {
                    return file;
                }
                if (isUnix() && file.getName().equals("q")) {
                    return file;
                }
            }
        }
        return null;
    }

    public File getExecutableFile(@NotNull Sdk sdk) {
        if (!Objects.equals(sdk.getSdkType(), this)) {
            return null;
        }
        final String homePath = sdk.getHomePath();
        if (homePath == null) {
            return null;
        }
        return findExecutable(new File(homePath));
    }

    public static KdbSdkType getInstance() {
        return SdkType.findInstance(KdbSdkType.class);
    }

    @Override
    public boolean setupSdkPaths(@NotNull Sdk sdk, @NotNull SdkModel sdkModel) {
        return super.setupSdkPaths(sdk, sdkModel);
    }

    @Nullable
    protected static String getKdbVersion(String path) {
        final File root = new File(path);
        final File executable = findExecutable(root);
        if (executable == null) {
            return null;
        }

        try {
            final Runtime runtime = Runtime.getRuntime();
            final String[] args = {"QHOME=" + root.getAbsolutePath()};
            final Process exec = runtime.exec(executable.getAbsolutePath(), args, root);

            try (BufferedWriter out = exec.outputWriter()) {
                out.write("string[.z.K],\" \",string[.z.k]");
                out.newLine();
                out.write("exit 0");
                out.flush();
            } catch (Exception ex) {
                final String errorMsg = read(exec.errorReader());
                log.warn("Q Version can't be detected from " + path + ":\n" + errorMsg);
                return null;
            }

            try (BufferedReader r = exec.inputReader()) {
                final Optional<String> version = r.lines().reduce((a, b) -> b);
                return version.map(s -> s.substring(1, s.length() - 1)).orElse(null);
            } catch (Exception ex) {
                final String errorMsg = read(exec.errorReader());
                log.warn("Response can't be read from Q process in " + path + ":\n" + errorMsg);
                return null;
            }
        } catch (Exception ex) {
            log.warn("Q Version can't be detected from " + path, ex);
            return null;
        }
    }

    private static String read(BufferedReader reader) {
        return reader.lines().collect(Collectors.joining("\n"));
    }

    @Override
    @Nullable
    public String getVersionString(Sdk sdk) {
        String path = sdk.getHomePath();
        if (path == null) {
            return null;
        }
        return getVersionString(path);
    }

    @Override
    public @Nullable String getVersionString(@NotNull String sdkHome) {
        return getKdbVersion(sdkHome);
    }

    @Override
    public @Nullable AdditionalDataConfigurable createAdditionalDataConfigurable(@NotNull SdkModel sdkModel, @NotNull SdkModificator sdkModificator) {
        return null;
    }

    @Override
    public void saveAdditionalData(@NotNull SdkAdditionalData additionalData, @NotNull Element additional) {
    }

    public static Sdk getModuleSdk(Module module) {
        if (module == null) {
            return null;
        }
        final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null || !sdk.getSdkType().equals(KdbSdkType.getInstance())) {
            return null;
        }
        return sdk;

    }

    @Override
    public @Nullable String suggestHomePath() {
        final String qhome = System.getenv().get("QHOME");
        if (qhome != null) {
            return qhome;
        }
        if (isWindows()) {
            return "c:/q";
        } else if (isUnix()) {
            return "~/q";
        }
        return null;
    }

    @Override
    public boolean isValidSdkHome(@NotNull String path) {
        final File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            return findExecutable(file) != null;
        }
        return false;
    }

    private static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }
}