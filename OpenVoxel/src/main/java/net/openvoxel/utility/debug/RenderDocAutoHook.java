package net.openvoxel.utility.debug;

import java.lang.management.ManagementFactory;

/**
 * Created by James on 09/04/2017.
 *
 * Automatically Start and Inject RenderDoc
 */
public class RenderDocAutoHook {

	private static int getPID() {
		long processID = ProcessHandle.current().pid();
		System.out.println("Process ID[debug]=" + processID);
		return (int)processID;
	}

	public static void callRenderDocInject() {
		int PID = getPID();
		String cmdLine1 = "cd \"C:\\Program Files\\RenderDoc\"";
		String cmdLine2 = "renderdoccmd.exe inject --PID="+PID;
		try {
			//Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe","/c","start"});
			//TODO: implement?
			System.in.read();
		}catch (Exception ignores) {}
	}

}
