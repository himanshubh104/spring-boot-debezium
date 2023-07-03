# Spring Boot + Debezium + MySQL
![image](https://github.com/HimanshBhatnagar/spring-boot-debezium/assets/36370178/60869fca-772c-4e45-b8a2-1895ef2efbaa)

(src- https://www.baeldung.com/debezium-intro)


## INTRODUCTION
Debezium is an open-source platform for CDC built on top of Apache Kafka. Its primary use is to record all row-level changes committed to each source database table in a transaction log. Each application listening to these events can perform needed actions based on incremental data changes.
Debezium provides a library of connectors, supporting multiple databases like MySQL, MongoDB, PostgreSQL, and others.
Moreover, Debezium monitors even if our applications are down. Upon restart, it will start consuming the events where it left off, so it misses nothing.
Debezium stores the last fecthed record info in offset and we can configure that to be stored in Kafka topic, File or Redis (I've used File, Redis in this example).
<br>

## OVERVIEW
In this project, I implement the Debezium service programmatically, and run via MySQL database server with an example table in order to monitor all events about data insertion or change.

<br>

## PREREQUISITES
- Java
- MySQL
- Redis

<br>

## HOW TO TEST
1. Boot up a Redis on local
   
### DESCRIPTIONS OF MYSQL BINLOG CONFIGURATION PROPERTIES
For MySql version 8.0 
eg. file= my.ini, path= C:\ProgramData\MySQL\MySQL Server 8.0
| Property | Description |
| :------- | :---------- |
| server-id | The value for the server-id must be unique for each server and replication client in the MySQL cluster. During MySQL connector set up, Debezium assigns a unique server ID to the connector. |
| log-bin | Specifies the base name to use for binary log files. With binary logging enabled, the server logs all statements that change data to the binary log, which is used for backup and replication. |
| binlog_format | The binlog-format must be set to ROW or row. |

For more information visit this [link](https://debezium.io/documentation/reference/stable/connectors/mysql.html#:~:text=Descriptions%20of%20MySQL%20binlog%20configuration%20properties).

1. Once inside, login into MySQL server:
```shell
mysql --user=user --password=password
```

2. Once logged in, create the database and table to run the demo application:
```shell
CREATE DATABASE practice_db;
```
```shell
USE customers;
```
```shell
CREATE TABLE `customers` (
  `ID` int NOT NULL,
  `NAME` varchar(100) DEFAULT NULL,
  `AGE` int DEFAULT NULL,
  `SEX` char(1) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8;
```

3. From another terminal, run the application:
```shell
./mvnw spring-boot:run
```

4. Insert some data into the `customerdb` table:
```shell
INSERT INTO practice_db.customers (ID, NAME, AGE, SEX) VALUES (1, 'John Doe', 25, M);
```

5. From the application's terminal, you should see a data insertion event log similar to the one below:
```log
2023-07-03T00:27:37.448+05:30  INFO 15444 --- [pool-2-thread-1] c.h.s.listener.DebeziumListener          : Key = Struct{ID=3}, Value = Struct{before=Struct{ID=3,NAME=Kiran,AGE=25,SEX=N},after=Struct{ID=3,NAME=Kiran,AGE=25,SEX=O},source=Struct{version=1.9.3.Final,connector=mysql,name=mysql_localhost_connect,ts_ms=1688324257000,db=practice_db,table=customers,server_id=1,file=LAPTOP-4I5AQTBL-bin.000263,pos=747,row=0,thread=9},op=u,ts_ms=1688324257323}
2023-07-03T00:27:37.448+05:30  INFO 15444 --- [pool-2-thread-1] c.h.s.listener.DebeziumListener          : SourceRecordChangeValue = 'Struct{before=Struct{ID=3,NAME=Kiran,AGE=25,SEX=N},after=Struct{ID=3,NAME=Kiran,AGE=25,SEX=O},source=Struct{version=1.9.3.Final,connector=mysql,name=mysql_localhost_connect,ts_ms=1688324257000,db=practice_db,table=customers,server_id=1,file=LAPTOP-4I5AQTBL-bin.000263,pos=747,row=0,thread=9},op=u,ts_ms=1688324257323}'
```
