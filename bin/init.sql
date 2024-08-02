-- Create opal-fines-db and user
CREATE DATABASE "opal-fines-db";
CREATE USER "opal-fines" WITH ENCRYPTED PASSWORD 'opal-fines';
GRANT ALL PRIVILEGES ON DATABASE "opal-fines-db" TO "opal-fines";

-- Create opal-user-db and user
CREATE DATABASE "opal-user-db";
CREATE USER "opal-user" WITH ENCRYPTED PASSWORD 'opal-user';
GRANT ALL PRIVILEGES ON DATABASE "opal-user-db" TO "opal-user";
