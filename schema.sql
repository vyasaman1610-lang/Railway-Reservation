CREATE DATABASE IF NOT EXISTS railway_simple;
USE railway_simple;

DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS trains;

CREATE TABLE trains (
    train_id INT PRIMARY KEY AUTO_INCREMENT,
    train_number VARCHAR(20) NOT NULL UNIQUE,
    train_name VARCHAR(100) NOT NULL,
    source_station VARCHAR(100) NOT NULL,
    destination_station VARCHAR(100) NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    fare DECIMAL(10,2) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL
);

CREATE TABLE bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    pnr VARCHAR(20) NOT NULL UNIQUE,
    train_id INT NOT NULL,
    journey_date DATE NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    total_fare DECIMAL(10,2) NOT NULL,
    status ENUM('CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (train_id) REFERENCES trains(train_id)
);

INSERT INTO trains
(train_number, train_name, source_station, destination_station,
 departure_time, arrival_time, fare, total_seats, available_seats)
VALUES
('12001', 'Delhi Shatabdi', 'Delhi', 'Mumbai', '06:00:00', '18:00:00', 1500.00, 50, 50),
('12002', 'Mumbai Express', 'Mumbai', 'Pune', '08:30:00', '12:00:00', 700.00, 40, 40),
('12003', 'Chennai Express', 'Chennai', 'Bengaluru', '07:15:00', '13:00:00', 900.00, 60, 60);
