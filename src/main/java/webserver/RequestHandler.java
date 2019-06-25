package webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import http.HttpRequest;
import http.HttpResponse;
import http.RequestMapping;
import web.Controller;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	/* 용	도 : 메인 로직
	 * 최종수정일 : 19.06.25 
	 * 수정   이력 : 19.06.17) 요구사항 2.1 - get 방식으로 회원가입
	 * 			19.06.19) 요구사항 4 - 302 상태코드 이용
	 * 			19.06.22) 5.2.1 - HttpRequest 사용
	 * 			19.06.25) 5.2.2 - HttpResponse 사용 
	 * 			19.06.25) 5.2.3 - Controller 사용
	 */
	public void run() {
		// log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		try (InputStream in = connection.getInputStream(); 
			 OutputStream out = connection.getOutputStream()) {
			// BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8")); // 19.06.22 5.2.1) HttpRequest 사용으로 삭제
			HttpRequest request = new HttpRequest(in); 		// 19.06.22 5.2.1) HttpRequest 사용
			HttpResponse response = new HttpResponse(out);	// 19.06.25 5.2.2) HttpResponse 사용 
			
			
			Controller controller = RequestMapping.getController(request.getPath());
			if(controller == null) {
				String path = getDefaultPath(request.getPath());
				response.forward(path);
			}else {
				controller.service(request, response);
			}
			
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private String getDefaultPath(String path) {
		if (path.equals("/")) {
			return "/index.html";
		}
		return path;
	}
}
