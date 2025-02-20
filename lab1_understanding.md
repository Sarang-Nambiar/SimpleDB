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
- Number of tuples that can fit into a page: floor((page_size * 8) / (tuple_size * 8 + 1)). '+1' because each tuple requires one additional bit of storage in the header
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

<br/>
<br/>
<br/>

## **HeapPage**

<br/>
<br/>
<br/>

## **HeapFile**

<br/>
<br/>
<br/>

## **SeqScan**

<br/>
<br/>
<br/>
