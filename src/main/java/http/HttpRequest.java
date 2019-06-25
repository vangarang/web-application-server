package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import http.RequestLine.HttpMethod;
import util.HttpRequestUtils;
import util.IOUtils;
import webserver.RequestHandler;

// 리팩토링 5.1) 요청 데이터를 처리하는 로직을 별도의 클래스로 분리
public class HttpRequest {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	private Map<String, String> headers = new HashMap<String, String>();
	private Map<String, String> params = new HashMap<String, String>();
	private RequestLine requestLine;

	public HttpRequest(InputStream in) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

			String line = br.readLine();
			if (line == null) {
				return;
			}

			requestLine = new RequestLine(line); // HTTP 메소드, URL, GET요청의 파라미터 분리
			line = br.readLine();
			while (!"".equals(line)) {
				if (line == null)
					break;
				log.debug("header: {}", line);
				String tokens[] = line.split(": ");
				if (tokens.length > 1) {
					headers.put(tokens[0].trim(), tokens[1].trim()); // key, value로 헤더 저장
				}
				line = br.readLine();
			}
			if (getMethod().isPost()) {
				String body = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
				params = HttpRequestUtils.parseQueryString(body);
			} else {
				params = requestLine.getParams();
			}
		} catch (IOException io) {
			log.error(io.getMessage());
		}
	}

	/*
	 * 19.06.22 RequestLine이라는 클래스로 분리 private void processRequestLine(String
	 * requestLine) { String[] tokens = requestLine.split(" "); method = tokens[0];
	 * 
	 * if ("POST".equals(method)) { path = tokens[1]; return; }
	 * 
	 * int index = tokens[1].indexOf("?"); if (index == -1) { path = tokens[1]; }
	 * else { path = tokens[1].substring(0, index); params =
	 * HttpRequestUtils.parseQueryString(tokens[1].substring(index + 1)); } }
	 */

	// 19.06.22 5.2) 리팩토링 : requestLine 분리에 따른 수정
	public HttpMethod getMethod() {
		return requestLine.getMethod();
	}

	public String getPath() {
		return requestLine.getPath();
	}

	public String getHeaders(String name) {
		return headers.get(name);
	}

	public String getParams(String name) {
		return params.get(name);
	}
}
