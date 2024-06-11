package HM;


import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.Scanner;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HotelManagementSystems {
    private static final String url = "jdbc:oracle:thin:@localhost:1521:xe"; // Oracle JDBC URL
    private static final String username = "system"; // Oracle username
    private static final String password = "2003"; // Oracle password

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection connection = DriverManager.getConnection(url, username, password);
            while (true) {
                System.out.println();
                System.out.println("HOTEL MANAGEMENT SYSTEM");
                Scanner scanner = new Scanner(System.in);
                System.out.println("1. Reserve a room");
                System.out.println("2. View Reservations");
                System.out.println("3. Get Room Number");
                System.out.println("4. Update Reservations");
                System.out.println("5. Delete Reservations");
                System.out.println("0. Exit");
                System.out.print("Choose an option: ");
                int choice = scanner.nextInt();
                switch (choice) {
                    case 1:
                        reserveRoom(connection, scanner);
                        break;
                    case 2:
                        viewReservations(connection);
                        break;
                    case 3:
                        getRoomNumber(connection, scanner);
                        break;
                    case 4:
                        updateReservation(connection, scanner);
                        break;
                    case 5:
                        deleteReservation(connection, scanner);
                        break;
                    case 0:
                        exit();
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
        }
    }

    private static void reserveRoom(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter guest name: ");
            String guestName = scanner.next();
            scanner.nextLine();
            System.out.print("Enter room number: ");
            int roomNumber = scanner.nextInt();
            System.out.print("Enter contact number: ");
            String contactNumber = scanner.next();

            // Check if the room number is already occupied
            if (isRoomOccupied(connection, roomNumber)) {
                System.out.println("Room is occupied. Please enter another room number.");
                return;
            }

            // Proceed with reservation if the room is available
            String sql = "INSERT INTO reservations (reservation_id, guest_name, room_number, contact_number) " +
                    "VALUES (reservation_seq.NEXTVAL, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, guestName);
                statement.setInt(2, roomNumber);
                statement.setString(3, contactNumber);

                int affectedRows = statement.executeUpdate();

                if (affectedRows > 0) {
                    System.out.println("Reservation successful!");
                } else {
                    System.out.println("Reservation failed.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
        }
    }

    private static boolean isRoomOccupied(Connection connection, int roomNumber) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM reservations WHERE room_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt("count");
                    return count > 0;
                }
            }
        }
        return false;
    }

    private static void viewReservations(Connection connection) {
        try {
            String sql = "SELECT reservation_id, guest_name, room_number, contact_number, reservation_date FROM reservations";

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                System.out.println("Current Reservations:");
                System.out.println("+----------------+-----------------+---------------+----------------------+-------------------------+");
                System.out.println("| Reservation ID | Guest           | Room Number   | Contact Number      | Reservation Date        |");
                System.out.println("+----------------+-----------------+---------------+----------------------+-------------------------+");

                while (resultSet.next()) {
                    int reservationId = resultSet.getInt("reservation_id");
                    String guestName = resultSet.getString("guest_name");
                    int roomNumber = resultSet.getInt("room_number");
                    String contactNumber = resultSet.getString("contact_number");
                    String reservationDate = resultSet.getTimestamp("reservation_date").toString();

                    // Format and display the reservation data in a table-like format
                    System.out.printf("| %-14d | %-15s | %-13d | %-20s | %-19s   |\n",
                            reservationId, guestName, roomNumber, contactNumber, reservationDate);
                }

                System.out.println("+----------------+-----------------+---------------+----------------------+-------------------------+");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
        }
    }

    private static void getRoomNumber(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter reservation ID: ");
            int reservationId = scanner.nextInt();
            System.out.print("Enter guest name: ");
            String guestName = scanner.next();

            String sql = "SELECT room_number FROM reservations " +
                    "WHERE reservation_id = ? AND guest_name = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, reservationId);
                statement.setString(2, guestName);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int roomNumber = resultSet.getInt("room_number");
                        System.out.println("Room number for Reservation ID " + reservationId +
                                " and Guest " + guestName + " is: " + roomNumber);
                    } else {
                        System.out.println("Reservation not found for the given ID and guest name.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
        }
    }

    private static void updateReservation(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter reservation ID to update: ");
            int reservationId = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character

            if (!reservationExists(connection, reservationId)) {
                System.out.println("Reservation not found for the given ID.");
                return;
            }

            System.out.print("Enter new guest name: ");
            String newGuestName = scanner.nextLine();
            System.out.print("Enter new room number: ");
            int newRoomNumber = scanner.nextInt();
            System.out.print("Enter new contact number: ");
            String newContactNumber = scanner.next();

            // Check if the new room number is already occupied
            if (isRoomOccupied(connection, newRoomNumber)) {
                System.out.println("Room is occupied. Please enter another room number.");
                return;
            }

            String sql = "UPDATE reservations SET guest_name = ?, " +
                    "room_number = ?, " +
                    "contact_number = ? " +
                    "WHERE reservation_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newGuestName);
                statement.setInt(2, newRoomNumber);
                statement.setString(3, newContactNumber);
                statement.setInt(4, reservationId);

                int affectedRows = statement.executeUpdate();

                if (affectedRows > 0) {
                    System.out.println("Reservation updated successfully!");
                } else {
                    System.out.println("Reservation update failed.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
        }
    }

    private static void deleteReservation(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter reservation ID to delete: ");
            int reservationId = scanner.nextInt();

            // Check if the reservation exists
            if (!reservationExists(connection, reservationId)) {
                System.out.println("Reservation not found for the given ID.");
                return;
            }

            String sql = "DELETE FROM reservations WHERE reservation_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, reservationId);

                int affectedRows = statement.executeUpdate();

                if (affectedRows > 0) {
                    System.out.println("Reservation deleted successfully!");
                } else {
                    System.out.println("Reservation deletion failed.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
        }
    }

    private static boolean reservationExists(Connection connection, int reservationId) {
        try {
            String sql = "SELECT reservation_id FROM reservations WHERE reservation_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, reservationId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next(); // If there's a result, the reservation exists
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
            return false; // Handle database errors as needed
        }
    }

    private static void exit() {
        try {
            System.out.print("Exiting System");
            int i = 5;
            while (i != 0) {
                System.out.print(".");
                Thread.sleep(1000);
                i--;
            }
            System.out.println();
            System.out.println("Thank you for using Hotel Reservation System!!!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
