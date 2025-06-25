import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

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

public class APIAutomation {
	public static String DB_DRIVER = "";
	public static String DB_URL = "";
	public static String DB_USER = "";
	public static String DB_PASS = "";
	public static Map<String, Object> data;
	public static String LOGIC_CLASS_NAME = "";

	public static void main(String[] args) throws Exception {
	//	InputStream inputStream = APIAutomationv3.class.getClassLoader().getResourceAsStream("employee-service.yml");

//        if (inputStream == null) {
//            throw new RuntimeException("Resource not found: input.yml");
//        }
//        Yaml yaml = new Yaml();
//      //  FileInputStream inputStream = new FileInputStream("/input.yml");
//        data = yaml.load(inputStream);
        
       
      //  PomModifier.modifyPom((String) data.get("groupid"), (String) data.get("artifactid"), String.valueOf(data.get("version")));
        

	    String yamlPath = args[0];
	    String logicClassName = args[1];  // e.g., "Logic"

	    // Load YAML from file path passed in arg 0
	    try (InputStream inputStream = new FileInputStream(yamlPath)) {
	        Yaml yaml = new Yaml();
	        data = yaml.load(inputStream);
	    } catch (IOException e) {
	        System.err.println("Failed to load YAML file: " + yamlPath);
	        e.printStackTrace();
	        System.exit(1);
	    }

        LOGIC_CLASS_NAME = logicClassName;
        
        DB_DRIVER = (String) data.get("db_driver");
        DB_URL = (String) data.get("db_url");
        DB_USER = (String) data.get("db_user");
        DB_PASS = (String) data.get("db_password");
		setupDatabase();
		HttpServer server = HttpServer.create(new InetSocketAddress((Integer) data.get("server_port")), 0);
		
		String getEndpoint = (String) data.get("get_endpoint");
		String postEndpoint = (String) data.get("post_endpoint");
		String putEndpoint = (String) data.get("put_endpoint");
		String deleteEndPoint = (String) data.get("delete_endpoint");
		String getAllEndpoint = (String) data.get("get_all_endpoint");
		
		if (getEndpoint != null && !getEndpoint.isBlank())
		server.createContext(data.get("base_path")+data.get("get_endpoint").toString(), new GetEmployeeHandler());
		if (postEndpoint != null && !postEndpoint.isBlank())
		server.createContext(data.get("base_path")+data.get("post_endpoint").toString(), new InsertEmployeeHandler());
		if (putEndpoint != null && !putEndpoint.isBlank())
		server.createContext(data.get("base_path")+data.get("put_endpoint").toString(), new UpdateEmployeeHandler());
		if (deleteEndPoint != null && !deleteEndPoint.isBlank())
		server.createContext(data.get("base_path")+data.get("delete_endpoint").toString(), new DeleteEmployeeHandler());
		if (getAllEndpoint != null && !getAllEndpoint.isBlank())
		server.createContext(data.get("base_path")+data.get("get_all_endpoint").toString(), new GetAllEmployeesHandler());
		server.setExecutor(null); // Creates a default executor
		server.start();
		System.out.println("Server started at port "+(Integer) data.get("server_port"));
		
		//generate jar
		//JarBuilder.generateJar();
	}

	// Handler for fetching a specific employee by ID
	static class GetEmployeeHandler implements HttpHandler {
		private ObjectMapper objectMapper = new ObjectMapper();

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				sendResponse(exchange, 405, objectMapper.writeValueAsString(Map.of("error", "Method Not Allowed")));
				return;
			}
			String path = exchange.getRequestURI().getPath();
			String[] segments = path.split("/");

