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
        int pageNumber = pid.getPageNumber();
        // Calculate the total number of bytes we need to offset
        Long bytesOffset = (long) pageNumber * BufferPool.getPageSize();
        // Initialize a buffer array to store the Page data we want to read
        byte[] pageData = new byte[BufferPool.getPageSize()];

        // https://www.digitalocean.com/community/tutorials/java-randomaccessfile-example
        try {
            RandomAccessFile raf = new RandomAccessFile(this.f,"r");
            // Move the pointer to the correct position
            raf.seek(bytesOffset);
            // Read the BufferPool.getPageSize() (because we create a byte array of BufferPool.getPageSize()) to pageData
            raf.read(pageData);
            raf.close();
            // Each instance of HeapPage stores data for one page of HeapFiles and 
            // implements the Page interface that is used by BufferPool.
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

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here

        //  * Need to implement HeapFile.iterator(): Iterate through tuples of each page in the HeapFile
        // * - Iterator must use the BufferPool.getPage() method to access pages in the `HeapFile`
        // * - This method loads the page into the buffer pool and will eventually be used (later lab) to implement
        // *   locking-based concurrency control and recovery
        // * - Do not load the entire table into memory on the open() call, it will cause out of memory error for very large tables
        return null;
    }

}

