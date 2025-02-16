package simpledb.common;

import simpledb.common.Type;
import simpledb.storage.DbFile;
import simpledb.storage.HeapFile;
import simpledb.storage.TupleDesc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Catalog keeps track of all available tables in the database and their
 * associated schemas.
 * For now, this is a stub catalog that must be populated with tables by a
 * user program before it can be used -- eventually, this should be converted
 * to a catalog that reads a catalog table from disk.
 * 
 * The Catalog (singleton) object manages adding new tables and viewing schemas and primary keys
 * 
 * Catalog class consists of a list of the tables as well as the schemas of the tables that are in 
 * the database
 * 
 * Need to write code to add a new table and get information about a particular table
 * 
 * Each table has an associated TupleDesc object that allows operators to determine the 
 * types and number of fields in a table (schema)
 * 
 * The global catalog is a single instance of Catalog that is allocated for the entire SimpleDB process
 * - Can be retrieved via Database.getCatalog()
 * 
 * @Threadsafe
 */
public class Catalog {


    // Mapping tables
    private ConcurrentHashMap<String, DbFile> tableNameToDbFile; 
    private ConcurrentHashMap<Integer, DbFile> tableIdToDbFile; 
    private ConcurrentHashMap<Integer, String> tableIdToPriKey; 

    /**
     * Constructor.
     * Creates a new, empty catalog.
     */
    public Catalog() {
        // some code goes here

        /*
        Python dictionary equivalent is ConcurrentHashMap
        https://www.geeksforgeeks.org/concurrenthashmap-in-java/
        */

       // Mapping tables
       tableNameToDbFile = new ConcurrentHashMap<String, DbFile>(); 
       tableIdToDbFile = new ConcurrentHashMap<Integer, DbFile>(); 
       tableIdToPriKey = new ConcurrentHashMap<Integer, String>(); 
    }

    /**
     * Add a new table to the catalog.
     * This table's contents are stored in the specified DbFile.
     * If there exists a table with the same name or ID, replace that old table with this one. 
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile. 
     * @param name the name of the table -- may be an empty string.  May not be null.  
     * @param pkeyField the name of the primary key field
     */
    public void addTable(DbFile file, String name, String pkeyField) {
        // some code goes here

        /**
         * If there exists a table with the same name or ID, replace that old table with this one. 
         * file: (DbFile) contents of table to add. file.getId() -> Identifier of the table file
         * name: Name of the table. Can be empty string but cannot be null
         * pkeyField: Name of primary key field
         * 
         * Handle case where 
         * - a duplicate id exists in tableIdtoDbFile (CatalogTest.java)
         *  - Need to remove the entry in tableIdtoDbFile
         *  - Need to remove the entry in tableIdtoPriKey
         *  - Need to remove the old name entry reference in tableNametoDbFile if not it will fail the getTableName() -> Duplicate file checks
         *  - Need to add the new id and name entry references
         * 
         * - a duplicate id exists in tableIdtoPriKey (potentially?)
         *  - Need to remove the entry in tableIdtoDbFile
         *  - Need to remove the entry in tableIdtoPriKey
         *  - Need to remove the old name entry reference in tableNametoDbFile
         *  - Need to add the new id and name entry references
         * 
         * - a duplicate name exists in tableNametoDbFile (potentially?)
         *  - Need to remove the entry in tableNametoDbFile
         *  - Need to remove the entry in tableIdtoPriKey
         *  - Need to remove the old id entry reference in tableIdtoDbFile 
         *  - Need to add the new id and name entry references
         */

        // name cannot be null
        if(name==null){
            throw new IllegalArgumentException("Name cannot be null");
        }

        // no need to check for primary key duplicates because it can be "" (see method below)

        Integer tableId = file.getId();

        // Handle Duplicate cases
        if(this.tableIdToDbFile.containsKey(tableId) || this.tableNameToDbFile.containsKey(name) || this.tableIdToPriKey.containsKey(tableId)) {
            
            // Try removing the old id entry reference in tableIdtoDbFile
            if(this.tableIdToDbFile.containsKey(tableId)) {
                this.tableIdToDbFile.remove(tableId);
            }


            // Try removing the old id entry reference in tableIdtoPriKey
            if(this.tableIdToPriKey.containsKey(tableId)) {
                this.tableIdToPriKey.remove(tableId);
            }


            // Try removing the old name entry reference in tableNametoDbFile
            for (Map.Entry<String, DbFile> entry: this.tableNameToDbFile.entrySet()) {
                String key = entry.getKey();
                DbFile value = entry.getValue();
                if(value.getId()==tableId) {
                    this.tableNameToDbFile.remove(key);
                    break;
                }
            }
        }


        this.tableNameToDbFile.put(name, file);
        this.tableIdToDbFile.put(tableId, file);
        this.tableIdToPriKey.put(tableId, pkeyField);
    }

    public void addTable(DbFile file, String name) {
        addTable(file, name, "");
    }

