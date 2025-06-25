import java.util.Map;

public class Logic {
    public static void invokeGetMethod(int id, String jsonreponse) {
        System.out.println("Invoked get method " + id+"--"+jsonreponse);
    }
    public static void invokeGetAllMethod(Map<String, String> responseData) {
        System.out.println("Invoked get all method"+responseData.get("response"));
    }   
    public static void invokePostMethod(Map<String, String> inputdata) {
        System.out.println("Invoked post method"+inputdata.toString());
    }    
    public static void invokePutMethod(int id, Map<String, String> inputdata) {
        System.out.println("Invoked put method"+id+"---"+inputdata.toString());
    }
    public static void invokeDeleteMethod(int id) {
        System.out.println("Invoked delete method"+id);
    }
}
