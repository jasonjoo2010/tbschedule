<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<input type="button" onclick="viewConfig();" value="View" />
<input type="button" onclick="saveConfig();" value="Export" />
<h2>TASKS：</h2>
<pre id="tasks"></pre>
<h2>STRATEGIES：</h2>
<pre id="strategies"></pre>
<h3 style="color: red"></h3>
<script type="text/javascript">
function viewConfig() {
	$.getJSON('exportToJSON', function(data) {
	    if (data.errno == 0) {
	        var html = '';
	        if (data.taskList) {
	            for (var i = 0; i < data.taskList.length; i ++) {
	                html += 'Task: ' + data.taskList[i].baseTaskType;
	                html += JSON.stringify(data.taskList[i]);
	            }
	        }
	        $("#tasks").html(html);
	        html = '';
	        if (data.strategyList) {
	            for (var i = 0; i < data.strategyList.length; i ++) {
	                html += 'Strategy: ' + data.strategyList[i].strategyName;
	                html += JSON.stringify(data.strategyList[i]);
	            }
	        }
	        $("#strategies").html(html);
	        $("h3").html('');
	    } else {
	        $("pre").html('');
	        $("h3").html(data.errdesc);
	    }
	});
}
function saveConfig() {
	window.open('exportToJSON?download=true');
}
</script>
<jsp:include page="../footer.jsp"/>