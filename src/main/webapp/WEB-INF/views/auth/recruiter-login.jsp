<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Recruiter sign in · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
</head>
<body class="professional-login-page">
  <main class="auth-layout">
    <aside class="auth-showcase recruiter-showcase">
      <a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a>
      <p class="auth-kicker">RECRUITER WORKSPACE</p>
      <h1>Build the team that builds what’s next.</h1>
      <p class="auth-copy">Bring your hiring workflow together, from company details to job listings and applicant conversations.</p>
      <div class="auth-benefits">
        <div><span>01</span><p><strong>Make every opening count</strong><small>Post roles and follow each listing through review.</small></p></div>
        <div><span>02</span><p><strong>Stay close to candidates</strong><small>Manage applications and conversations in one workspace.</small></p></div>
      </div>
      <div class="auth-footnote">A better hiring experience starts with a clear workspace.</div>
    </aside>
    <section class="auth-card">
      <div class="auth-mobile-brand"><a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a></div>
      <p class="eyebrow">RECRUITER / COMPANY PORTAL</p>
      <h2>Welcome back</h2>
      <p class="auth-intro">Sign in to manage your company and hiring activity.</p>
      <c:if test="${not empty error}"><div class="alert" role="alert"><c:out value="${error}"/></div></c:if>
      <c:if test="${param.loggedOut=='1'}"><div class="success-message" role="status">You have signed out.</div></c:if>
      <c:if test="${param.reset=='1'}"><div class="success-message" role="status">Your password has been changed. You can sign in now.</div></c:if>
      <form class="auth-form" method="post" action="${pageContext.request.contextPath}/recruiter/login">
        <input type="hidden" name="csrf" value="${csrf}">
        <label>Company email<input type="email" name="email" autocomplete="username" placeholder="name@company.com" required></label>
        <label>Password<input type="password" name="password" autocomplete="current-password" placeholder="Enter your password" required></label>
        <div class="auth-form-meta"><a href="${pageContext.request.contextPath}/forgot-password?type=recruiter">Forgot password?</a></div>
        <button type="submit">Sign in <span aria-hidden="true">→</span></button>
      </form>
      <div class="auth-register"><span>New company on Job.com?</span><a href="${pageContext.request.contextPath}/register?type=recruiter">Register your company</a></div>
      <p class="auth-privacy">Only approved company accounts can access the recruiter workspace.</p>
    </section>
  </main>
</body>
</html>
