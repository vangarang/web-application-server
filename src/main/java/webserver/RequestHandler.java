package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	// 19.06.17 요구사항 2.1) get 방식으로 회원가입
	// 19.06.19 요구사항 4) 302 상태코드 이용
	public void run() {
//		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
//				connection.getPort());
		// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			HttpRequest request = new HttpRequest(in); // 19.06.22 5.1.3) 리팩토링으로 생성한 HttpRequest 클래스 사용
			HttpResponse response = new HttpResponse(out);	//19.06.25 5.2.2) HttpResponse 사용 
			String path = getDefaultPath(request.getPath());
			/*
			 * 19.06.22 5.3) HttpRequest 클래스 생성으로 인해 필요 상실 Map<String, String> headerMap =
			 * headerReader(br); String url = headerMap.get("url"); int contentLength = 0;
			 * if (headerMap.get("contentLength") != null) { contentLength =
			 * Integer.parseInt(headerMap.get("contentLength")); }
			 */
			// if(url.contains("create")) { // refactoring : to startsWith() method
			// if(url.startsWith("/user/create")) { // refactoring : POST method 처리 위해 삭제
			if (("/user/create").equals(path)) {
				signIn(request, response);
				// response302Header(dos, "/index.html"); // 요구사항 5) 로그인 하기
				// 19.06.25 5.2.2) HttpResponse 사용
				
			} else if ("/user/login".equals(path)) {
//				boolean logInRes = logIn(br, contentLength);
				logIn(request, response);
				/*
				 * 06.25 5.2.2) HttpResponse 사용으로, logIn 메서드로 이전 
				if (loginSuccess) {
					response302LoginSuccessHeader(dos);
				} else {
					responseResource(dos, "/user/login_failed.html");
				}
				*/
			} else if ("user/list".equals(path)) {
				// if(!Boolean.parseBoolean(headerMap.get("logined"))) { // refactoring 으로 인해 삭제
				if (!isLogin(request.getHeaders("Cookie"))) {
					/*
					 * 06.25 5.2.2) HttpResponse 사용으로 리팩토링
					responseResource(dos, "/user/login.html");
					*/
					response.sendRedirect("/user/login.html");
					return;
				}
				/*
				 * 06.25 5.2.2) HttpResponse 사용으로, memberList 메서드로 이전
				byte[] body = memberList(request);
				response200Header(dos, body.length);
				responseBody(dos, body);
				*/
				memberList(request, response);
			}else {
				response.forward(path);
			}
			/*
			 * 06.25) response 사용으로 필요 상실
			} else if (path.endsWith(".css")) { // 요구사항 7 : css 지원하기
				response200CssResource(dos, path);

			} else {
				responseResource(dos, path);
			}
			*/
			
			/*
			 * 19.06.19 : responseResource로 통합됨 // 기본 응답 byte[] body =
			 * "Hello World".getBytes();
			 * 
			 * // 다른 응답 if (!"".equals(url)) { body = Files.readAllBytes(new File("./webapp"
			 * + url).toPath()); }
			 * 
			 * response200Header(dos, body.length); responseBody(dos, body);
			 */
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

	
	/*
	 * 19.06.25) HttpResponse 구현 및 사용으로 필요 상실
	 * 
	 * 용 도 : CSS 요청에 대한 응답 
	 * 최종수정일 : 19.06.22 
	 * 수정 내용 :
	private void response200CssResource(DataOutputStream dos, String path) throws IOException {
		byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
		dos.writeBytes("HTTP/1.1 200 OK \r\n");
		dos.writeBytes("Content-Type: text/css\r\n");
		dos.writeBytes("Content-Length: " + body.length + "\r\n");
		dos.writeBytes("\r\n");
		responseBody(dos, body);
	}

	// 19.06.19 요구사항 5 - 로그인하기
	private void responseResource(DataOutputStream dos, String url) throws IOException {
		byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
		response200Header(dos, body.length);
		responseBody(dos, body);
	}

	// 19.06.19 요구사항 5 - 로그인하기
	private void response302LoginSuccessHeader(DataOutputStream dos) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Set-Cookie: logined=true \r\n");
			dos.writeBytes("Location: /index.html \r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	*/
	
	/*
	 * 용	도 : 회원 리스트 출력 
	 * 최종수정일 : 19.06.25
	 * 수정   이력 : 19.06.22) 메소드로 분리 : private byte[] memberList(HttpRequest request) throws IOException {
	 * 			19.06.25) HttpResponse 사용 
	 */
	private void memberList(HttpRequest request, HttpResponse response) throws IOException {
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
		//byte[] body = sb.toString().getBytes();	// 06.25) response 사용으로 필요 상실
		response.forwardBody(sb.toString());
	}

	/*
	 * 용	도 : 로그인 
	 * 수   정   일 : 19.06.22 HttpRequest 구현 및 사용
	 * 최종수정일 : 19.06.25 HttpResponse 구현 및 사용 
	 * 19.06.19 요구사항 5 - 로그인하기 
	 * private boolean logIn(BufferedReader br, int contentLength) throws IOException{
	 * private boolean logIn(HttpRequest request) throws IOException {
	 */
	
	private void logIn(HttpRequest request, HttpResponse response) throws IOException {
		/*
		 * String body = IOUtils.readData(br, contentLength); Map<String, String> params
		 * = HttpRequestUtils.parseQueryString(body);
		 */
		User user = DataBase.findUserById(request.getParams("userId"));
		/*
		 * 06.25 HttpResponse 사용으로 리팩토링
		if (user == null) {
			return false;
		}
		if (user.getPassword().equals(request.getParams("password"))) {
			return true;
		} else {
			return false;
		}
		*/
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

	/*
	 * 용 도 : 회원가입 
	 * 최종수정일 : 19.06.25 
	 * 19.06.17 요구사항 2.1) GET 방식으로 회원가입하기 
	 * 19.06.19 요구사항 2.2) POST 방식으로 회원가입하기/ 요구사항 5) 로그인 하기(데이터베이스에 회원 정보 등록) 
	 * private String signIn(String url, int contentLength) throws IOException { 
	 * private String signIn(BufferedReader br, int contentLength) throws IOException { 
	 * 5.2.1) HttpRequest 리팩토링으로 인해 기능 변경
	 * 5.2.2) HttpResponse 구현 및 사용
	 */
	private void signIn(HttpRequest request, HttpResponse response) throws IOException {
		User user = new User(request.getParams("userId"), request.getParams("password"), request.getParams("name"),
				request.getParams("email"));
		DataBase.addUser(user);
		response.sendRedirect("index.html");
		/*
		 * int index = url.indexOf("?"); if(index != -1) {
		 * 
		 * String requestPath = url.substring(0, index); String params =
		 * url.substring(index + 1);
		 * 
		 * String body = IOUtils.readData(br, contentLength); Map<String, String> pMap =
		 * util.HttpRequestUtils.parseQueryString(body);
		 * 
		 * if (!pMap.isEmpty()) { user = new User(pMap.get("userId"),
		 * pMap.get("password"), pMap.get("name"), pMap.get("email"));
		 * DataBase.addUser(user); } return "/index.html";
		 */

	}

	/*
	 * 19.06.16 요구사항 1.1) BufferedReader 추가: InputStream을 한 줄 단위(readLine)로 읽기 위해 사용
	 * 19.06.17 요구사항 3) POST 방식으로 회원가입 최종 수정일 : 19.06.22 5.2) HttpRequest로 리팩토링: 하기
	 * 메소드 필요 상실 private Map<String, String> headerReader(BufferedReader br) throws
	 * IOException { String line = ""; String[] tokens = null; Map<String, String>
	 * resMap = new HashMap<String, String>();
	 * 
	 * // int contentLength = 0; do { line = br.readLine(); if (line == null) {
	 * return resMap; } log.debug("header : {}", line); tokens = line.split(" "); //
	 * 요구사항 1.2) 문자열 분리 // 요구사항 1.3) 요청 URL에 해당하는 파일 읽어 전달 if
	 * (tokens[0].equals("GET") || tokens[0].equals("POST")) { resMap.clear();
	 * resMap.put("url", tokens[1]); } if(line.contains("Cookie")) {
	 * if(isLogin(line)) { resMap.put("logined","true"); }else {
	 * resMap.put("logined","false"); } } if (line.contains("Content-Length")) {
	 * resMap.put("contentLength", getContentLength(line)); }
	 * }while(!"".equals(line));
	 * 
	 * return resMap; }
	 */

	/*
	 * 용 도 : 기 로그인 여부 체크 최종 수정일: 19.06.22 5.2) HttpRequest 리팩토링으로 인한 수정 private
	 * boolean isLogin(String line) {
	 */
	private boolean isLogin(String cookieValue) {
		/*
		 * String[] headerTokens = line.split(" "); 
		 * Map<String,String> cookies = HttpRequestUtils.parseCookies(headerTokens[1].trim());
		 */
		Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
		String value = cookies.get("logined");
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}

	private void response302Header(DataOutputStream dos, String url) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Location: " + url + " \r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

//	private int getContentLength(String line) {
	private String getContentLength(String line) {
		String[] headerTokens = line.split(":");
//		return Integer.parseInt(headerTokens[1].trim());
		return headerTokens[1].trim();
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
