package net.openvoxel.utility;

import net.openvoxel.api.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 15/09/2016.
 *
 * Detailed Error Information
 */
public class CrashReport extends RuntimeException{

	private String mainError;
	private List<String> state = new ArrayList<>();

	public CrashReport(String mainError) {
		super("==Reported Crash==");
		this.mainError = mainError;
	}

	public CrashReport invalidState(String error) {
		return _report("Invalid State: " + error);
	}

	public CrashReport unexpectedNull(String variable) {
		return _report("Unexpected Null: null="+variable);
	}

	public CrashReport caughtException(Exception e) {
		_report("Exception: " + e.getClass().getSimpleName());
		return _report("  "+e.getMessage());
	}

	public CrashReport withReport(CrashReport otherReport) {
		_report("===Attached Report=====");
		_report(otherReport.mainError);
		otherReport.state.forEach(this::_report);
		return this;
	}

	private CrashReport _report(String str) {
		state.add(str);
		return this;
	}

	@Override
	public void printStackTrace() {
		Logger.INSTANCE.Severe("==Reported Crash===");
		state.forEach(Logger.INSTANCE::Severe);
		super.printStackTrace();
	}
}
