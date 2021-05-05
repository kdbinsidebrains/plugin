package org.kdb.inside.brains.ide.sdk;

import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import icons.KdbIcons;
import org.apache.commons.lang.SystemUtils;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.Objects;

public class KdbSdkType extends SdkType {
    public KdbSdkType() {
        super("KDB SDK");
    }

    @Override
    public @Nullable String suggestHomePath() {
        final String qhome = System.getenv().get("QHOME");
        if (qhome != null) {
            return qhome;
        }
        if (isWindows()) {
            return "c:/q";
        } else if (isLinux()) {
            return "~/q";
        }
        return null;
    }

    @Override
    public boolean isValidSdkHome(String path) {
        final File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            return findExecutable(file) != null;
        }
        return false;
    }

    @Override
    public Icon getIcon() {
        return KdbIcons.Main.Application;
    }

    @Override
    public @NotNull Icon getIconForAddAction() {
        return getIcon();
    }

    @Override
    public @NotNull String suggestSdkName(@Nullable String currentSdkName, String sdkHome) {
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

    @Override
    @Nullable
    public String getVersionString(Sdk sdk) {
        String path = sdk.getHomePath();
        if (path == null) {
            return null;
        }
        final String kdbVersion = getKdbVersion(path);
        return kdbVersion == null ? "Undefined" : kdbVersion;
    }

    @Nullable
    private String getKdbVersion(String path) {
        final File executable = findExecutable(new File(path));
        if (executable == null) {
            return null;
        }

        try {
            final Runtime runtime = Runtime.getRuntime();
            final Process exec = runtime.exec(executable.getAbsolutePath());

            final InputStream in = exec.getInputStream();
            final OutputStream out = exec.getOutputStream();

            out.write("string[.z.K],\" \",string[.z.k]\n".getBytes());
            out.write("`the_end_of_the_command\n".getBytes());
            out.flush();

            BufferedReader r = new BufferedReader(new InputStreamReader(in));

            String lastLine = null;
            String s = r.readLine();
            while (s != null && !"`the_end_of_the_command".equals(s)) {
                lastLine = s;
                s = r.readLine();
            }
            exec.destroy();
            if (lastLine == null) {
                return null;
            }
            // cut quotes
            return lastLine.substring(1, lastLine.length() - 1);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public @Nullable AdditionalDataConfigurable createAdditionalDataConfigurable(@NotNull SdkModel sdkModel, @NotNull SdkModificator sdkModificator) {
        return null;
    }

    @Override
    public void saveAdditionalData(@NotNull SdkAdditionalData additionalData, @NotNull Element additional) {
    }

    private static File findExecutable(@NotNull File path) {
        for (File folder : path.listFiles(File::isDirectory)) {
            for (File file : folder.listFiles(f -> f.isFile() && f.canExecute())) {
                if (isWindows() && file.getName().equals("q.exe")) {
                    return file;
                }
                if (isWindows() && file.getName().equals("q")) {
                    return file;
                }
            }
        }
        return null;
    }

    private static boolean isLinux() {
        return SystemUtils.IS_OS_LINUX;
    }

    private static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }
}