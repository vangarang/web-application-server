package web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import model.User;

public class LoginController implements Controller {
	private static final Logger log = LoggerFactory.getLogger(LoginController.class);
	@Override
	public void service(HttpRequest request, HttpResponse response) {
		// TODO Auto-generated method stub
		User user = DataBase.findUserById(request.getParams("userId"));
		log.debug("user: {}",user);
		if (user != null) {
			if(user.login(request.getParams("password"))) {
				response.addHeader("Set-Cookie", "logined=true");
				response.sendRedirect("/index.html");
			}else {
				response.sendRedirect("/user/login_failed.html");
			}
		}else {
			response.sendRedirect("/user/login_failed.html");
		}
	}

}
