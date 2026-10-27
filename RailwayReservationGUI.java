import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Swing version of the simple railway reservation system.
 * Database work runs on a thread pool; Swing components are updated on the EDT.
 */
public class RailwayReservationGUI extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/railway_simple";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";

    private final ExecutorService databasePool = Executors.newFixedThreadPool(4);
    private final DefaultTableModel trainModel = new DefaultTableModel(
            new String[]{"ID", "Number", "Train", "From", "To", "Departure", "Arrival", "Fare", "Available"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable trainTable = new JTable(trainModel);
    private final JTextField sourceSearch = new JTextField(10);
    private final JTextField destinationSearch = new JTextField(10);
    private final JTextField passengerName = new JTextField(18);
    private final JTextField passengerAge = new JTextField(5);
    private final JTextField journeyDate = new JTextField(LocalDate.now().plusDays(1).toString(), 12);
    private final JComboBox<String> gender = new JComboBox<>(new String[]{"MALE", "FEMALE", "OTHER"});
    private final JTextField pnrSearch = new JTextField(14);
    private final JTextArea resultArea = new JTextArea();
    private final JTextArea statusArea = new JTextArea();
    private int selectedTrainId = -1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                new RailwayReservationGUI().setVisible(true);
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                        "MySQL Connector/J was not found in the classpath.", "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public RailwayReservationGUI() {
        super("Railway Reservation System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createTabs(), BorderLayout.CENTER);
        setContentPane(root);

        trainTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        trainTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && trainTable.getSelectedRow() >= 0) {
                selectTrainFromTable();
            }
        });
        trainTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() >= 1) selectTrainFromTable();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                databasePool.shutdownNow();
                dispose();
                System.exit(0);
            }
        });

        loadTrains("", "");
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Railway Reservation System");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(new Color(20, 70, 120));
        JLabel subtitle = new JLabel("Java Swing  |  JDBC  |  MySQL  |  Background threads");
        subtitle.setForeground(Color.DARK_GRAY);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Search and book", createBookingTab());
        tabs.addTab("PNR status", createStatusTab());
        tabs.addTab("Cancel ticket", createCancelTab());
        return tabs;
    }

    private JPanel createBookingTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        search.setBorder(BorderFactory.createTitledBorder("Search trains"));
        JButton searchButton = new JButton("Search");
        search.add(new JLabel("From:"));
        search.add(sourceSearch);
        search.add(new JLabel("To:"));
        search.add(destinationSearch);
        search.add(searchButton);
        searchButton.addActionListener(e -> loadTrains(sourceSearch.getText().trim(), destinationSearch.getText().trim()));
        panel.add(search, BorderLayout.NORTH);

        trainTable.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(trainTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 10));
        bottom.add(createPassengerForm());
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setRows(6);
        resultArea.setBackground(Color.WHITE);
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Booking details"));
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        bottom.add(resultPanel);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createPassengerForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Passenger details"));
        addFormRow(form, 0, "Name", passengerName);
        addFormRow(form, 1, "Age", passengerAge);
        addFormRow(form, 2, "Gender", gender);
        addFormRow(form, 3, "Journey date", journeyDate);
        JButton bookButton = new JButton("Book ticket");
        GridBagConstraints buttonConstraints = constraints(0, 4);
        buttonConstraints.gridwidth = 2;
        form.add(bookButton, buttonConstraints);
        bookButton.addActionListener(e -> bookTicket());
        return form;
    }

    private JPanel createStatusTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton findButton = new JButton("Find booking");
        top.add(new JLabel("PNR:"));
        top.add(pnrSearch);
        top.add(findButton);
        panel.add(top, BorderLayout.NORTH);
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        panel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        findButton.addActionListener(e -> findBooking());
        pnrSearch.addActionListener(e -> findBooking());
        return panel;
    }

    private JPanel createCancelTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel box = new JPanel(new GridBagLayout());
        box.setBorder(BorderFactory.createTitledBorder("Cancel confirmed ticket"));
        JTextField cancelPnr = new JTextField(16);
        JButton cancelButton = new JButton("Cancel ticket");
        addFormRow(box, 0, "PNR", cancelPnr);
        GridBagConstraints buttonConstraints = constraints(0, 1);
        buttonConstraints.gridwidth = 2;
        box.add(cancelButton, buttonConstraints);
        panel.add(box);
        cancelButton.addActionListener(e -> cancelTicket(cancelPnr.getText().trim()));
        return panel;
    }

    private void addFormRow(JPanel panel, int row, String label, java.awt.Component component) {
        GridBagConstraints labelConstraints = constraints(0, row);
        labelConstraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label + ":"), labelConstraints);
        GridBagConstraints fieldConstraints = constraints(1, row);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        panel.add(component, fieldConstraints);
    }

    private GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(5, 5, 5, 5);
        return constraints;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void loadTrains(String source, String destination) {
        loadTrains(source, destination, true);
    }

    private void loadTrains(String source, String destination, boolean updateResultMessage) {
        runInBackground(() -> queryTrains(source, destination), trains -> {
            trainModel.setRowCount(0);
            for (TrainRow train : trains) {
                trainModel.addRow(new Object[]{train.id, train.number, train.name, train.source, train.destination,
                        train.departure, train.arrival, train.fare, train.availableSeats + "/" + train.totalSeats});
            }
            if (updateResultMessage) {
                resultArea.setText(trains.size() + " train(s) found. Select a row to book.");
            }
        });
    }

    private List<TrainRow> queryTrains(String source, String destination) throws SQLException {
        String sql = "SELECT * FROM trains WHERE source_station LIKE ? AND destination_station LIKE ? ORDER BY train_name";
        List<TrainRow> trains = new ArrayList<>();
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + source + "%");
            statement.setString(2, "%" + destination + "%");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    trains.add(new TrainRow(result.getInt("train_id"), result.getString("train_number"),
                            result.getString("train_name"), result.getString("source_station"),
                            result.getString("destination_station"), result.getTime("departure_time"),
                            result.getTime("arrival_time"), result.getBigDecimal("fare"),
                            result.getInt("total_seats"), result.getInt("available_seats")));
                }
            }
        }
        return trains;
    }

    private void bookTicket() {
        if (selectedTrainId < 0 && trainTable.getSelectedRow() >= 0) {
            selectTrainFromTable();
        }
        if (selectedTrainId < 0) {
            showError("Click a train row first, then click Book ticket.");
            return;
        }
        try {
            String name = required(passengerName.getText(), "Passenger name");
            int age = Integer.parseInt(required(passengerAge.getText(), "Age"));
            if (age < 1 || age > 120) throw new IllegalArgumentException("Age must be between 1 and 120.");
            LocalDate date = LocalDate.parse(required(journeyDate.getText(), "Journey date"));
            if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Journey date cannot be in the past.");
            String selectedGender = String.valueOf(gender.getSelectedItem());

            resultArea.setText("Booking in progress... Please wait.");
            runInBackground(() -> createBooking(selectedTrainId, name, age, selectedGender, date), booking -> {
                resultArea.setText("BOOKING SUCCESSFUL\n\nPNR: " + booking.pnr + "\nSeat: " + booking.seatNumber
                        + "\nFare: Rs. " + booking.fare + "\n\nSave this PNR for status and cancellation.");
                loadTrains(sourceSearch.getText().trim(), destinationSearch.getText().trim(), false);
            });
        } catch (NumberFormatException e) {
            showError("Age must be a whole number.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void selectTrainFromTable() {
        int row = trainTable.getSelectedRow();
        if (row < 0) {
            showError("Click a train row first, then click Book ticket.");
            return;
        }
        int modelRow = trainTable.convertRowIndexToModel(row);
        selectedTrainId = (int) trainModel.getValueAt(modelRow, 0);
        resultArea.setText("Selected train ID: " + selectedTrainId
                + "\nNow enter passenger details and click Book ticket.");
    }

    private BookingRow createBooking(int trainId, String name, int age, String gender, LocalDate date) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                int totalSeats;
                int availableSeats;
                BigDecimal fare;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT total_seats, available_seats, fare FROM trains WHERE train_id=? FOR UPDATE")) {
                    statement.setInt(1, trainId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Train was not found.");
                        totalSeats = result.getInt("total_seats");
                        availableSeats = result.getInt("available_seats");
                        fare = result.getBigDecimal("fare");
                    }
                }
                if (availableSeats <= 0) throw new IllegalArgumentException("No seats are available.");
                int seat = findFreeSeat(connection, trainId, date, totalSeats);
                String pnr = createPnr(connection);

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE trains SET available_seats=available_seats-1 WHERE train_id=?")) {
                    update.setInt(1, trainId);
                    update.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO bookings(pnr,train_id,journey_date,passenger_name,age,gender,seat_number,total_fare) VALUES(?,?,?,?,?,?,?,?)")) {
                    insert.setString(1, pnr);
                    insert.setInt(2, trainId);
                    insert.setDate(3, Date.valueOf(date));
                    insert.setString(4, name);
                    insert.setInt(5, age);
                    insert.setString(6, gender);
                    insert.setInt(7, seat);
                    insert.setBigDecimal(8, fare);
                    insert.executeUpdate();
                }
                connection.commit();
                return new BookingRow(pnr, seat, fare);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void findBooking() {
        String pnr = pnrSearch.getText().trim().toUpperCase();
        if (pnr.isEmpty()) {
            showError("Enter a PNR.");
            return;
        }
        runInBackground(() -> queryBooking(pnr), booking -> statusArea.setText(booking));
    }

    private String queryBooking(String pnr) throws SQLException {
        String sql = "SELECT b.*, t.train_number, t.train_name, t.source_station, t.destination_station "
                + "FROM bookings b JOIN trains t ON b.train_id=t.train_id WHERE b.pnr=?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pnr);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return "Booking not found.";
                return "PNR: " + result.getString("pnr") + "\nStatus: " + result.getString("status")
                        + "\nTrain: " + result.getString("train_number") + " - " + result.getString("train_name")
                        + "\nRoute: " + result.getString("source_station") + " -> " + result.getString("destination_station")
                        + "\nJourney date: " + result.getDate("journey_date")
                        + "\nPassenger: " + result.getString("passenger_name")
                        + "\nSeat: " + result.getInt("seat_number")
                        + "\nFare: Rs. " + result.getBigDecimal("total_fare");
            }
        }
    }

    private void cancelTicket(String pnr) {
        if (pnr.isEmpty()) {
            showError("Enter a PNR.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Cancel booking " + pnr + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        runInBackground(() -> cancelBooking(pnr.toUpperCase()), message -> {
            statusArea.setText(message);
            loadTrains("", "");
        });
    }

    private String cancelBooking(String pnr) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                int bookingId;
                int trainId;
                String status;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT booking_id,train_id,status FROM bookings WHERE pnr=? FOR UPDATE")) {
                    statement.setString(1, pnr);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("PNR not found.");
                        bookingId = result.getInt("booking_id");
                        trainId = result.getInt("train_id");
                        status = result.getString("status");
                    }
                }
                if (!"CONFIRMED".equals(status)) throw new IllegalArgumentException("Ticket is already cancelled.");
                try (PreparedStatement statement = connection.prepareStatement("UPDATE bookings SET status='CANCELLED' WHERE booking_id=?")) {
                    statement.setInt(1, bookingId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("UPDATE trains SET available_seats=available_seats+1 WHERE train_id=?")) {
                    statement.setInt(1, trainId);
                    statement.executeUpdate();
                }
                connection.commit();
                return "Ticket cancelled successfully. Demonstration refund: 80% of fare.";
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int findFreeSeat(Connection connection, int trainId, LocalDate date, int totalSeats) throws SQLException {
        boolean[] occupied = new boolean[totalSeats + 1];
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT seat_number FROM bookings WHERE train_id=? AND journey_date=? AND status='CONFIRMED'")) {
            statement.setInt(1, trainId);
            statement.setDate(2, Date.valueOf(date));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int seat = result.getInt(1);
                    if (seat > 0 && seat <= totalSeats) occupied[seat] = true;
                }
            }
        }
        for (int seat = 1; seat <= totalSeats; seat++) if (!occupied[seat]) return seat;
        throw new IllegalArgumentException("No seat could be allocated.");
    }

    private String createPnr(Connection connection) throws SQLException {
        while (true) {
            String pnr = "PNR" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM bookings WHERE pnr=?")) {
                statement.setString(1, pnr);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    if (result.getInt(1) == 0) return pnr;
                }
            }
        }
    }

    private <T> void runInBackground(BackgroundTask<T> task, java.util.function.Consumer<T> success) {
        databasePool.submit(() -> {
            try {
                T value = task.run();
                SwingUtilities.invokeLater(() -> success.accept(value));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> showError(e.getMessage()));
            }
        });
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Operation failed." : message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface BackgroundTask<T> {
        T run() throws Exception;
    }

    private record TrainRow(int id, String number, String name, String source, String destination,
                            java.sql.Time departure, java.sql.Time arrival, BigDecimal fare,
                            int totalSeats, int availableSeats) {}

    private record BookingRow(String pnr, int seatNumber, BigDecimal fare) {}
}
