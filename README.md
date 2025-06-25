// Curl examples
// curl -X GET http://localhost:8002/employee/getEmployee/2
// curl -X POST -H "Content-Type: application/json" -d '{"id": 2, "name": "John", "department": "IT"}' http://localhost:8002/employee/insertEmployee
// curl -X PUT -H "Content-Type: application/json" -d '{"name": "JohnUpdated", "department": "HR"}' http://localhost:8002/employee/updateEmployee/2
// curl -X DELETE http://localhost:8002/employee/deleteEmployee/2
// curl -X GET http://localhost:8002/employee/getAllEmployees

// prepare input.yml file
// java -jar APIGenerator.jar input.yml --> it means we are deploying account-api service in production with simple yml file, 
// nothing is required apart from yml file to deploy a new service
// every yml file is a new service here

//javac Logic.java
//java -cp .:APIGeneratorAxe.jar APIAutomation employee-service.yml Logic

//liveuser@localhost-live:/mnt/sdb4/apigeneration$ javac Logic.java
//liveuser@localhost-live:/mnt/sdb4/apigeneration$ java -cp .:APIGeneratorAxe.jar APIAutomationv3 employee-service.yml Logic
