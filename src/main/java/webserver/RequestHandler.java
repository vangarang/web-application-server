package webserver;

import java.io.BufferedReader;
import java.io.DataOutput;
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
			DataOutputStream dos = new DataOutputStream(out);
			Map<String, String> headerMap = headerReader(br);
			String url = headerMap.get("url");
			int contentLength = 0;
			if (headerMap.get("contentLength") != null) {
				contentLength = Integer.parseInt(headerMap.get("contentLength"));
			}
			// if(url.contains("create")) { // refactoring : to startsWith() method
			// if(url.startsWith("/user/create")) { // refactoring : POST method 처리 위해 삭제
			if (("/user/create").equals(url)) {
				signIn(br, contentLength);
				response302Header(dos, "/index.html");	// 요구사항 5) 로그인 하기
			}else if("/user/login".equals(url)) {
				boolean logInRes = logIn(br, contentLength);
				if(logInRes){
					dos = new DataOutputStream(out);
					response302LoginSuccessHeader(dos);
				}else {
					responseResource(out, "/user/login_failed.html");
				}
			}else if("user/list".equals(url)){
				if(!Boolean.parseBoolean(headerMap.get("logined"))) {
					responseResource(out, "/user/login.html");
					return;
				}
				Collection<User> users = DataBase.findAll();
				StringBuilder sb = new StringBuilder();
				sb.append("<table border='1'>");
				for(User user : users) {
					sb.append("<tr>");
					sb.append("<td>"+user.getUserId()+"</td>");
					sb.append("<td>"+user.getName()+"</td>");
					sb.append("<td>"+user.getEmail()+"</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");
				byte[] body = sb.toString().getBytes();
				dos = new DataOutputStream(out);
				response200Header(dos, body.length);
				responseBody(dos, body);
				
			}else if(url.endsWith(".css")){	// 요구사항 7 : css 지원하기
				dos = new DataOutputStream(out);
				byte[] body = Files.readAllBytes(new File("./webapp"+url).toPath());
				response200CssHeader(dos, body.length);
				responseBody(dos, body);
			}
			else{
				responseResource(out, url);
			}

			/*
			 * 19.06.19 : responseResource로 통합됨
			// 기본 응답
			byte[] body = "Hello World".getBytes();

			// 다른 응답
			if (!"".equals(url)) {
				body = Files.readAllBytes(new File("./webapp" + url).toPath());
			}

			response200Header(dos, body.length);
			responseBody(dos, body);
			*/
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) throws IOException{
		dos.writeBytes("HTTP/1.1 200 OK \r\n");
		dos.writeBytes("Content-Type: text/css\r\n");
		dos.writeBytes("Content-Length: "+lengthOfBodyContent+"\r\n");
		dos.writeBytes("\r\n");
	}
	
	// 19.06.19 요구사항 5 - 로그인하기
	private void responseResource(OutputStream out, String url) throws IOException{
		DataOutputStream dos = new DataOutputStream(out);
		byte[] body = Files.readAllBytes(new File("./webapp"+url).toPath());
		response200Header(dos, body.length);
		responseBody(dos,body);
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
	
	// 19.06.19 요구사항 5 - 로그인하기	
	private boolean logIn(BufferedReader br, int contentLength) throws IOException{
		String body = IOUtils.readData(br, contentLength);
		Map<String, String> params = HttpRequestUtils.parseQueryString(body);
		User user = DataBase.findUserById(params.get("userId"));
		if(user == null) {
			return false;
		}
		if (user.getPassword().equals(params.get("password"))) {
			return true; 
		}else {
			return false;
		}
	}

	// 19.06.17 요구사항 2.1) GET 방식으로 회원가입하기
	// 최종 수정일: 19.06.19
	// 요구사항 2.2) POST 방식으로 회원가입하기
	// 요구사항 5) 로그인 하기(데이터베이스에 회원 정보 등록)
//	private String signIn(String url, int contentLength) throws IOException {
	private String signIn(BufferedReader br, int contentLength) throws IOException {
		model.User user = null;

		/*
		 * int index = url.indexOf("?"); if(index != -1) {
		 * 
		 * String requestPath = url.substring(0, index); String params =
		 * url.substring(index + 1);
		 */
		String body = IOUtils.readData(br, contentLength);
		Map<String, String> pMap = util.HttpRequestUtils.parseQueryString(body);

		if (!pMap.isEmpty()) {
			user = new User(pMap.get("userId"), pMap.get("password"), pMap.get("name"), pMap.get("email"));
			DataBase.addUser(user);
		} 
		return "/index.html";
	}

	// 19.06.16 요구사항 1.1) BufferedReader 추가: InputStream을 한 줄 단위(readLine)로 읽기 위해 사용
	// 최종 수정일 : 19.06.17 요구사항 3) POST 방식으로 회원가입
	private Map<String, String> headerReader(BufferedReader br) throws IOException {
		String line = "";
		String[] tokens = null;
		Map<String, String> resMap = new HashMap<String, String>();

//		int contentLength = 0;
		do {
			line = br.readLine();
			if (line == null) {
				return resMap;
			}
			log.debug("header : {}", line);
			tokens = line.split(" "); // 요구사항 1.2) 문자열 분리
			// 요구사항 1.3) 요청 URL에 해당하는 파일 읽어 전달
			if (tokens[0].equals("GET") || tokens[0].equals("POST")) {
				resMap.clear();
				resMap.put("url", tokens[1]);
			}
			if(line.contains("Cookie")) {
				if(isLogin(line)) {
					resMap.put("logined","true");
				}else {
					resMap.put("logined","false");
				}
			}
			if (line.contains("Content-Length")) {
				resMap.put("contentLength", getContentLength(line));
			}
		}while(!"".equals(line));

		return resMap;
	}
	
	private boolean isLogin(String line) {
		String[] headerTokens = line.split(" ");
		Map<String,String> cookies = HttpRequestUtils.parseCookies(headerTokens[1].trim());
		String value = cookies.get("logined");
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}
	
	private void response302Header(DataOutputStream dos, String url) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Location: "+url+" \r\n");
			dos.writeBytes("\r\n");
		}catch(IOException e) {
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
