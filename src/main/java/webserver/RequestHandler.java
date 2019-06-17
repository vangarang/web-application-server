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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.IOUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	// 19.06.17 요구사항 2.1) get 방식으로 회원가입
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
//			if(url.startsWith("/user/create")) { // refactoring : POST method 처리 위해 삭제
			if (("/user/create").equals(url)) { 
				signIn(br, contentLength);
			}
			
			// 기본 응답
			byte[] body = "Hello World".getBytes();

			// 다른 응답
			if (!"".equals(url)) {
				body = Files.readAllBytes(new File("./webapp" + url).toPath());
			}

			response200Header(dos, body.length);
			responseBody(dos, body);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	// 19.06.17 요구사항 2.1) GET 방식으로 회원가입하기
	// 수정일: 19.06.17
	// 요구사항 2.2) POST 방식으로 회원가입하기
//	private String signIn(String url, int contentLength) throws IOException {
	private void signIn(BufferedReader br, int contentLength) throws IOException {
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
			log.debug("User : {}", user);
		}
//		return body;
	}

	// 19.06.16 요구사항 1.1) BufferedReader 추가: InputStream을 한 줄 단위(readLine)로 읽기 위해 사용
	// 최종 수정일 : 19.06.17 요구사항 2.2) POST 방식으로 회원가입
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
			if (line.contains("Content-Length")) {
				resMap.put("contentLength", getContentLength(line));
			}
		} while (!"".equals(line));
		
		return resMap;
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
