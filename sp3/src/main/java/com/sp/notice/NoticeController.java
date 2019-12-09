package com.sp.notice;

import java.io.File;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sp.common.FileManager;
import com.sp.common.MyUtil;
import com.sp.member.SessionInfo;

@Controller("notice.noticeController")
public class NoticeController {
	@Autowired
	private NoticeService service;
	
	@Autowired
	private MyUtil util;
	
	@Autowired
	private FileManager fileManager;
	
	@RequestMapping(value="/notice/list")
	public String list(Model model,
					   @RequestParam(name="page", defaultValue="1") int current_page,
					   @RequestParam(defaultValue="all") String condition,
					   @RequestParam(defaultValue="") String keyword,
					   HttpServletRequest req) throws Exception {
		
		String cp = req.getContextPath();
		
		int rows = 5;
		int total_page = 0;
		int dataCount = 0;
		
		if(req.getMethod().equalsIgnoreCase("GET")) {
			keyword = URLDecoder.decode(keyword, "utf-8");
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("condition", condition);
		map.put("keyword", keyword);
		
		dataCount = service.dataCount(map);
		if(dataCount != 0)
			total_page = util.pageCount(rows, dataCount);
		
		if(total_page < current_page)
			current_page = total_page;
		
		// 1페이지인 경우에만 공지리스트 가져오기
		List<Notice> noticeList = null;
		if(current_page==1) {
			noticeList = service.listNoticeTop();
		}
		
		int offset = (current_page-1) * rows;
		if(offset < 0) offset = 0;
		map.put("offset", offset);
		map.put("rows", rows);
		
		List<Notice> list = service.listNotice(map);
		
		Date endDate = new Date();
		long gap;
		
		int listNum, n = 0;
		for(Notice dto : list) {
			listNum = dataCount - (offset + n);
			dto.setListNum(listNum);
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date beginDate = formatter.parse(dto.getCreated());
			
			gap = (endDate.getTime()-beginDate.getTime()) / (60*60*1000);
			dto.setGap(gap);
			
			dto.setCreated(dto.getCreated().substring(0, 10));
			n++;
		}
		
		String query = "";
		String listUrl;
		String articleUrl;
		if(keyword.length()!=0) {
			query = "condition=" +condition + 
       	         "&keyword=" + URLEncoder.encode(keyword, "utf-8");	
		}
		
		listUrl = cp+"/notice/list";
		articleUrl = cp + "/notice/article?page="+current_page;
		if(query.length()!=0) {
			listUrl = listUrl + "?" + query;
			articleUrl = articleUrl + "&" + query;
		}
		
		String paging = util.paging(current_page, total_page, listUrl);
		
		model.addAttribute("noticeList", noticeList);
		model.addAttribute("list", list);
		model.addAttribute("page", current_page);
		model.addAttribute("total_page", total_page);
		model.addAttribute("dataCount", dataCount);
		model.addAttribute("paging", paging);
		model.addAttribute("articleUrl", articleUrl);
		
		model.addAttribute("condition", condition);
		model.addAttribute("keyword", keyword);
		
		return ".notice.list";
	}
	
	@RequestMapping(value="/notice/created", method=RequestMethod.GET)
	public String createdForm(Model model,
							  HttpSession session) throws Exception {
		
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		if(! info.getUserId().equals("admin")) {
			return "redirect:/notice/list";
		}
		
		model.addAttribute("mode", "created");		
		return ".notice.created";
	}
	
	@RequestMapping(value="/notice/created", method=RequestMethod.POST)
	public String createdSubmit(Notice dto,
								HttpSession session) throws Exception {
		String root = session.getServletContext().getRealPath("/");
		String pathname = root+"uploads"+File.separator+"notice";
		
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		dto.setUserId(info.getUserId());
		
		try {
			service.insertNotice(dto, pathname);
		} catch (Exception e) {
		}	
		return "redirect:/notice/list";
	}
	
	@RequestMapping(value="/notice/update", method=RequestMethod.GET)
	public String updateForm(@RequestParam int num,
							 @RequestParam String page,
							 HttpSession session,
							 Model model) {
		
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		
		Notice dto = service.readNotice(num);
		if(dto==null || ! dto.getUserId().equals(info.getUserId())) {
			return "redirect:/notice/list?page="+page;
		}
		
		List<Notice> listFile = service.listFile(num);
		
		model.addAttribute("mode", "update");
		model.addAttribute("dto", dto);
		model.addAttribute("listFile", listFile);
		model.addAttribute("page", page);
		
		return ".notice.created";
	}
	
	@RequestMapping(value="/notice/update", method=RequestMethod.POST)
	public String updateSubmit(Notice dto,
							   @RequestParam String page,
							   HttpSession session) {
		
		String root = session.getServletContext().getRealPath("/");
		String pathname = root + "uploads" + File.separator + "notice";
		
		try {
			service.updateNotice(dto, pathname);
		} catch (Exception e) {			
		}
		return "redirect:/notice/list?page="+page;
	}
	
	@RequestMapping(value="/notice/zipdownload")
	public void zip(@RequestParam int num,
					HttpServletResponse resp,
					HttpSession session) throws Exception {
		
		String root = session.getServletContext().getRealPath("/");
		String pathname = root + "uploads" + File.separator + "notice";
		
		boolean b = false;
		List<Notice> list = service.listFile(num);
		
		if(list.size()>0) {
			String []sources = new String[list.size()];
			String []originals = new String[list.size()];
			String zipFilename = num+".zip";
			
			// 압축을 할 때는 경로가 꼭 있어야 한다. \aaaa.txt 같이.
			for(int idx=0; idx<list.size(); idx++) {
				sources[idx] = pathname+File.separator+list.get(idx).getSaveFilename();
				originals[idx] = File.separator+list.get(idx).getOriginalFilename();
			}
			b = fileManager.doZipFileDownload(sources, originals, zipFilename, resp);			
		}
		
		if(! b) {
			resp.setContentType("text/html;charset=utf-8");
			PrintWriter out = resp.getWriter();
			out.print("<script>alert('다운불가...');history.back();</script>");
		}
	}
	
	@RequestMapping(value="/notice/article", method=RequestMethod.GET)
	public String article(
			@RequestParam int num,
			@RequestParam String page,
			@RequestParam(defaultValue="all") String condition,
			@RequestParam(defaultValue="") String keyword,
			Model model,
			@CookieValue(defaultValue="0") int cnum,
			HttpServletResponse resp) throws Exception {
		
		keyword = URLDecoder.decode(keyword, "utf-8");
		
		String query = "page="+page;
		if(keyword.length()!=0) {
			query += "&condition="+condition+"&keyword="+URLEncoder.encode(keyword, "utf-8");
		}
		
		if(num!=cnum) {
			service.updateHitCount(num);
			
			Cookie ck = new Cookie("cnum", Integer.toString(num));
			resp.addCookie(ck);
		}
		
		Notice dto = service.readNotice(num);
		if(dto==null)
			return "redirect:/notice/list?"+query;
		
		// 스타일로 처리하는 경우 : style="white-space:pre;"
        // dto.setContent(dto.getContent().replaceAll("\n", "<br>"));
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("condition", condition);
        map.put("keyword", keyword);
        map.put("num", num);
        
        Notice preReadDto = service.preReadNotice(map);
        Notice nextReadDto = service.nextReadNotice(map);
        
        // 파일
        List<Notice> listFile = service.listFile(num);
        
        model.addAttribute("dto", dto);
        model.addAttribute("preReadDto", preReadDto);
        model.addAttribute("nextReadDto", nextReadDto);
        model.addAttribute("listFile", listFile);
        
        model.addAttribute("page", page);
        model.addAttribute("query", query);
		
		return ".notice.article";
	}
	
	
	@RequestMapping("/notice/download")
	public void down(@RequestParam int fileNum,
					HttpServletResponse resp,
					HttpSession session) throws Exception {
		
		String root = session.getServletContext().getRealPath("/");
		String pathname = root + "uploads" + File.separator +"notice";
		
		Notice dto = service.readFile(fileNum);
		boolean b = false;
		
		if(dto != null) {
			b = fileManager.doFileDownload(dto.getSaveFilename(), dto.getOriginalFilename(),
							pathname, resp);
		}
		
		if(! b) {
			resp.setContentType("text/html;charset=utf-8");
			PrintWriter out = resp.getWriter();
			out.print("<script>alert('파일 다운로드가 실패 했습니다.';history.back();</script>");
		}
	}
	
	@RequestMapping(value="/notice/deleteFile", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteFile(
					@RequestParam int fileNum,
					HttpSession session) throws Exception {
		
		String root = session.getServletContext().getRealPath("/");
		String pathname = root + "uploads" + File.separator +"notice";
		
		String state = "false";
		Notice dto = service.readFile(fileNum);
		if(dto!=null) {
			fileManager.doFileDelete(dto.getSaveFilename(), pathname);
			
			Map<String, Object> map = new HashMap<>();
			map.put("field", "fileNum");
			map.put("num", fileNum);
			
			try {
				service.deleteFile(map);
				state = "true";
			} catch (Exception e) {
				
			}
		}
		
		Map<String, Object> model = new HashMap<>();
		model.put("state", state);
		return model;
	}
	
	@RequestMapping(value="/notice/delete")
	public String delete(@RequestParam int num,
						 @RequestParam String page,
						 @RequestParam(defaultValue="all") String condition,
						 @RequestParam(defaultValue="") String keyword,
						 HttpSession session) throws Exception {
		
		keyword = URLDecoder.decode(keyword, "utf-8");
		
		String query = "page="+page;
		if(keyword.length()!=0) {
			query += "&condition="+condition+"&keyword="+URLEncoder.encode(keyword, "utf-8");
		}
		
		String root = session.getServletContext().getRealPath("/");
		String pathname = root + "uploads" + File.separator +"notice";
		
		SessionInfo info = (SessionInfo)session.getAttribute("member");
		Notice dto = service.readNotice(num);
		if(dto!=null && (dto.getUserId().equals(info.getUserId()) || info.getUserId().equals("admin"))) {
			service.deleteNotice(num, pathname);
		}
		
		return "redirect:/notice/list?"+query;
	}
	
}
