public class RealTimeTask extends Thread {

    private final MotorController motor;
    private final int workUnits;   // how much work this thread does once it has the motor
    private final long periodMs;   // how long to wait between attempts
    private final int iterations;

    public RealTimeTask(String name, int priority, MotorController motor,
                         int workUnits, long periodMs, int iterations) {
        super(name);
        setPriority(priority);
        this.motor = motor;
        this.workUnits = workUnits;
        this.periodMs = periodMs;
        this.iterations = iterations;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; i++) {
            System.out.println(Log.ts() + " " + getName() + " wants the motor (attempt " + (i + 1) + "/" + iterations + ")");
            motor.access(getName(), () -> Work.burn(workUnits));

            try {
                Thread.sleep(periodMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println(Log.ts() + " " + getName() + " is done for this run");
    }
}
