package web;

import java.util.Collection;
import java.util.Map;

import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import model.User;
import util.HttpRequestUtils;

public class ListUserController implements Controller {

	@Override
	public void service(HttpRequest request, HttpResponse response) {
		if (!isLogin(request.getHeaders("Cookie"))) {
			response.sendRedirect("/user/login.html");
			return;
		}
		
		Collection<User> users = DataBase.findAll();
		StringBuilder sb = new StringBuilder();
		sb.append("<table border='1'>");
		for (User user : users) {
			sb.append("<tr>");
			sb.append("<td>" + user.getUserId() + "</td>");
			sb.append("<td>" + user.getName() + "</td>");
			sb.append("<td>" + user.getEmail() + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		response.forwardBody(sb.toString());
	}
	
	/*
	 * 용	 도 : 기 로그인 여부 체크 
	 * 최종 수정일 : 19.06.22 
	 * 수정    이력 : 5.2 - HttpRequest 리팩토링으로 인한 수정 
	 * private boolean isLogin(String line) {
	 */
	private boolean isLogin(String cookieValue) {
		Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
		String value = cookies.get("logined");
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}
}
