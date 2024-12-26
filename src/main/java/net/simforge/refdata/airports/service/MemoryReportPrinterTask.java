package net.simforge.refdata.airports.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class MemoryReportPrinterTask {
    private static final Logger logger = LoggerFactory.getLogger(MemoryReportPrinterTask.class);

    @Scheduled(fixedDelay = 300000)
    public void printMemoryReport() {
        final Runtime runtime = Runtime.getRuntime();
        final long mm = runtime.maxMemory();
        final long fm = runtime.freeMemory();
        final long tm = runtime.totalMemory();
        final String str = "Memory report: Used = " + toMB(tm - fm) + ", " + "Free = " + toMB(fm) + ", " + "Total = " + toMB(tm) + ", " + "Max = " + toMB(mm);
        logger.info(str);
    }

    private static String toMB(final long size) {
        return Long.toString(size / 0x100000L);
    }
}
