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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
        	String line = "";
        	String[] tokens = null;
        	String url = "";
        	//190616 1) BufferedReader 추가: InputStream을 한 줄 단위(readLine)로 읽기 위해 사용
        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	do{
        		line = br.readLine();
        		if (line==null) {return;}
        		
        		//190616 2) 문자열 분리
        		tokens = line.split(" ");
        		
        		//190616 3) 요청 URL에 해당하는 파일 읽어 전달
        		if (tokens[0].equals("GET") ) {
        			url = tokens[1];
        		}
        	}while(!"".equals(line));
        	
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = "Hello World".getBytes();
            if(!"".equals(url)) {
            	body = Files.readAllBytes(new File("./webapp"+url).toPath()); 
            }
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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