    /**
     * Add a new table to the catalog.
     * This table has tuples formatted using the specified TupleDesc and its
     * contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     */
    public void addTable(DbFile file) {
        addTable(file, (UUID.randomUUID()).toString());
    }

    /**
     * Return the id of the table with a specified name,
     * @throws NoSuchElementException if the table doesn't exist
     */
    public int getTableId(String name) throws NoSuchElementException {
        // some code goes here

        // See CatalogTest.java nameThisTestRun var
        if(name==null) {
            throw new NoSuchElementException("Name cannot be null");
        }

        if(this.tableNameToDbFile.containsKey(name)) {
            return this.tableNameToDbFile.get(name).getId();
        } else{
            throw new NoSuchElementException("The table with the specified name does not exist");
        }
        // return 0;
    }

    /**
     * Returns the tuple descriptor (schema) of the specified table
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     * @throws NoSuchElementException if the table doesn't exist
     */
    public TupleDesc getTupleDesc(int tableid) throws NoSuchElementException {
        // some code goes here

        if(this.tableIdToDbFile.containsKey(tableid)) {
            // See simmpledb/storage/DbFile.java for DbFile interface
            return this.tableIdToDbFile.get(tableid).getTupleDesc();
        } else {
            throw new NoSuchElementException("The table with the specified id does not exist");
        }
        //return null;
    }

    /**
     * Returns the DbFile that can be used to read the contents of the
     * specified table.
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     */
    public DbFile getDatabaseFile(int tableid) throws NoSuchElementException {
        // some code goes here
        // todo: check if need to throw exception

        if(this.tableIdToDbFile.containsKey(tableid)) {
            return this.tableIdToDbFile.get(tableid);
        } else {
            throw new NoSuchElementException("The table with the specified id does not exist");
        }
        //return null;
    }

    public String getPrimaryKey(int tableid) {
        // some code goes here
        // todo: check if error is needed

        if(this.tableIdToPriKey.containsKey(tableid)) {
            return this.tableIdToPriKey.get(tableid);
        } else {
            throw new NoSuchElementException("The table with the specified id does not exist");
        }
        //return null;
    }

    public Iterator<Integer> tableIdIterator() {
        // some code goes here

        // https://www.geeksforgeeks.org/concurrenthashmap-in-java/
        return this.tableIdToDbFile.keySet().iterator();
        // return null;
    }

    public String getTableName(int id) {
        // some code goes here

        // todo: check if error is needed
        // https://stackoverflow.com/questions/1066589/iterate-through-a-hashmap
        if(this.tableIdToDbFile.containsKey(id)) {
            for (Map.Entry<String, DbFile> entry: tableNameToDbFile.entrySet()) {
                String key = entry.getKey();
                DbFile value = entry.getValue();
                if(value.getId()==id) {
                    return key;
                }
            }
        } else {
            throw new NoSuchElementException("The specified id does not exist");
        }

        //todo: why is this required
        return null;
    }
    
    /** Delete all tables from the catalog */
    public void clear() {
        // some code goes here

        // https://www.geeksforgeeks.org/java-concurrenthashmap-clear/
        tableNameToDbFile.clear();
        tableIdToDbFile.clear();
        tableIdToPriKey.clear();
    }
    
    /**
     * Reads the schema from a file and creates the appropriate tables in the database.
     * @param catalogFile
     */
    public void loadSchema(String catalogFile) {
        String line = "";
        String baseFolder=new File(new File(catalogFile).getAbsolutePath()).getParent();
        try {
            BufferedReader br = new BufferedReader(new FileReader(catalogFile));
            
            while ((line = br.readLine()) != null) {
                //assume line is of the format name (field type, field type, ...)
                String name = line.substring(0, line.indexOf("(")).trim();
                //System.out.println("TABLE NAME: " + name);
                String fields = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                String[] els = fields.split(",");
                ArrayList<String> names = new ArrayList<>();
                ArrayList<Type> types = new ArrayList<>();
                String primaryKey = "";
                for (String e : els) {
                    String[] els2 = e.trim().split(" ");
                    names.add(els2[0].trim());
                    if (els2[1].trim().equalsIgnoreCase("int"))
                        types.add(Type.INT_TYPE);
                    else if (els2[1].trim().equalsIgnoreCase("string"))
                        types.add(Type.STRING_TYPE);
                    else {
                        System.out.println("Unknown type " + els2[1]);
                        System.exit(0);
                    }
                    if (els2.length == 3) {
                        if (els2[2].trim().equals("pk"))
                            primaryKey = els2[0].trim();
                        else {
                            System.out.println("Unknown annotation " + els2[2]);
                            System.exit(0);
                        }
                    }
                }
                Type[] typeAr = types.toArray(new Type[0]);
                String[] namesAr = names.toArray(new String[0]);
                TupleDesc t = new TupleDesc(typeAr, namesAr);
                HeapFile tabHf = new HeapFile(new File(baseFolder+"/"+name + ".dat"), t);
                addTable(tabHf,name,primaryKey);
                System.out.println("Added table : " + name + " with schema " + t);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println ("Invalid catalog entry : " + line);
            System.exit(0);
        }
    }
}

