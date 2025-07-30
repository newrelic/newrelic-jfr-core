## JFR Telemetry Data

Below is a summary of the various New Relic events and dimensional metrics that are reported by the JFR daemon.

### Events

Below is a list of the New Relic events reported by JFR daemon and links to the mappers that convert the JFR data into event data.

* [JfrCompilation](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/toevent/JITCompilationMapper.java#L43)
* [JfrFlameLevel](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/profiler/ProfileSummarizer.java#L30)
* [JfrJavaMonitorWait](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/toevent/ThreadLockEventMapper.java#L32)
* JfrJVMInformation ([here](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/toevent/JVMInformationMapper.java#L39) and [here](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/toevent/JVMSystemPropertyMapper.java#L31))
* [JfrMethodSample](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/toevent/MethodSampleMapper.java#L45)
* [JfrValhallaVBCSync](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/toevent/ValhallaVBCDetector.java#L32)

There’s a lot of processing of stack traces involved in producing the **JfrFlameLevel** events that drive the Flamegraph charts and that logic is largely encapsulated in the [profiler mappers](https://github.com/newrelic/newrelic-jfr-core/tree/main/jfr-mappers/src/main/java/com/newrelic/jfr/profiler).

### Dimensional Metrics

Below is a list of the dimensional metrics reported by JFR daemon and links to the mappers that convert the JFR data into dimensional metric data.

* [jfr.AllocationRequiringGC.allocationSize](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/AllocationRequiringGCMapper.java#L26-L27) (Gauge metric)
* [jfr.ThreadContextSwitchRate](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/ContextSwitchRateMapper.java#L26) (Gauge metric)
* [jfr.ThreadCPULoad.user](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/CPUThreadLoadMapper.java#L36) (Gauge metric)
* [jfr.ThreadCPULoad.system](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/CPUThreadLoadMapper.java#L37) (Gauge metric)
* [jfr.GarbageCollection.longestPause](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/GarbageCollectionMapper.java#L25-L26) (Gauge metric)
* [jfr.GCHeapSummary.heapCommittedSize](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/GCHeapSummaryMapper.java#L32-L33) (Gauge metric)
* [jfr.GCHeapSummary.reservedSize](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/GCHeapSummaryMapper.java#L34) (Gauge metric)
* [jfr.GCHeapSummary.heapUsed](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/GCHeapSummaryMapper.java#L35) (Gauge metric)
* [jfr.MetaspaceSummary.metaspace.committed](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.metaspace.used](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.metaspace.reserved](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.dataSpace.committed](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.dataSpace.used](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.dataSpace.reserved](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.classSpace.committed](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.classSpace.used](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.MetaspaceSummary.classSpace.reserved](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/MetaspaceSummaryMapper.java#L25) (Gauge metric)
* [jfr.CPULoad.jvmUser](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/OverallCPULoadMapper.java#L28) (Gauge metric)
* [jfr.CPULoad.jvmSystem](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/OverallCPULoadMapper.java#L29) (Gauge metric)
* [jfr.CPULoad.machineTotal](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/OverallCPULoadMapper.java#L30) (Gauge metric)
* [jfr.ThreadAllocationStatistics.allocated](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tometric/ThreadAllocationStatisticsMapper.java#L28-L29) (Gauge metric)
* [jfr.GarbageCollection.minorDuration](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/BasicGarbageCollectionSummarizer.java#L31-L32) (Summary metric)
* [jfr.GarbageCollection.majorDuration](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/BasicGarbageCollectionSummarizer.java#L33-L34) (Summary metric)
* [jfr.G1GarbageCollection.duration](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/G1GarbageCollectionSummarizer.java#L21-L22) (Summary metric)
* [jfr.GarbageCollection.duration](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/GCHeapSummarySummarizer.java#L93) (Summary metric)
* [jfr.SocketRead.duration](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/PerThreadNetworkReadSummarizer.java#L19) (Summary metric)
* [jfr.SocketRead.bytesRead](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/PerThreadNetworkReadSummarizer.java#L20) (Summary metric)
* [jfr.SocketWrite.bytesWritten](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/PerThreadNetworkWriteSummarizer.java#L20) (Summary metric)
* [jfr.SocketWrite.duration](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/PerThreadNetworkWriteSummarizer.java#L21) (Summary metric)
* [jfr.ObjectAllocationInNewTLAB.allocation](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/PerThreadObjectAllocationInNewTLABSummarizer.java#L22-L23) (Summary metric)
* [jfr.ObjectAllocationOutsideTLAB.allocation](https://github.com/newrelic/newrelic-jfr-core/blob/487dfe87752d55ee768765b37be131e31b73c76f/jfr-mappers/src/main/java/com/newrelic/jfr/tosummary/PerThreadObjectAllocationOutsideTLABSummarizer.java#L18-L19) (Summary metric)