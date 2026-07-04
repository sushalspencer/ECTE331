public enum PriorityMode {
    BASELINE,     // just a lock
    INHERITANCE,  // boost the holder when someone more important is waiting
    CEILING       // holder always runs at the ceiling priority the whole time it holds the lock
}