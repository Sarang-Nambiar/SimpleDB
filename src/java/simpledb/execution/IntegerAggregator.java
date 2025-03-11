package simpledb.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simpledb.storage.Field;
import simpledb.storage.IntField;
import simpledb.common.Type;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.storage.TupleIterator;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;

    private Map<Field, int[]> groups;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *                    the 0-based index of the group-by field in the tuple, or
     *                    NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *                    the type of the group by field (e.g., Type.INT_TYPE), or
     *                    null
     *                    if there is no grouping
     * @param afield
     *                    the 0-based index of the aggregate field in the tuple
     * @param what
     *                    the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.groups = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        int aggVal = ((IntField) tup.getField(afield)).getValue();
        Field groupKey = (gbfield == NO_GROUPING) ? null : tup.getField(gbfield);
        int[] aggInfo = groups.get(groupKey);

        if (aggInfo == null) {
            aggInfo = new int[4];
            // set sum, count, min, and max to aggVal.
            aggInfo[0] = aggVal; // sum
            aggInfo[1] = 1; // count
            aggInfo[2] = aggVal; // min
            aggInfo[3] = aggVal; // max
            groups.put(groupKey, aggInfo);
        } else {
            // update sum, count, min, max.
            aggInfo[0] += aggVal;
            aggInfo[1] += 1;
            aggInfo[2] = Math.min(aggInfo[2], aggVal);
            aggInfo[3] = Math.max(aggInfo[3], aggVal);
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     * 
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        // throw new
        // UnsupportedOperationException("please implement me for lab2");
        List<Tuple> results = new ArrayList<>();
        TupleDesc td;

        if (gbfield == NO_GROUPING) {
            td = new TupleDesc(new Type[] { Type.INT_TYPE }, new String[] { "aggregateVal" });
        } else {
            td = new TupleDesc(new Type[] { gbfieldtype, Type.INT_TYPE }, new String[] { "groupVal", "aggregateVal" });
        }

        // Iterate over each group and compute the aggregate based on the operator.
        for (Map.Entry<Field, int[]> entry : groups.entrySet()) {
            Tuple t = new Tuple(td);
            int aggregate;
            int[] info = entry.getValue();

            switch (what) {
                case AVG:
                    aggregate = info[0] / info[1];
                    break;
                case COUNT:
                    aggregate = info[1];
                    break;
                case MIN:
                    aggregate = info[2];
                    break;
                case MAX:
                    aggregate = info[3];
                    break;
                case SUM:
                    aggregate = info[0];
                    break;
                default:
                    throw new IllegalStateException("Unsupported aggregate operator");
            }

            if (gbfield == NO_GROUPING) {
                t.setField(0, new IntField(aggregate));
            } else {
                t.setField(0, entry.getKey());
                t.setField(1, new IntField(aggregate));
            }

            results.add(t);
        }

        return new TupleIterator(td, results);
    }

}
