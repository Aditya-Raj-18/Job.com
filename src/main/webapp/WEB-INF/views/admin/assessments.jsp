<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Assessment PDFs · Job.com Admin</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
  <style>
    .assessment-page{max-width:1240px;margin:0 auto;padding:36px 28px}.assessment-page h1{margin:18px 0 8px}.assessment-lead{color:#61748a;margin:0 0 22px}.assessment-upload{background:#fff;border:1px solid #dce7e3;border-radius:18px;padding:24px;box-shadow:0 14px 34px #193b3210;margin-bottom:28px}.assessment-upload-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.assessment-upload label{display:grid;gap:7px;color:#344b60;font-weight:700}.assessment-upload input{font:inherit;padding:12px 14px;border:1px solid #cfded9;border-radius:10px;background:#fff}.assessment-upload input[type=file]{grid-column:1/-1}.assessment-upload .upload-file{grid-column:1/-1}.assessment-upload button{margin-top:18px;background:#127c72;color:#fff;border:0;border-radius:10px;padding:12px 18px;font:inherit;font-weight:750;cursor:pointer}.assessment-upload button:hover{background:#0d685f}.assessment-help{font-size:13px;color:#718398;margin:14px 0 0}.assessment-table{background:#fff;border:1px solid #dce7e3;border-radius:16px;overflow:hidden}.assessment-table table{width:100%;border-collapse:collapse}.assessment-table th,.assessment-table td{text-align:left;padding:14px 16px;border-bottom:1px solid #e5ece9}.assessment-table th{background:#f0f6f4;color:#476071;font-size:12px;text-transform:uppercase;letter-spacing:.08em}.assessment-table a{color:#08796f;font-weight:700}.assessment-table form{margin:0}.assessment-table button{border:0;border-radius:8px;padding:8px 12px;background:#fff0eb;color:#a4472b;font-weight:700;cursor:pointer}.category-pill{display:inline-block;padding:5px 9px;background:#edf6f3;border-radius:20px;color:#27655d;font-size:13px;font-weight:700}.assessment-alert{margin:14px 0;padding:13px 16px;border-radius:10px;background:#fff2ed;color:#9c442b;border:1px solid #ffd7ca}@media(max-width:700px){.assessment-page{padding:22px 14px}.assessment-upload-grid{grid-template-columns:1fr}.assessment-table{overflow-x:auto}.assessment-table table{min-width:740px}}
  </style>
</head>
<body>
<main class="main assessment-page">
  <p><a href="${pageContext.request.contextPath}/admin">← Admin dashboard</a></p>
  <p class="eyebrow">ADMIN CONSOLE <span>/</span> CAREER MATERIALS</p>
  <h1>Assessment PDFs</h1>
  <p class="assessment-lead">Upload an assignment or practice PDF and tag it with the company and job role it belongs to. Seekers will see it in the matching category.</p>
  <c:if test="${not empty error}"><div class="assessment-alert"><c:out value="${error}"/></div></c:if>
  <form class="assessment-upload" method="post" enctype="multipart/form-data">
    <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrf}'/>">
    <div class="assessment-upload-grid">
      <label>Company
        <input name="company" list="assessment-companies" maxlength="120" placeholder="e.g. Google" required>
        <datalist id="assessment-companies"><option value="Google"><option value="Amazon"><option value="Microsoft"><option value="Meta"><option value="Apple"><option value="Adobe"><option value="NVIDIA"><option value="Netflix"></datalist>
      </label>
      <label>Job role / placement
        <input name="role" list="assessment-roles" maxlength="120" placeholder="e.g. Software Engineer" required>
        <datalist id="assessment-roles"><option value="Software Engineer"><option value="Data Analyst"><option value="Product Manager"><option value="UX Designer"><option value="DevOps Engineer"><option value="QA Engineer"></datalist>
      </label>
      <label>PDF title
        <input name="title" maxlength="180" placeholder="e.g. Software Engineering Practice Assignment" required>
      </label>
      <label class="upload-file">PDF file (maximum 5 MB)
        <input type="file" name="file" accept="application/pdf,.pdf" required>
      </label>
    </div>
    <button type="submit">Upload PDF</button>
    <p class="assessment-help">Choose a suggestion or type a different company and job role. These categories help seekers find the right PDF.</p>
  </form>
  <section class="assessment-table" aria-label="Uploaded assessment PDFs">
    <table><thead><tr><th>Company</th><th>Job role</th><th>PDF title</th><th>Uploaded</th><th>File</th><th>Action</th></tr></thead>
      <tbody>
        <c:forEach items="${rows}" var="r"><tr>
          <td><span class="category-pill"><c:out value="${r.company}"/></span></td>
          <td><c:out value="${r.role}"/></td>
          <td><strong><c:out value="${r.title}"/></strong></td>
          <td><c:out value="${r.created_at}"/></td>
          <td><a href="${pageContext.request.contextPath}/admin/assessments?download=<c:out value='${r.id}'/>">Download</a></td>
          <td><c:if test="${r.active}"><form method="post"><input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrf}'/>"><input type="hidden" name="disable" value="<c:out value='${r.id}'/>"><button type="submit">Disable</button></form></c:if><c:if test="${not r.active}"><span>Disabled</span></c:if></td>
        </tr></c:forEach>
        <c:if test="${empty rows}"><tr><td colspan="6">No assessment PDFs have been uploaded yet.</td></tr></c:if>
      </tbody>
    </table>
  </section>
</main>
</body>
</html>
