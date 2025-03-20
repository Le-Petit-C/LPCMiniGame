package lpcminigame.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class FileUtils {
    public static boolean ensureFile(Path filePath){
    if(Files.exists(filePath)) return !Files.isDirectory(filePath);
    if(!ensureDir(filePath.getParent())) return false;
    try {
        Files.createFile(filePath);
        return true;
    } catch (IOException e) {
        return false;
    }
}
    public static boolean ensureDir(Path dirPath){
        try {
            Files.createDirectories(dirPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deletePath(Path path) {
        if(!Files.exists(path)) return true;
        if(Files.isDirectory(path)){
            try (Stream<Path> files = Files.list(path)){
                if (files != null) {
                    for (Iterator<Path> it = files.iterator(); it.hasNext(); ) {
                        Path file = it.next();
                        deletePath(file);
                    }
                }
            } catch (IOException ignore) {return false;}
        }
        try {return Files.deleteIfExists(path);}
        catch (IOException ignore) {return false;}
    }
}
