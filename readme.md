##### **About Mavcat**

Mavcat is a fast udp file transfer tool for long distance with high packet loss.Compare to tsunami,
It has a user friendly sdk,and it support client with no public ip address.Definitly,it's faster than tsunami.


##### **Compile**
`./gradlew build`

##### **Using Client**
```
java -jar client-java.jar
type command next
connect ip port password   // connect to server
get remotepath localpath  // download file
set bandwidth             // set server sending rate
```

##### **Using Server**

```
Usage: java -jar server.jar [-hV] [-enable_api]... [-d=<dir>] [-p=<port>]
                    [-P=<password>] [-up=<udpPort>]
  -d, --dir=<dir>     directory will be allowed to download file
  -h, --help          Show this help message and exit.
  -p, --port=<port>   tcp port server listen to
  -P, --password=<password> the server password that client  needs to know
  -up, --udp-port=<udpPort> udp port will listen on
  -V, --version       Print version information and exit.
```