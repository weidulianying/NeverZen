package shit.zen.config;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Getter;

/**
 * Lightweight config metadata — GUI never touches File I/O directly.
 * This is what ConfigsPage renders as cards.
 */
@Getter
public class ConfigData {
    private final String name;
    private final File file;
    private final long createTime;
    private final long size;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ConfigData(String name, File file) {
        this.name = name;
        this.file = file;
        this.createTime = file.exists() ? file.lastModified() : System.currentTimeMillis();
        this.size = file.exists() ? file.length() : 0;
    }

    public String getDateString() {
        return DATE_FMT.format(new Date(createTime));
    }

    public String getSizeString() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    @Override
    public String toString() {
        return name + " (" + getDateString() + ")";
    }
}