import java.sql.*;
import java.util.Scanner;

public class myapp {

    private static final String URL = "jdbc:db2://winter2025-comp421.cs.mcgill.ca:50000/comp421";
    private static String user_id = null;
    private static String user_password = null;
    private static Connection con;
    private static Statement statement;

    // Main menu commands
    public static void printMenu() {
        System.out.println("Card store Menu");
        System.out.println("\t1. Display the users and the total amount they spent");
        System.out.println("\t2. Create new Post");
        System.out.println("\t3. Display reputable sellers and their average rating");
        System.out.println("\t4. Update Review");
        System.out.println("\t5. Create/Update User");
        System.out.println("\t6. Quit");
        System.out.println("Please enter your option:");
    }
    
    // Setup the connection to the database
    public static void setup() {
        // Register the driver
        try {
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception cnfe) {
            System.out.println("Class not found");
        }
        // Check user credentials
        if (user_id == null && (user_id = System.getenv("SOCSUSER")) == null) {
            System.err.println("Error!! do not have a password to connect to the database!");
            System.exit(1);
        }
        if (user_password == null && (user_password = System.getenv("SOCSPASSWD")) == null) {
            System.err.println("Error!! do not have a password to connect to the database!");
            System.exit(1);
        }

        // Get a connection
        try {
            con = DriverManager.getConnection(URL, user_id, user_password);
            statement = con.createStatement();
        } catch (SQLException e) {
            System.out.println("Database connection failed" + e.getMessage());
            System.exit(-1);
        }
    }

