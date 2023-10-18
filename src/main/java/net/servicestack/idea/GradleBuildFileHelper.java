package net.servicestack.idea;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by Layoric on 2/04/2015.
 * Methods to help insert gradle dependency
 */
public class GradleBuildFileHelper {

    private static final String kotlinGroupName = "org.jetbrains.kotlin";

    public static boolean addDependency(AnActionEvent event,String groupId, String packageName, String version) throws FileNotFoundException {
        File gradleFile = getGradleBuildFile(event);
        if(gradleFile == null) {
            return false;
        }
        int dependenciesStartIndex = -1;
        int dependenciesEndIndex = -1;
        List<String> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(gradleFile));
        try {
            int count = 0;
            for(String line; (line = br.readLine()) != null; ) {
                list.add(line);
                if(dependenciesStartIndex > -1 && line.startsWith("}") && dependenciesEndIndex == -1) {
                    dependenciesEndIndex = count;
                }
                if(dependenciesStartIndex == -1 && line.startsWith("dependencies {")) {
                    dependenciesStartIndex = count;
                }
                count++;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (dependenciesStartIndex == -1 || dependenciesEndIndex == -1) {
            return false;
        }

        boolean dependencyRequired = true;
        //Check if groupId + package already listed as dependency
        for (int i = dependenciesStartIndex; i < dependenciesEndIndex; i++) {
            String dependencyLoC = list.get(i);
            if(dependencyLoC.contains(groupId + ":" + packageName)) {
                dependencyRequired = false;
                break;
            }
        }

        if (!dependencyRequired) {
            return false;
        }
        String buildGradleDep = "    implementation '" + groupId + ":" + packageName + ":" + version + "'";
        String buildGrableKtsDep = "    implementation(\"" + groupId + ":" + packageName + ":" + version + "\")";
        String gradleDep = gradleFile.getName().endsWith(".kts") ? buildGrableKtsDep : buildGradleDep;
        list.add(dependenciesEndIndex, gradleDep);
        try {
            PrintWriter writer = new PrintWriter(gradleFile);
            for(String item : list) {
                writer.println(item);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static Boolean isGradleModule(AnActionEvent event) {
        return getGradleBuildFile(event) != null;
    }

    public static Boolean isDartProject(Module module) {
        return getDartPubspec(module) != null;
    }

    public static boolean isAndroidProject(@NotNull Module module) {
        Facet[] facetsByType = FacetManager.getInstance(module).getAllFacets();
        for (Facet facet : facetsByType) {
            if (facet.getTypeId().toString().equals("android")) {
                return true;
            }
        }
        return false;
    }

    public static Boolean isUsingKotlin(AnActionEvent event){
        if (!isGradleModule(event)) {
            return false;
        }
        File buildFile = getGradleBuildFile(event);
        if (buildFile == null) {
            return false;
        }
        if(buildFile.getName().endsWith(".kts")) {
            return true;
        }
        boolean result = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(buildFile));
            for (String line; (line = br.readLine()) != null; ) {
                if(line.contains(kotlinGroupName)) {
                    result = true;
                    break;
                }
            }
            br.close();
        }  catch (IOException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public static File getGradleBuildFile(AnActionEvent event) {
        VirtualFile vFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        Project project = event.getData(CommonDataKeys.PROJECT);

        if (project == null || vFile == null) {
            return null;
        }

        String projectBase = project.getBasePath();
        if (projectBase == null) {
            return null;
        }

        File projectBaseFile = new File(projectBase);
        projectBase = projectBaseFile.getAbsolutePath();
        String basePath = vFile.isDirectory() ? vFile.getPath() : vFile.getParent().getPath();
        File file = new File(basePath);
        File gradleFile = null;

        int count = 0;
        int maxDepth = 8;

        while (true) {
            File[] matchingFiles = file.listFiles((dir, name) -> name.startsWith("build.gradle"));
            boolean foundFile = matchingFiles != null && matchingFiles.length != 0;

            if (foundFile) {
                gradleFile = matchingFiles[0];
                break;
            }

            // project base even on Windows value is "c:/x/" using the wrong file separator.
            if (file.getAbsolutePath().equals(projectBase) || count >= maxDepth) {
                break;
            }

            count++;
            file = file.getParentFile();
        }
        return gradleFile;
    }


    public static File getDartPubspec(Module module) {
        String projDir = module.getProject().getBasePath();
        if (projDir == null) {
            return null;
        }
        File file = new File(projDir);
        File[] matchingFiles = file.listFiles((dir, name) -> name.startsWith("pubspec.yaml"));
        return matchingFiles == null || matchingFiles.length == 0
            ? null
            : matchingFiles[0];
    }
}
