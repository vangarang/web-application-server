package http;

import java.util.HashMap;
import java.util.Map;

import web.Controller;
import web.CreateUserController;
import web.ListUserController;
import web.LoginController;

public class RequestMapping {
	private static Map<String, Controller> controllers = new HashMap<String, Controller>();
	
	static {
		controllers.put("user/create",new CreateUserController());
		controllers.put("user/login",new LoginController());
		controllers.put("user/create",new ListUserController());
	}
	
	public static Controller getController(String requestUrl) {
		return controllers.get(requestUrl);
	}
}
