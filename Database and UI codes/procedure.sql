-- Connect to the DB
CONNECT TO COMP421@

-- Drop the procedure if it exists
DROP PROCEDURE UpdateCartTotalPrice@

-- Create the procedure
CREATE PROCEDURE UpdateCartTotalPrice(
    IN buyer_email VARCHAR(50)
)
LANGUAGE SQL
BEGIN
    -- Declare variables
    DECLARE sum DECIMAL(31, 2) DEFAULT 0;  -- Initialize sum to 0
    DECLARE qty INTEGER;
    DECLARE price DECIMAL(31, 2);
    DECLARE done INT DEFAULT 0;  -- Variable to track if cursor is done
    DECLARE c CURSOR FOR 
        SELECT Card.quantity, Card.price
        FROM User
        INNER JOIN Belongs ON Belongs.email = User.email
        INNER JOIN Card ON Card.postId = Belongs.postId
        WHERE User.email = buyer_email;

    -- Declare a continue handler to set done flag when no more rows are fetched
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Open the cursor
    OPEN c;

    -- Initialize sum to 0 (explicit SET)
    SET sum = 0;

    FETCH c INTO qty, price;
    -- Loop to fetch rows and calculate sum
    WHILE done = 0 DO
        SET sum = sum + (qty * price);  -- Add to sum
        FETCH c INTO qty, price;
    END WHILE;

    -- Close the cursor
    CLOSE c;

    -- Update the Cart table with the total price
    UPDATE Cart
    SET totalPrice = sum
    WHERE email = buyer_email;
END@
