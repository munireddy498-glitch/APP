import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class StudentManager extends JFrame {

    JTextField txtId, txtName, txtDept, txtEmail;
    JButton btnAdd, btnSearch, btnUpdate, btnDelete, btnView, btnClear;
    JTable table;
    DefaultTableModel model;
    Connection con;

    public StudentManager() {
        super("Student Record Management");
        connect();
        buildUI();
        loadAll();
    }

    void connect() {
        String url = "jdbc:mysql://localhost:3306/company";
        String user = "root";
        String password = "your_password";
        try {
            con = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Could not connect to database:\n" + e.getMessage(),
                "Connection error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void buildUI() {
        txtId = new JTextField();
        txtName = new JTextField();
        txtDept = new JTextField();
        txtEmail = new JTextField();

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.setBorder(BorderFactory.createTitledBorder("Student Details"));
        form.add(new JLabel("Student ID:"));   form.add(txtId);
        form.add(new JLabel("Name:"));         form.add(txtName);
        form.add(new JLabel("Department:"));   form.add(txtDept);
        form.add(new JLabel("Email:"));        form.add(txtEmail);

        btnAdd = new JButton("Add");
        btnSearch = new JButton("Search");
        btnUpdate = new JButton("Update");
        btnDelete = new JButton("Delete");
        btnView = new JButton("View All");
        btnClear = new JButton("Clear");

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(btnAdd);
        buttons.add(btnSearch);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnView);
        buttons.add(btnClear);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.SOUTH);

        model = new DefaultTableModel(
            new String[]{"ID", "Name", "Department", "Email"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);

        
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                txtId.setText(model.getValueAt(row, 0).toString());
                txtName.setText(model.getValueAt(row, 1).toString());
                txtDept.setText(model.getValueAt(row, 2).toString());
                txtEmail.setText(model.getValueAt(row, 3).toString());
            }
        });

        btnAdd.addActionListener(e -> addStudent());
        btnSearch.addActionListener(e -> searchStudent());
        btnUpdate.addActionListener(e -> updateStudent());
        btnDelete.addActionListener(e -> deleteStudent());
        btnView.addActionListener(e -> loadAll());
        btnClear.addActionListener(e -> clearFields());

        setLayout(new BorderLayout(10, 10));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        setSize(650, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

   
    void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    void clearFields() {
        txtId.setText("");
        txtName.setText("");
        txtDept.setText("");
        txtEmail.setText("");
        table.clearSelection();
    }

    int readId() {
        try {
            return Integer.parseInt(txtId.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric Student ID.");
            return -1;
        }
    }

    void fillTable(ResultSet rs) throws SQLException {
        model.setRowCount(0);
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("student_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("email")
            });
        }
    }

   
    void loadAll() {
        try {
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT * FROM students ORDER BY student_id");
            fillTable(rs);
        } catch (Exception e) {
            showError(e);
        }
    }

    void addStudent() {
        int id = readId();
        if (id == -1) return;
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty.");
            return;
        }
        try {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO students VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setString(2, txtName.getText().trim());
            ps.setString(3, txtDept.getText().trim());
            ps.setString(4, txtEmail.getText().trim());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Student added.");
            loadAll();
            clearFields();
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this, "A student with this ID already exists.");
        } catch (Exception e) {
            showError(e);
        }
    }

    void searchStudent() {
        try {
            PreparedStatement ps;
            if (!txtId.getText().trim().isEmpty()) {
                int id = readId();
                if (id == -1) return;
                ps = con.prepareStatement("SELECT * FROM students WHERE student_id = ?");
                ps.setInt(1, id);
            } else if (!txtName.getText().trim().isEmpty()) {
                ps = con.prepareStatement("SELECT * FROM students WHERE name LIKE ?");
                ps.setString(1, "%" + txtName.getText().trim() + "%");
            } else {
                JOptionPane.showMessageDialog(this, "Enter a Student ID or a Name to search.");
                return;
            }
            fillTable(ps.executeQuery());
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No matching students found.");
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    
    void updateStudent() {
        int id = readId();
        if (id == -1) return;
        try {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE students SET name = ?, department = ?, email = ? WHERE student_id = ?");
            ps.setString(1, txtName.getText().trim());
            ps.setString(2, txtDept.getText().trim());
            ps.setString(3, txtEmail.getText().trim());
            ps.setInt(4, id);
            if (ps.executeUpdate() == 0) {
                JOptionPane.showMessageDialog(this, "No student found with that ID.");
            } else {
                JOptionPane.showMessageDialog(this, "Student updated.");
                loadAll();
                clearFields();
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    void deleteStudent() {
        int id = readId();
        if (id == -1) return;
        int ok = JOptionPane.showConfirmDialog(this,
            "Delete student " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            PreparedStatement ps = con.prepareStatement(
                "DELETE FROM students WHERE student_id = ?");
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                JOptionPane.showMessageDialog(this, "No student found with that ID.");
            } else {
                JOptionPane.showMessageDialog(this, "Student deleted.");
                loadAll();
                clearFields();
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentManager().setVisible(true));
    }
}