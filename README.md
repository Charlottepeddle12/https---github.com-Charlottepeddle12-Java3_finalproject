 To create the database, copy and paste the code from db.sql and run.
 In web.xml and tomact/conf/context.xml file add:
  <Resource
      name="jdbc/javaproject"
      auth="Container"
      type="javax.sql.DataSource"
      maxTotal="100"
      maxIdle="30"
      maxWaitMillis="10000"
      username= Your credential
      password= Your credential
      driverClassName="org.mariadb.jdbc.Driver"
      url="jdbc:mariadb://localhost:3306/javaproject"
    />
then run ./gradlew clean build
add tomcat to server -> click on lib/app.war -> run on server ->choose tomcat -> click on tomcat -> publish full
