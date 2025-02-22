package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * Write methods to calculate the number of pages in a file and to read a page from the file
 * Be able to fetch tuples from a file stored on disk
 * 
 * To read a page from the disk, we need to calculate the correct offset in the file
 * - Need random access to the file in order to read and write pages at arbitrary offsets
 * - Should not call BufferPool methods when reading a page from the disk
 * 
 * Need to implement HeapFile.iterator(): Iterate through tuples of each page in the HeapFile
 * - Iterator must use the BufferPool.getPage() method to access pages in the `HeapFile`
 * - This method loads the page into the buffer pool and will eventually be used (later lab) to implement
 *   locking-based concurrency control and recovery
 * - Do not load the entire table into memory on the open() call, it will cause out of memory error for very large tables
 * 
 * 
 * HeapFile reads tuples a file (bytes). The 'pages' in the file are represented by HeapPages -> Stores header and tuples.
 * HeapPageId is a reference to the specified page number and table id. A unique identifier/reference to a specific page of a specific table
 * RecordId is a reference to a specific tuple on a specific page of a specific table.
 * Tuple stores the tuple information, TupleDesc defines the schema
 * 
 * @see HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private File f;
    private TupleDesc td;
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here

        // Constructing a heap file given a specified file and the schema of the tuples
        // File is a collection of all the pages
        this.f = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return this.f;
        //return null;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here

        // As suggested: Hash the absolute file name of the file underlying the heapfile
        // to return a unique ID identifying this HeapFile
        return f.getAbsoluteFile().hashCode();
        //throw new UnsupportedOperationException("implement this");
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here

        return this.td;
        //throw new UnsupportedOperationException("implement this");
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        
        // Write methods to calculate the number of pages in a file and to read a page from the file
        // To read a page from the disk, we need to calculate the correct offset in the file
        // Need random access to the file in order to read and write pages at arbitrary offsets

        // See PageId.java
        // Calculate the total number of bytes we need to offset - page data includes header data and tuple data
        // 0-th indexed page number
        Long bytesOffset = (long) pid.getPageNumber() * BufferPool.getPageSize();
        // Initialize a buffer array to store the Page data we want to read
        byte[] pageData = new byte[BufferPool.getPageSize()];

        // https://www.digitalocean.com/community/tutorials/java-randomaccessfile-example
        try {
            RandomAccessFile raf = new RandomAccessFile(this.f,"r");
            // Move the pointer to the correct position
            // 0-th index
            raf.seek(bytesOffset);
            // Read "BufferPool.getPageSize()" bytes (because we create a byte array of BufferPool.getPageSize()) to pageData
            raf.readFully(pageData);
            raf.close();
            // Each instance of HeapPage stores data for one page of HeapFiles and 
            // implements the Page interface that is used by BufferPool.
            // The HeapPage is created from a set of bytes of data read from disk. 
            // It is initialized with the page Id and the pageData byte array
            return new HeapPage((HeapPageId) pid, pageData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here

        // File is from io library 
        // https://docs.oracle.com/javase/8/docs/api/java/io/File.html
        // length() -> The length, in bytes, of the file
        // BufferPool.getPageSize() -> number of bytes in a page 
        return (int) Math.ceil(this.f.length() / BufferPool.getPageSize());
        //return 0;
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }


    /**
     * According to DbFileIterator.java: DbFileIterator is the iterator interface that all 
     * SimpleDB Dbfile should implement.
     * 
     * Methods to override and implement
     * 
     * void open(): Opens the iterator
     * - throws DbException, TransactionAbortedException; when there are problems opening/accessing the DB
     * 
     * boolean hasNext(): Returns true if there are more tuples available, false if no more tuples or iterator isn't open.
     * - throws DbException, TransactionAbortedException;
     * 
     * Tuple next(): Gets the next tuple from the operator. Returns the next tuple in the iterator
     * - throws DbException, TransactionAbortedException, NoSuchElementException; if there are no more tuples
     * 
     * void rewind(): Resets the iterator to the start
     * - throws DbException, TransactionAbortedException; when rewind is unsupported.
     * 
     * void close(): Closes the iterator
     */
    public class HeapFileIterator implements DbFileIterator {

        // TransactionId is a class that contains the identifier of a transaction.
        private TransactionId transactionId;
        private HeapFile heapFile;
        private HeapPage heapPage;
        private int tableId;
        private Iterator<Tuple> heapPageIterator;
        private boolean isOpen;
        //private int pageNumber;
        
        // Define the constructor
        public HeapFileIterator(TransactionId transactionId, HeapFile heapFile) {
            this.transactionId = transactionId;
            this.heapFile = heapFile;
            this.tableId = heapFile.getId();
            this.isOpen = false;
        
        }



        // Define the open method that opens the iterator
        // Recall: Iterator must use the BufferPool.getPage() method to access pages in the `HeapFile`
        // Recall: BufferPool manages the reading and writing of pages into memory from
        // disk. Access methods call into it to retrieve pages, and it fetches
        // pages from the appropriate location
        // The BufferPool singleton object manages all page access and modifications. 
        // Because BufferPool has a global view of all page accesses, it can cache 
        // frequently used pages in memory so that page fetches doesn’t always go to disk. 
        // Once the BufferPool cache gets full, it will need to evict pages using some 
        // eviction algorithm. The BufferPool evicts pages using the no-steal algorithm to 
        // provide ACID transaction guarantees, which is discussed more in the Transactions 
        // section
        @Override
        public void open() throws DbException, TransactionAbortedException {

            // Initialize the heapPageId with the tableId (fileId) and page number starting from 0
            // Identifies the file/table it belongs to and what page number
            HeapPageId heapPageId = new HeapPageId(this.tableId, 0);
            // getPage requires a TransactionId, PageId, Permissions
            // In the getPage method, there is a readPage method that is called on the HeapFile
            // dbFile (HeapFile) readPage -> returns a HeapFile
            // HeapPage stores header and tuple data for one page of HeapFiles
            this.heapPage = (HeapPage) Database.getBufferPool().getPage(this.transactionId, heapPageId, null);
            // Return an iterator that stores tuples: ArrayList<Tuple>
            this.heapPageIterator = this.heapPage.iterator();
            this.isOpen = true;
        }


        // Define the hasNext method that returns true if there are more tuples available, 
        // false if no more tuples or iterator isn't open
        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException{

            // Return false if the iterator isn't open
            if(!(this.isOpen)) {
                return false;
            }

            // Check if more tuples are available 
            // More tuples available in the current page
            if(this.heapPageIterator.hasNext()) {
                return true;
            // There are no more tuples in the current page,
            // check if there is a next page
            } else {
                // Check if there is a next page
                // Get the Page Number -> heapPage, get the heapPageId passed to heapPage, get the 
                // page number passed to the HeapPageId
                // If there ae still pages left
                // this.heapFile.numPages()-1 -> -1 is needed because 0 indexed 
                // todo change to while loop because if the next page does not, we have to go the next page as well

                while (this.heapPage.getId().getPageNumber() < this.heapFile.numPages()-1) {
                    // Initialize a new heapPageId with the tableId (fileId) and incremented page number
                    // Identifies the file/table it belongs to and what page number
                    HeapPageId heapPageId = new HeapPageId(this.tableId, this.heapPage.getId().getPageNumber() + 1);
                    // Set this as the new heap page
                    this.heapPage = (HeapPage) Database.getBufferPool().getPage(this.transactionId, heapPageId, null);
                    // Return an iterator that stores tuples: ArrayList<Tuple>
                    this.heapPageIterator = this.heapPage.iterator();
                    // If the next page has data, return true
                    if(this.heapPageIterator.hasNext()) {
                        return true;
                    } 
                    // If not, continue on to the next page that may contain data
                } 

                // After exhausing all the pages, there is no more page data
                return false;

            }
        }


        // Define the next method that gets the next tuple from the operator. Returns the next tuple in the iterator
        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException{
            if(this.hasNext()) {
                return this.heapPageIterator.next();
            } else {
                throw new NoSuchElementException();
            }
        }


        // Resets the iterator to the start
        @Override
        public void rewind() throws DbException, TransactionAbortedException{
            this.isOpen = false;
            this.open();
        }

        
        // Closes the iterator
        @Override
        public void close() {
            this.transactionId = null;
            this.heapFile = null;
            this.heapPage = null;
            this.heapPageIterator = null;
            this.isOpen = false;
        }



    }




    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here

        //  * Need to implement HeapFile.iterator(): Iterate through tuples of each page in the HeapFile
        // * - Iterator must use the BufferPool.getPage() method to access pages in the `HeapFile`
        // * - This method loads the page into the buffer pool and will eventually be used (later lab) to implement
        // *   locking-based concurrency control and recovery
        // * - Do not load the entire table into memory on the open() call, it will cause out of memory error for very large tables
        return new HeapFileIterator(tid, this);
    }

}

