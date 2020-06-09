Fork of [Eclipse Leshan](https://github.com/eclipse/leshan/) to build an LwM2M Gateway to AWS IoT.

      mvn clean install
      # or mvn package
      java -jar leshan-server-demo/target/leshan-server-demo-*-SNAPSHOT-jar-with-dependencies.jar

Goals:

- persist device properties received via LwM2M in AWS IoT reported shadow
- publish changes to AWS IoT desired shadow to device using LwM2M
