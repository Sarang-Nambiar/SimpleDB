# **Lab 1 Overview**

## **TupleDesc**

Describes the schema of a tuple (database table row): Defines Field Names and Field Types of a tuple

**TDItem**

Stores the Field Names and Field Types for each TupleDesc Item

<u>Constructor</u>
- ```public TDItem(Type t, String n)```: Initialize the Field Name and Field Type for a single TD Item

**TupleDesc**

<u>Constructor</u>

- ```public TupleDesc(Type[] typeAr, String[] fieldAr)```: Initialize an ArrayList of TDItem types that store fieldType and fieldName
- ```public TupleDesc(Type[] typeAr)```: Initialize an ArrayList of TDItem types that store fieldType and null fieldName

<br/>

<u>Methods</u>

- ```public Iterator<TDItem> iterator()```: Returns an iterator over the ArrayList: TDItems
- ```public int numFields()```: Returns the length of the ArrayList: TDItems
- ```public String getFieldName(int i)```: Return the fieldName of the i-th TDItem in the ArrayList: TDItems
- ```public Type getFieldType(int i)```: Return the fieldType of the i-th TDItem in the ArrayList: TDItems
- ```public int fieldNameToIndex(String name)```: Return the index of the fieldName in the ArrayList: TDItems
- ```public int getSize()```: Return the size in bytes of tuples that use the TupleDesc schema
- ```public static TupleDesc merge(TupleDesc td1, TupleDesc td2)```: Merge 2 TupleDesc into 1
- ```public boolean equals(Object o)```: Compare 2 TupleDesc for equality

<br/>
<br/>
<br/>

## **Tuple**

Maintains information about the contents of a Tuple. Its schema is defined by TupleDesc. Tuples consist of a collection of Field objects, one per field in the Tuple

<u>Constructor</u>

- ```public Tuple(TupleDesc td)```: Initialize the TupleDesc schema used, the recordId of the tuple, a Field type array used to store the Tuple's fields

<br/>

<u>Methods</u>

- ```public TupleDesc getTupleDesc()```: Returns the TupleDesc schema used
- ```public RecordId getRecordId()```: Returns the recordId of the Tuple (represents the location of the tuple on the disk)
- ```public void setRecordId(RecordId rid)```: Set the recordId information of the Tuple
- ```public void setField(int i, Field f)```: Set the value (Field type) of the i-th field of the tuple
- ```public Field getField(int i)```: Return the value of the i-th field of the tuple
- ```public Iterator<Field> fields()```: Returns an iterator over the fields array of the Tuple
- ```public void resetTupleDesc(TupleDesc td)```: Sets the TupleDesc schema used


<br/>
<br/>
<br/>

## **Catalog**

A catalog of the table names, Db File (contains db table data), table Id, Primary key

Keeps track of all db table info

<u>Constructor</u>

- ```public Catalog()```: Initialize mapping tables tableNameToDbFile, tableIdToDbFile, tableIdToPriKey

<br/>

<u>Methods</u>

- ```public void addTable(DbFile file, String name, String pkeyField)```: Get the table Id from the Db File. Populate the 3 mapping tables mentioned earlier
- ```public void addTable(DbFile file)```: The table name is given as the UUID of the file. The primary key field is null. 
- ```public int getTableId(String name)```: Return the table Id given the table name
- ```public TupleDesc getTupleDesc(int tableid)```: Return the TupleDesc of the table (taken from the DbFile)
- ```public DbFile getDatabaseFile(int tableid)```: Return the DbFile (db table data) given the table Id
- ```public String getPrimaryKey(int tableid)```: Return the primary key given the table Id
- ```public Iterator<Integer> tableIdIterator()```: Return an iterator over the table Ids and Db Files
- ```public String getTableName(int id)```: Return the table name given the table Id
- ```public void clear()```: Clear all 3 mapping tables

<br/>
<br/>
<br/>

## **BufferPool**

Manages caching pages in memory that have been recently read from the disk

All operators read and write pages (Part of a Db File, a Db File contains many pages. Each page stores many tuples.) through the BufferPool

The BufferPool can hold a fixed number of pages

