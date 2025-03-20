package simpledb.execution;

import simpledb.common.Database;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;
import simpledb.common.Type;
import simpledb.common.DbException;
import simpledb.storage.DbFileIterator;
import simpledb.storage.HeapFile;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.storage.DbFile; // added import

import java.util.*;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 * 
 * Operators:
 * The query parser takes a SQL query and converts it into a logical plan. 
 * This logical plan represents the SQL query as a tree of relational algebra operators.
 * The query optimizer will then take this logical plan and convert it into a physical plan composed of 
 * physical DBIterator operators by applying equivalence rules and cost-based optimization.
 * 
 * The DBIterator physical operators are the actual primitives used to execute the query
 * 
 * The DbIterator interface lets physical operators fetch tuples from their children using hasNext() 
 * and next(). These tuples flow starting from the leaves of physical plan tree to the root while 
 * undergoing transformations performed by intermediate operators. The leaf nodes of the physical plan 
 * tree are always going to be operators that read tuples from the buffer pool. After the tuples 
 * reach the root node, they are displayed to the user as query results.
 * 
 * Sequential Table Scans is one such physical operator currently supported by SimpleDB
 * 
 * Operators are responsible for the actual execution of the query plan. 
 * They implement the operations of the relational algebra. In SimpleDB, 
 * operators are iterator based; each operator implements the DbIterator interface.
 * 
 * Operators are connected together into a plan by passing lower-level operators into the 
 * constructors of higher-level operators, i.e., by 'chaining them together.' 
 * Special access method operators at the leaves of the plan are responsible for reading data 
 * from the disk (and hence do not have any operators below them).
 * 
 * At the top of the plan, the program interacting with SimpleDB simply calls getNext on the root operator; 
 * this operator then calls getNext on its children, and so on, until these leaf operators are called. 
 * They fetch tuples from disk and pass them up the tree (as return arguments to getNext); 
 * tuples propagate up the plan in this way until they are output at the root or combined or 
 * rejected by another operator in the plan.
 * 
 * Sequential Scan: Sequentially scans all of the tuples from the pages of the table specified by the 
 * tableid in the constructor. This operator should access tuples through the DbFile.iterator() method.
 * 
 * 
 * 
 * 
 */
public class SeqScan implements OpIterator {

    private static final long serialVersionUID = 1L;

    private TransactionId tId;
    private int tableId;
    private String tableAlias;
    private DbFile file;
    private DbFileIterator fileIterator;

    /**
     * Creates a sequential scan over the specified table as a part of the
     * specified transaction.
     *
     * @param tid
     *            The transaction this scan is running as a part of.
     * @param tableid
     *            the table to scan.
     * @param tableAlias
     *            the alias of this table (needed by the parser); the returned
     *            tupleDesc should have fields with name tableAlias.fieldName
     *            (note: this class is not responsible for handling a case where
     *            tableAlias or fieldName are null. It shouldn't crash if they
     *            are, but the resulting name can be null.fieldName,
     *            tableAlias.null, or null.null).
     */
    public SeqScan(TransactionId tid, int tableid, String tableAlias) {
        // some code goes here
        this.tId = tid;
        this.tableId = tableid;
        this.tableAlias = tableAlias;
        // Check if its ok to use DbFile
        this.file = Database.getCatalog().getDatabaseFile(this.tableId);
        this.fileIterator = this.file.iterator(this.tId);
    }

    /**
     * @return
     *       return the table name of the table the operator scans. This should
     *       be the actual name of the table in the catalog of the database
     * */
    public String getTableName() {
	// some code goes here
        return Database.getCatalog().getTableName(this.tableId);
        //return null;
    }

    /**
     * @return Return the alias of the table this operator scans.
     * */
    public String getAlias()
    {
        // some code goes here
        return this.tableAlias;
        //return null;
    }

    /**
     * Reset the tableid, and tableAlias of this operator.
     * @param tableid
     *            the table to scan.
     * @param tableAlias
     *            the alias of this table (needed by the parser); the returned
     *            tupleDesc should have fields with name tableAlias.fieldName
     *            (note: this class is not responsible for handling a case where
     *            tableAlias or fieldName are null. It shouldn't crash if they
     *            are, but the resulting name can be null.fieldName,
     *            tableAlias.null, or null.null).
     */
    public void reset(int tableid, String tableAlias) {
        // some code goes here

        this.tableId = tableid;
        this.tableAlias = tableAlias;
        // Check if its ok to use DbFile
        this.file = Database.getCatalog().getDatabaseFile(this.tableId);
        this.fileIterator = this.file.iterator(this.tId);
    }

    public SeqScan(TransactionId tid, int tableId) {
        this(tid, tableId, Database.getCatalog().getTableName(tableId));
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here

        // Sequential Scan: Sequentially scans all of the tuples from the pages of the table specified by the 
        // *tableid in the constructor. This operator should access tuples through the DbFile.iterator() method
        this.fileIterator.open();
    }

    /**
     * Returns the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor. This prefix
     * becomes useful when joining tables containing a field(s) with the same
     * name.  The alias and name should be separated with a "." character
     * (e.g., "alias.fieldName").
     *
     * @return the TupleDesc with field names from the underlying HeapFile,
     *         prefixed with the tableAlias string from the constructor.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here

        TupleDesc old_td = this.file.getTupleDesc();
        // See TupleDesc.java
        Type[] new_tdTypeArr = new Type[old_td.numFields()];
        String[] new_tdFieldArr = new String[old_td.numFields()];

        for(int i=0; i < old_td.numFields(); i++) {
            new_tdTypeArr[i] = old_td.getFieldType(i);
            // alias and name should be separated with a "." character: alias.fieldName
            new_tdFieldArr[i] = this.tableAlias + "." + old_td.getFieldName(i);
        }

        TupleDesc new_td = new TupleDesc(new_tdTypeArr, new_tdFieldArr);
        return new_td;
        //return null;
    }

    public boolean hasNext() throws TransactionAbortedException, DbException {
        // some code goes here
        return this.fileIterator.hasNext();
        //return false;
    }

    public Tuple next() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        // some code goes here
        return this.fileIterator.next();
        //return null;
    }

    public void close() {
        // some code goes here
        this.fileIterator.close();
    }

    public void rewind() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        this.fileIterator.rewind();
    }
}
