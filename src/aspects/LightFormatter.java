package aspects;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LightFormatter extends Formatter {

	public String format(LogRecord r) {
		return r.getMessage() + "\n";
	}

}
