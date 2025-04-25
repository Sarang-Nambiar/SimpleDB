package simpledb.transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PageLock manages the low-level locking logic for a single page.
 */
public class PageLock {
    private final Set<TransactionId> activeTransactions;
    private final Map<TransactionId, Boolean> pendingRequests;
    private boolean isExclusive;
    private int readers;
    private int writers;

    public PageLock() {
        this.activeTransactions = new HashSet<TransactionId>();
        this.pendingRequests = new HashMap<TransactionId, Boolean>();
        this.isExclusive = false;
        this.readers = 0;
        this.writers = 0;
    }

    public void lockShared(TransactionId tid) {
        if (activeTransactions.contains(tid) && !isExclusive) return;

        pendingRequests.put(tid, false);
        synchronized (this) {
            try {
                while (writers > 0) {
                    this.wait();
                }
                ++readers;
                activeTransactions.add(tid);
                isExclusive = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        pendingRequests.remove(tid);
    }

    public void lockExclusive(TransactionId tid) {
        if (activeTransactions.contains(tid) && isExclusive) return;

        if (pendingRequests.containsKey(tid) && pendingRequests.get(tid)) return;
        pendingRequests.put(tid, true);

        synchronized (this) {
            try {
                if (activeTransactions.contains(tid)) {
                    while (activeTransactions.size() > 1) {
                        this.wait();
                    }
                    unlockSharedInternal(tid);
                }

                while (readers != 0 || writers != 0) {
                    this.wait();
                }

                ++writers;
                activeTransactions.add(tid);
                isExclusive = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        pendingRequests.remove(tid);
    }

    private void unlockSharedInternal(TransactionId tid) {
        if (!activeTransactions.contains(tid)) return;
        synchronized(this) {
            --readers;
            activeTransactions.remove(tid);
        }
    }

    public void unlockShared(TransactionId tid) {
        if (!activeTransactions.contains(tid)) return;

        synchronized (this) {
            --readers;
            activeTransactions.remove(tid);
            notifyAll();
        }
    }

    public void unlockExclusive(TransactionId tid) {
        if (!activeTransactions.contains(tid) || !isExclusive) return;

        synchronized (this) {
            --writers;
            activeTransactions.remove(tid);
            notifyAll();
        }
    }


    public void unlock(TransactionId tid) {
        if (isExclusive) {
            unlockExclusive(tid);
        } else {
            unlockShared(tid);
        }
    }

    public Set<TransactionId> getCurrentHolders() {
        return activeTransactions;
    }

    public boolean isHeldExclusively() {
        return isExclusive;
    }

    public Set<TransactionId> getPendingRequestors() {
        return pendingRequests.keySet();
    }

    public boolean heldBy(TransactionId tid) {
        return activeTransactions.contains(tid);
    }
}