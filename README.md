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

// You can use Logic.java file if you want to implement some logic eg. 
//calling third party services from your services or some logic implementation
// if you want to keep only straight-forward SQLs, then only yml file enough. simply create yml file and then deploy it using API generator like below

java -jar APIGeneratorAxe.jar employee-service.yml

//if you implement Logic, then do deploy like below

//liveuser@localhost-live:/mnt/sdb4/apigeneration$ javac Logic.java

//liveuser@localhost-live:/mnt/sdb4/apigeneration$ java -cp .:APIGeneratorAxe.jar APIAutomationv3 employee-service.yml Logic 
