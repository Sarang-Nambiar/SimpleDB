package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Catalog;
import simpledb.transaction.TransactionId;

import java.util.*;
import java.io.*;

/**
 * Each instance of HeapPage stores data for one page of HeapFiles and 
 * implements the Page interface that is used by BufferPool.
 *
 * @see HeapFile
 * @see BufferPool
 *
 */
public class HeapPage implements Page {

    final HeapPageId pid;
    final TupleDesc td;
    final byte[] header;
    final Tuple[] tuples;
    final int numSlots;
    private TransactionId dirtyTid;

    byte[] oldData;
    private final Byte oldDataLock= (byte) 0;

    /**
     * Create a HeapPage from a set of bytes of data read from disk.
     * The format of a HeapPage is a set of header bytes indicating
     * the slots of the page that are in use, some number of tuple slots.
     *  Specifically, the number of tuples is equal to: <p>
     *          floor((BufferPool.getPageSize()*8) / (tuple size * 8 + 1))
     * <p> where tuple size is the size of tuples in this
     * database table, which can be determined via {@link Catalog#getTupleDesc}.
     * The number of 8-bit header words is equal to:
     * <p>
     *      ceiling(no. tuple slots / 8)
     * <p>
     * @see Database#getCatalog
     * @see Catalog#getTupleDesc
     * @see BufferPool#getPageSize()
     */
    public HeapPage(HeapPageId id, byte[] data) throws IOException {
        // Store the page id, the TupleDesc (schema definition of the table), and
        // how many tuples can fit on this page
        this.pid = id;
        this.td = Database.getCatalog().getTupleDesc(id.getTableId());
        this.numSlots = getNumTuples();

        // Wrap the raw byte data array into a stream for sequential reading
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        // allocate and read the header slots of this page
        // Allocate the necessary bytes required to store the header: Each bit in the header
        // represents 1 tuple slot. 1 if occupied, 0 if free. 
        header = new byte[getHeaderSize()];
        // Read the header bytes and store it in header
        for (int i=0; i<header.length; i++)
            header[i] = dis.readByte();
        // Allocate an array to store all tuples
        tuples = new Tuple[numSlots];
        try{
            // allocate and read the actual records of this page
            // Pass the binary stream to read tuple data and the slotId where the tuple will be stored
            for (int i=0; i<tuples.length; i++)
                tuples[i] = readNextTuple(dis,i);
        }catch(NoSuchElementException e){
            e.printStackTrace();
        }
        dis.close();

        setBeforeImage();
    }

    /** Retrieve the number of tuples on this page.
        @return the number of tuples on this page
    */
    private int getNumTuples() {        
        // some code goes here
        // tuples_per_page = floor((page_size * 8) / (tuple_size * 8 + 1))
        // floor((BufferPool.getPageSize()*8) / (tuple_size * 8 + 1))
        // Integer division in java automatically floors the result?
        return (int) Math.floor((BufferPool.getPageSize() * 8) / (this.td.getSize() * 8 + 1));
        //return 0;

    }

    /**
     * Computes the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     * @return the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     */
    private int getHeaderSize() {        
        
        // some code goes here

        //todo CHECK ON THE NEED FOR DOUBLE
        
        // headerBytes = ceiling(tuples_per_page/8)
        // - Each tuple is assumed to require one bit of storage
        // - Take the total number of tuples, divided by 8, to get the bytes
        // Double is needed to preserve decimal
        return (int) Math.ceil((double)this.getNumTuples()/8);
        //return 0;
                 
    }
    
    /** Return a view of this page before it was modified
        -- used by recovery */
    public HeapPage getBeforeImage(){
        try {
            byte[] oldDataRef = null;
            synchronized(oldDataLock)
            {
                oldDataRef = oldData;
            }
            return new HeapPage(pid,oldDataRef);
        } catch (IOException e) {
            e.printStackTrace();
            //should never happen -- we parsed it OK before!
            System.exit(1);
        }
        return null;
    }
    
    public void setBeforeImage() {
        synchronized(oldDataLock)
        {
        oldData = getPageData().clone();
        }
    }

    /**
     * @return the PageId associated with this page.
     */
    public HeapPageId getId() {
    // some code goes here
    
        return this.pid;
    //throw new UnsupportedOperationException("implement this");
    }

