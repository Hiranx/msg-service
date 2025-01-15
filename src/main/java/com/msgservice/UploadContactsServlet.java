package com.msgservice;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/uploadContacts")
@MultipartConfig // Required for handling file uploads
public class UploadContactsServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/msgservice?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "hiran9204";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");

        // Retrieve the uploaded file
        Part filePart = request.getPart("file");
        String fileName = filePart.getSubmittedFileName();

        // Validate the file type
        if (!fileName.endsWith(".csv")) {
            response.getWriter().write("Error: Please upload a valid CSV file.");
            return;
        }

        // Process the file
        InputStream fileContent = filePart.getInputStream();
        List<Contact> contacts = processCSV(fileContent);

        // Save to database
        if (contacts.isEmpty()) {
            response.getWriter().write("No valid contacts found in the uploaded file.");
        } else {
            saveContactsToDatabase(contacts);
            response.getWriter().write("Contacts uploaded successfully! Total valid contacts: " + contacts.size());
        }
    }

    private List<Contact> processCSV(InputStream fileContent) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileContent));
        List<Contact> contacts = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            String[] data = line.split(",");
            if (data.length >= 2) { // Expecting at least Name and Phone fields
                String name = data[0].trim();
                String phone = data[1].trim();

                if (isValidPhoneNumber(phone)) {
                    contacts.add(new Contact(name, phone));
                }
            }
        }
        return contacts;
    }

    private boolean isValidPhoneNumber(String phone) {
        // Validate phone number with a regex pattern
        return phone.matches("\\+?[0-9]{10,15}");
    }

    private void saveContactsToDatabase(List<Contact> contacts) {
        String sql = "INSERT INTO contacts (name, phone) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Contact contact : contacts) {
                pstmt.setString(1, contact.getName());
                pstmt.setString(2, contact.getPhone());
                pstmt.addBatch(); // Batch insert for efficiency
            }

            pstmt.executeBatch(); // Execute the batch
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DatabaseConnection class for handling DB connection
    public static class DatabaseConnection {
        public static Connection getConnection() throws SQLException {
            try {
                // Explicitly load the MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new SQLException("MySQL JDBC Driver not found.");
            }

            // Return the database connection
            String url = "jdbc:mysql://localhost:3306/msgservice?useSSL=false&serverTimezone=UTC";
            String username = "root";  // Replace with your database username
            String password = "hiran9204";  // Replace with your database password

            return DriverManager.getConnection(url, username, password);
        }
    }

    // Contact class for encapsulating contact data
    public static class Contact {
        private final String name;
        private final String phone;

        public Contact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }
    }
}
