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

        // Critical Section because it contains modification
        synchronized (this) {
            // Add the transaction as an acquirer for potential deadlock detection
            SLacquirers.add(tid);
            try {
                // Wait until there is no transaction holding an EL
                while(ELholder.size()>0) {
                    // Enter waiting state until a notifyAll is called on the same object (this -> current instance of the PageLock class)
                    // such that it wakes up and rechecks the condition
                    this.wait();
                } 
                // Once there is no transaction holding the EL
                // Add the transaction to the set of transactions holding a SL on this page
                SLholders.add(tid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Remove the transaction from SL acquirers
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

        // Critical Section because it contains modification
        synchronized (this) {
            // Add the transaction as an acquirer for potential deadlock detection
            ELacquirers.add(tid);
            try {
                // In the case of a lock upgrade where a transaction needs to acquire a SL first
                if(SLholders.contains(tid)){
                    // While other transactions are holding on to the read lock 
                    // wait
                    // Enter waiting state until a notifyAll is called on the same object (this -> current instance of the PageLock class)
                    // such that it wakes up and rechecks the condition
                    while(SLholders.size()>1){
                        this.wait();
                    }
                    // Now, only this transaction is holding the shared lock
                    // Release it so it can be upgraded to an exclusive lock
                    SLholders.remove(tid);
                }

                // Wait until there is no transaction holding ANY lock
                // Still needed even if there is a lock upgrade previously to check for EL holders
                while(SLholders.size() > 0 || ELholder.size()>0) {
                    // Enter waiting state until a notifyAll is called on the same object (this -> current instance of the PageLock class)
                    // such that it wakes up and rechecks the condition
                    this.wait();
                } 

                // Once there is no transaction holding ANY lock
                // Set the transaction to the only transaction holding an EL on this page
                ELholder.add(tid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Remove the transaction from EL acquirers
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
                return;
            }
            return;
        }
    }

    // Unlock an exclusive (write) lock from this page
    public void unlockExclusiveLock(TransactionId tid) {
        synchronized(this){
            if(ELholder.contains(tid)) {
                ELholder.remove(tid);
                // Wake up all waiting threads on 'this'
                this.notifyAll();
                return;
            }
            return;
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
