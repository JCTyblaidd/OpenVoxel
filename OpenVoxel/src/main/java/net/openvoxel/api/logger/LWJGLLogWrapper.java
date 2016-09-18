package net.openvoxel.api.logger;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Created by James on 02/08/2016.
 */
public class LWJGLLogWrapper implements Supplier<PrintStream>{

	public LWJGLLogWrapper() {}



	@SideOnly(side= Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	public static void Load() {
		//Logger.INSTANCE.Info(System.getProperty("org.lwjgl.util.DebugStream"));
		System.setProperty("org.lwjgl.util.DebugStream",LWJGLLogWrapper.class.getName());
		//Logger.INSTANCE.Info(System.getProperty("org.lwjgl.util.DebugStream"));
	}


	@Override
	public PrintStream get() {
		return new PrintStream(new LoggerOutputStream(Logger.getLogger("LWJGL")).setTrim("[LWJGL] "));
	}
}
