<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Job seeker sign in · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
</head>
<body class="professional-login-page">
  <main class="auth-layout">
    <aside class="auth-showcase seeker-showcase">
      <a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a>
      <p class="auth-kicker">YOUR NEXT CHAPTER STARTS HERE</p>
      <h1>Find work that moves you forward.</h1>
      <p class="auth-copy">Discover approved openings, keep your applications organized, and connect with recruiters when you apply.</p>
      <div class="auth-benefits">
        <div><span>01</span><p><strong>One clear job search</strong><small>Explore opportunities that match your goals.</small></p></div>
        <div><span>02</span><p><strong>Your career, in one place</strong><small>Manage your profile, resume, and applications.</small></p></div>
      </div>
      <div class="auth-footnote">A thoughtful place to take your next career step.</div>
    </aside>
    <section class="auth-card">
      <div class="auth-mobile-brand"><a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a></div>
      <p class="eyebrow">JOB SEEKER PORTAL</p>
      <h2>Welcome back</h2>
      <p class="auth-intro">Sign in to continue your job search.</p>
      <c:if test="${not empty error}"><div class="alert" role="alert"><c:out value="${error}"/></div></c:if>
      <c:if test="${param.reset=='1'}"><div class="success-message" role="status">Your password has been changed. You can sign in now.</div></c:if>
      <form class="auth-form" method="post" action="${pageContext.request.contextPath}/seeker/login">
        <input type="hidden" name="csrf" value="${csrf}">
        <label>Email address<input type="email" name="email" autocomplete="email" placeholder="you@example.com" required></label>
        <label>Password<input type="password" name="password" autocomplete="current-password" placeholder="Enter your password" required></label>
        <div class="auth-form-meta"><a href="${pageContext.request.contextPath}/forgot-password?type=seeker">Forgot password?</a></div>
        <button type="submit">Sign in <span aria-hidden="true">→</span></button>
      </form>
      <div class="auth-register"><span>New to Job.com?</span><a href="${pageContext.request.contextPath}/register?type=seeker">Create a job seeker account</a></div>
      <p class="auth-privacy">Your account details are protected and only used to manage your job search.</p>
    </section>
  </main>
</body>
</html>
