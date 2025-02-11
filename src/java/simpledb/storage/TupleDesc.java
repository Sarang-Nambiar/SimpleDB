package simpledb.storage;

import simpledb.common.Type;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 * 
 * Tuple, Tuple Desc (tuple schema) helps to manage tuples
 * Tuple -> Database Rows
 * Each tuple has a set of fields which represent the col values for the given row
 * Tuples consist of a collection of 'Field' objects -> One 'Field' object per field in the Tuple
 * The type/schema of a Tuple is represented by a TupleDesc object
 * The TupleDesc object consists of a collection of 'Type' objects -> One 'Type' object per field in the Tuple,
 * each of which decribes the types of the field 
 * 
 * 
 * 
 * Tuples consist of a collection of `Field` objects, one per field in the `Tuple`
 * `Field`: interface that different data types
 * Tuples have a type/schema represented by `TupleDesc` object
 * `TupleDesc` object consists of a collection of `Type` objects, one per field in the tuple
 * `Type`: Describes the type of the corresponding field
 * 
 * 
 * 
 * Serializable -> Interface that specifies class is serializable: Convert the class instance/object
 * into a format which can be written to a disk
 */
public class TupleDesc implements Serializable {

    // `TupleDesc` object consists of a collection of `Type` objects, one per field in the tuple
    // ArrayList to store all the TDItem (describes the corresponding field) objects that are included in this TupleDesc
    private ArrayList<TDItem> TDItems;

    /**
     * A help class to facilitate organizing the information of each field
     * 
     * Help to represent individual fields of the tuple
     * 
     * static -> things within the class can be called directly as such: ClassName.whatever
     * */
    public static class TDItem implements Serializable {


        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * 
         * Class representing a type in SimpleDB.
         * */
        public final Type fieldType;
        
        /**
         * The name of the field
         * */
        public final String fieldName;

        /*
         * Constructor: initialize field name and field type when a TDItem object is created
         */
        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        /*
         * Override the default toString() method
         * To display the field name and field type as: fieldName(fieldType)
         */
        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }



    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        // some code goes here

        // Return an iterator of the ArrayList
        // https://www.geeksforgeeks.org/arraylist-iterator-method-in-java-with-examples/
        return this.TDItems.iterator();
        //return null;
    }



    private static final long serialVersionUID = 1L;



    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // some code goes here

        // Constructor for TupleDesc class
        // Initialize the array to store the field TDItems
        this.TDItems = new ArrayList<TDItem>();

        // Create the TDItems with the specified types and names
        for(int i=0; i < typeAr.length; i++) {
            TDItem tditem = new TDItem(typeAr[i], fieldAr[i]); 
            this.TDItems.add(tditem);
        }
    }



    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // some code goes here

        // Constructor overloading
        this.TDItems = new ArrayList<TDItem>();
        // Create the TDItems with the specified types and null name
        for(int i=0; i < typeAr.length; i++) {
            TDItem tditem = new TDItem(typeAr[i], null); 
            this.TDItems.add(tditem);
        }
    }



    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        // some code goes here
        
        // Use size property
        return this.TDItems.size();
        //return 0;
    }




    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     * 
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        // some code goes here

        // Get the ith element, and then use the fieldName property of the ith element 
        return this.TDItems.get(i).fieldName;
        //return null;
    }



    /**
     * Gets the type of the ith field of this TupleDesc.
     * 
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        // some code goes here

        // Get the ith element, and then use the fieldType property of the ith element 
        return this.TDItems.get(i).fieldType;
        //return null;
    }



    /**
     * Find the index of the field with a given name.
     * 
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        // some code goes here

        // See TupleDescTest.java
        if(name==null) {
            throw new NoSuchElementException("null name provided");
        }

        // Loop through TDItems to find the TDItem with the corresponding name
        // See TupleDescTest.java -> For addition of null condition
        for(int i=0; i < this.TDItems.size(); i++) {
            if(getFieldName(i)!=null && getFieldName(i).equals(name)){
                return i;
            }
        }

        throw new NoSuchElementException("No field with a matching name is found");
        //return 0;
    }


    

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        // some code goes here

        int size = 0;
        for(int i=0; i < this.TDItems.size(); i++) {
            // See common/Type getLen -> number of bytes required to store a field of this type
            size+=TDItems.get(i).fieldType.getLen();
        }
        return size;
        //return 0;
    }



    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     * 
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        // some code goes here

        Type[] td3_types = new Type[td1.numFields() + td2.numFields()];
        String[] td3_names = new String[td1.numFields() + td2.numFields()];

        for(int i=0; i < td1.numFields(); i++){
            td3_types[i] = td1.getFieldType(i);
            td3_names[i] = td1.getFieldName(i);
        }

        for(int i = 0; i < td2.numFields(); i++){
            td3_types[td1.numFields()+i] = td2.getFieldType(i);
            td3_names[td1.numFields()+i] = td2.getFieldName(i);
        }

        TupleDesc td3 = new TupleDesc(td3_types, td3_names);

        return td3;
        //return null;
    }



    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     * 
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        // some code goes here

        // See TupleDescTest.java
        if(!(o instanceof TupleDesc)){
            return false;
        }

        TupleDesc td2 = (TupleDesc) o;

        // Check if both TDItems have the same number of items
        if(this.numFields()!=td2.numFields()) {
            return false;
        }

        // Check if both TDItems have the same types for every element 
        // i-th types are all equal
        for(int i=0; i < this.numFields(); i++){
            if(!this.getFieldType(i).equals(td2.getFieldType(i))){
                return false;
            }
        }

        return true;
    }



    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }



    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     * 
     * @return String describing this descriptor.
     */
    public String toString() {
        // some code goes here

        StringBuilder sb = new StringBuilder();
        for(int i=0; i < this.getSize(); i++){
            if(i==this.getSize()-1){
                sb.append(this.getFieldType(i).toString() + "(" + this.getFieldType(i) + ")");
            }
            else {
                sb.append(this.getFieldType(i).toString() + "(" + this.getFieldType(i) + "),");
            }
        }
        return sb.toString();
        //return "";
    }
}
