-- Include your drop table DDL statements in this file.
-- Make sure to terminate each statement with a semicolon (;)

-- LEAVE this statement on. It is required to connect to your database.
CONNECT TO COMP421;

-- Remember to put the drop table ddls for the tables with foreign key references
--    BEFORE the ddls to drop the parent tables (reverse of the creation order).

-- This is only an example of how you add drop table ddls to this file.
--   You may remove it.
-- DROP TABLE User CASCADE;

-- Relations
DROP TABLE Contains;
DROP TABLE Moderates;
DROP TABLE Belongs;
DROP TABLE Checks;
DROP TABLE Includes;
DROP TABLE Bids;

-- Entities
DROP TABLE Review;
DROP TABLE Card;
DROP TABLE Cart;
DROP TABLE Wishlist;
DROP TABLE Auction;
DROP TABLE Post;
DROP TABLE Order;
DROP TABLE Buyer;
DROP TABLE Seller;
DROP TABLE Admin;
DROP TABLE User;
