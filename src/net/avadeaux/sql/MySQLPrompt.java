package net.avadeaux.sql;

import java.sql.*;
import java.io.*;
import java.util.*;

// By Jesper Larsson, jesl@avadeaux.net

/** A simple JDBC client that works with MySQL. */
public class MySQLPrompt {
    /** Prompts the user for something, with an optional default. */
    static String prompt(String what, String dflt) {
        if (dflt == null) {
            return System.console().readLine("%s: ", what).trim();
        } else {
            String s = System.console().readLine("%s [%s]: ", what, dflt).trim();
            return s.length() > 0 ? s : dflt;
        }
    }

    /** Runs the client. Arguments are host, port, user, database, and
      * password, in that order. If less than four arguments are
      * given, or if any of the arguments is "-" the user is prompted
      * for what is missing. If an argument is "+" it is taken to be a
      * default (if available). */
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (ClassNotFoundException ex) {
            System.err.println("Can't load JDBC driver, " + ex);
            System.exit(1);
        } catch (InstantiationException ex) {
            System.err.println("Can't load JDBC driver, " + ex);
            System.exit(1);
        } catch (IllegalAccessException ex) {
            System.err.println("Can't load JDBC driver, " + ex);
            System.exit(1);
        }

        String host = args.length <= 0 || "-".equals(args[0]) ? prompt("Host", null) : args[0];
        String port = args.length <= 1 || "-".equals(args[1]) ?
            prompt("Port", "3306") :
            "+".equals(args[1]) ? "3306" : args[1];
        String user = args.length <= 2 || "-".equals(args[2]) ? prompt("User", null) : args[2];
        String database = args.length <= 3 || "-".equals(args[3]) ?
            prompt("Database", user) :
            "+".equals(args[3]) ? user : args[3];
        String password = args.length > 4 ? args[4] : new String(System.console().readPassword("Password: "));
        
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + user + "&password=" + password);
        } catch (SQLException ex) {
            System.err.println("Terrible! Problem with connection: " + ex);
            System.exit(1);
        }

        JDBCPrompt prmpt = new JDBCPrompt(conn, new BufferedReader(new InputStreamReader(System.in)), new PrintWriter(System.out), new PrintWriter(System.err));
        try {
            prmpt.run();
        } catch (IOError err) {
            System.exit(1);
        }
    }
}
