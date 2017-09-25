package net.openvoxel.api.logger;

import net.openvoxel.api.PublicAPI;

import java.io.PrintStream;

/**
 * Created by James on 01/08/2016.
 *
 * Logging Framework
 */
public class Logger {

	@PublicAPI
	public static Logger INSTANCE;

	static {
		INSTANCE = new Logger(System.out);
		setLogLevel(LogLevel.DEBUG);
	}

	private PrintStream stream;
	private String deliminator;
	private static LogLevel current_level;

	@PublicAPI
	public enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARNING,
		SEVERE,
		NONE
	}


	private Logger(PrintStream stream) {
		this.stream = stream;
		this.deliminator = "";
	}

	private Logger(PrintStream stream, String deliminator) {
		this.stream = stream;
		this.deliminator = deliminator;
	}

	@PublicAPI
	public Logger getSubLogger(String delim) {
		return new Logger(stream,deliminator + "["+delim+"] ");
	}

	@PublicAPI
	public static Logger getLogger(String name) {
		return INSTANCE.getSubLogger(name);
	}

	@PublicAPI
	public static void setLogLevel(LogLevel level) {
		current_level = level;
	}

	//OUTPUT//
	private void _preOut() {
		stream.print((char)27 + "[34m");
		stream.print(deliminator);
	}

	private void _type(String type, int col) {
		stream.print((char)27 + "["+col+"m");
		stream.print("["+type+"] ");
	}
	private void _reset() {
		stream.print((char)27 + "[0m");
	}
	private void _print(String raw) {
		stream.println(raw);
	}

	private String _merge(Object... objects) {
		StringBuilder builder = new StringBuilder();
		for(Object object : objects) {
			builder.append(object.toString());
		}
		return builder.toString();
	}

	private boolean shouldLog(LogLevel level) {
		return level != LogLevel.NONE && level.ordinal() >= current_level.ordinal();
	}

	@PublicAPI
	public boolean isEnabled(LogLevel level) {
		return shouldLog(level);
	}

	@PublicAPI
	public void Log(Object... objects) {
		Log(_merge(objects));
	}

	@PublicAPI
	public void Log(String log) {
		synchronized (Logger.class) {
			_preOut();
			_reset();
			_print(log);
		}
	}

	@PublicAPI
	public void Trace(Object... objects) {
		Trace(_merge(objects));
	}

	@PublicAPI
	public void Trace(String trace) {
		if(!shouldLog(LogLevel.TRACE)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("TRACE", 36);
			_reset();
			_print(trace);
		}
	}

	@PublicAPI
	public void Debug(Object... objects) {
		Debug(_merge(objects));
	}

	@PublicAPI
	public void Debug(String debug) {
		if(!shouldLog(LogLevel.INFO)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("DEBUG", 36);
			_reset();
			_print(debug);
		}
	}

	@PublicAPI
	public void Info(Object... objects) {
		Info(_merge(objects));
	}

	@PublicAPI
	public void Info(String info) {
		if(!shouldLog(LogLevel.INFO)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("INFO", 36);
			_reset();
			_print(info);
		}
	}

	@PublicAPI
	public void Warning(Object... objects) {
		Warning(_merge(objects));
	}

	@PublicAPI
	public void Warning(String warning) {
		if(!shouldLog(LogLevel.WARNING)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("WARNING", 33);
			_reset();
			_print(warning);
		}
	}

	@PublicAPI
	public void Severe(Object... objects) {
		Severe(_merge(objects));
	}

	@PublicAPI
	public void Severe(String severe) {
		if(!shouldLog(LogLevel.SEVERE)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("SEVERE", 31);
			_reset();
			_print(severe);
		}
	}

	@PublicAPI
	public void StackTrace(Throwable throwable) {
		if(!shouldLog(LogLevel.SEVERE)) return;
		synchronized (Logger.class) {
			Severe("Exception Caught: " + throwable.getMessage());
			throwable.printStackTrace(stream);
		}
	}

}
