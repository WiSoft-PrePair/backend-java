package io.wisoft.prepair.prepair_api.external.storage;

final class FileExtensions {

    private static final String DEFAULT = ".tmp";

    private FileExtensions() {}

    static String extract(String filename) {
        if (filename == null || filename.isBlank()) return DEFAULT;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return DEFAULT;
        return filename.substring(idx);
    }
}