<u>Constructor</u>

- ```public BufferPool(int numPages)```: Initialize the number of pages the BufferPool can cache and the pageId to Page mapping

<br/>

<u>Methods</u>

- ```public static int getPageSize()```: Return the default page size in bytes 
- ```public Page getPage(TransactionId tid, PageId pid, Permissions perm)```: If the page is in the mapping table (cache), use the pageId to return the page. If it is not in the mapping table and the total number of entries in the mapping table does not exceed the maximum number of pages that can be cached, store the PageId and Page into the mapping table

<br/>
<br/>
<br/>

## **HeapPageId**

A reference to the specified page of a specific table (table Id). A unique identifier for HeapPage objects.

About HeapFiles: Heap Files are an unordered file of tuples, there is one HeapFile object for each db table. They are arranged into a set of pages defined by HeapPage. Each page has a 'header' of bytes where 1 bit corresponds to 1 tuple slot. If the bit is 1, that tuple is valid. If the bit is 0, that tuple is invalid. 

To be more detailed, 
- Number of tuples that can fit into a page: floor((page_size * 8) / (tuple_size * 8 + 1)). '+1' because each tuple requires one additional bit of storage in the header. floor((BufferPool.getPageSize()*8) / (tuple_size * 8 + 1))
- Number of bytes required to store the header: ceiling(tuples_per_page/8)

<u>Constructor</u>

- ```public HeapPageId(int tableId, int pgNo)```: Initializes the table Id and the page number

<br/>

<Methods>

- ```public int getTableId()```: Return the table Id that the page Id belongs to
- ```public int getPageNumber()```: Return the page number associated with the page Id
- ```public int hashCode()```: Return the hash code for this page. Combo of table id and page number.
- ```public boolean equals(Object o)```: Check if two PageId objects are equal

<br/>
<br/>
<br/>

## **RecordId**

Acts as a reference to a specific tuple/row on a specific page of a specific table/db file

<u>Constructor</u>

- ```public RecordId(PageId pid, int tupleno)```: Initialize the page Id of the page where the tuple resides and the tuple number/slot id

<br/>

<u>Methods</u>

- ```public int getTupleNumber()```: Return the tuple number/slot id
- ```public PageId getPageId()```: Return the page Id of the page where the tuple resides 
- ```public boolean equals(Object o)```: Compare 2 RecordId objects to check if they are equal
- ```public int hashCode()```: Hash the RecordId using a combo if the page Id and tuple number/slot id

<br/>
<br/>
<br/>

## **HeapPage**

Maintains a HeapPage from a set of bytes read from a file

<u>Constructor</u>

- ```public HeapPage(HeapPageId id, byte[] data)```: Initialize the HeapPageId, the TupleDesc, the number of tuple slots/rows in the HeapPage. It reads the byte data and populates the header (header byte array) (each bit in the header represents 1 tuple slot. 1 if occupied, 0 if free) as well as the tuples in the page (tuples array)

<br/>

<u>Methods</u>. 

- ```private int getNumTuples()```: Get the number of tuples in the page. ```(int) Math.floor((BufferPool.getPageSize() * 8) / (this.td.getSize() * 8 + 1));```
- ```private int getHeaderSize()```: Get the number of bytes required for the header. ```return (int) Math.ceil((double)this.getNumTuples()/8);```. **double seems to be needed here**
- ```public HeapPageId getId()```: Return the PageId of this HeapPage
- ```private Tuple readNextTuple(DataInputStream dis, int slotId)```: Reads tuples from a byte array
- ```public byte[] getPageData()```: Convert the header byte array and tuples array back into a byte array
- ```public int getNumEmptySlots()```: Return the number of empty slots/rows on this HeapPage
- ```public boolean isSlotUsed(int i)```: Return true if the associated slot on the HeapPage is filled, given the slot id 
- ```public Iterator<Tuple> iterator()```: Return an iterator over the tuples array (does not return tuples in empty slots)

<br/>
<br/>
<br/>

## **HeapFile**

HeapFile is an implementation of DbFile that stores a collection of HeapPages (that stores header and tuples). All in all it stores a collection of tuples in no particular order. 

