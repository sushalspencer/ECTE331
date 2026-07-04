public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("calibrating cpu work units");
        long unitsPerMs = Work.calibrateUnitsPerMs();
        System.out.println("using " + unitsPerMs + " work units per ms on this machine");

        // tasks 1 & 2: normal continuous operation 
        System.out.println();
        System.out.println("TASKS 1 & 2: NORMAL OPERATION");
        MotorController motor = new MotorController(PriorityInversionScenario.CEILING_PRIORITY);
        motor.setMode(PriorityMode.BASELINE);

        RealTimeTask safety = new RealTimeTask("SafetyMonitor", PriorityInversionScenario.HIGH_PRIORITY,
                motor, (int) (unitsPerMs * 20), 300, 5);
        RealTimeTask planner = new RealTimeTask("MotionPlanner", PriorityInversionScenario.MEDIUM_PRIORITY,
                motor, (int) (unitsPerMs * 60), 250, 5);
        RealTimeTask logger = new RealTimeTask("Logger", PriorityInversionScenario.LOW_PRIORITY,
                motor, (int) (unitsPerMs * 15), 200, 5);

        safety.start();
        planner.start();
        logger.start();
        safety.join();
        planner.join();
        logger.join();

        // task 3: priority inversion 
        System.out.println();
        System.out.println("TASK 3: PRIORITY INVERSION");
        PriorityInversionScenario scenario = new PriorityInversionScenario(unitsPerMs);
        PriorityInversionScenario.Result baselineResult = scenario.run(PriorityMode.BASELINE);
        System.out.println("high priority thread waited " + baselineResult.highWaitMs + " ms with no priority management");

        // task 4: priority inheritance
        System.out.println();
        System.out.println("TASK 4: PRIORITY INHERITANCE");
        PriorityInversionScenario.Result inheritResult = scenario.run(PriorityMode.INHERITANCE);
        System.out.println("high priority thread waited " + inheritResult.highWaitMs + " ms with inheritance turned on");

        // task 5: priority ceiling
        System.out.println();
        System.out.println("TASK 5: PRIORITY CEILING");
        PriorityInversionScenario.Result ceilingResult = scenario.run(PriorityMode.CEILING);
        System.out.println("high priority thread waited " + ceilingResult.highWaitMs + " ms with the ceiling protocol turned on");

        System.out.println();
        System.out.println("quick before/after: baseline=" + baselineResult.highWaitMs + "ms, inheritance="
                + inheritResult.highWaitMs + "ms, ceiling=" + ceilingResult.highWaitMs + "ms");

        // task 6: performance evaluation
        PerformanceEvaluation evaluation = new PerformanceEvaluation(8, unitsPerMs);
        evaluation.run();

        System.out.println();
        System.out.println("operation completed");
    }
}
