package net.openvoxel.api.logger;

import java.io.PrintStream;

/**
 * Created by James on 01/08/2016.
 *
 * Logging Framework
 */
public class Logger {

	public static Logger INSTANCE;

	static {
		INSTANCE = new Logger(System.out);
		setLogLevel(LogLevel.DEBUG);
	}

	private PrintStream stream;
	private String deliminator;
	private static LogLevel current_level;

	public enum LogLevel {
		TRACE,
		DEBUG,//0
		INFO,//1
		WARNING,//2
		SEVERE,//3
		NONE//4
	}


	public Logger(PrintStream stream) {
		this.stream = stream;
		this.deliminator = "";
	}

	private Logger(PrintStream stream, String deliminator) {
		this.stream = stream;
		this.deliminator = deliminator;
	}

	public Logger getSubLogger(String delim) {
		return new Logger(stream,deliminator + "["+delim+"] ");
	}

	public static Logger getLogger(String name) {
		return INSTANCE.getSubLogger(name);
	}

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

	private boolean shouldLog(LogLevel level) {
		return level != LogLevel.NONE && level.ordinal() >= current_level.ordinal();
	}

	public boolean isEnabled(LogLevel level) {
		return shouldLog(level);
	}

	public void Log(String log) {
		synchronized (Logger.class) {
			_preOut();
			_reset();
			_print(log);
		}
	}

	public void Trace(String trace) {
		if(!shouldLog(LogLevel.TRACE)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("TRACE", 36);
			_reset();
			_print(trace);
		}
	}

	public void Debug(String debug) {
		if(!shouldLog(LogLevel.INFO)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("DEBUG", 36);
			_reset();
			_print(debug);
		}
	}

	public void Info(String info) {
		if(!shouldLog(LogLevel.INFO)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("INFO", 36);
			_reset();
			_print(info);
		}
	}

	public void Warning(String warning) {
		if(!shouldLog(LogLevel.WARNING)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("WARNING", 33);
			_reset();
			_print(warning);
		}
	}
	public void Severe(String severe) {
		if(!shouldLog(LogLevel.SEVERE)) return;
		synchronized (Logger.class) {
			_preOut();
			_type("SEVERE", 31);
			_reset();
			_print(severe);
		}
	}

	public void StackTrace(Throwable throwable) {
		if(!shouldLog(LogLevel.SEVERE)) return;
		synchronized (Logger.class) {
			Severe("Exception Caught: " + throwable.getMessage());
			throwable.printStackTrace(stream);
		}
	}

}
