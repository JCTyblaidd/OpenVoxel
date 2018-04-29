package net.openvoxel.utility;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.api.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by James on 15/09/2016.
 *
 * Detailed Error Information
 */
public class CrashReport {

	private final String mainError;
	private List<Object> state = new ArrayList<>();
	private final List<StackWalker.StackFrame> frames;

	@PublicAPI
	public CrashReport(String mainError) {
		super();
		this.mainError = mainError;
		frames = StackWalker.getInstance().walk(s -> s.collect(Collectors.toList()));
	}

	@PublicAPI
	public CrashReport invalidState(String error) {
		return _report("Invalid State: " + error);
	}

	@PublicAPI
	public CrashReport unexpectedNull(String variable) {
		return _report("Unexpected Null: null="+variable);
	}

	@PublicAPI
	public CrashReport caughtException(Exception e) {
		if(e instanceof CrashReportException) {
			CrashReportException reportEx = (CrashReportException)e;
			return withReport(reportEx.report);
		}else {
			return _report(e);
		}
	}

	@PublicAPI
	public CrashReport withReport(CrashReport otherReport) {
		return _report(otherReport);
	}

	private CrashReport _report(Object obj) {
		state.add(obj);
		return this;
	}

	public RuntimeException getThrowable() {
		return new CrashReportException(this);
	}

	private static class CrashReportException extends RuntimeException {
		private CrashReport report;
		private CrashReportException(CrashReport report) {
			this.report = report;
		}

		@Override
		public void printStackTrace() {
			Logger _logger = Logger.getLogger("Crash Report");
			//TODO: REMOVE DEPENDENCY ON SPECIAL SYNCHRONISATION
			synchronized (Logger.class) {
				String report_str = report.toString();
				String[] split = report_str.split("\n");
				for(String str : split) {
					_logger.Severe(str);
				}
			}
		}
	}

	private String toStringInternal(String prelim1) {
		String prelim2 = prelim1 + "  ";

		StringBuilder builder  = new StringBuilder();
		builder.append(prelim1).append("CrashReport: ").append(mainError).append('\n');
		builder.append(prelim1).append(" Caused By:").append('\n');
		for(Object stateObj : state) {
			if(stateObj instanceof CrashReport) {
				CrashReport subCrash = (CrashReport)stateObj;
				String subCrashString = subCrash.toStringInternal(prelim2);
				builder.append(subCrashString);
			}else if(stateObj instanceof Exception) {
				Exception except = (Exception)stateObj;
				builder.append(prelim2).append(except.toString()).append('\n');
				for(StackTraceElement element : except.getStackTrace()) {
					builder.append(prelim2).append(" at ").append(element.toString()).append('\n');
				}
			}else{
				builder.append(prelim2).append(stateObj.toString()).append('\n');
			}
		}

		builder.append(prelim1).append(" Stack Trace:").append('\n');
		for(StackWalker.StackFrame frame : frames) {
			builder.append(prelim2).append("at ").append(frame.toString()).append('\n');
		}

		return builder.toString();
	}

	@Override
	public String toString() {
		return toStringInternal("");
	}
}
