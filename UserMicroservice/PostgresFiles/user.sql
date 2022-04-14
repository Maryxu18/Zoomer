DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Coupons;

CREATE TABLE Users(
	uid SERIAL NOT NULL PRIMARY KEY,
	email varchar NOT NULL UNIQUE,
	prefer_name varchar NOT NULL,
	"password" varchar NOT NULL,
	rides NUMERIC NOT NULL,
	isDriver bool NOT NULL DEFAULT false,
	availableCoupons integer[] NOT NULL,
    redeemedCoupons integer[] NOT NULL
);

CREATE TABLE Coupons(
    cid SERIAL NOT NULL PRIMARY KEY,
    "name" varchar(255) NOT NULL,
    description varchar(1000) NOT NULL,
	discount numeric NOT NULL,
    expiry date NOT NULL
);


INSERT INTO Coupons VALUES (1,'Ride one Get $10 off','This coupon is added after you take your first ride from Zoomer!',10,'2099-12-31');
INSERT INTO Coupons VALUES (2,'Have a $5 off','This coupon is only avaliable when you ride is greater than $10',5,'2099-12-31');