HeapFile reads a file (bytes). The 'pages' in the file are represented by HeapPages -> Stores header and tuples.
HeapPageId is a reference to the specified page number and table id. A unique identifier/reference to a specific page of a specific table
Tuple stores the tuple information, TupleDesc defines the schema, RecordId is a reference to a specific tuple on a specific page of a specific table.

<u>Constructor</u>

- ```public HeapFile(File f, TupleDesc td)```: Initialize the file and the TupleDesc

<br/>

<u>Methods</u>

- ```public File getFile()```: Return the file backing this HeapFile
- ```public int getId()```: Return the hashcode of the file backing this HeapFile (an id uniquely identifying this HeapFile)
- ```public TupleDesc getTupleDesc()```: Return the TupleDesc of the table stored in the file backing this HeapFile
- ```public Page readPage(PageId pid)```: Given a page id/HeapPageId, read it's page data from the file into a HeapPage 
- ```public int numPages()```: Return the number of pages in this HeapFile/file backing this HeapFile
- ```public DbFileIterator iterator(TransactionId tid)```: Return an iterator over the HeapFile (get the tuples in the HeapFile)

<br/>

**HeapFileIterator**

Inner class to iterate over the HeapFile

<u>Constructor</u>

- ```public HeapFileIterator(TransactionId transactionId, HeapFile heapFile)```: Initialize the transaction Id, the HeapFile, the table Id (Id of the HeapFile)

<br/>

<u>Method</u>

- ```public void open() throws DbException, TransactionAbortedException```: Opens the iterator. Initialise the HeapPageId with this tableId and 0 (1st page, 0 indexed). Use it to get the page from the BufferPool. Return an iterator over the HeapPage
- ```public boolean hasNext()```: Returns true if there are more tuples available in this HeapPage. If there are no more on this page, get the next page and return an iterator over it. Return true if it has tuples.
- ```public Tuple next()```: Get the next tuple
- ```public void rewind()```: Resets the iterator to the start
- ```public void close()```: Closes the iterator

<br/>
<br/>
<br/>

## **SeqScan**

An implementation of a sequential scan access method that reads each tuple of a table in no particular order

<u>Constructor</u>

- ```public SeqScan(TransactionId tid, int tableid, String tableAlias)```: Initialize the transaction id, table Id, table alias, the db file by using the catalog and an iterator over the db file
- ```public SeqScan(TransactionId tid, int tableId)```: Same as the first constructor but the tableAlias is the table name retrieved from the Catalog using the table Id

<br/>

<u>Method</u>

- ```public String getTableName()```: Return the table name of the table being scanned by using the Catalog
- ```public String getAlias()```: Return the table alias
- ```public void reset(int tableid, String tableAlias)```: Reset -> Basically same as the constructor - the transaction id
- ```public void open()```: Open the file iterator
- ```public TupleDesc getTupleDesc()```: Return the TupleDesc. But, add the table alias as a prefix to the field names of the TupleDesc
- ```public boolean hasNext()```: Return whether there is a next tuple or not
- ```public Tuple next()```: Return the next tuple
- ```public void close()```: Close the file iterator
- ```public void rewind()```: Rewind the file iterator


<br/>
<br/>
<br/>

## **Tests**

| Test Name                            | Result            | Remarks |
|--------------------------------------|-------------------|---------|
| ant runtest -Dtest=TupleTest         | BUILD SUCCESSFUL  |         |
| ant runtest -Dtest=TupleDescTest     | BUILD SUCCESSFUL  |         |
| ant runtest -Dtest=CatalogTest       | BUILD SUCCESSFUL  |         |
| ant runtest -Dtest=HeapPageIdTest    | BUILD SUCCESSFUL  |         |
| ant runtest -Dtest=RecordIdTest      | BUILD SUCCESSFUL  |         |
| ant runtest -Dtest=HeapPageReadTest  | BUILD SUCCESSFUL  |         |
| ant runtest -Dtest=HeapFileReadTest  | BUILD SUCCESSFUL  |         |
| ant runsystest -Dtest=ScanTest       | BUILD SUCCESSFUL  |         |