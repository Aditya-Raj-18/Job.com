<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title><c:out value="${pageTitle}"/> · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
</head>
<body>
<div class="shell seeker-shell seeker-shell-no-sidebar">
  <main class="main seeker-main">
    <%@ include file="account-menu.jspf" %>
    <c:choose>
      <c:when test="${section=='dashboard'}">
        <header class="page-header seeker-dashboard-heading">
          <div><p class="eyebrow">JOB.COM <span>/</span> CAREER CENTER</p><h1>Your career dashboard</h1><p class="page-subtitle">Find opportunities, keep your profile ready, and stay on top of every application.</p></div>
        </header>
      </c:when>
      <c:otherwise>
        <header class="page-header">
          <div><p class="eyebrow">JOB.COM <span>/</span> CAREERS</p><h1><c:out value="${pageTitle}"/></h1><p class="page-subtitle"><c:out value="${pageSubtitle}"/></p></div>
          <div class="signed-in"><span class="online-dot"></span>Signed in as <strong><c:out value="${sessionScope.seekerName}"/></strong></div>
        </header>
      </c:otherwise>
    </c:choose>

    <c:if test="${not empty error}"><div class="alert"><c:out value="${error}"/></div></c:if>
    <c:if test="${not empty flash}"><div class="${flashError?'alert':'success-message'}"><c:out value="${flash}"/></div></c:if>

    <c:choose>
      <c:when test="${section=='dashboard'}">
        <section class="seeker-home-grid" aria-label="Career dashboard overview">
          <div class="seeker-home-profile-column">
            <article class="seeker-home-card seeker-profile-summary">
              <div class="seeker-profile-ring" style="--profile-progress:${profileCompletion}%">
                <c:choose><c:when test="${not empty profile.profile_photo_path}"><img src="${pageContext.request.contextPath}/seeker/profile-photo" alt="Profile photo"></c:when><c:otherwise><span>J</span></c:otherwise></c:choose>
                <small><c:out value="${profileCompletion}"/>%</small>
              </div>
              <h2><c:choose><c:when test="${not empty profile.full_name}"><c:out value="${profile.full_name}"/></c:when><c:otherwise><c:out value="${sessionScope.seekerName}"/></c:otherwise></c:choose></h2>
              <p class="seeker-profile-headline"><c:choose><c:when test="${not empty profile.profile_headline}"><c:out value="${profile.profile_headline}"/></c:when><c:otherwise>Add a professional headline to introduce yourself.</c:otherwise></c:choose></p>
              <p class="seeker-profile-qualification"><c:if test="${not empty profile.qualification}"><c:out value="${profile.qualification}"/></c:if><c:if test="${not empty profile.city}"><c:if test="${not empty profile.qualification}"> · </c:if><c:out value="${profile.city}"/></c:if></p>
              <a class="seeker-profile-cta" href="${pageContext.request.contextPath}/seeker?section=profile" aria-label="View and update your profile"><span class="seeker-profile-cta-label"><c:choose><c:when test="${profileCompletion lt 100}">Complete profile</c:when><c:otherwise>View profile</c:otherwise></c:choose></span><span class="seeker-profile-cta-arrow" aria-hidden="true">&#x2192;</span></a>
              <div class="seeker-profile-performance"><h3>Profile performance</h3><div class="seeker-performance-stats"><a href="${pageContext.request.contextPath}/seeker?section=applications"><strong><c:out value="${applicationCount}"/></strong><span>Applications</span></a><c:choose><c:when test="${hasChats}"><a href="${pageContext.request.contextPath}/chat"><strong><c:out value="${chatCount}"/></strong><span>Conversations</span></a></c:when><c:otherwise><a href="${pageContext.request.contextPath}/seeker/jobs"><strong><c:out value="${openJobCount}"/></strong><span>Open jobs</span></a></c:otherwise></c:choose></div><a class="seeker-performance-link" href="${pageContext.request.contextPath}/seeker?section=profile">Improve your profile <span>›</span></a></div>
            </article>
          </div>

          <div class="seeker-home-center-column">
            <section class="seeker-career-banner" data-photo-slideshow aria-label="Career inspiration">
              <span class="seeker-banner-photo is-active" style="--slide-image:url('https://images.unsplash.com/photo-1521737711867-e3b97375f902?auto=format&amp;fit=crop&amp;w=1800&amp;q=85')" aria-hidden="true"></span>
              <span class="seeker-banner-photo" style="--slide-image:url('https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&amp;fit=crop&amp;w=1800&amp;q=85')" aria-hidden="true"></span>
              <span class="seeker-banner-photo" style="--slide-image:url('https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&amp;fit=crop&amp;w=1800&amp;q=85')" aria-hidden="true"></span>
              <div><p class="welcome-kicker">YOUR NEXT MOVE</p><h2>Find a role that moves you forward.</h2><p>Explore approved openings and bring your experience to the right team.</p><div class="seeker-banner-actions"><a href="${pageContext.request.contextPath}/seeker/jobs">Explore jobs</a><a href="${pageContext.request.contextPath}/resume">Build your resume</a></div></div>
              <div class="seeker-banner-illustration" aria-hidden="true">
                <svg viewBox="0 0 220 180" role="presentation">
                  <ellipse cx="116" cy="160" rx="78" ry="10" fill="#092f2b" opacity=".28"/>
                  <g class="seeker-illustration-card">
                    <rect x="88" y="19" width="112" height="121" rx="15" fill="#f9fffc"/>
                    <rect x="103" y="36" width="38" height="7" rx="3.5" fill="#75cbb0"/>
                    <rect x="103" y="51" width="78" height="5" rx="2.5" fill="#d4e9df"/>
                    <rect x="103" y="62" width="67" height="5" rx="2.5" fill="#d4e9df"/>
                    <rect x="103" y="83" width="82" height="37" rx="9" fill="#e5f5ed"/>
                    <rect x="113" y="96" width="26" height="6" rx="3" fill="#258f78"/>
                    <rect x="113" y="107" width="52" height="4" rx="2" fill="#a6d9c1"/>
                  </g>
                  <g class="seeker-illustration-person">
                    <circle cx="61" cy="76" r="17" fill="#ffc899"/>
                    <path d="M45 73c1-13 8-20 18-20 9 0 15 6 16 16-5-3-9-7-12-12-5 7-12 12-22 16Z" fill="#173e3b"/>
                    <path d="M30 146c2-31 12-48 31-48s30 17 32 48H30Z" fill="#d9f0e6"/>
                    <path d="m53 102 8 10 8-10 7 44H46l7-44Z" fill="#238b75"/>
                    <path d="m57 114 4 5 4-5-4 18-4-18Z" fill="#f7fffb"/>
                  </g>
                  <g class="seeker-illustration-spark seeker-illustration-spark-one" fill="#9ce6cd"><path d="M42 32v11m-5.5-5.5h11" stroke="#9ce6cd" stroke-width="3" stroke-linecap="round"/></g>
                  <g class="seeker-illustration-spark seeker-illustration-spark-two" fill="#ffe3a0"><path d="M193 149v8m-4-4h8" stroke="#ffe3a0" stroke-width="2.5" stroke-linecap="round"/></g>
                  <circle class="seeker-illustration-orbit" cx="185" cy="25" r="6" fill="#60d1ad"/>
                </svg>
              </div>
            </section>

            <section class="seeker-recommendations-card">
              <div class="seeker-home-section-heading"><div><p class="eyebrow">CURATED FOR YOU</p><h2>Jobs matched to your field</h2><small class="seeker-match-caption">Showing approved <c:out value="${careerField}"/> roles, newest first</small></div><a href="${pageContext.request.contextPath}/seeker/jobs">View all <span>→</span></a></div>
              <div class="seeker-recommended-jobs">
                <c:forEach items="${recommendedJobs}" var="job">
                  <c:url var="recommendedJobUrl" value="/seeker/jobs"><c:param name="q" value="${job.title}"/></c:url>
                  <article class="seeker-recommended-job"><div class="seeker-company-mark" aria-hidden="true">J<span>.</span></div><p class="seeker-job-company"><c:out value="${job.company}"/></p><h3><c:out value="${job.title}"/></h3><div class="seeker-job-tags"><c:if test="${not empty job.jobType}"><span><c:out value="${job.jobType}"/></span></c:if><span><c:out value="${not empty job.packageCtc ? job.packageCtc : 'Compensation not specified'}"/></span></div><a href="${recommendedJobUrl}">View job <span>→</span></a></article>
                </c:forEach>
                <c:if test="${empty recommendedJobs}"><div class="seeker-no-recommendations"><span>&#x2315;</span><strong>No matching openings yet</strong><small>New approved jobs in <c:out value="${careerField}"/> will appear here automatically. Check that your career field is set correctly in your profile.</small><a href="${pageContext.request.contextPath}/seeker?section=profile">Update career field</a></div></c:if>
              </div>
            </section>
          </div>

          <aside class="seeker-home-right-column">
            <section class="seeker-next-steps-card"><p class="eyebrow">NEXT STEPS</p><h2>Keep your search moving</h2>
              <a class="seeker-journey-step" href="${pageContext.request.contextPath}/seeker?section=profile"><span class="seeker-journey-icon">✓</span><span><strong>Complete your profile</strong><small>Help recruiters understand your strengths</small></span><b><c:out value="${profileCompletion}"/>%</b></a>
              <a class="seeker-journey-step" href="${pageContext.request.contextPath}/seeker/jobs"><span class="seeker-journey-icon">⌕</span><span><strong>Explore open roles</strong><small>Find an opportunity that fits</small></span><b><c:out value="${openJobCount}"/></b></a>
              <a class="seeker-journey-step" href="${pageContext.request.contextPath}/seeker?section=applications"><span class="seeker-journey-icon">▤</span><span><strong>Track applications</strong><small>Follow updates from recruiters</small></span><b><c:out value="${applicationCount}"/></b></a>
            </section>
            <section class="seeker-career-tools-card"><p class="eyebrow">CAREER TOOLS</p><h2>Build your next step</h2>
              <a href="${pageContext.request.contextPath}/seeker/resources?type=preparation"><span class="seeker-tool-icon">✦</span><strong>Preparation</strong><span>→</span></a>
              <a href="${pageContext.request.contextPath}/seeker/resources?type=assessments"><span class="seeker-tool-icon">▤</span><strong>Assessment PDFs</strong><span>→</span></a>
              <a href="${pageContext.request.contextPath}/support"><span class="seeker-tool-icon">?</span><strong>Contact support</strong><span>→</span></a>
            </section>
          </aside>
        </section>

        <section class="seeker-section">
          <div class="section-heading"><div><p class="eyebrow">ACTIVITY</p><h2>Recent applications</h2></div><a href="${pageContext.request.contextPath}/seeker?section=applications">View all</a></div>
          <div class="table-wrap"><table class="seeker-table"><thead><tr><th>Role</th><th>Company</th><th>Applied</th><th>Status</th><th>Chat</th></tr></thead><tbody>
            <c:forEach items="${recentApplications}" var="application"><tr><td><strong><c:out value="${application.title}"/></strong><small><c:out value="${application.jobType}"/></small></td><td><c:out value="${application.company}"/></td><td><c:out value="${application.appliedAt}"/></td><td><span class="status-pill"><c:out value="${application.status}"/></span></td><td><a href="${pageContext.request.contextPath}/chat?applicationId=${application.id}">Open chat</a></td></tr></c:forEach>
            <c:if test="${empty recentApplications}"><tr><td colspan="5"><div class="empty-state"><strong>No applications yet</strong><small>Apply to an opening to see it here and start a recruiter chat.</small></div></td></tr></c:if>
          </tbody></table></div>
        </section>

      </c:when>

      <c:when test="${section=='applications'}">
        <section class="review-note"><span class="review-note-icon">i</span><span>Each application has a private chat with its recruiter. Your chat is available only to the people connected to that job application.</span></section>
        <div class="table-wrap"><table class="seeker-table"><thead><tr><th>Role</th><th>Company</th><th>Job type</th><th>Package / CTC</th><th>Applied</th><th>Status</th><th>Conversation</th></tr></thead><tbody>
          <c:forEach items="${applications}" var="application"><tr><td><strong><c:out value="${application.title}"/></strong></td><td><c:out value="${application.company}"/></td><td><c:out value="${application.jobType}"/></td><td><c:out value="${not empty application.packageCtc ? application.packageCtc : 'Not specified'}"/></td><td><c:out value="${application.appliedAt}"/></td><td><span class="status-pill"><c:out value="${application.status}"/></span></td><td><a class="seeker-button secondary" href="${pageContext.request.contextPath}/chat?applicationId=${application.id}">Open chat</a></td></tr></c:forEach>
          <c:if test="${empty applications}"><tr><td colspan="7"><div class="empty-state"><span>▤</span><strong>You have not applied to a job yet</strong><small>Browse openings and use Apply now to start tracking applications here.</small><a href="${pageContext.request.contextPath}/seeker/jobs">Find jobs</a></div></td></tr></c:if>
        </tbody></table></div>
      </c:when>

      <c:when test="${section=='notifications'}">
        <section class="content-card seeker-profile-card">
          <div class="section-heading"><div><p class="eyebrow">UPDATES</p><h2>Job.com announcements</h2></div><c:if test="${unreadNotificationCount gt 0}"><form method="post" action="${pageContext.request.contextPath}/seeker?section=notifications"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="mark-notifications-read"><button class="seeker-button secondary" type="submit">Mark all as read</button></form></c:if></div>
          <div class="notification-list"><c:forEach items="${announcements}" var="announcement"><article class="notification-card ${announcement.isRead?'':'notification-unread'}"><div class="notification-card-top"><h3><c:out value="${announcement.name}"/></h3><span class="status-pill"><c:out value="${announcement.email}"/></span></div><p><c:out value="${announcement.body}"/></p><small><c:out value="${announcement.created_at}"/></small><c:if test="${not empty announcement.jobId}"><c:url var="announcementJobUrl" value="/seeker/jobs"><c:param name="jobId" value="${announcement.jobId}"/></c:url><a class="seeker-button secondary" href="${announcementJobUrl}">View matching job</a></c:if></article></c:forEach>
            <c:if test="${empty announcements}"><div class="empty-state"><strong>No announcements yet</strong><small>Updates from Job.com will appear here.</small></div></c:if>
          </div>
        </section>
      </c:when>

      <c:otherwise>
        <section class="content-card seeker-profile-card">
          <div class="section-heading"><div><p class="eyebrow">CAREER PROFILE</p><h2>Your profile details</h2></div><span class="status-pill"><c:out value="${profileCompletion}"/>% complete</span></div>
          <p class="form-hint">These details help recruiters understand your experience. Your government ID information stays in the private verification record.</p>
          <section class="seeker-photo-panel" aria-label="Profile photo">
            <c:choose><c:when test="${not empty profile.profile_photo_path}"><img class="seeker-profile-photo" src="${pageContext.request.contextPath}/seeker/profile-photo" alt="Your profile photo"></c:when><c:otherwise><div class="seeker-profile-photo seeker-profile-photo-empty" aria-hidden="true">J</div></c:otherwise></c:choose>
            <div class="seeker-photo-content"><h3>Profile photo</h3><p>Upload or replace the photo shown on your seeker profile. JPG and PNG files up to 5 MB are supported.</p>
              <form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/seeker?section=profile" class="seeker-photo-form"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="upload-profile-photo"><label>Choose a photo<input type="file" name="profilePhoto" accept="image/jpeg,image/png" required></label><button type="submit">Upload photo</button></form>
            </div>
          </section>
          <form method="post" action="${pageContext.request.contextPath}/seeker?section=profile" class="seeker-profile-form">
            <input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="save-profile">
            <label>Full name<input name="full_name" maxlength="160" value='<c:out value="${profile.full_name}"/>' required></label>
            <label>Email<input type="email" value='<c:out value="${profile.email}"/>' readonly><small>Contact support if you need to change your sign-in email.</small></label>
            <label>Phone<input name="phone" maxlength="40" value='<c:out value="${profile.phone}"/>'></label>
            <label>Career field<select name="career_field" required><c:forEach items="${careerFields}" var="field"><option value="${field}" ${profile.career_field==field?'selected':''}><c:out value="${field}"/></option></c:forEach></select><small>We use this to show you relevant openings on your dashboard and job feed.</small></label>
            <label>Professional headline<input name="profile_headline" maxlength="220" value='<c:out value="${profile.profile_headline}"/>' placeholder="e.g. Java developer seeking backend roles"></label>
            <label>Highest qualification<input name="qualification" maxlength="180" value='<c:out value="${profile.qualification}"/>'></label>
            <label>Experience in years<input type="number" name="experience_years" min="0" max="99.99" step="0.01" value='<c:out value="${profile.experience_years}"/>'></label>
            <label>City<input name="city" maxlength="100" value='<c:out value="${profile.city}"/>'></label>
            <label>State<input name="state" maxlength="100" value='<c:out value="${profile.state}"/>'></label>
            <label class="seeker-field-wide">Address<input name="address" maxlength="300" value='<c:out value="${profile.address}"/>'></label>
            <label>Postal code<input name="postal_code" maxlength="20" value='<c:out value="${profile.postal_code}"/>'></label>
            <label>LinkedIn URL<input type="url" name="linkedin_url" maxlength="500" value='<c:out value="${profile.linkedin_url}"/>' placeholder="https://www.linkedin.com/in/..."></label>
            <label>Portfolio URL<input type="url" name="portfolio_url" maxlength="500" value='<c:out value="${profile.portfolio_url}"/>' placeholder="https://..."></label>
            <label class="seeker-field-wide">Skills<textarea name="skills" rows="3" maxlength="4000" placeholder="Java, SQL, communication"><c:out value="${profile.skills}"/></textarea></label>
            <label class="seeker-field-wide">Professional summary<textarea name="profile_summary" rows="6" maxlength="5000" placeholder="Summarize your experience, strengths, and career goals"><c:out value="${profile.profile_summary}"/></textarea></label>
            <div class="seeker-field-wide seeker-form-actions"><button type="submit">Save profile</button><a class="seeker-button secondary" href="${pageContext.request.contextPath}/resume">Continue to resume builder</a></div>
          </form>
        </section>
      </c:otherwise>
    </c:choose>
  </main>
</div>
</body>
</html>
