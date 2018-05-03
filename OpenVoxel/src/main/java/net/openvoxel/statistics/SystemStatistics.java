package net.openvoxel.statistics;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by James on 15/04/2017.
 *
 * Returns Execution Statistics
 *
 * TODO: find a method to track MemoryUtil allocations
 *
 */
public class SystemStatistics {

	public static AtomicLong updateGraphicsLocalMemUsage = new AtomicLong(0);
	public static AtomicLong updateGraphicsGPUMemUsage = new AtomicLong(0);

	private static long processMemUsage = 0;
	private static long jvmMemUsage = 0;
	private static long graphicsLocalMemUsage = 0;
	private static long graphicsGPUMemUsage = 0;

	private static final int processorCount = Runtime.getRuntime().availableProcessors();
	private static int threadCount = 0;
	private static double processorUsage = 0.0F;

	private static int updateCountdown = 0;

	public static double[] processor_history = new double[32];
	public static double[] graphics_history = new double[32];
	public static double[] graphics_world_history = new double[32];
	public static double[] graphics_gui_history = new double[32];
	//public static long[] memory_history = new long[32];
	public static int write_index = 0;


	public static void requestUpdate() {
		updateCountdown++;
		if(updateCountdown > 100) {
			updateCountdown = 0;
			update();
		}
	}

	private static void update() {
		processMemUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() + MemoryStatistics.getChunkMemory();
		jvmMemUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
		try {
			processorUsage = (Double)ManagementFactory.getPlatformMBeanServer().
                           getAttribute(ObjectName.getInstance("java.lang:type=OperatingSystem"),"ProcessCpuLoad");
		}catch (Exception ignored) {ignored.printStackTrace();}

		write_index = (write_index + 1) % 32;
		//memory_history[write_index] = processMemUsage;
		processor_history[write_index] = processorUsage;

		graphicsLocalMemUsage = updateGraphicsLocalMemUsage.get();
		graphicsGPUMemUsage = updateGraphicsGPUMemUsage.get();
	}

	public static long getProcessMemoryUsage() {
		return processMemUsage;
	}

	public static long getJVMMemoryUsage() {
		return jvmMemUsage;
	}

	public static int getProcessorCount() {
		return processorCount;
	}

	public static double getProcessingUsage() {
		return processorUsage;
	}

	public static int getThreadCount() {
		return threadCount;
	}

	public static long getGraphicsGpuMemoryUsage() {
		return graphicsGPUMemUsage;
	}

	public static long getGraphicsLocalMemoryUsage() {
		return graphicsLocalMemUsage;
	}
}