    // Close the connection to the database
    private static void closeConnection() {
        try {
            if (con != null) {
                statement.close(); // close statment
                con.close(); // close connection
            }
            System.out.println("Connection closed.");
        } catch (SQLException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws SQLException {
        // setup
        setup();
        while (true) {

            printMenu();

            // Get command
            Scanner nScanner = new Scanner(System.in);
            int command = nScanner.nextInt();
            nScanner.nextLine(); // remove extra line

            // Execute command
            switch (command) {
                case 1:
                    viewBuyersTotalOrdersPrice();
                    break;
                case 2:
                    createNewPost(nScanner);
                    break;
                case 3:
                    viewBestSellers();
                    break;
                case 4:
                    updateReview(nScanner);
                    break;
                case 5:
                    manageUsers(nScanner);
                    break;
                case 6:
                    System.out.println("👋 Exiting the application🫡...");
                    nScanner.close();
                    closeConnection();
                    return;
                default:
                    System.out.println("Invalid command number. Please enter a command between 1 and 5");
            }

        }

    }

    private static void viewBuyersTotalOrdersPrice() {
        int sqlCode;
        String sqlState;
        try {
            String querySQL = "SELECT B.email, SUM (C.quantity * C.price) AS totalPrice FROM Buyer B, Order O, Contains Contains, Post P, Card C WHERE B.email = O.email AND O.status <> 'canceled' AND Contains.postId = P.postId AND O.receipt = Contains.receipt AND C.postId = P.postId GROUP BY B.email ORDER BY B.email;";
            ResultSet rs = statement.executeQuery(querySQL);
            System.out.printf("%-50s %-10s%n", "Email", "Total Price");
            System.out.println("--------------------------------------------------------------");

            while (rs.next()) {
                String email = rs.getString(1);
                int totalPrice = rs.getInt(2);
                System.out.printf("%-50s %-10d%n", email, totalPrice);
            }

            System.out.println("\nExecution done\n");
        } catch (SQLException e) {
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Code: " + sqlCode + " sqlState: " + sqlState);
            System.out.println(e);
        }
    }

    // Create a new post for a seller
    private static void createNewPost(Scanner scanner) {
        int sqlCode;
        String sqlState;
        System.out.println("Enter the seller's email: ");
        String sellerEmail = scanner.nextLine();
        System.out.println("Enter the post status (open, closed, deleted, archived): ");
        String status = scanner.nextLine().toLowerCase();
        System.out.println("Enter creation date (YYYY-MM-DD): ");
        String creationDate = scanner.nextLine();


        //check if seller exists
        String checkSellerSql = "SELECT * FROM Seller WHERE email = '" + sellerEmail +"'";
        try{
            ResultSet rs = statement.executeQuery(checkSellerSql);
            if (!rs.next()) {
                System.out.println("Error: Seller does not exist. Cannot create post.");
                return;
            }

        } catch (SQLException e) {
            System.out.println("Error checking seller existence: " + e.getMessage());
            return;
        }

        // insert new post
        String sql = "INSERT INTO Post (creationDate, status, email) VALUES (?, ?, ?)";

        try (PreparedStatement pStatement = con.prepareStatement(sql)) {
            pStatement.setDate(1, Date.valueOf(creationDate));
            pStatement.setString(2, status);
            pStatement.setString(3, sellerEmail);
            pStatement.executeUpdate();
            System.out.println("New post created successfully!");
        } catch (SQLException e) {
            System.out.println("Error creating post:");
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
        }

    }

    // Display sellers with an average review score of at least 3
    private static void viewBestSellers() {
        int sqlCode;
        String sqlState;
        try {
            String querySQL = "SELECT S.email, AVG (R.reviewScore) AS averageScore FROM Seller S, Post P, Review R WHERE S.email = P.email AND P.postId = R.postId GROUP BY S.email HAVING 3 <= AVG (R.reviewScore) ORDER BY S.email;";
            ResultSet rs = statement.executeQuery(querySQL);
            System.out.printf("%-50s %-10s%n", "Email", "Average Review"); 
            System.out.println("--------------------------------------------------------------");

            while (rs.next()) {
                String email = rs.getString(1);
                int avgScore = rs.getInt(2);
                System.out.printf("%-50s %-10d%n", email, avgScore);
            }

            System.out.println("\nExecution done\n");
        }

        catch (SQLException e) {
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Error getting best sellers "+ e.getMessage());
            System.out.println("Code: " + sqlCode + " sqlState: " + sqlState);
        }

    }

    private static void updateReview(Scanner scanner) {
        int sqlCode;
        String sqlState;
        System.out.println("Please enter review ID for the review which you want to change");

        String reviewID = scanner.nextLine();
        //System.out.println(reviewID);

        System.out.println("Please enter the seller's email of the post you bought from");
        String selleremail = scanner.nextLine();
        //System.out.println(selleremail);

        System.out.println("Please enter the post ID of the post you bought from");
        String postID = scanner.nextLine();
        //System.out.println(postID);

        System.out.println("Please enter your buyer email");
        String buyeremail = scanner.nextLine();
        //System.out.println(buyeremail);

        boolean bad = false;

        try {
            String buyercheckquerySQL = "SELECT buyerEmail FROM REVIEW WHERE reviewId = " + reviewID
                    + " AND sellerEmail = " + String.format("'%s'", selleremail) + " AND postId = " + postID;
            //System.out.println(buyercheckquerySQL);
            ResultSet rs = statement.executeQuery(buyercheckquerySQL);

            rs.next();
            String actualbuyeremail = rs.getString(1);
            if (!actualbuyeremail.equals(buyeremail)) {
                bad = true;
                System.out.println("Inputted buyer's email does not match the email of the buyer who made this review");
            }
        } catch (SQLException e) {
            sqlCode = e.getErrorCode(); // Get SQLCODE
            sqlState = e.getSQLState(); // Get SQLSTATE

            bad = true;
            System.out.println("Either the seller's email, the post id, or the review Id are incorrect.");
            System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
            System.out.println(e);
        }

        if (!bad) {
            System.out.println("Please enter the new review score (1-5)");
            String reviewScore = scanner.nextLine();

            System.out.println(
                    "if you want to change the description of the review you left, enter the new description, if not leave it empty");
            String newdescription = scanner.nextLine();

            if (newdescription.equals("")) {
                try {
                    String updateSQL = "UPDATE REVIEW SET reviewScore =" + reviewScore
                            + ",reviewDate = CURRENT DATE WHERE sellerEmail =" + String.format("'%s'", selleremail)
                            + " AND postId =" + postID + " AND reviewID =" + reviewID;
                    //System.out.println(updateSQL);
                    statement.executeUpdate(updateSQL);
                    System.out.println("Review successfully updated");
                } catch (SQLException e) {
                    sqlCode = e.getErrorCode(); // Get SQLCODE
                    sqlState = e.getSQLState(); // Get SQLSTATE
                    System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
                    System.out.println(e);
                }

                try {
                    String querySQL = "SELECT reviewScore FROM REVIEW WHERE reviewID =" + reviewID + " AND postId ="
                            + postID + " AND sellerEmail =" + String.format("'%s'", selleremail);
                    //System.out.println(querySQL);
                    java.sql.ResultSet rs = statement.executeQuery(querySQL);

                    while (rs.next()) {
                        int score = rs.getInt(1);
                        System.out.println("New score:  " + score);
                    }
                    System.out.println("DONE");
                } catch (SQLException e) {
                    sqlCode = e.getErrorCode(); // Get SQLCODE
                    sqlState = e.getSQLState(); // Get SQLSTATE
                    System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
                    System.out.println(e);
                }
            } else {
                try {
                    String updateSQL = "UPDATE REVIEW SET reviewScore =" + reviewScore
                            + ",reviewDate = CURRENT DATE, description =" + String.format("'%s'", newdescription)
                            + " WHERE sellerEmail =" + String.format("'%s'", selleremail) + " AND postId =" + postID
                            + " AND reviewID =" + reviewID;
                    //System.out.println(updateSQL);
                    statement.executeUpdate(updateSQL);
                    System.out.println("Review successfully updated");
                } catch (SQLException e) {
                    sqlCode = e.getErrorCode();
                    sqlState = e.getSQLState();

                    System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
                    System.out.println(e);
                }

                try {
                    String querySQL = "SELECT reviewScore, description FROM REVIEW WHERE reviewID =" + reviewID
                            + " AND postId =" + postID + " AND sellerEmail =" + String.format("'%s'", selleremail);
                    //System.out.println(querySQL);
                    ResultSet rs = statement.executeQuery(querySQL);

                    while (rs.next()) {
                        int score = rs.getInt(1);
                        String description = rs.getString(2);
                        System.out.println("New score:  " + score);
                        System.out.println("Description:  " + description);
                    }
                    System.out.println("Execution done");
                } catch (SQLException e) {
                    sqlCode = e.getErrorCode();
                    sqlState = e.getSQLState();

                    System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
                    System.out.println(e);
                }
            }
        }
    }

    private static void createNewUser(Scanner scanner) {
        int sqlCode;
        String sqlState;
        System.out.println("Please enter your email:");
        String email = scanner.nextLine();
        System.out.println("Please enter a password:");
        String password = scanner.nextLine();
        System.out.println("Please enter your birthdate (YYYY-MM-DD):");
        String birthDate = scanner.nextLine();

        String sql = "INSERT INTO User (email, password, birthDate) VALUES (?, ?, ?)";

        try (PreparedStatement newStatement = con.prepareStatement(sql)) { // statement closed on completion
            newStatement.setString(1, email);
            newStatement.setString(2, password);
            newStatement.setDate(3, Date.valueOf(birthDate));
            newStatement.executeUpdate();
            System.out.println("New user created successfully!\n");
        } catch (SQLException e) {
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Error creating user: " + e.getMessage());
            System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
        }

    }

    private static void updateUserPassword(Scanner scanner, String userEmail) {
        int sqlCode;
        String sqlState;
        System.out.println("Please enter your current password:");
        String currentPassword = scanner.nextLine();
        String actualPassword = "";

        // Check if the passwords match
        String getPassordSql = "SELECT password FROM User WHERE email = '" + userEmail + "'";
        try {
            ResultSet rs = statement.executeQuery(getPassordSql);
            if (rs.next()) {
                actualPassword = rs.getString(1);
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving user password: " + e.getMessage());
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
        }

        if (!actualPassword.equals(currentPassword)) {
            // Second chance
            System.out.println("Wrong password! Please try again");
            currentPassword = scanner.nextLine();

            if (!currentPassword.equals(actualPassword)) {
                System.out.println("Access denied, wrong password inputted");
                return;
            }

        }

        System.out.println("Please enter your new password:");
        String newPassword = scanner.nextLine();

        // Modify the password
        String updatePasswordSql = "UPDATE User SET password = " + String.format("'%s'", newPassword)
                + " WHERE email = " + String.format("'%s'", userEmail);
        try {
            statement.executeUpdate(updatePasswordSql);
            System.out.println("Password successfully updated!\n");
        } catch (SQLException e) {
            System.out.println("Error updating user password: " + e.getMessage());
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
        }

    }

    private static void updateUserCard(Scanner scanner, String userEmail) {
        int sqlCode;
        String sqlState;
        System.out.println("Please enter your card number");
        int cardNumber = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Please enter your card expiration date");
        String expirationDate = scanner.nextLine();

        System.out.println("Please enter the card holder name");
        String cardHolder = scanner.nextLine();

        System.out.println("Please enter your CVV");
        int CVV = scanner.nextInt();
        scanner.nextLine();

        String updateCardSql = "UPDATE USER SET cardNumber = " + cardNumber + ", expirationDate = '" + expirationDate
                + "', cardHolder = '" + cardHolder + "', CVV = "
                + CVV + " WHERE email = '" + userEmail + "'";
        try {
            statement.executeUpdate(updateCardSql);
            System.out.println("Payment info successfully updated!\n");
        } catch (SQLException e) {
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState(); 
            System.out.println("Error creating post: " + e.getMessage());
            System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
        }

    }

    //update address
    private static void updateUserAddress(Scanner scanner, String userEmail) {
        int sqlCode;
        String sqlState;
        System.out.println("Please enter your address");
        String address = scanner.nextLine();

        String updateAdressSql = "UPDATE USER SET address = '" + address + "' WHERE email = '"
                + userEmail + "'";
        try {
            statement.executeUpdate(updateAdressSql);
            System.out.println("Address successfully updated!\n");
        } catch (SQLException e) {
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Error updating address: " + e.getMessage());
            System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
        }

    }

   //Check if given useremail is valid
    private static boolean checkUserValidity(Scanner scanner, String userEmail) {
        boolean isValid = true;
        int sqlCode;
        String sqlState;

        try {
            String querySQL = "SELECT * FROM User WHERE email ='" + userEmail + "'";
            ResultSet rs = statement.executeQuery(querySQL);
        }

        catch (SQLException e) {
            isValid = true;
            System.out.println("Invalid userEmail provided, please try again");
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            System.out.println("Code: " + sqlCode + " sqlState: " + sqlState);
            System.out.println(e);
        }

        return isValid;
    }

    public static void printSubMenu() {
        System.out.println("Card store - User management Menu");
        System.out.println("\t1. Add new User");
        System.out.println("\t2. Update User");
        System.out.println("\t3. Go back to main menu");
        System.out.println("Please enter your option:");
    }

    public static void printUpdateSubMenu() {
        System.out.println("Card store - Update User sub-menu");
        System.out.println("\t1. Change Password");
        System.out.println("\t2. Change payment info");
        System.out.println("\t3. Change address");
        System.out.println("\t4 Go back to main menu");
        System.out.println("Please enter your option:");
    }

    private static void manageUsers(Scanner scanner) {
        printSubMenu();

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1: // add a new user
                createNewUser(scanner);
                break;
            case 2: // modify users
                System.out.println("Please enter your email:");
                String userEmail = scanner.nextLine();

                // Check if the user email is valid
                if (!checkUserValidity(scanner, userEmail)) {
                    // Offer a second chance
                    System.out.println("Reenter enter your email:");
                    userEmail = scanner.nextLine();
                    if (!checkUserValidity(scanner, userEmail)) {
                        System.out.println("Going back to main menu\n");
                        return;
                    }
                }

                while (true) {
                    printUpdateSubMenu();

                    int subchoice = scanner.nextInt();
                    scanner.nextLine();
                    boolean quit = false;

                    switch (subchoice) {
                        case 1:
                            updateUserPassword(scanner, userEmail);
                            break;
                        case 2:
                            updateUserCard(scanner, userEmail);
                            break;
                        case 3:
                            updateUserAddress(scanner, userEmail);
                            break;
                        case 4:
                            quit = true;
                            break;
                        default:
                            System.out.println("Invalid command\n");
                    }

                    if (quit) {
                        break;
                    }
                }
                break;
            case 3:
                System.out.println("Back to main menu\n");
                return;
            default:
                System.out.println("Invalid command\n");
        }

    }
}
