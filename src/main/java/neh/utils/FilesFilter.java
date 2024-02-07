package neh.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FilesFilter {
    private static final Comparator DIR_FIRST_AND_UNSENSITIVE_COMP = new Comparator() {
        public int compare(Object o1, Object o2) {
            File f1 = (File)o1;
            File f2 = (File)o2;
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else {
                return !f1.isDirectory() && f2.isDirectory() ? 1 : f1.getName().compareToIgnoreCase(f2.getName());
            }
        }
    };

    private FilesFilter() {
    }

    private static File[] sortByFileName(File[] files) {
        Arrays.sort(files, DIR_FIRST_AND_UNSENSITIVE_COMP);
        return files;
    }

    private static File[] showHidden(File dir, boolean showHidden) {
        return showHidden ? dir.listFiles() : dir.listFiles((pathname) -> {
            return !pathname.isHidden();
        });
    }

    public static File[] sortByFileNameAndShowHidden(File dir, boolean showHidden) {
        return sortByFileName(showHidden(dir, showHidden));
    }
}
