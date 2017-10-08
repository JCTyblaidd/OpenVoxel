package net.openvoxel.statistics;

import oshi.SystemInfo;

import java.lang.management.ManagementFactory;

/**
 * Created by James on 15/04/2017.
 *
 * Returns Execution Statistics
 *
 * TODO: find a method to track MemoryUtil allocations
 *
 */
public class SystemStatistics {

	private static SystemInfo systemInfo;
	private static long processMemUsage = 0;
	private static long jvmMemUsage = 0;
	private static final int processorCount = Runtime.getRuntime().availableProcessors();
	private static int threadCount = 0;
	private static long deamonThreadCount = 0;
	private static double processorUsage = 0.0F;

	private static int updateCountdown = 0;
	private static final ProcessHandle currentProcess;
	private static long cpuUsageTime;
	private static long cpuMeasurement;
	static {
		systemInfo = new SystemInfo();
		currentProcess = ProcessHandle.current();
		currentProcess.info().totalCpuDuration().ifPresent(duration -> cpuUsageTime = duration.toNanos());
		cpuMeasurement = System.nanoTime();
	}

	public static void requestUpdate() {
		updateCountdown++;
		if(updateCountdown > 100) {
			updateCountdown = 0;
			update();
		}
	}

	private static void update() {
		long prevUsage = cpuUsageTime;
		long prevTime = cpuMeasurement;
		currentProcess.info().totalCpuDuration().ifPresent((duration)-> {
			cpuUsageTime = duration.toNanos();
			cpuMeasurement = System.nanoTime();
		});
		processorUsage = (double)(cpuUsageTime - prevUsage) / (double)(cpuMeasurement - prevTime);
		//
		processMemUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() + MemoryStatistics.getChunkMemory();
		jvmMemUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
		/**try {
			processorUsage = (Double)ManagementFactory.getPlatformMBeanServer().
                           getAttribute(ObjectName.getInstance("java.lang:type=OperatingSystem"),"ProcessCpuLoad");
		}catch (Exception ignored) {ignored.printStackTrace();}**/
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

}