			if (segments.length == 4) { // /employee/getEmployee/{id}
				int id = Integer.parseInt(segments[3]);
				String response;
				try {
					String query = (String) data.get("get_endpoint_query");
					String logic = (String) data.get("get_endpoint_logic");
					response = getDataFromQuery(id, query);
					try {
						if(!LOGIC_CLASS_NAME.isBlank())
						 executeGetMethodRuntime(logic, id, response);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sendResponse(exchange, 200, response);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				sendResponse(exchange, 400, "Invalid URL");
			}
		}

		private String getDataFromQuery(int id, String query) throws SQLException, JsonProcessingException {
			Pattern pattern = Pattern
					.compile("SELECT\\s+(.+)\\s+FROM\\s+([a-zA-Z0-9_]+)\\s+WHERE\\s+([a-zA-Z0-9_]+)\\s*=\\s*\\?");
			Matcher matcher = pattern.matcher(query);

			if (matcher.find()) {
				String selectedColumns = matcher.group(1);
				String tableName = matcher.group(2);
				String conditionColumn = matcher.group(3);
				String finalQuery = String.format("SELECT %s FROM %s WHERE %s = ?", selectedColumns, tableName,
						conditionColumn);

				try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
						PreparedStatement stmt = conn.prepareStatement(finalQuery)) {
					stmt.setInt(1, id);
					ResultSet rs = stmt.executeQuery();
					if (rs.next()) {
						Map<String, Object> result = new LinkedHashMap<>();
						String[] columns = selectedColumns.split(",");
						for (String column : columns) {
							column = column.trim(); // Clean up any extra spaces
							result.put(column, rs.getObject(column)); // Get the value dynamically
						}
						return objectMapper.writeValueAsString(result);
					} else {
						return objectMapper.writeValueAsString(Collections.singletonMap("message", "No data found"));
					}
				}
			} else {
				throw new SQLException("Invalid query format. Could not parse the SQL.");
			}
		}

	}

	// Handler for getting all employees
	static class GetAllEmployeesHandler implements HttpHandler {
		private ObjectMapper objectMapper = new ObjectMapper();

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				sendResponse(exchange, 405, objectMapper.writeValueAsString(Map.of("error", "Method Not Allowed")));
				return;
			}
			String response;
			try {
				//String query = "SELECT id,name,department FROM employee";
				String query = (String) data.get("get_all_endpoint_query");
				String logic = (String) data.get("get_all_endpoint_logic");
				//System.out.println("query1all"+query);
				response = getAllDataFromQuery(query);
				Map<String, String> responseMap = new HashMap<String, String>();
				responseMap.put("response", response);
				try {
					if(!LOGIC_CLASS_NAME.isBlank())
					  executeGetAllMethodRuntime(logic, responseMap);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				sendResponse(exchange, 200, response);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		private String getAllDataFromQuery(String query) throws SQLException, JsonProcessingException {
			Pattern pattern = Pattern.compile("(?i)SELECT\\s+(.+)\\s+FROM\\s+([a-zA-Z0-9_]+)");
			Matcher matcher = pattern.matcher(query);

			if (matcher.find()) {
				String selectedColumns = matcher.group(1);
				String tableName = matcher.group(2);
				String finalQuery = String.format("SELECT %s FROM %s", selectedColumns, tableName);

				try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
						Statement stmt = conn.createStatement()) {
					ResultSet rs = stmt.executeQuery(finalQuery);
					List<Map<String, Object>> resultList = new ArrayList<>();
					while (rs.next()) {
						Map<String, Object> row = new LinkedHashMap<>();
						String[] columns = selectedColumns.split(",");
						for (String column : columns) {
							column = column.trim();
							Object value = rs.getObject(column);
							row.put(column, value);
						}
						resultList.add(row);
					}

					return objectMapper.writeValueAsString(resultList);
				}
			} else {
				throw new SQLException("Invalid query format. Could not parse the SQL.");
			}
		}

	}

	// Handler for inserting an employee
	static class InsertEmployeeHandler implements HttpHandler {
		private ObjectMapper objectMapper = new ObjectMapper();

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				try {
					Map<String, String> inputdata = parseJsonRequestBody(exchange);
					System.out.println("exchange data "+exchange);
					String query = (String) data.get("post_endpoint_query");
					String logic = (String) data.get("post_endpoint_logic");
					//System.out.println("insert data"+inputdata.toString());
					String response = insertDataFromQuery(query, inputdata);
					
					if(!LOGIC_CLASS_NAME.isBlank())
						executePostMethodRuntime(logic, inputdata);
					
					sendResponse(exchange, 201, response);
				} catch (SQLException | JsonProcessingException e) {
					e.printStackTrace();
					sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", e.getMessage())));
				} catch (Exception e) {
					e.printStackTrace();
					sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", "Unexpected error")));
				}
			} else {
				sendResponse(exchange, 405, objectMapper.writeValueAsString(Map.of("error", "Method Not Allowed")));
			}
		}

		private String insertDataFromQuery(String query, Map<String, String> data)
				throws SQLException, JsonProcessingException {
			Pattern pattern = Pattern
					.compile("INSERT INTO\\s+([a-zA-Z0-9_]+)\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)");
			System.out.println("query"+query);
			Matcher matcher = pattern.matcher(query);
			if (matcher.find()) {
				String columnNames = matcher.group(2);
				String placeholders = matcher.group(3);
				String[] columns = columnNames.split(",");
				String[] valuesPlaceholders = placeholders.split(",");
				if (columns.length != valuesPlaceholders.length) {
					throw new SQLException("The number of columns does not match the number of values placeholders.");
				}
				try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
						PreparedStatement stmt = conn.prepareStatement(query)) {
					for (int i = 0; i < columns.length; i++) {
						Object value = data.get(columns[i].trim());
						if (value == null) {
							throw new SQLException("Missing value for column: " + columns[i].trim());
						}
						stmt.setObject(i + 1, value);
					}
					int rows = stmt.executeUpdate();
					return objectMapper.writeValueAsString(Map.of("message", "Inserted " + rows + " rows"));
				}
			} else {
				throw new SQLException("Invalid query format. Could not parse the SQL.");
			}
		}

	}

	// Handler for updating an employee
	static class UpdateEmployeeHandler implements HttpHandler {
		private ObjectMapper objectMapper = new ObjectMapper();

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
				sendResponse(exchange, 405, objectMapper.writeValueAsString(Map.of("error", "Method Not Allowed")));
				return;
			}

			String path = exchange.getRequestURI().getPath();
			String[] segments = path.split("/");

			if (segments.length != 4) {
				sendResponse(exchange, 400, objectMapper.writeValueAsString(Map.of("error", "Invalid URL")));
				return;
			}

			int id = Integer.parseInt(segments[3]);

			try {
				Map<String, String> inputdata = parseJsonRequestBody(exchange);

				if (!inputdata.containsKey("name") || !inputdata.containsKey("department")) {
					sendResponse(exchange, 400,
							objectMapper.writeValueAsString(Map.of("error", "Missing required fields")));
					return;
				}
				//String query = "UPDATE employee SET name=?, department=? WHERE id=?";
				String query = (String) data.get("put_endpoint_query");
				String logic = (String) data.get("put_endpoint_logic");
				String response = updateDataFromQuery(id, inputdata, query);
				if(!LOGIC_CLASS_NAME.isBlank())
					executePutMethodRuntime(logic, id, inputdata);
				sendResponse(exchange, 200, response);
			} catch (SQLException | JsonProcessingException e) {
				e.printStackTrace();
				sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", e.getMessage())));
			} catch (Exception e) {
				e.printStackTrace();
				sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", "Unexpected error")));
			}
		}

		private String updateDataFromQuery(int id, Map<String, String> data, String query)
				throws SQLException, JsonProcessingException {
			Pattern pattern = Pattern
					.compile("UPDATE\\s+([a-zA-Z0-9_]+)\\s+SET\\s+(.+)\\s+WHERE\\s+([a-zA-Z0-9_]+)\\s*=\\s*\\?");
			Matcher matcher = pattern.matcher(query);

			if (matcher.find()) {
				String setColumns = matcher.group(2);
				String[] columnSetParts = setColumns.split(",");

				try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
						PreparedStatement stmt = conn.prepareStatement(query)) {
					int paramIndex = 1;
					for (String column : columnSetParts) {
						String[] columnValuePair = column.split("=");
						String columnName = columnValuePair[0].trim();

						String value = data.get(columnName);
						if (value == null) {
							throw new SQLException("Missing value for column: " + columnName);
						}
						stmt.setString(paramIndex++, value);
					}
					stmt.setInt(paramIndex, id);
					int rows = stmt.executeUpdate();
					return objectMapper.writeValueAsString(Map.of("message", "Updated " + rows + " rows"));
				}
			} else {
				throw new SQLException("Invalid query format. Could not parse the SQL.");
			}
		}

	}
	
	// Handler for updating an employee
		static class PatchEmployeeHandler implements HttpHandler {
			private ObjectMapper objectMapper = new ObjectMapper();

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
					sendResponse(exchange, 405, objectMapper.writeValueAsString(Map.of("error", "Method Not Allowed")));
					return;
				}

				String path = exchange.getRequestURI().getPath();
				String[] segments = path.split("/");

				if (segments.length != 4) {
					sendResponse(exchange, 400, objectMapper.writeValueAsString(Map.of("error", "Invalid URL")));
					return;
				}

				int id = Integer.parseInt(segments[3]);

				try {
					Map<String, String> inputdata = parseJsonRequestBody(exchange);

					if (!data.containsKey("name") || !data.containsKey("department")) {
						sendResponse(exchange, 400,
								objectMapper.writeValueAsString(Map.of("error", "Missing required fields")));
						return;
					}
					//String query = "UPDATE employee SET name=?, department=? WHERE id=?";
					String query = (String) data.get("patch_endpoint_query");
					String logic = (String) data.get("patch_endpoint_logic");
					String response = patchDataFromQuery(id, inputdata, query);
					if(!LOGIC_CLASS_NAME.isBlank())
						executePatchMethodRuntime(logic, id, inputdata);
					sendResponse(exchange, 200, response);
				} catch (SQLException | JsonProcessingException e) {
					e.printStackTrace();
					sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", e.getMessage())));
				} catch (Exception e) {
					e.printStackTrace();
					sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", "Unexpected error")));
				}
			}

			private String patchDataFromQuery(int id, Map<String, String> data, String query)
					throws SQLException, JsonProcessingException {
				Pattern pattern = Pattern
						.compile("UPDATE\\s+([a-zA-Z0-9_]+)\\s+SET\\s+(.+)\\s+WHERE\\s+([a-zA-Z0-9_]+)\\s*=\\s*\\?");
				Matcher matcher = pattern.matcher(query);

				if (matcher.find()) {
					String setColumns = matcher.group(2);
					String[] columnSetParts = setColumns.split(",");

					try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
							PreparedStatement stmt = conn.prepareStatement(query)) {
						int paramIndex = 1;
						for (String column : columnSetParts) {
							String[] columnValuePair = column.split("=");
							String columnName = columnValuePair[0].trim();

							String value = data.get(columnName);
							if (value == null) {
								throw new SQLException("Missing value for column: " + columnName);
							}
							stmt.setString(paramIndex++, value);
						}
						stmt.setInt(paramIndex, id);
						int rows = stmt.executeUpdate();
						return objectMapper.writeValueAsString(Map.of("message", "Updated " + rows + " rows"));
					}
				} else {
					throw new SQLException("Invalid query format. Could not parse the SQL.");
				}
			}

		}

	// Handler for deleting an employee
	static class DeleteEmployeeHandler implements HttpHandler {
		private ObjectMapper objectMapper = new ObjectMapper();

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
				sendResponse(exchange, 405, objectMapper.writeValueAsString(Map.of("error", "Method Not Allowed")));
				return;
			}

			String path = exchange.getRequestURI().getPath();
			String[] segments = path.split("/");

			if (segments.length != 4) {
				sendResponse(exchange, 400, objectMapper.writeValueAsString(Map.of("error", "Invalid URL")));
				return;
			}

			int id = Integer.parseInt(segments[3]);

			try {
				//String query = "DELETE FROM employee WHERE id=?";
				String query = (String) data.get("delete_endpoint_query");
				String logic = (String) data.get("delete_endpoint_logic");
				String response = deleteDataFromQuery(query, id);
				if(!LOGIC_CLASS_NAME.isBlank())
					executeDeleteMethodRuntime(logic, id);
				sendResponse(exchange, 200, response);
			} catch (SQLException | JsonProcessingException e) {
				e.printStackTrace();
				sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", e.getMessage())));
			} catch (Exception e) {
				e.printStackTrace();
				sendResponse(exchange, 500, objectMapper.writeValueAsString(Map.of("error", "Unexpected error")));
			}
		}

		private String deleteDataFromQuery(String query, int id) throws SQLException, JsonProcessingException {
			try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
					PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setInt(1, id);
				int rows = stmt.executeUpdate();
				return objectMapper.writeValueAsString(Map.of("message", "Deleted " + rows + " rows"));
			}
		}

	}

	// Helper method to parse JSON request body
	private static Map<String, String> parseJsonRequestBody(HttpExchange exchange) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null)
			sb.append(line);
		reader.close();
		return new ObjectMapper().readValue(sb.toString(), Map.class);
	}

	// Helper method to send JSON response
	private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(statusCode, response.getBytes().length);
		OutputStream os = exchange.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	// Initialize the database and create employee table
	private static void setupDatabase() {
	    try {
	        // Explicitly load the H2 driver (optional but ensures driver registration)
	        Class.forName(DB_DRIVER);
	        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
	             Statement stmt = conn.createStatement()) {

	            // Create the employee table
	            stmt.execute("CREATE TABLE IF NOT EXISTS employee (" +
	                         "id INT PRIMARY KEY, " +
	                         "name VARCHAR(100), " +
	                         "department VARCHAR(100))");

	            // Create the Account table
	            stmt.execute("CREATE TABLE IF NOT EXISTS account (" +
	                         "id INT PRIMARY KEY, " +
	                         "accountname VARCHAR(100), " +
	                         "salary DOUBLE, " +
	                         "employeeid INT)");

	            System.out.println("H2 database initialized.");
	        }
	    } catch (ClassNotFoundException e) {
	        System.err.println("H2 Driver not found. Make sure it's on the classpath.");
	        e.printStackTrace();
	    } catch (SQLException e) {
	        System.err.println("Database error:");
	        e.printStackTrace();
	    }
	}
	

	public static Object executeGetMethodRuntime(String logicCall, int id, String jsonResponse) throws Exception {
	    if (logicCall != null && !logicCall.isBlank()) {
	        String methodName = logicCall.trim();

	        // Use the logic class name from command-line
	        Class<?> logicClass = Class.forName(LOGIC_CLASS_NAME);
	        Method method = logicClass.getMethod(methodName, int.class, String.class);

	        return method.invoke(null, id, jsonResponse);
	    }
	    return null;
	}
	
	public static Object executeGetAllMethodRuntime(String logicCall, Map<String, String> responseMap) throws Exception {
	    if (logicCall != null && !logicCall.isBlank()) {
	        String methodName = logicCall.trim();

	        // Use the logic class name from command-line
	        Class<?> logicClass = Class.forName(LOGIC_CLASS_NAME);
	        Method method = logicClass.getMethod(methodName, Map.class);

	        return method.invoke(null, responseMap);
	    }
	    return null;
	}
	
	public static Object executePostMethodRuntime(String logicCall, Map<String, String> inputdata) throws Exception {
	    if (logicCall != null && !logicCall.isBlank()) {
	        String methodName = logicCall.trim();

	        // Use the logic class name from command-line
	        Class<?> logicClass = Class.forName(LOGIC_CLASS_NAME);
	        Method method = logicClass.getMethod(methodName, Map.class);

	        return method.invoke(null, inputdata);
	    }
	    return null;
	}
	
	public static Object executePutMethodRuntime(String logicCall, int id, Map<String, String> inputdata) throws Exception {
	    if (logicCall != null && !logicCall.isBlank()) {
	        String methodName = logicCall.trim();

	        // Use the logic class name from command-line
	        Class<?> logicClass = Class.forName(LOGIC_CLASS_NAME);
	        Method method = logicClass.getMethod(methodName, int.class, Map.class);

	        return method.invoke(null, id, inputdata);
	    }
	    return null;
	}
	
	public static Object executePatchMethodRuntime(String logicCall, int id, Map<String, String> inputdata) throws Exception {
	    if (logicCall != null && !logicCall.isBlank()) {
	        String methodName = logicCall.trim();

	        // Use the logic class name from command-line
	        Class<?> logicClass = Class.forName(LOGIC_CLASS_NAME);
	        Method method = logicClass.getMethod(methodName, int.class, Map.class);

	        return method.invoke(null, inputdata);
	    }
	    return null;
	}
	
	public static Object executeDeleteMethodRuntime(String logicCall, int id) throws Exception {
	    if (logicCall != null && !logicCall.isBlank()) {
	        String methodName = logicCall.trim();

	        // Use the logic class name from command-line
	        Class<?> logicClass = Class.forName(LOGIC_CLASS_NAME);
	        Method method = logicClass.getMethod(methodName, int.class);

	        return method.invoke(null, id);
	    }
	    return null;
	}


}


