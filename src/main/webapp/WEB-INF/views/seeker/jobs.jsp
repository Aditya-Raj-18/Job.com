<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Find jobs · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
  <style>
    .job-board{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:18px}
    .job-card{background:#fff;border:1px solid #dfe5ef;border-radius:16px;padding:22px;box-shadow:0 12px 28px #1d33520a}
    .job-card h2{margin:5px 0 8px}.job-card p{white-space:pre-wrap;color:#56657b}
    .job-meta{display:flex;flex-wrap:wrap;gap:8px;margin:12px 0}
    .job-meta span{background:#eef4f7;color:#246a63;border-radius:999px;padding:6px 10px;font-size:13px}
    .job-actions{display:flex;gap:10px;align-items:center;margin-top:16px}
    .job-actions a,.job-actions button{border:0;border-radius:8px;padding:10px 14px;background:#127c72;color:#fff;text-decoration:none;font-weight:700;cursor:pointer}
    .job-actions .secondary{background:#eaf5f1;color:#126c62}.job-empty{grid-column:1/-1}
    .job-search{display:flex;gap:10px;flex-wrap:wrap;margin:0 0 18px;padding:15px;background:#fff;border:1px solid #dce7e3;border-radius:12px}
    .job-search input,.job-search select{padding:10px 12px;border:1px solid #d7e3de;border-radius:8px;font:inherit;color:#203735}
    .job-search input{flex:1;min-width:220px}.job-search button{padding:10px 16px}
    .apply-panel{margin-top:16px}.apply-panel summary{display:inline-flex;list-style:none;border:0;border-radius:8px;padding:10px 14px;background:#127c72;color:#fff;font-weight:700;cursor:pointer}
    .apply-panel summary::-webkit-details-marker{display:none}.apply-form{margin-top:14px;padding:16px;border:1px solid #dce7e3;border-radius:12px;background:#f8fbfa}
    .apply-contact-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin:12px 0}
    .apply-contact-grid label,.apply-resume-label{display:grid;gap:6px;color:#56657b;font-size:13px;font-weight:700}
    .apply-contact-grid input{width:100%;box-sizing:border-box;padding:10px 12px;border:1px solid #d7e3de;border-radius:8px;background:#edf3f1;color:#203735;font:inherit}
    .apply-form button{border:0;border-radius:8px;padding:10px 14px;background:#127c72;color:#fff;font-weight:700;cursor:pointer}
    .apply-resume-label{margin:12px 0}
  </style>
</head>
<body>
<div class="shell seeker-shell seeker-shell-no-sidebar">
  <main class="main seeker-main">
    <%@ include file="account-menu.jspf" %>
    <header class="page-header"><div><p class="eyebrow">JOB.COM <span>/</span> CAREERS</p><h1>Find a job</h1><p class="page-subtitle">Search approved openings, compare compensation, and apply to start a private conversation.</p></div><div class="signed-in"><span class="online-dot"></span>Signed in as <strong><c:out value="${sessionScope.seekerName}"/></strong></div></header>
    <section class="review-note seeker-field-filter-note"><span class="review-note-icon">✓</span><span>Showing approved openings matched to <strong><c:out value="${careerField}"/></strong>. Update your career field in <a href="${pageContext.request.contextPath}/seeker?section=profile">My profile</a> if this is incorrect.</span></section>
    <c:if test="${not empty error}"><div class="alert"><c:out value="${error}"/></div></c:if>
    <c:if test="${not empty sessionScope.seekerJobsFlash}"><div class="${sessionScope.seekerJobsFlashError ? 'alert' : 'success-message'}"><c:out value="${sessionScope.seekerJobsFlash}"/></div><c:remove var="seekerJobsFlash" scope="session"/><c:remove var="seekerJobsFlashError" scope="session"/></c:if>
    <form class="job-search" method="get" action="${pageContext.request.contextPath}/seeker/jobs">
      <input type="search" name="q" value='<c:out value="${searchQuery}"/>' placeholder="Search job title, company, or skills" aria-label="Search jobs">
      <select name="type" aria-label="Filter by job type"><option value="">All job types</option><option value="Full-time" ${selectedJobType=='Full-time'?'selected':''}>Full-time</option><option value="Internship" ${selectedJobType=='Internship'?'selected':''}>Internship</option></select>
      <button type="submit">Search jobs</button>
    </form>
    <section class="job-board" aria-label="Approved job openings">
      <c:forEach items="${jobs}" var="job">
        <article class="job-card">
          <p class="eyebrow"><c:out value="${job.company}"/></p><h2><c:out value="${job.title}"/></h2>
          <div class="job-meta"><span><c:out value="${job.careerField}"/></span><span><c:out value="${not empty job.jobType ? job.jobType : 'Job'}"/></span><span><c:out value="${not empty job.packageCtc ? job.packageCtc : 'Compensation not specified'}"/></span></div>
          <p><c:out value="${job.description}"/></p>
          <div class="job-actions"><c:choose>
            <c:when test="${job.applied}"><span class="status-pill">Application submitted</span><a class="secondary" href="${pageContext.request.contextPath}/seeker?section=applications">View application</a></c:when>
            <c:otherwise>
              <c:choose>
                <c:when test="${not applicationReady}"><p class="alert">Add your name, email, and phone in <a href="${pageContext.request.contextPath}/seeker?section=profile">My profile</a> before applying.</p></c:when>
                <c:otherwise><details class="apply-panel"><summary>Apply now</summary><form class="apply-form" method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/seeker/jobs"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="jobId" value="${job.id}"><strong>Application contact details from your profile</strong><div class="apply-contact-grid"><label>Full name<input type="text" value="<c:out value='${profile.full_name}'/>" readonly></label><label>Email<input type="email" value="<c:out value='${profile.email}'/>" readonly></label><label>Phone<input type="tel" value="<c:out value='${profile.phone}'/>" readonly></label></div><label class="apply-resume-label">Resume (PDF, up to 5 MB)<input type="file" name="resume" accept=".pdf,application/pdf" required></label><button type="submit">Submit application</button></form></details></c:otherwise>
              </c:choose>
            </c:otherwise>
          </c:choose></div>
        </article>
      </c:forEach>
      <c:if test="${empty jobs}"><article class="content-card job-empty"><div class="empty-state"><span>▤</span><strong>No matching approved jobs</strong><small>Try another title, company, or job type. New openings appear here after admin approval.</small></div></article></c:if>
    </section>
  </main>
</div>
</body>
</html>
