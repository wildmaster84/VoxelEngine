package engine.plugin;

import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

public class PluginDescriptor {
    public static String getMainClassName(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry entry = jar.getEntry("plugin.yml");
            if (entry == null) return null;
            try (InputStream is = jar.getInputStream(entry)) {
                Map<String, String> yml = parseSimpleYaml(is);
                return yml.get("main");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Very simple YAML parser, only for 'key: value' lines
    private static Map<String, String> parseSimpleYaml(InputStream is) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (java.util.Scanner scanner = new java.util.Scanner(is)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf(':');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    map.put(key, value);
                }
            }
        }
        return map;
    }
}