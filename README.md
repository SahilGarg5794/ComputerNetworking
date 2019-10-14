# ComputerNetworking
A computer networking basic program to implement FTP Server using socket programming .

Steps to run the code :
run Server:
  cd Server
  javac FtpServer.java
  java FtpServer
  
run Client(Run following commands):
  cd <ClientFolder>
  javac <ClientName>.java
  java <ClientName>
  Run the following command to connect to server :
  ftpclient localhost 8081
  Enter username and passwored separated by space :
  username1 password1
  Then the client will be connected to server , then you can issues commands : dir , get <fileName> , upload <FileName>
  
  
