package simpledb.transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Debug {
    public static void log(String message) {
        try (FileWriter fw = new FileWriter("debug_log.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            out.println(message);
        } catch (IOException e) {
            // You can log to stderr if needed
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
}
