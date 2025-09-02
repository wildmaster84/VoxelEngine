package engine.server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	private enum Level {
		INFO,
		WARN,
		ERROR,
		DEBUG
		
	}
    private final String prefix;

    public Logger(String prefix) {
        this.prefix = prefix;
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warn(String message) {
        log(Level.WARN, message);
    }

    public void error(String message) {
        log(Level.ERROR, message);
    }

    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    private void log(Level level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("[" + timestamp + "] [" + level.name() + "] [" + prefix + "] " + message);
    }
}