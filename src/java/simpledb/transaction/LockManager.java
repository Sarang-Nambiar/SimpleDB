package simpledb.transaction;

import java.util.*;

import simpledb.storage.PageId;

public class LockManager {

    /*
     * 
     * Lab 3
     * 
     * MANAGES locks belonging to transactions and pages
     * 
     * Need to add calls to SimpleDB (in BufferPool, for example), that allow a caller to request or release a 
     * (shared or exclusive) lock on a specific object on behalf of a specific transaction.
     * 
     * Create data structures that keep track of which locks each transaction holds
     * 
     * Need to also check if a lock should be granted to a transaction when requested
     * 
     * Before a transaction can read an object, it must have a shared lock on it.
     * - Multiple transactions can have a shared lock on an object.
     * 
     * Before a transaction can write an object, it must have an exclusive lock on it.
     * - Only one transaction may have an exclusive lock on an object.
     * 
     * If a transaction requests a lock that cannot be immediately granted, 
     * your code should block, waiting for that lock to become available 
     * (i.e., be released by another transaction running in a different thread).
     * 
     * Everything is synchronised on 'this'
     * 
     */

    // Create data structure that keeps track of which pages each transaction has a lock on
    HashMap<TransactionId, Set<PageId>> transactionIdToPages;

    // Create data structure that keeps track of the lock that belongs to each page (needed for holdsLock)
    HashMap<PageId, PageLock> pageIdToPageLock;

    // Constructor
    public LockManager() {
        this.transactionIdToPages = new HashMap<TransactionId, Set<PageId>>();
        this.pageIdToPageLock = new HashMap<PageId, PageLock>();        
    }

    // Helper method to create a page lock or get a page lock if it already exists
    public PageLock getPageLock(PageId pid) {
        if(this.pageIdToPageLock.containsKey(pid)) {
            return this.pageIdToPageLock.get(pid);
        } else {
            this.pageIdToPageLock.put(pid, new PageLock(pid));
            return this.pageIdToPageLock.get(pid);
        }
    }

    // Helper method to get or create a mapping for the transaction id to the pages which it has locks on 
    public Set<PageId> getTransactionPages(TransactionId tid){
        if(this.transactionIdToPages.containsKey(tid)){
            return this.transactionIdToPages.get(tid);
        } else {
            this.transactionIdToPages.put(tid, new HashSet<PageId>());
            return this.transactionIdToPages.get(tid);
        }
    }

    // Helper method to check if the specified transaction has a lock on the specified page
    public boolean holdsLock(TransactionId tid, PageId pid) {
        if(this.transactionIdToPages.containsKey(tid)) {
            if(this.transactionIdToPages.get(tid).contains(pid)) {
                return true;
            }
            return false;
        }
        return false;
    }
    

    // Acquire a shared (read) lock on a page
    public void acquireSharedLock(TransactionId tid, PageId pid) {
        PageLock pageLock;
        synchronized (this) {
            // Get or create the page lock (if it does not exist) on this page
            pageLock = getPageLock(pid);
            // Acquire a shared lock on the page by passing the transactionId
            pageLock.acquireSharedLock(tid);
            // Once the lock is acquired, update the transactionId to pages mapping
            getTransactionPages(tid).add(pid);
        }
    }

    // Acquire an exclusive (write) lock on a page
    public void acquireExclusiveLock(TransactionId tid, PageId pid) {
        PageLock pageLock;
        synchronized(this) {
            // Get or create the page lock (if it does not exist) on this page
            pageLock = getPageLock(pid);
            // Acquire a exclusive lock on the page by passing the transactionId
            pageLock.acquireExclusiveLock(tid);
            // Once the lock is acquired, update the transactionId to page mapping
            getTransactionPages(tid).add(pid);
        }
    }


    // Helper function to help a transaction release a lock from a page
    public void releaseOneTransactionLock(TransactionId tid, PageId pid) {
        // Critical section because it contains modification
        synchronized(this) {
            // If this page has a lock on it
            if(this.pageIdToPageLock.containsKey(pid)) {
                // Get the page lock
                PageLock pageLock = pageIdToPageLock.get(pid);

                // If the transaction has a shared (read) lock on the page, unlock its shared lock
                if(pageLock.SLheldBy(tid)) {
                    pageLock.unlockSharedLock(tid);
                // Else if the transaction has an exclusive (write) lock on the page, unlock its exclusive lock
                } else if (pageLock.ELheldBy(tid)) {
                    pageLock.unlockExclusiveLock(tid);
                }
                // Remove the page from the mapping: transactionIdToPageLocks
                this.transactionIdToPages.get(tid).remove(pid);
                return;
            }
            return;
        }
    }


    // Helper function to release all locks from a transaction
    public void releaseAllTransactionLocks(TransactionId tid) {
        // Critical section because it contains modification
        synchronized(this) {
            // If this transaction has locks on pages
            if(this.transactionIdToPages.containsKey(tid)) {
                // Get all the locks the transaction has and release them
                Set<PageId> pages = this.transactionIdToPages.get(tid);
                for(Object pageId: pages.toArray()) {
                    releaseOneTransactionLock(tid, (PageId) pageId);
                }
                // Remove the transaction from the mapping: transactionIdToPageLocks
                this.transactionIdToPages.remove(tid);
                return;
            }
            return;
        }
    }









}
