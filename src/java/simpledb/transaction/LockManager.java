package simpledb.transaction;

import simpledb.storage.PageId;

import java.util.*;

/**
 * LockManager handles lock acquisition, release, and deadlock detection.
 */
public class LockManager {
    Map<PageId, PageLock> pageLocks;
    Map<TransactionId, Set<TransactionId>> waitGraph;
    Map<TransactionId, Set<PageId>> txToPages;

    public LockManager() {
        this.pageLocks = new HashMap<>();
        this.waitGraph = new HashMap<>();
        this.txToPages = new HashMap<>();
    }

    public void acquireRead(TransactionId tid, PageId pid) throws TransactionAbortedException {
        PageLock lock;
        synchronized (this) {
            lock = getOrCreateLock(pid);
            if (lock.heldBy(tid)) return;

            if (lock.isHeldExclusively() && !lock.getCurrentHolders().isEmpty()) {
                waitGraph.put(tid, lock.getCurrentHolders());
                if (detectCycle(tid)) {
                    waitGraph.remove(tid);
                    throw new TransactionAbortedException();
                }
            }
        }

        lock.lockShared(tid);

        synchronized (this) {
            waitGraph.remove(tid);
            getLockedPages(tid).add(pid);
        }
    }

    public void acquireWrite(TransactionId tid, PageId pid) throws TransactionAbortedException {
        PageLock lock;
        synchronized (this) {
            lock = getOrCreateLock(pid);
            if (lock.heldBy(tid) && lock.isHeldExclusively()) return;

            Set<TransactionId> blockers = lock.getCurrentHolders();
            if (!blockers.isEmpty()) {
                waitGraph.put(tid, blockers);
                if (detectCycle(tid)) {
                    waitGraph.remove(tid);
                    throw new TransactionAbortedException();
                }
            }
        }

        lock.lockExclusive(tid);

        synchronized (this) {
            waitGraph.remove(tid);
            getLockedPages(tid).add(pid);
        }
    }

    public synchronized void release(TransactionId tid, PageId pid) {
        if (!pageLocks.containsKey(pid)) return;
        PageLock lock = pageLocks.get(pid);

        lock.unlock(tid);
        if (txToPages.containsKey(tid)) {
            txToPages.get(tid).remove(pid);
        }
    }

    public synchronized void releaseAll(TransactionId tid) {
        if (!txToPages.containsKey(tid)) return;
        Set<PageId> heldPages = new HashSet<>(txToPages.get(tid));
        for (PageId pid : heldPages) {
            release(tid, pid);
        }
        txToPages.remove(tid);
    }

    private PageLock getOrCreateLock(PageId pid) {
        if(!pageLocks.containsKey(pid)){
            pageLocks.put(pid, new PageLock());
        }
        return pageLocks.get(pid);
    }

    private Set<PageId> getLockedPages(TransactionId txnId) {
        if(!txToPages.containsKey(txnId)){
            txToPages.put(txnId, new HashSet<>());
        }
        return txToPages.get(txnId);
    }
    private boolean detectCycle(TransactionId tid) {
        Set<TransactionId> visited = new HashSet<>();
        Set<TransactionId> visiting = new HashSet<>();
        return this.dfs(tid, visiting, visited);
    }

    private boolean dfs(TransactionId curr, Set<TransactionId> visiting, Set<TransactionId> visited) {
        // Sanity check
        if(!this.waitGraph.containsKey(curr)) return false; // If node has no neighbours, then cycle detection cannot take place
        
        if(visiting.contains(curr)) return true;

        if(visited.contains(curr)) return false;

        visiting.add(curr);

        for(TransactionId nextTid : this.waitGraph.get(curr)) {
            if(nextTid.equals(curr)) continue;

            if(dfs(nextTid, visiting, visited)) {
                return true;
            }
        }
        
        visiting.remove(curr);
        visited.add(curr);
        return false;
    }

    public synchronized boolean holds(TransactionId tid, PageId pid) {
        return txToPages.containsKey(tid) && txToPages.get(tid).contains(pid);
    }

    public synchronized Set<PageId> getHeldPages(TransactionId tid) {
        if (txToPages.containsKey(tid)) {
            return txToPages.get(tid);
        } else {
            return Collections.emptySet();
        }
    }

}