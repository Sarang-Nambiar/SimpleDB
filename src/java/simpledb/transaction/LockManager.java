package simpledb.transaction;

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import simpledb.common.Permissions;
import simpledb.storage.PageId;


class Lock {
    TransactionId tid;
    PageId pid;
    boolean exclusive;

    public Lock(TransactionId tid, PageId pid, boolean exclusive){
        this.tid = tid;
        this.pid = pid;
        this.exclusive = exclusive;
    }
}


public class LockManager {
    private ConcurrentHashMap<PageId, ArrayList<Lock>> pageIdToLocks; // Stores the locks on the page
    private ConcurrentHashMap<TransactionId, HashSet<PageId>> transactionIdToPageIds; // Store the pages/locks that the transaction has

    public LockManager(){
        this.pageIdToLocks = new ConcurrentHashMap<PageId, ArrayList<Lock>>();
        this.transactionIdToPageIds = new ConcurrentHashMap<TransactionId, HashSet<PageId>>();
    }

    // To prevent reads from being 'corrupted'
    // synchronized is used for thread safety
    public synchronized boolean acquireSharedLock(TransactionId tid, PageId pid) {
        // Check if the transaction already has a lock on the page 
        if(transactionIdToPageIds.contains(pid)) {
            if(transactionIdToPageIds.get(tid).contains(pid)) {
                return true;
            }
        }
        
        // If not, acquire a shared lock for the transaction

    }

}
