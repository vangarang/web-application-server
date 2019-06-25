package web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import model.User;

public class CreateUserController implements Controller {

	private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);
	
	@Override
	public void service(HttpRequest request, HttpResponse response) {
		// TODO Auto-generated method stub
		User user = new User(
				request.getParams("userId"), request.getParams("password"), 
				request.getParams("name"), request.getParams("email"));
		log.debug("user {}:", user);
		DataBase.addUser(user);
		response.sendRedirect("index.html");
	}

}
