package com.bbn.marti.takcl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SystemMonitor {

	private static final Logger logger = LoggerFactory.getLogger(SystemMonitor.class);

	public static class MemStat {
		private static final int IDX_TOTAL = 0;
		private static final int IDX_FREE = 1;
		private static final int IDX_AVAILABLE = 2;

		private int totalMemory = -1;
		private int availableMemory = -1;

		private synchronized int checkTotalMemoryKB() {
			if (totalMemory == -1) {
				refresh();
			}
			return totalMemory;
		}

		public synchronized int checkAvailableMemoryKB() {
			refresh();
			return availableMemory;
		}

		public synchronized void refresh() {
			try {
				ProcessBuilder pb = new ProcessBuilder()
						.redirectErrorStream(true)
						.command("cat", "/proc/meminfo");
				Process p = pb.start();
				p.waitFor();
				if (p.exitValue() == 0) {
					String val = new String(p.getInputStream().readAllBytes()).strip();
					String[] vals = val.split("\n");

					totalMemory = Integer.parseInt(vals[IDX_TOTAL].split("\\s+")[1]);
					availableMemory = Integer.parseInt(vals[IDX_AVAILABLE].split("\\s+")[1]);
				}
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class CpuStat {
		private static final int IDX_USER = 0;
		private static final int IDX_NICE = 1;
		private static final int IDX_SYSTEM = 2;
		private static final int IDX_IDLE = 3;
		private static final int IDX_IOWAIT = 4;
		private static final int IDX_IRQ = 5;
		private static final int IDX_SOFTIRQ = 6;
		private static final int IDX_STEAL = 7;

		private long[] previousCpuStats = null;

		private static long getIdle(long[] statValues) {
			return statValues[IDX_IDLE] + statValues[IDX_IOWAIT];
		}

		private static long getNonIdle(long[] statValues) {
			return statValues[IDX_USER] + statValues[IDX_NICE] + statValues[IDX_SYSTEM] + statValues[IDX_IRQ] + statValues[IDX_SOFTIRQ] + statValues[IDX_STEAL];
		}

		private static float calculateCpuPercentage(long[] prevStatValues, long[] currentStatValues) {
			long prevIdle = getIdle(prevStatValues);
			long currIdle = getIdle(currentStatValues);

			long prevNonIdle = getNonIdle(prevStatValues);
			long currNonIdle = getNonIdle(currentStatValues);

			long prevTotal = prevIdle + prevNonIdle;
			long currTotal = currIdle + currNonIdle;

			float totalD = currTotal - prevTotal;
			float idleD = currIdle - prevIdle;

			return (totalD - idleD) / totalD;
		}

		public double cpuPercentage() {
			double result = -1;
			try {
				String[] mainCpuString = null;

				ProcessBuilder pb = new ProcessBuilder()
						.redirectErrorStream(true)
						.command("cat", "/proc/stat");
				Process p = pb.start();
				p.waitFor();
				if (p.exitValue() == 0) {
					mainCpuString = new String(pb.start().getInputStream().readAllBytes()).strip().split("\n")[0].split("\\s+");

					long[] currentCpuStats = new long[mainCpuString.length - 1];
					for (int i = 0; i < mainCpuString.length - 1; i++) {
						currentCpuStats[i] = Long.parseLong(mainCpuString[i + 1]);
					}

					if (previousCpuStats != null) {
						result = (double) CpuStat.calculateCpuPercentage(previousCpuStats, currentCpuStats);
					}
					previousCpuStats = currentCpuStats;
				}
				return result;
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final NumberFormat nf = NumberFormat.getInstance(new Locale("en", "US"));

	private final long logFrequencyMS;
	private final CpuStat cpuStat;
	private final MemStat memStat;
	private Timer timer;

	public SystemMonitor(int logFrequencyMS) {
		this.logFrequencyMS = logFrequencyMS;
		this.cpuStat = new CpuStat();
		this.memStat = new MemStat();
	}

	public synchronized void start() {
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				double cp = cpuStat.cpuPercentage();
				int memAvailable = memStat.checkAvailableMemoryKB();
				int memTotal = memStat.checkTotalMemoryKB();

				logger.info("CPU: " + (int) (cp * 100) + "%, Mem: " + nf.format(memAvailable) + "/" + nf.format(memTotal) + " KiB");
			}
		}, logFrequencyMS, logFrequencyMS);
	}

	public synchronized void stop() {
		timer.cancel();
	}
}
