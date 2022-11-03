This branch contains a prototype of the JFR-daemon using JFR streaming instead of the JFR recording as in the main branch.

Java 17 is required to compile it.

Memory allocation is decreased by 50%.

It needs more finesse to try to keep the code more aligned with the Java 8 version.

Some adjustments are needed on the configuration of the JFR events. To test these, it is probably easier to run the daemon as a separate process.