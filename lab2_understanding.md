## **Final Test**

- Create ```some_data_file1.txt``` Remember to enter a new line after the last line.

```
1,1,1
2,2,2
3,4,4
4,10,10
5,20,20
```

- Create ```some_data_file2.txt``` Remember to enter a new line after the last line.

```
6,2,2
7,4,4
8,10,10
9,20,20
10,30,30
```

- Run ```ant```
- Run ```java -jar dist/simpledb.jar convert some_data_file1.txt 3```
- Run ```java -jar dist/simpledb.jar convert some_data_file2.txt 3```
- Create ```jointest.java``` under ```src/java/simpledb/``` and copy the code from GitHub
    - Add the follow imports:
        ```
        import simpledb.common.*;
        import simpledb.storage.*;
        import simpledb.execution.*;
        import simpledb.transaction.*;
        ```

- Run ```ant```
- Run ```java -classpath dist/simpledb.jar simpledb.jointest```
- Expected output

```
2,2,2,6,2,2
3,4,4,7,4,4
4,10,10,8,10,10
5,20,20,9,20,20
```