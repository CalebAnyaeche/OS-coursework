package nachos.threads;

import java.util.LinkedList;
import java.util.List;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    private Lock lock;
    private List<ThreadInfo> speakers;
    private List<ThreadInfo> listeners;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
        speakers = new LinkedList<>();  // Chosen due to need to remove from start of list.
        listeners = new LinkedList<>();
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param word the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();

        if (! listeners.isEmpty()) {
            ThreadInfo listener = listeners.remove(0);
            listener.word = word;
            listener.condition.wake();
        } else {
            ThreadInfo speaker = new ThreadInfo(lock, word);
            speakers.add(speaker);
            speaker.condition.sleep();
        }

        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        lock.acquire();

        int word;

        if (! speakers.isEmpty()) {
            ThreadInfo speaker = speakers.remove(0);
            word = speaker.word;
            speaker.condition.wake();
        } else {
            ThreadInfo listener = new ThreadInfo(lock, 0);  // 0 is just a placeholder.
            listeners.add(listener);
            listener.condition.sleep();
            word = listener.word;
        }

        lock.release();

        return word;
    }

    private static class ThreadInfo {
        Condition condition;
        int word;

        ThreadInfo(Lock lock, int word) {
            this.condition = new Condition(lock);
            this.word = word;
        }
    }
}
