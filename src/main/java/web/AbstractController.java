package web;

import http.HttpRequest;
import http.HttpResponse;
import http.RequestLine.HttpMethod;

public class AbstractController implements Controller {

	@Override
	public void service(HttpRequest request, HttpResponse response) {
		HttpMethod method = request.getMethod();
		
		if(method.isPost()) {
			doPost(request, response);
		}else {
			doGet(request, response);
		}
	}
	
	protected void doPost(HttpRequest request, HttpResponse response) {
		
	}
	
	protected void doGet(HttpRequest request, HttpResponse response) {
		
	}
}