package simpledb.execution;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.BufferPool;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.IOException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid;
    private OpIterator child;
    private TupleDesc td;

    // Needed because of fetchNext(): return null if called more than once
    private Boolean fetched;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     * 
     * @param t
     *            The transaction this delete runs in
     * @param child
     *            The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, OpIterator child) {
        // some code goes here

        this.tid = t;
        // Child operator contains tuples that the Insert operator will operate on 
        // The child operator is an iterator that contains tuples. See execution/OpIterator
        this.child = child;

        // Initialize the tuple desc of the tuple to return
        // "returning a single tuple with 1 integer field containing the count"
        this.td = new TupleDesc(new Type[]{Type.INT_TYPE});
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        // return null;
        return this.td;
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here

        // super.open() is needed because Delete extends the Operator class
        // Delete is also an iterator
        super.open();
        this.child.open();
        // Re-initialized fetched flag to false
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
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     * 
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        // return null;

        // This operator deletes the tuples it reads from its child operator from the tableid specified in its constructor. 
        // It should use the BufferPool.deleteTuple() method to do this.

        // Deletes tuples as they are read from the child operator

        // If fetchNext() has been called more than once, return null
        // It's a one time action for an operator
        if(this.fetched){
            return null;
        }

        // Set fetched to true
        this.fetched = true;
        int num_deletions = 0;

        // Read tuples from the child operator
        // Delete the tuples read from its child operator in its corresponding table id (table id is fetched in deleteTuple from the tuple)
        // Deletes are processed via the BufferPool. An instances of BufferPool is available via Database.getBufferPool()
        // Use BufferPool.deleteTuple()
        while(this.child.hasNext()) {
            Tuple tuple = child.next();
            try {
                Database.getBufferPool().deleteTuple(this.tid, tuple);
                num_deletions++;
            } catch (IOException e) {
                throw new DbException("Error occured during deletio");
            }
        }

        // Return a one field tuple containing the number of deleted records
        Tuple num_deletions_res = new Tuple(this.td);
        num_deletions_res.setField(0, new IntField(num_deletions));
        return num_deletions_res;
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
        // Deletion Operator should only have one source/child
        if(children.length > 0){
            this.child = children[0];
        }
    }

}
