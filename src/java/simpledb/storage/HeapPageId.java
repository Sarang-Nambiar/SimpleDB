package simpledb.storage;

import java.util.Objects;

/**
 * Heap Files
 * 
 * https://pages.cs.wisc.edu/~dbbook/openAccess/Minibase/spaceMgr/heap_file.html
 * A heap file is an unordered set of records
 * - Heap files can be created and destroyed.
 * - Existing heapfiles can be opened and closed.
 * - Records can be inserted and deleted.
 * - Records are uniquely identified by a record id (rid). A specific record can be retrieved by using the record id.
 * 
 * Heap files are unsorted files of tuples
 * 
 * Implement Heap File access method
 * 
 * HeapFile object is arranged into a set of pages. 
 * - Each object consists of a fixed number of bytes for storing tuples,
 *   as defined by constant BufferPool.DEFAULT_PAGE_SIZE
 * 
 * There is one HeapFile object for each table in the database
 * - Each page in HeapFile is arranged as a set of slots
 * - Each slot can hold one tuple (tuples for a given table in SimpleDB are all of the same size)
 * - Each page has a header that consists of a bitmap with one bit per tuple slot
 *   - If the bit corresponding to a particular tuple is 1: Tuple is valid
 *   - If the bit corresponding to a particular tuple is 0: Tuple is invalid -> Deleted or was never initialized
 * 
 * Pages in HeapFile are of type HeapPage which implements the Page interface
 * Pages are stored in the buffer pool but are read and written by the HeapFile class
 * 
 * Heap files consists of page data arranged consecutively on disk
 * Each page consists of one or more bytes representing the
 * - header
 * - page_size bytes of actual page content: page_size*8 (in bits)
 * Each tuple requires tuple_size*8 bits for its content and 1 bit for the header: tuple_size * 8 + 1 (in bits) -> 1 bit for header to indicate if it is valid or invalid
 * Therefore, the number of tuples that can fit in a single page is
 * tuples_per_page = floor((page_size * 8) / (tuple_size * 8 + 1))
 * Each tuple requires one additional bit of storage in the header
 * 
 * Number of bytes required to store the header is:
 * headerBytes = ceiling(tuples_per_page/8)
 * - Each tuple is assumed to require one bit of storage
 * - Take the total number of tuples, divided by 8, to get the bytes
 * 
 * Least Significant Bit of each byte represents status of slots in the file
 * Lowest bit of the first byte: First slot in page is in use or not
 * Second lowest bit of first byte: Second slot in page in use or not
 * 
 * The high-order bits of the last byte may not correspond to a slot that is 
 * actually in the file, since the number of slots may not be a multiple of 8
 * 
 */


// A HeapPageId is a reference to the specified page number and table id
// Page Id structure for a specific page of a specific table
// Unique identifier/reference to a specific page of a specific table
// HeapPageId
/** Unique identifier for HeapPage objects. */
public class HeapPageId implements PageId {

    private Integer tableId;
    private Integer pgNo;

    /**
     * Constructor. Create a page id structure for a specific page of a
     * specific table.
     *
     * @param tableId The table that is being referenced
     * @param pgNo The page number in that table.
     */
    public HeapPageId(int tableId, int pgNo) {
        // some code goes here

        this.tableId = tableId;
        this.pgNo = pgNo;
    }

    /** @return the table associated with this PageId */
    public int getTableId() {
        // some code goes here

        return this.tableId;
        // return 0;
    }

    /**
     * @return the page number in the table getTableId() associated with
     *   this PageId
     */
    public int getPageNumber() {
        // some code goes here

        return this.pgNo;
        //return 0;
    }

    /**
     * @return a hash code for this page, represented by a combination of
     *   the table number and the page number (needed if a PageId is used as a
     *   key in a hash table in the BufferPool, for example.)
     * @see BufferPool
     */
    public int hashCode() {
        // some code goes here

        // https://www.baeldung.com/java-objects-hash-vs-objects-hashcode
        return Objects.hash(this.tableId, this.pgNo);
        //throw new UnsupportedOperationException("implement this");
    }

    /**
     * Compares one PageId to another.
     *
     * @param o The object to compare against (must be a PageId)
     * @return true if the objects are equal (e.g., page numbers and table
     *   ids are the same)
     */
    public boolean equals(Object o) {
        // some code goes here

        if(!(o instanceof PageId)){
            return false;
        }

        PageId PageId2 = (PageId) o;

        if(this.hashCode()==PageId2.hashCode()){
            return true;
        } else{
            return false;
        }
        // return false;
    }

    /**
     *  Return a representation of this object as an array of
     *  integers, for writing to disk.  Size of returned array must contain
     *  number of integers that corresponds to number of args to one of the
     *  constructors.
     */
    public int[] serialize() {
        int[] data = new int[2];

        data[0] = getTableId();
        data[1] = getPageNumber();

        return data;
    }

}