    /**
     * Suck up tuples from the source file.
     */
    private Tuple readNextTuple(DataInputStream dis, int slotId) throws NoSuchElementException {
        // if associated bit is not set, read forward to the next tuple, and
        // return null.
        // Check the header bit for this slot. If 0: Empty. If 1: Not empty
        if (!isSlotUsed(slotId)) {
            // In the case where the tuple slot is empty,
            // Read and discard the bytes for the empty slot (td.getSize() bytes size of one tuple)
            // Ensures correct positioning for reading the next tuple
            for (int i=0; i<td.getSize(); i++) {
                try {
                    dis.readByte();
                } catch (IOException e) {
                    throw new NoSuchElementException("error reading empty tuple");
                }
            }
            return null;
        }


        // read fields in the tuple
        // read in field values into a tuple and return it
        // Create a new tuple using the tuple description/schema of the table
        Tuple t = new Tuple(td);
        // Create a new record id: Reference to a specific tuple on a specific page of a specific table
        // the pageid of the page on which the tuple resides, the tuple number within the page.
        RecordId rid = new RecordId(pid, slotId);
        // Set the recordId of the tuple
        t.setRecordId(rid);
        try {
            // td.numFields(): Get the number of fields/columns of a tuple using the TupleDesc (stores field names, field types)
            for (int j=0; j<td.numFields(); j++) {
                // Get the field type of the current field (indexed by j), read and parse the field value from the stream
                Field f = td.getFieldType(j).parse(dis);
                // Set the field value for the current field (indexed by j)
                t.setField(j, f);
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            throw new NoSuchElementException("parsing error!");
        }
        // Return the tuple
        return t;
    }

    /**
     * Generates a byte array representing the contents of this page.
     * Used to serialize this page to disk.
     * <p>
     * The invariant here is that it should be possible to pass the byte
     * array generated by getPageData to the HeapPage constructor and
     * have it produce an identical HeapPage object.
     *
     * Serialize a HeapPage into a byte array so it can written to disk
     * Requirement: When the byte array is read back later using the HeapPage constructor,
     * it should reconstruct the same page
     * 
     * 
     * @see #HeapPage
     * @return A byte array correspond to the bytes of this page.
     */
    public byte[] getPageData() {
        // Retrieve the size of the page in bytes
        // boas stores data in memory 
        // Initialise a data output stream to write binary data to boas
        int len = BufferPool.getPageSize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
        DataOutputStream dos = new DataOutputStream(baos);

        // create the header of the page
        // for each byte in the header defined earlier,
        // write the byte to the boas using the dos
        for (byte b : header) {
            try {
                dos.writeByte(b);
            } catch (IOException e) {
                // this really shouldn't happen
                e.printStackTrace();
            }
        }

        // create the tuples
        // for each tuple (total number of tuples possible on a page)
        for (int i=0; i<tuples.length; i++) {

            // empty slot
            // Check the header bit for this slot. If 0: Empty. If 1: Not empty
            // If the slot is empty
            if (!isSlotUsed(i)) {
                // Write empty bytes for the empty slot (td.getSize() bytes size of one tuple)
                // Ensures correct positioning for reading the next tuple
                for (int j=0; j<td.getSize(); j++) {
                    try {
                        dos.writeByte(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                continue;
            }


            // non-empty slot
            // for each field (indexed by j) in the current tuple (indexed by i)
            //  get the field value
            //  write the field value (binary data) into the dos, into the boas
            for (int j=0; j<td.numFields(); j++) {
                Field f = tuples[i].getField(j);
                try {
                    f.serialize(dos);
                
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // padding
        // Fill the remaining page space with zero bytes
        int zerolen = BufferPool.getPageSize() - (header.length + td.getSize() * tuples.length); //- numSlots * td.getSize();
        byte[] zeroes = new byte[zerolen];
        try {
            dos.write(zeroes, 0, zerolen);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Return the boas as a byte array
        return baos.toByteArray();
    }

    /**
     * Static method to generate a byte array corresponding to an empty
     * HeapPage.
     * Used to add new, empty pages to the file. Passing the results of
     * this method to the HeapPage constructor will create a HeapPage with
     * no valid tuples in it.
     *
     * @return The returned ByteArray.
     */
    public static byte[] createEmptyPageData() {
        int len = BufferPool.getPageSize();
        return new byte[len]; //all 0
    }





    /**
     * Delete the specified tuple from the page; the corresponding header bit should be updated to reflect
     *   that it is no longer stored on any page.
     * @throws DbException if this tuple is not on this page, or tuple slot is
     *         already empty.
     * @param t The tuple to delete
     */
    public void deleteTuple(Tuple t) throws DbException {
        // some code goes here
        // not necessary for lab1

        // Throw DbException if tuple is not on page
        if(t.getRecordId().getPageId()!=this.pid) {
            throw new DbException("Tuple is not on this page");
        }

        // Throw DbException if tuple slot is already empty
        if(!this.isSlotUsed(t.getRecordId().getTupleNumber())) {
            throw new DbException("Tuple slot is already empty");
        }

        // Delete the tuple from the page
        tuples[t.getRecordId().getTupleNumber()] = null;

        // Corresponding header bit should be updated to reflect that it is
        // no longer stored on any page
        this.markSlotUsed(t.getRecordId().getTupleNumber(), false);
    }




    /**
     * Adds the specified tuple to the page;  the tuple should be updated to reflect
     *  that it is now stored on this page.
     * @throws DbException if the page is full (no empty slots) or tupledesc
     *         is mismatch.
     * @param t The tuple to add.
     */
    public void insertTuple(Tuple t) throws DbException {
        // some code goes here
        // not necessary for lab1
        // You may find that the getNumEmptySlots() and isSlotUsed() methods we asked you to implement in Lab 1 serve as useful abstractions

        if(this.getNumEmptySlots()==0){
            throw new DbException("Page is full (no empty slots)");
        }

        if(!this.td.equals(t.getTupleDesc())){
            throw new DbException("Tuple Desc is a mismatch");
        }

        // Get an empty slot
        for(int i=0; i<this.numSlots; i++) {
            // If the current slot is empty
            if(!isSlotUsed(i)) {
                // Update the tuple to reflect that it is now stored on this page
                t.setRecordId(new RecordId(this.pid, i));
                // Store the tuple in the slot. Add the specified tuple to the page
                this.tuples[i] = t;
                // Mark that the slot is used
                this.markSlotUsed(i, true);
                break;
            }
        }
    }





    /**
     * Marks this page as dirty/not dirty and record that transaction
     * that did the dirtying
     */
    public void markDirty(boolean dirty, TransactionId tid) {
        // some code goes here
	// not necessary for lab1

        // Mark who modified the page
        if (dirty) {
            this.dirtyTid = tid;
        } else {
            this.dirtyTid = null;
        }
    }




    /**
     * Returns the tid of the transaction that last dirtied this page, or null if the page is not dirty
     */
    public TransactionId isDirty() {
        // some code goes here
	// Not necessary for lab1
        
        return this.dirtyTid;
        //return null;      
    }




    /**
     * Returns the number of empty slots on this page.
     */
    public int getNumEmptySlots() {
        // some code goes here

        int emptySlots = 0;
        for(int i=0; i<this.numSlots;i++) {
            if(!(isSlotUsed(i))) {
                emptySlots++;
            }
        }
        
        return emptySlots;
        //return 0;
    }

    /**
     * Returns true if associated slot on this page is filled.
     */
    public boolean isSlotUsed(int i) {
        // some code goes here
        // These require pushing around bits in the page header.
        // Each element in header is 1 byte (tracks 8 slots)
        
        // Example 10/8 = 1
        // slotId 10 is stored in header[1]. Gives the position of the byte that contains the bit for slot i
        int byteIndex = i/8;
        // Example 10%8 = 2
        // slotId 10 is in bit 2 of header[1]. Gives the position of bit inside the byte
        int bitIndex = i%8;
        // (byteIndex >> bitIndex) -> Shift the byte to the right by i%8 positions. Move the bit for slot i to the LSB
        // https://stackoverflow.com/questions/18806481/how-can-i-get-the-value-of-the-least-significant-bit-in-a-number
        // & 1 isolates the LSB
        return ((header[byteIndex] >> bitIndex) & 1)==1;
        //return false;
    }




    /**
     * Abstraction to fill or clear a slot on this page.
     */
    private void markSlotUsed(int i, boolean value) {
        // some code goes here
        // not necessary for lab1

        // abstraction to modify the filled or cleared status of a tuple in the page header

        // Example 10/8 = 1
        // slotId 10 is stored in header[1]. Gives the position of the byte that contains the bit for slot i
        int byteIndex = i/8;
        // Example 10%8 = 2
        // slotId 10 is in bit 2 of header[1]. Gives the position of bit inside the byte
        int bitIndex = i%8;

        // Create bit mask
        // Example: 0b11111011 (Target bit 2 of the current byte) -> corresponds to the current slot
        byte bitMask = (byte) ~(1 << bitIndex);

        // Use & operation to set the target bit (current slot) to 0
        byte reset_target_bit = (byte) (header[byteIndex] & bitMask);

        // Set used to 1 (true) or 0 (false)
        byte used = (byte) (value ? 1:0);

        // First, shift 'used' to the slot position. Next, use the | operation to mark whether the slot is used
        // 0 | 0 -> 0. 0 | 1 -> 1
        header[byteIndex] = (byte) (reset_target_bit | (used << bitIndex));
    }




    /**
     * @return an iterator over all tuples on this page (calling remove on this iterator throws an UnsupportedOperationException)
     * (note that this iterator shouldn't return tuples in empty slots!)
     */
    public Iterator<Tuple> iterator() {
        // some code goes here
        
        ArrayList<Tuple> tuples = new ArrayList<Tuple>();

        for(int i=0; i < this.getNumTuples(); i++){
            // See the null returned in readNextTuple
            if(this.tuples[i]!=null) {
                tuples.add(this.tuples[i]);
            }
        } 

        return tuples.iterator();

        // return null;
    }

}

