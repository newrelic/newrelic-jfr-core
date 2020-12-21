package com.newrelic.jfr.gradle;

import com.netflix.gradle.plugins.deb.Deb;
import com.netflix.gradle.plugins.rpm.Rpm;
import org.beryx.jlink.data.JlinkPluginExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.AppliedPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Tar;
import org.gradle.api.tasks.bundling.Zip;
import org.redline_rpm.header.Os;

import java.io.File;
import java.util.Locale;
import java.util.stream.Stream;

import static org.gradle.api.tasks.bundling.Compression.BZIP2;

public class PackagePlugin implements Plugin<org.gradle.api.Project> {
    public static final String PACKAGE_TASK = "package";
    public static final String GROUP = "distribution";

    @Override
    public void apply(Project project) {
        project.getTasks().register(PACKAGE_TASK, new Action<Task>() {
            @Override
            public void execute(Task task) {
                task.setGroup(GROUP);
                task.setDescription("Life-cycle task for creating packages");
            }
        });

        DirectoryProperty distDir = project.getObjects().directoryProperty();
        distDir.set(new File(project.getBuildDir(), "distributions"));

        configureForApplication(project);
        configureForRpmDeb(project);
        configureForJLink(project, distDir);
    }

    void configureForRpmDeb(Project project) {
        project.getPluginManager().withPlugin("nebula.ospackage", new Action<AppliedPlugin>() {
            @Override
            public void execute(AppliedPlugin appliedPlugin) {
                var tasks = project.getTasks();
                var jlink = project.getExtensions().getByType(JlinkPluginExtension.class);
                var deb = tasks.named("buildDeb", Deb.class);
                var rpm = tasks.named("buildRpm", Rpm.class);
                tasks.named(PACKAGE_TASK).configure(t -> t.dependsOn(deb, rpm));

                Stream.of(deb, rpm).forEach(t -> t.configure(p -> {
                    String type = p.getName().substring(5).toLowerCase(Locale.US);
                    p.setGroup(GROUP);
                    p.setDescription("Bundles the jlink-ed application as a ." + type + " package");
                    p.dependsOn(tasks.named("jlink"));
                    p.setArchStr("X86_64");
                    p.from(project.getRootDir(), cs -> {
                        cs.include("LICENSE", "README.md");
                        cs.into("/usr/share/doc/" + project.getName());
                    });
                    p.from(jlink.getImageDir(), cs -> {
                        cs.include("**");
                        cs.into("/usr/lib/" + project.getName());
                    });
                    p.from("/src/" + type, cs -> {
                        cs.include("**");
                        cs.eachFile(f -> {
                            if (f.getFile().getParentFile().getName().equals("bin")) {
                                f.setMode(0755);
                            }
                        });
                    });
                }));

                rpm.configure(p -> {
                    p.setVendor("New Relic Infrastructure Team <infrastructure-eng@newrelic.com>");
                    p.setPackageGroup("Application/System");
                    p.setLicense("Apache 2.0");
                    p.setRelease("1");

                    p.setOs(Os.LINUX);
                });
            }
        });
    }

    void configureForApplication(Project project) {
        var tasks = project.getTasks();
        project.getPluginManager().withPlugin("application", new Action<AppliedPlugin>() {
            @Override
            public void execute(AppliedPlugin appliedPlugin) {
                tasks.named(PACKAGE_TASK).configure(p -> p.dependsOn(
                        tasks.named("distZip"),
                        tasks.named("distTar")
                ));
                tasks.named("distTar").configure(t -> {
                    ((Tar) t).setCompression(BZIP2);
                    ((Tar) t).getArchiveExtension().set("tar.bz2");
                });
            }
        });
    }

    void configureForJLink(Project project, DirectoryProperty distDir) {
        TaskContainer tasks = project.getTasks();

        project.getPluginManager().withPlugin("org.beryx.jlink", new Action<AppliedPlugin>() {
            @Override
            public void execute(AppliedPlugin appliedPlugin) {
                var zip = tasks.register("jlinkDistZip", Zip.class);
                var tar = tasks.register("jlinkDistTar", Tar.class);
                var jlink = project.getExtensions().getByType(JlinkPluginExtension.class);

                Stream.of(zip, tar).forEach(t -> t.configure(p -> {
                    p.setGroup(GROUP);
                    p.setDescription("Bundles the jlink-ed application as a distribution");
                    p.getDestinationDirectory().set(distDir);
                    p.into(project.getName() + "-" + project.getVersion().toString());
                    p.from(project.getRootDir(), cs -> {
                        cs.include("LICENSE", "README.md");
                    });
                    p.from(jlink.getImageDir(), cs -> {
                        cs.include("**");
                    });
                    p.getArchiveVersion().set(project.provider(() -> project.getVersion().toString()));
                    p.getArchiveBaseName().set(project.getName());
                    p.getArchiveClassifier().set("jlink");
                    p.dependsOn("jlink");
                }));

                zip.configure(t -> t.getArchiveExtension().set("zip"));
                tar.configure(t -> t.getArchiveExtension().set("tar.bz2"));
                tar.configure(t -> t.setCompression(BZIP2));

                tasks.named(PACKAGE_TASK).configure(p -> p.dependsOn(zip, tar));
            }
        });
    }
}
