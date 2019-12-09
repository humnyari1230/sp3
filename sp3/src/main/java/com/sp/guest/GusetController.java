package com.sp.guest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sp.common.MyUtil;
import com.sp.member.SessionInfo;

@Controller("guest.guestController")
public class GusetController {
	
	@Autowired
	private GuestService service;
	
	@Autowired
	private MyUtil util;
	
	@RequestMapping("/guest/guest")
	public String main() throws Exception {
		
		return ".guest.guest";
	}
	
	@RequestMapping(value="/guest/insert", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> submit(Guest dto,
									  HttpSession session) throws Exception {
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		dto.setUserId(info.getUserId());
		
		Map<String, Object> model = new HashMap<>();
		String state = "true";
		
		try {
			service.insertGuest(dto);
		} catch (Exception e) {
			state = "false";
		}
		
		model.put("state", state);
		
		return model;
	}
	
	@RequestMapping(value="/guest/list")
	@ResponseBody
	public Map<String, Object> list(@RequestParam(value="pageNo", defaultValue="1") int current_page) throws Exception {
		
		
		// model에 list, dataCount, pageNo, total_page, paging을 put
		
		int rows=5;
		int total_page;
		int dataCount = service.dataCount();
		
		total_page = util.pageCount(rows, dataCount);
		if(current_page>total_page)
			current_page = total_page;
		
		Map<String, Object> map = new HashMap<String, Object>();
		int offset = (current_page-1)*rows;
		if(offset < 0) offset = 0;
		map.put("offset", offset);
        map.put("rows", rows);
		
		List<Guest> list = service.listGuest(map);
		
		for(Guest dto : list) {
			dto.setContent(util.htmlSymbols(dto.getContent()));
		}
		
		String paging = util.pagingMethod(current_page, total_page, "listPage");
		
		// 작업 결과를 JSON으로 전송
		Map<String, Object> model = new HashMap<>();
		model.put("dataCount", dataCount);
		model.put("pageNo", current_page);
		model.put("list", list);
		model.put("total_page", total_page);
		model.put("paging", paging);
		
		return model;
	}
	
	@ResponseBody
	@RequestMapping(value="/guest/delete", method=RequestMethod.POST)
	public Map<String, Object> deleteGuest(@RequestParam Map<String, Object> paramMap, 
										   HttpSession session) throws Exception {
		
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		try {
			paramMap.put("userId", info.getUserId());
			service.deleteGuest(paramMap);
		} catch (Exception e) {
		}
		
		String page = (String)paramMap.get("pageNo");
		
		return list(Integer.parseInt(page));
	}
}
