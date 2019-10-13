package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });

        this.waitQueue = new PriorityQueue<>();
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        boolean oldInterruptState = Machine.interrupt().disable();

        while (! waitQueue.isEmpty()) {
            if (waitQueue.peek().wakeupTime <= Machine.timer().getTime()) {
                waitQueue.poll().thread.ready();
            } else {
                break;
            }
        }

        Machine.interrupt().restore(oldInterruptState);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param x the minimum number of clock ticks to wait.
     * @see nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        /* 1. Disable interrupts
         * 2. Determine the approximate time at which the thread should wake up.
         * 3. Queue the current thread, then put it to sleep.
         * 4. Restore interrupts.
         */
        boolean oldInterruptState = Machine.interrupt().disable();

        long wakeupTime = Machine.timer().getTime() + x;

        waitQueue.add(new TimedWaitingProcess(KThread.currentThread(), wakeupTime));

        KThread.sleep();

        Machine.interrupt().restore(oldInterruptState);
    }

    private class TimedWaitingProcess implements Comparable<TimedWaitingProcess> {
        KThread thread;
        long wakeupTime;

        TimedWaitingProcess(KThread thread, long wakeupTime) {
            this.thread = thread;
            this.wakeupTime = wakeupTime;
        }

        public int compareTo(TimedWaitingProcess other) {
            return Long.compare(this.wakeupTime, other.wakeupTime);
        }
    }

    private PriorityQueue<TimedWaitingProcess> waitQueue;
}
