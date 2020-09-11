# CustomerAgent-Backend

This is an java project wrapped with spring-mvc framework which is the backend for customer agent communcation app.

The master branch is old code and with less features and all the core business logic are in node js.

https://github.com/HarikumarG/CustomerAgent-Webserver

The masterv1 branch is new updated code with all the additional features and here all the core logic are in java itself.

# Prerequisites

1. Install Java (openjdk version 11.0.7)
2. Install maven
3. Install tomcat server (version 8.0)
4. Install mysql (version 14.14)
4. The code is written in Eclipse-Helios, If you want you can install any IDE or You can run in terminal also

# Steps to Installation and Run

1. `git clone https://github.com/HarikumarG/CustomerAgent-Backend.git && cd CustomerAgent-Backend`
2. Run maven install command to download the required dependencies from pom.xml
3. Login to Mysql and create a database with the command `create database customeragent` and exit Mysql
4. Run the command `mysql -u root -p customeragent < DumpFile.sql`
5. Before running the application Goto src->main->webapp->WEB-INF->classes->dispatcher-servlet.xml in that configure your mysql "username" and "password" and save it
6. Now run the project on tomcat server

Note: To check the project, If you are using Postman or any other api tool
1. Select method as "POST"
2. Url is "http://localhost:8080/CustomerAgent/login"
3. Select type as JSON
4. Sample login as Agent:
`{
	"name":"sampleagent",
	"password":"agent",
	"isAgent":"true"	
}`
5. Sample login as Customer:
`{
	"name":"samplecustomer",
	"password":"customer",
	"isAgent":"false"	
}`
For both the request you will get the response as "SUCCESS"
