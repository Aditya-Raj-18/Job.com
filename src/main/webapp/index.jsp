<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Job.com · Find your next opportunity</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
  <style>
    body{background:radial-gradient(ellipse at 82% 3%,#dcefe7 0,transparent 34%),#f4f7f5;color:#203735}
    .landing{max-width:1180px;margin:auto;padding:24px clamp(18px,5vw,58px) 56px}
    .landing-header{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:8px 0 26px}
    .landing-header .brand{padding:0;margin:0;color:#163433;font-size:29px}
    .landing-links{display:flex;gap:18px;align-items:center;flex-wrap:wrap}
    .landing-links a{font-size:12px;font-weight:700;text-decoration:none}
    .landing-hero{padding:clamp(30px,7vw,76px);border-radius:26px;background:linear-gradient(115deg,#153d3b,#16766c 72%,#28937e);color:#fff;box-shadow:0 22px 60px rgba(20,83,74,.17)}
    .landing-hero .eyebrow{color:#b3d7cc}.landing-hero h1{max-width:700px;color:#fff;font-size:clamp(34px,6vw,62px);line-height:1.04;letter-spacing:-1.8px;text-transform:none}
    .landing-hero p{max-width:590px;color:#d5ebe4;font-size:16px;line-height:1.6;margin:16px 0 26px}
    .landing-cta{display:flex;gap:11px;flex-wrap:wrap}.landing-cta a{display:inline-flex;padding:12px 17px;border-radius:9px;background:#fff;color:#17685e!important;text-decoration:none;font-size:12px;font-weight:750}.landing-cta a.secondary{border:1px solid #ffffff55;background:#ffffff12;color:#fff!important}
    .portal-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:15px;margin-top:20px}
    .portal-card{display:block;padding:22px;background:#fff;border:1px solid #dce7e3;border-radius:16px;text-decoration:none;box-shadow:0 12px 28px rgba(22,52,51,.06)}
    .portal-card h2{margin:0 0 7px;color:#203735;font-size:18px}.portal-card p{margin:0;color:#70827e;font-size:12px;line-height:1.6}.portal-card span{display:block;margin-top:16px;color:#14786e;font-size:11px;font-weight:750}
    .landing-footer{display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-top:25px;color:#84938e;font-size:10px}
    @media(max-width:760px){.landing-header{align-items:flex-start;flex-direction:column}.portal-grid{grid-template-columns:1fr}.landing-hero{border-radius:18px}.landing-hero p{font-size:14px}}
  </style>
</head>
<body>
<main class="landing">
  <header class="landing-header"><a class="brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a><nav class="landing-links" aria-label="Sign in"><a href="${pageContext.request.contextPath}/seeker/login">Job seeker</a><a href="${pageContext.request.contextPath}/recruiter/login">Recruiter / company</a><a href="${pageContext.request.contextPath}/admin/login">Administrator</a></nav></header>
  <section class="landing-hero"><p class="eyebrow">CAREERS START HERE</p><h1>Find work that moves you forward.</h1><p>Discover approved openings, build an ATS-ready resume, and stay connected with recruiters throughout your application.</p><div class="landing-cta"><a href="${pageContext.request.contextPath}/seeker/login">Sign in as a job seeker</a><a class="secondary" href="${pageContext.request.contextPath}/register?type=seeker">Create a seeker account</a></div></section>
  <section class="portal-grid" aria-label="Job.com portals"><a class="portal-card" href="${pageContext.request.contextPath}/seeker/login"><h2>Job seekers</h2><p>Browse openings, manage your applications, chat with recruiters, and create an ATS resume.</p><span>Open seeker portal →</span></a><a class="portal-card" href="${pageContext.request.contextPath}/recruiter/login"><h2>Recruiters and companies</h2><p>Manage your company profile, post openings for review, and follow up with applicants.</p><span>Open company portal →</span></a><a class="portal-card" href="${pageContext.request.contextPath}/admin/login"><h2>Administrator</h2><p>Review account requests and job listings, manage templates, and monitor the portal.</p><span>Open admin console →</span></a></section>
  <footer class="landing-footer"><span>Job.com · Career connections made easier</span><span>Accounts and job posts are reviewed before publication.</span></footer>
</main>
</body>
</html>
