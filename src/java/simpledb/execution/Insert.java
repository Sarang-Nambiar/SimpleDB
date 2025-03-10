package simpledb.execution;

import java.io.IOException;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.storage.BufferPool;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

//Added
import simpledb.common.Type;
import simpledb.storage.IntField;

/**
 * Inserts tuples read from the child operator into the tableId specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid;
    private OpIterator child;
    private int tableId;
    private TupleDesc td;

    // Needed because of fetchNext(): return null if called more than once
    private Boolean fetched;

    /**
     * Constructor.
     *
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableId
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */
    public Insert(TransactionId t, OpIterator child, int tableId)
            throws DbException {
        // some code goes here

        // Insert operator is also an iterator
        // Operators act as iterators

        this.tid = t;
        // Child operator contains tuples that Insert will operate on 
        // The child operator is an iterator that contains tuples. See execution/OpIterator
        this.child = child;
        this.tableId = tableId;

        // Initialize the tuple desc of the tuple to return
        // "returning a single tuple with 1 integer field containing the count"
        this.td = new TupleDesc(new Type[]{Type.INT_TYPE});

        // Throw DbException if TupleDesc of child differs from table into which we are to insert
        if(!child.getTupleDesc().equals(Database.getCatalog().getTupleDesc(this.tableId))){
            throw new DbException("TupleDesc of child differs from the table which its to be inserted into");
        }
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        // return null;
        return this.td;
    }


    public void open() throws DbException, TransactionAbortedException {
        // some code goes here

        // super.open() is needed because Insert extends the Operator class
        // Insert is also an iterator
        super.open();
        this.child.open();
        this.fetched = false;
    }

    public void close() {
        // some code goes here

        super.close();
        this.child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        this.child.rewind();
        this.close();
        this.open();
    }

    /**
     * Inserts tuples read from child into the tableId specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        // return null;

        // Modifies pages on disk. Returns the number of affected tuples.
        // Implemented by returning a single tuple with 1 integer field containing the count
        // Adds tuples it reads from its child operator to the tableid specified in its constructor
        // Use BufferPool.insertTuple() to do this

        // If fetchNext() has been called more than once, return null
        // Its a one time action
        if(this.fetched){
            return null;
        }

        // Set fetched to true
        this.fetched = true;
        int num_inserts = 0;

        // Read tuples from the child operator
        // Insert the tuples to the table id specified in the constructor
        // Use BufferPool.insertTuple()
        while(this.child.hasNext()) {
            Tuple tuple = child.next();
            try {
                Database.getBufferPool().insertTuple(this.tid, this.tableId, tuple);
                num_inserts++;
            } catch (IOException e) {
                throw new DbException("Error occured during insertion");
            }
        }
        
        // Return a one field tuple containing the number of inserted records
        Tuple num_inserts_res = new Tuple(this.td);
        num_inserts_res.setField(0, new IntField(num_inserts));
        return num_inserts_res;
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        // return null;
        return new OpIterator[] {this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        if(children.length > 0){
            this.child = children[0];
        }
    }
}
