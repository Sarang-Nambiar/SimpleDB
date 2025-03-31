package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.Permissions;
import simpledb.common.DbException;
import simpledb.common.DeadlockException;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;
import simpledb.transaction.LockManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 * 
 * 
 * Tuples are stored in pages, which are stored on disk. Pages belonging to the 
 * same table are grouped together under the same DbFile instance, which 
 * provides an interface to read/write pages and tuples to disk. 
 * Each database table is stored as a DbFile instance.
 * DbFile Interface (aka table) -> Page 1...Page N. Each entry in a page is a tuple
 * Each column is a field
 * 
 * 
 * The BufferPool singleton object manages all page access and modifications. 
 * Because BufferPool has a global view of all page accesses, it can cache 
 * frequently used pages in memory so that page fetches doesn’t always go to disk. 
 * Once the BufferPool cache gets full, it will need to evict pages using some 
 * eviction algorithm. The BufferPool evicts pages using the no-steal algorithm to 
 * provide ACID transaction guarantees, which is discussed more in the Transactions 
 * section
 * 
 * 
 * BufferPool is responsible for caching pages in memory that have been 
 * recently read from disk. 
 * All operators read and write pages from various files on disk through the 
 * buffer pool. It consists of a fixed number of pages, defined by the `numPages` 
 * parameter to the `BufferPool` constructor. 
 * In later labs, you will implement an eviction policy. 
 * 
 * 
 * For this lab, you only need to implement the constructor and the 
 * `BufferPool.getPage()` method used by the SeqScan operator. 
 * The BufferPool should store up to `numPages` pages. For this lab, 
 * if more than `numPages` requests are made for different pages, then 
 * instead of implementing an eviction policy, you may throw a DbException. 
 * In future labs you will be required to implement an eviction policy.
 * 
 * 
 * Lab 3
 * Need to implement Strict 2PL: Transactions should acquire the appropriate type of lock on any object 
 * before accessing that object and shouldn't release any locks until after the transaction commits
 * 
 * Transaction commit (transactionComplete) is done in Exercise 4
 * 
 * It is possible to obtain locks on pages in BufferPool.getPage() before you read or modify them.
 * Recommended to acquire locks in getPage(). It is possible that we do not need to acquire a lock anywhere else
 * 
 * Need to acquire a shared lock on any page (or tuple) before you read it
 * Need to acquire an exclusive lock on any page (or tuple) before you write it
 * 
 * Permissions objects in the BufferPool indicate the type of lock that the caller would like to have 
 * on the object being accessed
 * 
 * Double check that implementation of BufferPool.insertTuple() and BufferPool.deleteTupe() 
 * call markDirty() on any of the pages they access
 * 
 * After acquiring locks, think about when to release
 * - Clear that we should release all locks associated with a transaction after it has committed or
 * aborted to ensure strict 2PL
 * - However, it is possible for there to be other scenarios in which releasing a lock before a transaction
 *  ends might be useful
 * 
 * Some actions (not all) that we need to verify are working properly
 * 
 * 1) Reading tuples off of pages during a SeqScan
 * - If we implemented locking in BufferPool.getPage(), this should work correctly as long as
 *   HeapFile.iterator() uses BufferPool.getPage()
 * 
 * 2) Inserting and deleting tuples through BufferPool and HeapFile methods
 * - If we implemented locking in BufferPool.getPage(), this should work correctly as long as
 *   HeapFile.insertTuple() and HeapFile.deleteTuple() use BufferPool.getPage()
 * 
 * Some situations to think about acquiring and releasing locks
 * 
 * 1) Add new page to HeapFile
 * - When do we physically write the page to disk
 * - Are there race conditions with other transactions or other threads that may need special attention
 *   at the HeapFile level regardless of page-level locking?
 * 
 * 2) Looking for an empty slot into which we can insert tuples
 * - Most implementations scan pages looking for an empty slot, and will need a READ_ONLY lock to do this
 * - However, if a transaction t finds no free slot on a page p, t may immediately release the lock on p
 *   because t did not use any data from the page
 * 
 * 
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;
    
    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    // BufferPool should use the numPages argument to the
    // constructor
    private int numPages; 

    private class Frame {
        private Page page;
        private int timestamp; // The current pin count of the buffer pool
        
        public Frame(Page page, int timestamp) {
            this.page = page;
            this.timestamp = timestamp;
        }

        public Page get_Page() {
            return this.page;
        }

        public int getTimeStamp() {
            return this.timestamp;
        }

        public void set_Page(Page p) {
            this.page = p;
        }

        public void setTimeStamp(int timestamp) {
            this.timestamp = timestamp;
        }
    }
    private int currTimeStamp = 1;
    private ConcurrentHashMap<PageId, Frame> pageToFrame;
    private PriorityQueue<Frame> minHeap;

    // Lab 3
    private final LockManager lockManager;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        this.numPages = numPages;
        this.pageToFrame = new ConcurrentHashMap<>();
        this.minHeap = new PriorityQueue<>( (p1, p2) -> Integer.compare(p1.getTimeStamp(), p2.getTimeStamp()) );
        // Lab 3
        this.lockManager = new LockManager();
    }
    
    public static int getPageSize() {
      return pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
    	BufferPool.pageSize = pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
    	BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // some code goes here

        // Lab3
        /*
         * Transactions: Group of DB actions
         * It should always seem like the operations in a transaction were executed as a single, indivisible action
         * Use Strict 2PL for concurrency control and lock data at Page-Level
         * 
         * Locks are grabbed from the LockManager when a page is fetched from BufferPool
         * The page fetch function blocks until the page’s lock is acquired from the LockManager
         * 
         * Add call to SimpleDB in the BufferPool that allows a caller to request a lock
         * on a specific object 
         * 
         * Modify getPage() to block and acquire the desired lock before returning a page
         * 
         * 
         * Depending on implementation, may only need to acquire lock in BufferPool.getPage()
         * -  Need to acquire a shared lock on any page (or tuple) before you read it
         * -  Need to acquire an exclusive lock on any page (or tuple) before you write it
         */

        if (perm == Permissions.READ_ONLY) {
            this.lockManager.acquireSharedLock(tid, pid);
        } else if (perm == Permissions.READ_WRITE) {
            this.lockManager.acquireExclusiveLock(tid, pid);
        }


        // Still necessary to have a simple intrinsic mutex lock to prevent race conditions for shared/read lock as there's modification
        synchronized(this) {
            // The retrieved page should be looked up in the buffer pool. If it is present, it should be returned
            if(this.pageToFrame.containsKey(pid)) {
                this.pageToFrame.get(pid).setTimeStamp(this.currTimeStamp++);
                return this.pageToFrame.get(pid).get_Page();
            // If it is not present, it should be added to the buffer pool and returned.
            } else {
                // If there is insufficient space in the buffer pool
                // For this lab, if more than `numPages` requests are made for different pages, then 
                // instead of implementing an eviction policy, you may throw a DbException. 
                if(this.pageToFrame.size()>=this.numPages){
                    this.evictPage();
                }
                // See PageId.java. Return the unique tableid hashcode of this PageId
                int tableId = pid.getTableId(); 
                // See Catalog.java. Get the Database file using the table id
                // Returns the DbFile that can be used to read the contents of the specified table.
                DbFile dbFile = Database.getCatalog().getDatabaseFile(tableId);
                // See DbFile.java. Read the Page from the Database file. 
                // Read the specified page from disk.
                // Hint for lab1: You should use the DbFile.readPage method to access pages of a DbFile.
                Page page = dbFile.readPage(pid);
                // Add to the buffer pool
                Frame newEntry = new Frame(page, this.currTimeStamp++);
                this.pageToFrame.put(pid, newEntry);
                this.minHeap.add(newEntry);
                return page;
            }
        }
        // return null;
    }




    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void unsafeReleasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2

        /*
         * Lab 3
         * 
         * Add call to SimpleDB in the BufferPool that allows a caller to release a lock
         * on a specific object 
         * 
         * Instructions said that this method is primarily used for testing and at the end of transactions
         * 
         */

         // Help a transaction release a lock from a page
        this.lockManager.releaseOneTransactionLock(tid, pid);
    }



    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) {
        // some code goes here
        // not necessary for lab1|lab2
    }




    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2

        // Lab3
        // Helps to determine whether a page is already locked by a transaction
        return this.lockManager.holdsLock(tid, p);
    }




    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit) {
        // some code goes here
        // not necessary for lab1|lab2
    }








    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other 
     * pages that are updated (Lock acquisition is not needed for lab2). 
     * May block if the lock(s) cannot be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     * 
     * 
     * Lab 3
     * Double check that BufferPool.insertTuple(), BufferPool.deleteTuple() call markDirty() 
     * on any of the pages they access
     * -> its called in the HeapFile insertTuple implementation. But just call again?
     * 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        // Lab 2
        // Lock acquisition is not needed for lab2

        // These methods should call the appropriate methods in the HeapFile that 
        // belong to the table being modified 
        // (this extra level of indirection is needed to support other types of files 
        // — like indices — in the future)
        
        // Get the HeapFile
        HeapFile file = (HeapFile) Database.getCatalog().getDatabaseFile(tableId);
        // Add a tuple to the specified table on behalf of transaction tid
        // Get the modified pages
        ArrayList<Page> modified_pages = (ArrayList<Page>) file.insertTuple(tid, t);

        for(Page page: modified_pages) {
            // Pages are already marked dirty by file.insertTuple
            // All pages in this loop are dirty 
            // Add the versions of these pages to the cache
            // i.e. (replacing any existing versions of those pages)
            // so that future requests see up-to-date pages

            // Lab3 -> Double check that pages accessed are marked dirty
            page.markDirty(true, tid);

            // If the page is already in the cache, remove it
            if (this.pageToFrame.containsKey(page.getId())) {
                Frame frame = this.pageToFrame.get(page.getId());
                this.pageToFrame.remove(page.getId());
                this.minHeap.remove(frame);
            }
            
            // If there is no space in the cache/BufferPool, run the eviction policy
            if(this.pageToFrame.size() >= this.numPages) {
                this.evictPage();
            } 

            // Add the page to the cache
            Frame newEntry = new Frame(page, this.currTimeStamp++);
            this.pageToFrame.put(page.getId(), newEntry);
            this.minHeap.add(newEntry);
        }
    }






    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     * 
     * 
     * Double check that BufferPool.insertTuple(), BufferPool.deleteTuple() call markDirty() 
     * on any of the pages they access
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        // Lab 2

        // Lock acquisition is not needed for lab2

        // These methods should call the appropriate methods in the HeapFile that 
        // belong to the table being modified 
        // (this extra level of indirection is needed to support other types of files 
        // — like indices — in the future)
        
        // Get the HeapFile
        HeapFile file = (HeapFile) Database.getCatalog().getDatabaseFile(t.getRecordId().getPageId().getTableId());
        // Delete a tuple from the specified table on behalf of transaction tid
        // Get the modified pages
        ArrayList<Page> modified_pages = (ArrayList<Page>) file.deleteTuple(tid, t);

        for(Page page: modified_pages) {
            // Pages are already marked dirty by file.insertTuple
            // All pages in this loop are dirty 
            // Add the versions of these pages to the cache
            // i.e. (replacing any existing versions of those pages)
            // so that future requests see up-to-date pages

            // Lab3 -> Double check that pages accessed are marked dirty
            page.markDirty(true, tid);

            // If the page is already in the cache, remove it
            if (this.pageToFrame.containsKey(page.getId())) {
                Frame frame = this.pageToFrame.get(page.getId());
                this.pageToFrame.remove(page.getId());
                this.minHeap.remove(frame);
            }
            
            // If there is no space in the cache/BufferPool, run the eviction policy
            if(this.pageToFrame.size() >= this.numPages) {
                this.evictPage();
            } 

            // Add the page to the cache
            Frame newEntry = new Frame(page, this.currTimeStamp++);
            this.pageToFrame.put(page.getId(), newEntry);
            this.minHeap.add(newEntry);
        }


    }







    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        Iterator<ConcurrentHashMap.Entry<PageId, Frame>> i = this.pageToFrame.entrySet().iterator();

        while (i.hasNext()) {
            this.flushPage(i.next().getKey());
        }

    }



    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
        
        Also used by B+ tree files to ensure that deleted pages
        are removed from the cache so they can be reused safely
    */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        this.minHeap.remove(this.pageToFrame.get(pid));
        this.pageToFrame.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        Page page = this.pageToFrame.get(pid).get_Page();
        HeapFile file = (HeapFile) Database.getCatalog().getDatabaseFile(pid.getTableId());
        TransactionId tid = page.isDirty();

        // Checking if the page is not dirty
        if (tid == null) { 
            return;
        }

        file.writePage(page);
        this.pageToFrame.get(pid).setTimeStamp(this.currTimeStamp++);
        page.markDirty(false, tid);
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
        Frame frame = this.minHeap.poll();
        PageId pidToRemove = frame.get_Page().getId();
        try {
            this.flushPage(pidToRemove); // Write to disk if the page is dirty
        } catch (IOException e) {
            throw new DbException("IO error while flushing page");
        }
        this.pageToFrame.remove(pidToRemove);
    }
}
