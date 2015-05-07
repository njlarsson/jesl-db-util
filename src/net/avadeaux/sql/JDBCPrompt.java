package net.avadeaux.sql;

import java.sql.*;
import java.io.*;
import java.util.*;

// A simple general prompt-based JDBC client.
//
// Jesper Larsson, jesl@avadeaux.net

/** A simple JDBC client that works with MySQL. */
public class JDBCPrompt implements Runnable {
    final private Connection conn;
    final private BufferedReader in;
    final private PrintWriter out;
    final private PrintWriter err;

    public JDBCPrompt(Connection conn, BufferedReader in, PrintWriter out, PrintWriter err) {
        this.conn = conn;
        this.in = in;
        this.out = out;
        this.err = err;
    }
    
    private String readLine(String format, Object... args) throws IOException {
        out.format(format, args).flush();
        return in.readLine();
    }

    private String prompt(String what, String dflt) throws IOException {
        if (dflt == null) {
            return readLine("%s: ", what).trim();
        } else {
            return readLine("%s [%s]: ", what, dflt).trim();
        }
    }

    public void run() {
        try {
            Statement stmt;
            try {
                stmt = conn.createStatement();
            } catch (SQLException ex) {
                err.format("Terrible! Problem with connection: %s\n", ex).flush();
                throw new IOError(ex);
            }
            while (true) {
                try {
                    if (!promptEval(stmt, "sql> ")) { break; }
                } catch (SQLException ex) {
                    String className = ex.getClass().getName();
                    err.format("%s: %s\n", className.substring(className.lastIndexOf('.')+1), ex.getMessage()).flush();
                }
            }
        } catch (IOException ex) {
            err.format("Terrible! I/O problem: %s\n", ex).flush();
            throw new IOError(ex);
        }
    }
    
    /** Prompts for an sql snippet, executes it, and writes the result
      * to out. */
    private boolean promptEval(Statement stmt, String prompt) throws SQLException, IOException {
        String s = readLine("%s", prompt);
        if (s == null) { return false; }
        s = s.trim();
        if (s.length() == 0) { return true; }
        long startTime = System.currentTimeMillis();
        boolean itsARs = stmt.execute(s);
        if (itsARs) {
            ResultSet rs = stmt.getResultSet();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            
            int[] widths = new int[cols];
            StringBuilder headB = new StringBuilder(), sepB = new StringBuilder(), dataB = new StringBuilder();
            String[] leftJust = new String[cols];
            for (int i = 0; i < cols; i++) {
                widths[i] = md.getColumnName(i+1).length();
                int colType = md.getColumnType(i+1);
                leftJust[i] = colType == Types.CHAR || colType == Types.VARCHAR || colType == Types.LONGVARCHAR ? "-" : ""; 
            }
            List<Object[]> batch = getBatch(rs, cols);
            for (Object[] row : batch) {
                for (int i = 0; i < cols; i++) {
                    widths[i] = Math.max(widths[i], row[i] == null ? 4 : row[i].toString().length() + 2);
                }
            }
            for (int i = 0; i < cols; i++) {
                headB.append("| %-").append(Integer.toString(widths[i])).append("s ");
                dataB.append("| %").append(leftJust[i]).append(Integer.toString(widths[i])).append("s ");
                sepB.append("+");
                for (int j = 0; j < widths[i]+2; j++) { sepB.append("-"); }
            }
            String headFmt = headB.append("|\n").toString();
            String sepFmt = sepB.append("+\n").toString();
            String dataFmt = dataB.append("|\n").toString();

            out.format(sepFmt);
            Object[] colNames = new String[cols];
            for (int i = 0; i < cols; i++) { colNames[i] = md.getColumnName(i+1); }
            out.format(headFmt, colNames);
            out.format(sepFmt);
            
            int ct = 0;
            while (batch.size() > 0) {
                for (Object[] row : batch) { out.format(dataFmt, row); }
                ct += batch.size();
                batch = getBatch(rs, cols);
            }
            out.format(sepFmt);
            out.format("%d row%s", ct, ct == 1 ? "" : "s");
        } else {
            int ct = stmt.getUpdateCount();
            if (ct >= 0) out.format("%d row%s affected", ct, ct == 1 ? "" : "s");
            else out.format("ok");
        }
        long time = System.currentTimeMillis() - startTime;
        out.format(" (%.2f seconds)\n", time / 1000.0);
        out.flush();
        return true;
    }

    private static List<Object[]> getBatch(ResultSet rs, int cols) throws SQLException {
        ArrayList<Object[]> batch = new ArrayList<Object[]>();
        for (int j = 0; j < 100 && rs.next(); j++) {
            Object[] row = new String[cols];
            for (int i = 0; i < cols; i++) { row[i] = rs.getString(i+1); }
            batch.add(row);
        }
        return batch;
    }
}
