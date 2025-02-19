package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.Permissions;
import simpledb.common.DbException;
import simpledb.common.DeadlockException;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.*;

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
    private ConcurrentHashMap<PageId, Page> pageIdToPage;

    // Define a simple intrinsic lock: Recall, every Java object
    // can implictly act as a lock for purposes of synchronisation
    // Intrinsic locks act as mutexes (mutual exclusion locks)
    // At most 1 thread may own the lock
    private static Object simpleLock = new Object();

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        this.numPages = numPages;
        this.pageIdToPage = new ConcurrentHashMap<>();
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
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // some code goes here

        // Try to acquire a lock to run critical section code that may access
        // shared resources
        // https://www.baeldung.com/java-mutex
        synchronized(simpleLock) {
            // The retrieved page should be looked up in the buffer pool. If it is present, it should be returned
            if(this.pageIdToPage.containsKey(pid)) {
                return this.pageIdToPage.get(pid);
            // If it is not present, it should be added to the buffer pool and returned.
            } else {
                // If there is insufficient space in the buffer pool
                // For this lab, if more than `numPages` requests are made for different pages, then 
                // instead of implementing an eviction policy, you may throw a DbException. 
                if(this.pageIdToPage.size()>=this.numPages){
                    throw new DbException("More than `numPages` requests have been made for different pages");
                } else {
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
                    this.pageIdToPage.put(pid, page);
                    return page;
                }
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
        return false;
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
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
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
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1

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
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
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
    }

}
