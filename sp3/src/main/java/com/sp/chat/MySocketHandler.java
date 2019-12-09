package com.sp.chat;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class MySocketHandler extends TextWebSocketHandler {
	private final Logger logger = LoggerFactory.getLogger(MySocketHandler.class);
	private Map<String, User> sessionMap = new Hashtable<>(); // Hashtable : 동시성의 문제를 해결할 수 있다.

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// WebSocket 연결이 열리고 사용할 준비가 된 경우
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		super.handleMessage(session, message);
		
		// 클라이언트에게서 메시지가 도착할 때마다 호출
		JSONObject jsonReceive = null;
		try {
			jsonReceive = new JSONObject(message.getPayload().toString());
		} catch (Exception e) {			
		}
		
		if(jsonReceive==null) return;
		
		String cmd = jsonReceive.getString("cmd");
		if(cmd==null) return;
		
		if(cmd.equals("connect")) { // 처음 접속한 경우
			// 접속한 아이디를 키로 session 정보를 저장
			String userId = jsonReceive.getString("userId");
			String nickName = jsonReceive.getString("nickName");
			
			User user = new User();
			user.setUserId(userId);
			user.setNickName(nickName);
			user.setSession(session);
			sessionMap.put(userId, user);
			
			// 현재 접속한 사용자 리스트를 전송
			Iterator<String> it = sessionMap.keySet().iterator();
			while(it.hasNext()) {
				String key = it.next();
				
				if(userId.equals(key)) continue;
				
				User vo = sessionMap.get(key);
				
				JSONObject ob = new JSONObject();
				ob.put("cmd", "connectList");
				ob.put("userId", vo.getUserId());
				ob.put("nickName", vo.getNickName());
				
				sendMessage(ob.toString(), session);
			}
			
			// 다른 클라이언트에게 접속 사실을 알림
			JSONObject ob = new JSONObject();
			ob.put("cmd", "connect");
			ob.put("userId", userId);
			ob.put("nickName", nickName);
			sendAllMessage(ob.toString(), userId);
			
		} else if(cmd.equals("message")) { // 채팅 문자열을 전송한 경우
			User vo = getUser(session);
			String s = jsonReceive.getString("chatMsg");
			
			JSONObject ob = new JSONObject();
			ob.put("cmd", "message");
			ob.put("chatMsg", s);
			ob.put("userId", vo.getUserId());
			ob.put("nickName", vo.getNickName());
			
			// 다른 사용자에게 메시지 전송
			sendAllMessage(ob.toString(), vo.getUserId());
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// 전송에 문제가 생길 때
		removeUser(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		super.afterConnectionClosed(session, status);
		
		// WebSocket 연결이 닫혔을 때 자동으로 호출
		removeUser(session);
		
		logger.info("remove session");
	}
	
	// 모든 사용자에게 메시지 전송
	protected void sendAllMessage(String message, String out) {
		Iterator<String> it = sessionMap.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			if(out!=null && out.equals(key)) continue;
			
			User user = sessionMap.get(key);
			WebSocketSession session = user.getSession();
			
			try {
				if(session.isOpen()) {
					session.sendMessage(new TextMessage(message));
				}
			} catch (Exception e) {
				removeUser(session);
			}
			
		}
	}
	
	protected void sendMessage(String message, WebSocketSession session) {
		if(session.isOpen()) {
			try {
				session.sendMessage(new TextMessage(message));
			} catch (Exception e) {
				logger.error("fail to send message !! ", e);
			}
		}
	}
	
	protected User getUser(WebSocketSession session) {
		User user = null;
		
		Iterator<String> it = sessionMap.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			
			User u = sessionMap.get(key);
			if(u.getSession() == session) {
				user = u;
				break;
			}
		}
		
		return user;
	}
	
	protected void removeUser(WebSocketSession session) {
		// 접속 해제된 사실을 다른 클라이언트에게 전송하고 접속 해제된 유저를 제거
		User user = getUser(session);
		if(user!=null) {
			JSONObject job = new JSONObject();
			job.put("cmd", "disconnect");
			job.put("userId", user.getUserId());
			job.put("nickName", user.getNickName());
			
			sendAllMessage(job.toString(), user.getUserId());
			
			try {
				user.getSession().close();
			} catch (Exception e) {				
			}
			
			sessionMap.remove(user.getUserId());
		}
	}
	
	@PostConstruct
	public void init() throws Exception {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Calendar cal = Calendar.getInstance();
				JSONObject job = new JSONObject();
				job.put("cmd", "time");
				job.put("hour", cal.get(Calendar.HOUR_OF_DAY));
				job.put("minute", cal.get(Calendar.MINUTE));
				job.put("second", cal.get(Calendar.SECOND));
				
				sendAllMessage(job.toString(), null);
			}
		};
		
		Timer t = new Timer();
		// 20초 후 20초 마다
		t.schedule(task, new Date(System.currentTimeMillis()+20000), 20000);
	}
	
}
