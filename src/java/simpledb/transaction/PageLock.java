package simpledb.transaction;
import java.util.HashSet;
import simpledb.storage.PageId;


// Define a custom lock to lock on the page
public class PageLock {
    // Define the PageId that this lock is for
    PageId pageId;
    // Define a set of transactions that are trying to acquire a shared lock on this page for deadlock detection
    HashSet<TransactionId> SLacquirers;
    // Define a set of transactions that are trying to acquire an exclusive lock on this page for deadlock detection
    HashSet<TransactionId> ELacquirers;
    // Define a set of transactions that are holding a shared (read) lock on this page
    HashSet<TransactionId> SLholders;
    // Define a set to reference the transaction that is holding an exclusive (write) lock on this page
    HashSet<TransactionId> ELholder;

    public PageLock(PageId pageId) {
        this.pageId = pageId;
        this.SLacquirers = new HashSet<TransactionId>();
        this.ELacquirers = new HashSet<TransactionId>();
        this.SLholders = new HashSet<TransactionId>();
        this.ELholder = new HashSet<TransactionId>();
    }

    // Acquire a shared/read type lock on the page
    public void acquireSharedLock(TransactionId tid) {
        /*
         * Transactions need to acquire a SharedLock (SL) for reading a page
         * Multiple transactions can acquire a SL on the same page for reading
         * An EL request will be blocked until all SL on the page are released
         */

        // If the transaction already has a SL/EL or is trying to acquire a SL/EL, return
        // We need to check for EL because if we don't, when the transaction enters the critical section
        // ELholders.size() will still be > 0, it will be waiting for itself to release the lock. The test will hang
        // (according to how the test case acquireWriteAndReadLocks() is written)
        // Addresses the test case acquireWriteAndReadLocks() -> If it already has an EL, it technically also has a SL (I think this is what they're testing)
        // "A single transaction should be able to acquire a read lock after it already has a write lock"
        if(SLholders.contains(tid) || SLacquirers.contains(tid) || ELholder.contains(tid) || ELacquirers.contains(tid)) return;

        synchronized (this) {
            SLacquirers.add(tid);
            try {
                // Wait if there is an exclusive lock holder
                while (!ELholder.isEmpty()) {
                    System.out.println("[WAITING] " + tid + " waiting for SHARED lock on " + pageId + " | ELholder=" + ELholder);
                    this.wait(1000);  // wait with timeout
                    if (!ELholder.isEmpty()) {
                        throw new RuntimeException("[TIMEOUT] Waiting too long for SHARED lock on " + pageId + " by " + tid);
                    }
                }
                SLholders.add(tid);
                System.out.println("[LOCK] " + tid + " got SHARED lock on " + pageId);
            } catch (Exception e) {
                throw new RuntimeException("[ERROR] Shared lock failed for " + tid + ": " + e.getMessage());
            }
            SLacquirers.remove(tid);
        }
    }


    // Acquire an exclusive/write type lock on the page
    public void acquireExclusiveLock(TransactionId tid) {
        /*
         * Transactions need to acquire an Exclusive Lock (EL) for writing a page
         * Only 1 transaction can acquire an EL on a page for reading
         * ALL other lock requests are blocked until the EL is released
         */

        // If the transaction already has an EL or is trying to acquire an EL, return
        // Allow transaction to hold SL because of the possibility to upgrade to EL
        if(ELholder.contains(tid) || ELacquirers.contains(tid)) return;

        synchronized (this) {
            ELacquirers.add(tid);
            try {
                // Upgrade from shared lock
                if (SLholders.contains(tid)) {
                    while (SLholders.size() > 1) {
                        System.out.println("[WAITING-UPGRADE] " + tid + " waiting to upgrade to EXCLUSIVE lock on " + pageId);
                        this.wait(5000);
                        if (SLholders.size() > 1) {
                            throw new RuntimeException("[TIMEOUT] Lock upgrade waiting too long on " + pageId + " by " + tid);
                        }
                    }
                    SLholders.remove(tid);
                }

                // Wait until no one else holds a shared or exclusive lock
                while (!SLholders.isEmpty() || !ELholder.isEmpty()) {
                    System.out.println("[WAITING] " + tid + " waiting for EXCLUSIVE lock on " + pageId + " | SLholders=" + SLholders + " | ELholder=" + ELholder);
                    this.wait(5000);
                    if (!SLholders.isEmpty() || !ELholder.isEmpty()) {
                        throw new RuntimeException("[TIMEOUT] Waiting too long for EXCLUSIVE lock on " + pageId + " by " + tid);
                    }
                }

                ELholder.add(tid);
                System.out.println("[LOCK] " + tid + " got EXCLUSIVE lock on " + pageId);
            } catch (Exception e) {
                throw new RuntimeException("[ERROR] Exclusive lock failed for " + tid + ": " + e.getMessage());
            }
            ELacquirers.remove(tid);
        }
    }


    // Unlock a shared (read) lock from this page
    public void unlockSharedLock(TransactionId tid) {
        synchronized(this){
            if(SLholders.contains(tid)) {
                SLholders.remove(tid);
                // Wake up all waiting threads on 'this'
                this.notifyAll();
                System.out.println("[UNLOCK] " + tid + " released SHARED lock on " + pageId);
                return;
            }
        }
    }

    // Unlock an exclusive (write) lock from this page
    public void unlockExclusiveLock(TransactionId tid) {
        synchronized(this){
            if(ELholder.contains(tid)) {
                ELholder.remove(tid);
                // Wake up all waiting threads on 'this'
                this.notifyAll();
                System.out.println("[UNLOCK] " + tid + " released EXCLUSIVE lock on " + pageId);
                return;
            }
        }
    }


    // Convenience methods
    public HashSet<TransactionId> getSLacquirers(){
        return this.SLacquirers;
    }

    public HashSet<TransactionId> getELacquirers(){
        return this.ELacquirers;
    }

    public HashSet<TransactionId> getSLholders(){
        return this.SLholders;
    }

    public HashSet<TransactionId> getELholder(){
        return this.ELholder;
    }

    public boolean SLacquiringBy(TransactionId tid) {
        return this.SLacquirers.contains(tid);
    }

    public boolean ELacquiringBy(TransactionId tid) {
        return this.ELacquirers.contains(tid);
    }

    public boolean SLheldBy(TransactionId tid) {
        return this.SLholders.contains(tid);
    }

    public boolean ELheldBy(TransactionId tid) {
        return this.ELholder.contains(tid);
    }


}
