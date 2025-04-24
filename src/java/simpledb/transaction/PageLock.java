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
        synchronized (this) {
            if (SLholders.contains(tid) || ELholder.contains(tid)) return;
            SLacquirers.add(tid);
            try {
                while (!ELholder.isEmpty()) {
                    //System.out.printf("[WAIT-SHARED] %s waiting on %s | ELholder=%s\n", tid, pageId, ELholder);
                    this.wait();
                }
                SLholders.add(tid);
                //System.out.printf("[LOCKED-SHARED] %s acquired SHARED lock on %s\n", tid, pageId);
            } catch (InterruptedException e) {
                throw new RuntimeException("Shared lock failed for " + tid + ": " + e.getMessage());
            } finally {
                SLacquirers.remove(tid);
            }
        }
    }
    


    // Acquire an exclusive/write type lock on the page
    public void acquireExclusiveLock(TransactionId tid) {
        synchronized (this) {
            if (ELholder.contains(tid)) return;
            ELacquirers.add(tid);
            try {
                long start = System.currentTimeMillis();
                long timeout = 10000;
    
                // Upgrade case
                if (SLholders.contains(tid)) {
                    while (SLholders.size() > 1 || (!ELholder.isEmpty() && !ELholder.contains(tid))) {
                        this.wait(100); // shorter wait
                        if (System.currentTimeMillis() - start > timeout) {
                            // retry once
                            start = System.currentTimeMillis();
                        }
                    }
                    SLholders.remove(tid);
                }
    
                // Wait for exclusive access
                while (!SLholders.isEmpty() || (!ELholder.isEmpty() && !ELholder.contains(tid))) {
                    this.wait(100);
                    if (System.currentTimeMillis() - start > timeout) {
                        // retry again
                        start = System.currentTimeMillis();
                    }
                }
    
                ELholder.add(tid);
            } catch (InterruptedException e) {
                throw new RuntimeException("Exclusive lock failed for " + tid + ": " + e.getMessage());
            } finally {
                ELacquirers.remove(tid);
            }
        }
    }
    
    
    


    // Unlock a shared (read) lock from this page
    public void unlockSharedLock(TransactionId tid) {
        synchronized(this){
            if(SLholders.contains(tid)) {
                SLholders.remove(tid);
                // Wake up all waiting threads on 'this'
                this.notifyAll();
                //System.out.println("[UNLOCK] " + tid + " released SHARED lock on " + pageId);
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
                //System.out.println("[UNLOCK] " + tid + " released EXCLUSIVE lock on " + pageId);
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

    public synchronized HashSet<TransactionId> getSLholders() {
        return new HashSet<>(this.SLholders);  // return a copy
    }
    
    public synchronized HashSet<TransactionId> getELholder() {
        return new HashSet<>(this.ELholder);  // return a copy
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
