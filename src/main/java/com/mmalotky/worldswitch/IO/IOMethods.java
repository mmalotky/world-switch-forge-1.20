package com.mmalotky.worldswitch.IO;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class IOMethods {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean deleteDirectory(File directory) {
        if(Files.isSymbolicLink(directory.toPath())) return directory.delete();

        File[] files = directory.listFiles();
        if(files == null) return false;
        else if(files.length == 0) return directory.delete();

        for(File file : files) {
            LOGGER.info(String.format("Deleting %s", file.getName()));
            if(file.isDirectory()) deleteDirectory(file);
            else if(!file.delete()) return false;
        }
        return directory.delete();
    }

    public static void copyDirectory(Path src, Path destination) {
        try {
            Files.walkFileTree(src, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(destination.resolve(src.relativize(dir).toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, destination.resolve(src.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
