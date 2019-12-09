<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%
   String cp = request.getContextPath();
%>

<style type="text/css">
.guest-write {
    border: #d5d5d5 solid 1px;
    padding: 10px;
    min-height: 50px;
}
</style>

<script type="text/javascript">
function login() {
	location.href="<%=cp%>/member/login";
}

function ajaxJSON(url, type, query, fn) {
	$.ajax({
		type:type, // get인지 post인지
		url:url, // 보내는 주소
		data:query, // 넘겨줄 값
		dataType:"json",
		success:function(data) {
			fn(data);
		},
		beforeSend:function(jqXHR) {
			jqXHR.setRequestHeader("AJAX", true); // 헤더에 AJAX라는 이름으로 true를 전송
		},
		error:function(jqXHR) {
			if(jqXHR.status==403) {
				login();
				return false;
			}
			console.log(jqXHR.responseText);
		}
	});
}

$(function() {
	listPage(1);
});

function listPage(page) {
	var url = "<%=cp%>/guest/list";
	var query = "pageNo="+page;
	var fn = function(data) {
		printGuest(data);
	};
	
	ajaxJSON(url, "get", query, fn);
}

function printGuest(data) {
	// console.log(data);
	var uid="${sessionScope.member.userId}";
	var list = data.list;
	var dataCount = data.dataCount;
	var total_page = data.total_page;
	var pageNo = data.pageNo;
	var paging = data.paging;
	
	var out="";
	if(dataCount!=0) {
		for(var idx=0; idx<list.length; idx++) {
			var num=list[idx].num;
			var userName=list[idx].userName;
			var userId=list[idx].userId;
			var content=list[idx].content;
			var created=list[idx].created;
			
			out+="    <tr height='35' bgcolor='#eeeeee'>";
			out+="      <td width='50%' style='padding-left: 5px; border:1px solid #cccccc; border-right:none;'>"+ userName+"</td>";
			out+="      <td width='50%' align='right' style='padding-right: 5px; border:1px solid #cccccc; border-left:none;'>" + created;
			if(uid==userId || uid=="admin") {
				out+=" | <a onclick='deleteGuest(\""+num+"\", \""+pageNo+"\");'>삭제</a></td>" ;
			} else {
				out+=" | <a href='#'>신고</a></td>" ;
			}
			out+="    </tr>";
			out+="    <tr style='height: 50px;'>";
			out+="      <td colspan='2' style='padding: 5px;' valign='top'>"+content+"</td>";
			out+="    </tr>";
		}
		out+="    <tr style='height: 35px;'>";
		out+="      <td colspan='2' style='text-align: center;'>";
		out+=paging;
		out+="      </td>";
		out+="    </tr>";
	}
	
	$("#listGuestBody").html(out);
}

function sendGuest() {
	if(! $("#content").val().trim()) {
		$("#content").focus();
		return;
	}
	
	var url = "<%=cp%>/guest/insert";
	// var content = encodeURIComponent($("#content").val());
	// var query = "content="+content;
	var query = $("form[name=guestForm]").serialize();
	
	var fn = function(data) {
		$("#content").val("");
		
		listPage(1);
	};
	
	ajaxJSON(url, "post", query, fn);
}

function deleteGuest(num, page) {	
	if(confirm("삭제 하시겠습니까 ?")) {
		
	var url="<%=cp%>/guest/delete";
	var query = "num="+num+"&pageNo="+page;
	
	var fn = function(data) {
		printGuest(data);
	};
	ajaxJSON(url, "post", query, fn);
	
	}
}
</script>

</head>

<div class="body-container" style="width: 700px;">
    <div class="body-title">
        <h3><i class="far fa-edit"></i> 방명록 </h3>
    </div>
    
    <div>
             <form name="guestForm" method="post" action="">
             <div class="guest-write">
                 <div style="clear: both;">
                         <span style="font-weight: bold;">방명록쓰기</span><span> - 타인을 비방하거나 개인정보를 유출하는 글의 게시를 삼가 주세요.</span>
                 </div>
                 <div style="clear: both; padding-top: 10px;">
                       <textarea name="content" id="content" class="boxTF" rows="3" style="display:block; width: 100%; padding: 6px 12px; box-sizing:border-box; resize: none;" required="required"></textarea>
                  </div>
                  <div style="text-align: right; padding-top: 10px;">
                       <button type="button" class="btn" onclick="sendGuest();" style="padding:8px 25px;"> 등록하기 </button>
                  </div>           
            </div>
           </form>
         
           <div id="listGuest">
           	<table style="width: 100%; margin: 10px auto 0px; border-spacing: 0; border-collapse: collapse">
           		<thead>
           			<tr height="35">
           				<td width="50%">
           					<span style="color: #3EA9CD; font-weight: 700;">방명록</span>
           					<span>[목록]</span>
           				</td>
           				<td width="50%">&nbsp;</td>
           			</tr>
           		</thead>
           		<tbody id="listGuestBody"></tbody>
           	</table>
           </div>
           
 	 </div>
    
</div>