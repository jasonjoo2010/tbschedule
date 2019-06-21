<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
rootPath: <input type="text" name="rootPath" value="${rootPath }" style="width:330px;" />
<input type="button" onclick="viewConfig();" value="View" />
<input type="button" onclick="saveConfig();" value="Export" />
<pre>
</pre>
<h3>
</h3>
<script>
function viewConfig() {
	$.getJSON('exportToJSON', function(data) {
	    if (data.errno == 0) {
	        $("pre").html(data.configData);
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