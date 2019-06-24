<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<c:if test="${errorMessage != null }">
<div style="border: 1px solid red; width: 500px;">
当前连接状态消息：${errorMessage }
</div>
</c:if>
<h1>基础信息配置</h1>
<form id="configForm" method="get" name="configForm" action="save">
<table class="noborder">
<tr>
	<td>Zookeeper地址:</td>
	<td><input type="text" name="address"  value="${address }" style="width:300"></td>
	<td>格式: IP地址：端口</td>
</tr>
<tr>
	<td>Timeout:</td>
	<td><input type="text" name="timeout" value="${timeout }" style="width:300"></td>
	<td>单位毫秒</td>
</tr>
<tr>
	<td>rootPath：</td>
	<td><input type="text" name="rootPath" value="${rootPath }" style="width:300"></td>
	<td>例如：/taobao-pamirs-schedule/huijin,，可以是一级目录，也可以是多级目录，注意不同调度域间不能有父子关系<br/>
	    通过切换此属性来实现多个调度域的管理
	</td>
</tr>
<tr>
	<td>Username：</td>
	<td><input type="text" name="userName" value="${userName }" style="width:300"></td>
	<td></td>
</tr>
<tr>
	<td>Password：</td>
	<td><input type="text" name="password" value="${password }" style="width:300"	></td>
	<td></td>
</tr>
</table>
<br/>
<input type="submit" value="Save" style="width:100px" >
<a href="index?manager=true">MANAGEMENT INDEX...</a>
<br/><br/>

<c:if test="isInitial">
<b>You are here possibly because:</b><br>
&nbsp;&nbsp;&nbsp;&nbsp; 1. This is the initial time.<br>
&nbsp;&nbsp;&nbsp;&nbsp; 2. The old configuration has been cleared.<br>
&nbsp;&nbsp;&nbsp;&nbsp; 3. The storage can not be connected.<br>
</c:if>

</form>
<script>
$(function() {
    $("#configForm").ajaxForm({
        dataType: 'json',
        success: function(data) {
            if (data.errno == 0) {
                setTimeout(function() {
                	document.location.href = '/strategy/index?manager=true';
                }, 1000);
            } else {
                alert(data.errdesc);
            }
        }
    });
})
</script>
<jsp:include page="../footer.jsp"/>