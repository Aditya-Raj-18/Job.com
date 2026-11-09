<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Resume Builder · Job.com</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
<style>
.template-picker{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin:18px 0 22px}
.template-option{display:flex;flex-direction:column;align-items:flex-start;gap:8px;min-height:164px;padding:15px;text-decoration:none;color:#18334b;background:#fff;border:1px solid #d8e5e2;border-radius:13px;transition:transform .16s,border-color .16s,box-shadow .16s}
.template-option:hover{transform:translateY(-2px);border-color:#168476;box-shadow:0 9px 22px #123d3214}
.template-option.is-selected{border:2px solid #11796e;box-shadow:0 0 0 3px #d9f1e9}
.template-option strong{font-size:15px}.template-option p{margin:0;color:#64758a;font-size:12px;line-height:1.45}
.template-tag{padding:4px 8px;background:#eef5f3;color:#147a70;border-radius:999px;font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.05em}
.template-action{margin-top:auto;color:#087c71;font-size:12px;font-weight:700}
.template-swatch{height:36px;width:56px;border:1px solid #dce5e2;border-radius:5px;background:linear-gradient(#163d3a 0 5px,#fff 5px 100%);position:relative}
.template-swatch:before,.template-swatch:after{content:"";position:absolute;left:7px;right:7px;height:3px;background:#dce5e2;border-radius:3px}
.template-swatch:before{top:13px}.template-swatch:after{top:21px;right:17px}
.template-swatch.modern,.template-swatch.ocean{background:linear-gradient(90deg,#eaf1ff 0 16px,#fff 16px 100%);border-color:#bfd0f2}
.template-swatch.modern:before,.template-swatch.modern:after,.template-swatch.ocean:before,.template-swatch.ocean:after{left:22px;background:#315fbb}
.template-swatch.compact{height:36px;background:linear-gradient(#14796e 0 4px,#fff 4px 100%)}
.template-swatch.compact:before{top:11px}.template-swatch.compact:after{top:18px;right:7px}
.template-swatch.creative{background:linear-gradient(#f16a58 0 7px,#fff1ed 7px 100%);border-color:#efb4a9}
.template-swatch.creative:before,.template-swatch.creative:after{background:#f16a58}
.template-swatch.sunset{background:linear-gradient(110deg,#ed7652 0 48%,#c64b72 48% 100%);border-color:#e9a09b}
.template-swatch.sunset:before,.template-swatch.sunset:after{background:#fff}
.template-swatch.photo-violet{background:linear-gradient(90deg,#5b3b9c 0 19px,#fff 19px 100%);border-color:#c8b8ea}
.template-swatch.photo-violet:before,.template-swatch.photo-violet:after{left:26px;background:#8666c5}
.template-swatch.photo-emerald{background:linear-gradient(90deg,#087b68 0 19px,#fff 19px 100%);border-color:#a8d5c4}
.template-swatch.photo-emerald:before,.template-swatch.photo-emerald:after{left:26px;background:#15977f}
.template-swatch.photo-violet:after,.template-swatch.photo-emerald:after{right:7px}
.resume-layout{display:grid;grid-template-columns:minmax(320px,1fr) minmax(360px,1fr);gap:18px}
.resume-form,.resume-preview{background:#fff;border:1px solid #dfe5ef;border-radius:14px;padding:22px}
.resume-form label{display:block;margin:12px 0;font-size:12px;color:#56657b}
.resume-form input,.resume-form textarea,.resume-form select{display:block;width:100%;padding:10px;margin-top:5px;border:1px solid #d9e0eb;border-radius:7px;background:#fff;color:#203735;font:inherit;font-size:12px}
.resume-preview{min-height:600px;color:#222;box-shadow:0 8px 22px #152d3710}
.resume-personal{display:flex;align-items:center;gap:18px;margin-bottom:16px}
.resume-photo{width:104px;height:104px;flex:0 0 auto;object-fit:cover;border-radius:50%;border:4px solid #fff;box-shadow:0 2px 12px #152d3730}
.resume-preview h1{font-size:26px;margin:0 0 6px}.resume-preview h2{font-size:13px;border-bottom:1px solid #333;padding-bottom:4px;margin-top:22px;page-break-after:avoid}
.resume-preview p{white-space:pre-wrap;line-height:1.5}.resume-section{break-inside:avoid}
.resume-preview.layout-compact{font-family:Arial,sans-serif;padding:18px;font-size:12px}
.resume-preview.layout-compact h1{font-size:21px}.resume-preview.layout-compact h2{margin-top:13px}.resume-preview.layout-compact p{margin:6px 0;line-height:1.35}
.resume-preview.layout-modern{font-family:Arial,sans-serif;border-top:7px solid #315fbb;border-radius:4px}
.resume-preview.layout-modern h1{color:#17305a;font-size:30px}
.resume-preview.layout-modern .resume-personal-copy>p{padding:10px 12px;background:#edf3ff;color:#334d75;border-left:3px solid #315fbb}
.resume-preview.layout-modern h2{color:#315fbb;text-transform:uppercase;letter-spacing:.06em;border-bottom-color:#b7c8e9}
.resume-preview.layout-classic{font-family:Georgia,"Times New Roman",serif}
.resume-preview.layout-classic .resume-personal{display:block;text-align:center;border-bottom:2px solid #314454;padding-bottom:12px}
.resume-preview.layout-creative{border-top:9px solid #ef705d;background:linear-gradient(180deg,#fff8f5 0,#fff 190px)}
.resume-preview.layout-creative h1{color:#a83c2d;font-size:30px}
.resume-preview.layout-creative h2{color:#b94b38;border-bottom:2px solid #f0a394}
.resume-preview.layout-creative .resume-personal-copy>p{color:#74463f}
.resume-preview.layout-ocean{border-left:9px solid #168eaa;background:linear-gradient(135deg,#f0fbff 0,#fff 42%)}
.resume-preview.layout-ocean h1{color:#07576e;font-size:30px}
.resume-preview.layout-ocean h2{color:#087b98;border-bottom:2px solid #9ed8e4}
.resume-preview.layout-ocean .resume-personal-copy>p{color:#31576a}
.resume-preview.layout-sunset{border-top:8px solid #ea7651;background:linear-gradient(145deg,#fff4ec 0,#fff 38%)}
.resume-preview.layout-sunset h1{color:#a43e59;font-size:30px}
.resume-preview.layout-sunset h2{color:#bd573d;border-bottom:2px solid #f2bd97}
.resume-preview.layout-sunset .resume-personal-copy>p{color:#7b4550}
.resume-preview.layout-photo-violet,.resume-preview.layout-photo-emerald{display:grid;grid-template-columns:minmax(150px,185px) minmax(0,1fr);gap:0;padding:0;overflow:hidden}
.resume-preview.layout-photo-violet .resume-personal,.resume-preview.layout-photo-emerald .resume-personal{grid-column:1;grid-row:1;display:flex;flex-direction:column;justify-content:flex-start;align-items:center;text-align:center;margin:0;padding:25px 16px}
.resume-preview.layout-photo-violet .resume-body,.resume-preview.layout-photo-emerald .resume-body{grid-column:2;grid-row:1;padding:24px}
.resume-preview.layout-photo-violet .resume-personal{background:linear-gradient(160deg,#4a337f,#7652b4);color:#fff}
.resume-preview.layout-photo-violet .resume-personal h1{color:#fff;font-size:21px}
.resume-preview.layout-photo-violet .resume-personal-copy>p{color:#eee7fb;font-size:12px;overflow-wrap:anywhere}
.resume-preview.layout-photo-violet .resume-photo{border-color:#e5d9ff}
.resume-preview.layout-photo-violet h2{color:#6849a1;border-bottom:2px solid #cbb8ec}
.resume-preview.layout-photo-emerald .resume-personal{background:linear-gradient(160deg,#075e51,#0d9077);color:#fff}
.resume-preview.layout-photo-emerald .resume-personal h1{color:#fff;font-size:21px}
.resume-preview.layout-photo-emerald .resume-personal-copy>p{color:#e1fff6;font-size:12px;overflow-wrap:anywhere}
.resume-preview.layout-photo-emerald .resume-photo{border-color:#c2f4e3}
.resume-preview.layout-photo-emerald h2{color:#087765;border-bottom:2px solid #a9ddce}
.toolbar{display:flex;gap:8px;flex-wrap:wrap}.toolbar button{padding:10px 14px}
.muted{color:#66758b;font-size:12px}
.resume-builder-page{background:#e9f1ef url('https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=2000&q=85') center/cover fixed}
.resume-builder-page .shell{background:linear-gradient(rgba(239,247,245,.87),rgba(241,246,246,.90))}
.resume-builder-page .resume-form,.resume-builder-page .resume-preview,.resume-builder-page .content-card{background-color:rgba(255,255,255,.94);backdrop-filter:blur(3px)}
.resume-form h2{margin:0 0 16px;color:#173d39}.builder-field-group{margin:21px 0;padding:15px;border:1px solid #dce7e3;border-radius:13px;background:#f8fbfa}
.entry-group-heading{margin-bottom:12px}.entry-group-heading h3{margin:0;color:#173d39;font-size:15px}.entry-group-heading p{margin:5px 0 0;color:#70817c;font-size:11px;line-height:1.5}
.entry-card{margin:11px 0;padding:13px;border:1px solid #dce7e3;border-radius:10px;background:#fff}.entry-card-heading{display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:8px}.entry-card-heading strong{color:#14786e;font-size:11px}.entry-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 12px}.entry-grid label{margin:7px 0}.entry-grid .entry-wide{grid-column:1/-1}.entry-card .remove-entry{border:0;background:transparent;color:#a64a43;font:inherit;font-size:10px;font-weight:700;cursor:pointer}.entry-add{border:1px dashed #75ae9d;background:#eef8f3;color:#126f64;border-radius:8px;padding:9px 12px;font:inherit;font-size:11px;font-weight:750;cursor:pointer}.entry-help{margin:7px 0 0;color:#7c8a85;font-size:10px;line-height:1.5}
.resume-entry{padding:8px 0;border-bottom:1px solid #e7ecea;break-inside:avoid}.resume-entry:last-child{border-bottom:0}.resume-entry-top{display:flex;justify-content:space-between;align-items:baseline;gap:12px}.resume-entry-top strong{color:#26394a;font-size:13px}.resume-entry-top span{color:#70817c;font-size:11px}.resume-entry p{margin:3px 0;color:#53665f;font-size:11px}.resume-entry small{color:#72817b;font-size:10px}.resume-notes-preview{white-space:pre-wrap;color:#5c6d67;font-size:11px}
@media(max-width:600px){.resume-builder-page .main{padding-left:14px;padding-right:14px}.entry-grid{grid-template-columns:1fr}.entry-grid .entry-wide{grid-column:auto}.builder-field-group{padding:11px}.entry-card{padding:10px}}
@media(max-width:900px){.resume-layout{grid-template-columns:1fr}}
@media(max-width:600px){.resume-preview.layout-photo-violet,.resume-preview.layout-photo-emerald{grid-template-columns:1fr}.resume-preview.layout-photo-violet .resume-personal,.resume-preview.layout-photo-emerald .resume-personal{grid-column:1;grid-row:auto}.resume-preview.layout-photo-violet .resume-body,.resume-preview.layout-photo-emerald .resume-body{grid-column:1;grid-row:auto}}
@media print{.sidebar,.page-header,.seeker-dashboard-toolbar,.resume-form,.no-print{display:none!important}.shell,.main{display:block;padding:0;margin:0}.resume-layout{display:block}.resume-preview{border:0;width:100%;min-height:0;box-shadow:none;-webkit-print-color-adjust:exact;print-color-adjust:exact}.resume-preview.layout-photo-violet,.resume-preview.layout-photo-emerald{break-inside:avoid}}
</style>
</head>
<body class="resume-builder-page">
<div class="shell seeker-shell seeker-shell-no-sidebar">
<main class="main seeker-main resume-builder-main">
<%@ include file="account-menu.jspf" %>
<header class="page-header"><div><p class="eyebrow">JOB.COM / CAREER TOOLS</p><h1>ATS resume builder</h1><p class="page-subtitle">Choose from ready-to-use designs, enter your details, save your draft, then print or save as PDF.</p></div><a class="no-print" href="${pageContext.request.contextPath}/seeker/login?logout=1">Sign out</a></header>
<c:if test="${not empty error}"><div class="alert"><c:out value="${error}"/></div></c:if>
<c:if test="${param.saved=='1'}"><div class="success-message">Resume draft saved.</div></c:if>
<c:choose>
<c:when test="${empty templates}"><div class="alert">Resume designs could not be loaded. Please refresh the page or contact support.</div></c:when>
<c:otherwise>
<section class="content-card no-print"><p class="eyebrow">CHOOSE A DESIGN</p><h2>Start with a resume template</h2><p class="muted">Select a style to preview it. Your text is editable, and two designs can include your saved profile photo.</p>
<div class="template-picker"><c:forEach items="${templates}" var="t"><a class="template-option ${selectedTemplate==t.template_id?'is-selected':''}" href="${pageContext.request.contextPath}/resume?template=${t.template_id}" ${selectedTemplate==t.template_id?'aria-current="true"':''}><span class="template-swatch ${t.layout}"></span><strong><c:out value="${t.name}"/></strong><span class="template-tag"><c:out value="${t.layout}"/> design</span><p><c:out value="${t.description}"/></p><span class="template-action">${selectedTemplate==t.template_id?'Selected · edit below':'Choose this template →'}</span></a></c:forEach></div>
<c:if test="${not empty sampleFile}"><a href="${pageContext.request.contextPath}/resume-template-file?id=${selectedTemplate}" target="_blank" rel="noopener">View sample format PDF</a></c:if>
<c:if test="${photoTemplate and not profilePhotoAvailable}"><p class="muted">To add your photo to this design, <a href="${pageContext.request.contextPath}/seeker?section=profile">upload a profile photo</a> first.</p></c:if>
</section>
<div class="resume-layout">
<form class="resume-form no-print" method="post" action="${pageContext.request.contextPath}/resume"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="template_id" value="${selectedTemplate}">
<div class="ats-score-result no-print" id="builderAtsResult" aria-live="polite"><strong>ATS readiness estimate: <span id="builderAtsScore">—</span>/100</strong><div id="builderAtsBreakdown" class="ats-score-breakdown"></div><ul id="builderAtsAdvice"><li>Add resume details to see suggestions.</li></ul><small>General content checks only; employer systems and job requirements vary.</small></div>
<h2>Edit your details</h2>
<label>Resume title<input name="title" maxlength="180" value='<c:out value="${data.title}"/>'></label>
<label>Full name<input name="full_name" value='<c:out value="${data.full_name}"/>' required></label><label>Email<input type="email" name="email" value='<c:out value="${data.email}"/>'></label><label>Phone<input name="phone" value='<c:out value="${data.phone}"/>'></label><label>Professional headline<input name="headline" value='<c:out value="${data.headline}"/>' placeholder="e.g. Java developer"></label>
<label>Professional summary<textarea name="summary" rows="5"><c:out value="${data.summary}"/></textarea></label><label>Skills<textarea name="skills" rows="3"><c:out value="${data.skills}"/></textarea></label><label>Work experience<textarea name="experience" rows="6" placeholder="Role, company, dates, achievements"><c:out value="${data.experience}"/></textarea></label>
<section class="builder-field-group"><div class="entry-group-heading"><h3>Education</h3><p>Add your course, school or college, completion year, and result. Use one card for each qualification.</p></div><div id="educationEntries"><c:forEach items="${educationRows}" var="education" varStatus="loop"><div class="entry-card" data-entry-row="education"><div class="entry-card-heading"><strong>Qualification <c:out value="${loop.count}"/></strong><button class="remove-entry" type="button" data-remove-row>Remove</button></div><div class="entry-grid"><label>Course / degree<input name="education_course" value='<c:out value="${education.course}"/>' placeholder="e.g. MCA" maxlength="180"></label><label>School / college name<input name="education_institution" value='<c:out value="${education.institution}"/>' placeholder="e.g. University or college" maxlength="220"></label><label>Year<input name="education_year" value='<c:out value="${education.year}"/>' placeholder="e.g. 2024" maxlength="30"></label><label>Percentage / grade<input name="education_percentage" value='<c:out value="${education.percentage}"/>' placeholder="e.g. 82% or CGPA 8.2" maxlength="40"></label></div></div></c:forEach></div><button class="entry-add" type="button" data-add-entry="education">＋ Add education</button><label>Additional education details (optional)<textarea name="education_notes" rows="2" placeholder="Honors, coursework, or other academic details"><c:out value="${data.education_notes}"/></textarea></label></section>
<section class="builder-field-group"><div class="entry-group-heading"><h3>Projects</h3><p>Show what you built, the tools you used, and the result. Add a separate card for each project.</p></div><div id="projectEntries"><c:forEach items="${projectRows}" var="project" varStatus="loop"><div class="entry-card" data-entry-row="project"><div class="entry-card-heading"><strong>Project <c:out value="${loop.count}"/></strong><button class="remove-entry" type="button" data-remove-row>Remove</button></div><div class="entry-grid"><label>Project name<input name="project_name" value='<c:out value="${project.name}"/>' placeholder="e.g. Job matching portal" maxlength="220"></label><label>Tools used<input name="project_tools" value='<c:out value="${project.tools}"/>' placeholder="e.g. Java, MySQL, HTML, CSS" maxlength="300"></label><label class="entry-wide">Project details<textarea name="project_description" rows="3" placeholder="What you built, your contribution, and the outcome"><c:out value="${project.description}"/></textarea></label></div></div></c:forEach></div><button class="entry-add" type="button" data-add-entry="project">＋ Add project</button><label>Additional project details (optional)<textarea name="project_notes" rows="2" placeholder="Awards, links, or other project details"><c:out value="${data.project_notes}"/></textarea></label></section>
<label>Certifications<textarea name="certifications" rows="3"><c:out value="${data.certifications}"/></textarea></label><label>LinkedIn<input name="linkedin" value='<c:out value="${data.linkedin}"/>'></label><label>Portfolio<input name="portfolio" value='<c:out value="${data.portfolio}"/>'></label>
<div class="toolbar"><button type="submit">Save resume</button><button type="button" onclick="syncPreview();window.print()">Print / Save PDF</button></div><p class="muted">In the print dialog, choose “Save as PDF” as the destination. Turn on background graphics for the colors.</p>
</form>
<article class="resume-preview layout-${selectedLayout}" id="preview">
<div class="resume-personal">
<c:if test="${photoTemplate and profilePhotoAvailable}"><img class="resume-photo" src="${profilePhotoUrl}" alt="Profile photo"></c:if>
<div class="resume-personal-copy"><h1 data-out="full_name"><c:out value="${data.full_name}"/></h1><p><span data-out="headline"><c:out value="${data.headline}"/></span><br><span data-out="email"><c:out value="${data.email}"/></span> · <span data-out="phone"><c:out value="${data.phone}"/></span></p></div>
</div>
<div class="resume-body">
<section class="resume-section"><h2>PROFESSIONAL SUMMARY</h2><p data-out="summary"><c:out value="${data.summary}"/></p></section>
<section class="resume-section"><h2>SKILLS</h2><p data-out="skills"><c:out value="${data.skills}"/></p></section>
<section class="resume-section"><h2>EXPERIENCE</h2><p data-out="experience"><c:out value="${data.experience}"/></p></section>
<section class="resume-section"><h2>EDUCATION</h2><div id="educationPreview"><c:forEach items="${educationRows}" var="education"><c:if test="${not empty education.course or not empty education.institution or not empty education.year or not empty education.percentage}"><article class="resume-entry"><div class="resume-entry-top"><strong><c:out value="${education.course}"/></strong><span><c:out value="${education.year}"/></span></div><p><c:out value="${education.institution}"/></p><small><c:out value="${education.percentage}"/></small></article></c:if></c:forEach></div><p class="resume-notes-preview" id="educationNotesPreview"><c:out value="${data.education_notes}"/></p></section>
<section class="resume-section"><h2>PROJECTS</h2><div id="projectPreview"><c:forEach items="${projectRows}" var="project"><c:if test="${not empty project.name or not empty project.tools or not empty project.description}"><article class="resume-entry"><div class="resume-entry-top"><strong><c:out value="${project.name}"/></strong></div><p>Tools used: <c:out value="${project.tools}"/></p><small><c:out value="${project.description}"/></small></article></c:if></c:forEach></div><p class="resume-notes-preview" id="projectNotesPreview"><c:out value="${data.project_notes}"/></p></section>
<section class="resume-section"><h2>CERTIFICATIONS</h2><p data-out="certifications"><c:out value="${data.certifications}"/></p></section>
<section class="resume-section"><h2>LINKS</h2><p><span data-out="linkedin"><c:out value="${data.linkedin}"/></span><br><span data-out="portfolio"><c:out value="${data.portfolio}"/></span></p></section>
</div></article>
</div>
</c:otherwise>
</c:choose>
</main></div>
<template id="educationEntryTemplate"><div class="entry-card" data-entry-row="education"><div class="entry-card-heading"><strong>Education</strong><button class="remove-entry" type="button" data-remove-row>Remove</button></div><div class="entry-grid"><label>Course / degree<input name="education_course" placeholder="e.g. MCA" maxlength="180"></label><label>School / college name<input name="education_institution" placeholder="e.g. University or college" maxlength="220"></label><label>Year<input name="education_year" placeholder="e.g. 2024" maxlength="30"></label><label>Percentage / grade<input name="education_percentage" placeholder="e.g. 82% or CGPA 8.2" maxlength="40"></label></div></div></template>
<template id="projectEntryTemplate"><div class="entry-card" data-entry-row="project"><div class="entry-card-heading"><strong>Project</strong><button class="remove-entry" type="button" data-remove-row>Remove</button></div><div class="entry-grid"><label>Project name<input name="project_name" placeholder="e.g. Job matching portal" maxlength="220"></label><label>Tools used<input name="project_tools" placeholder="e.g. Java, MySQL, HTML, CSS" maxlength="300"></label><label class="entry-wide">Project details<textarea name="project_description" rows="3" placeholder="What you built, your contribution, and the outcome"></textarea></label></div></div></template>
<script>
function updateCount(kind){var rows=document.querySelectorAll('[data-entry-row="'+kind+'"]');rows.forEach(function(row,index){var title=row.querySelector('.entry-card-heading strong');if(title)title.textContent=(kind==='education'?'Qualification ':'Project ')+(index+1)});var add=document.querySelector('[data-add-entry="'+kind+'"]');if(add)add.disabled=rows.length>=10}
function addEntry(kind){var container=document.getElementById(kind==='education'?'educationEntries':'projectEntries');var template=document.getElementById(kind==='education'?'educationEntryTemplate':'projectEntryTemplate');if(!container||!template||container.querySelectorAll('[data-entry-row]').length>=10)return;container.appendChild(template.content.cloneNode(true));updateCount(kind);syncPreview()}
function renderEntryPreview(kind){var container=document.getElementById(kind+'Preview');if(!container)return;container.replaceChildren();document.querySelectorAll('[data-entry-row="'+kind+'"]').forEach(function(row){var value=function(name){var field=row.querySelector('[name="'+kind+'_'+name+'"]');return field?field.value.trim():''};var first=value(kind==='education'?'course':'name'),second=value(kind==='education'?'institution':'tools'),third=value(kind==='education'?'year':'description'),fourth=kind==='education'?value('percentage'):'';if(!first&&!second&&!third&&!fourth)return;var article=document.createElement('article');article.className='resume-entry';var top=document.createElement('div');top.className='resume-entry-top';var title=document.createElement('strong');title.textContent=first;top.appendChild(title);if(kind==='education'&&third){var year=document.createElement('span');year.textContent=third;top.appendChild(year)}article.appendChild(top);if(second){var detail=document.createElement('p');detail.textContent=kind==='education'?second:'Tools used: '+second;article.appendChild(detail)}if(kind==='education'&&fourth){var percent=document.createElement('small');percent.textContent=fourth;article.appendChild(percent)}if(kind==='project'&&third){var projectDetail=document.createElement('small');projectDetail.textContent=third;article.appendChild(projectDetail)}container.appendChild(article)})}
var atsUpdateTimer,atsController;function updateBuilderAts(){clearTimeout(atsUpdateTimer);if(atsController)atsController.abort();atsUpdateTimer=setTimeout(async function(){var form=document.querySelector('.resume-form'),get=function(name){var field=form.querySelector('[name="'+name+'"]');return field?field.value.trim():''},lines=[];if(get('full_name'))lines.push(get('full_name'));if(get('email'))lines.push(get('email'));if(get('phone'))lines.push(get('phone'));if(get('headline'))lines.push(get('headline'));[['summary','PROFESSIONAL SUMMARY'],['skills','SKILLS'],['experience','EXPERIENCE']].forEach(function(pair){if(get(pair[0]))lines.push(pair[1]+'\n'+get(pair[0]))});var education=[];form.querySelectorAll('[data-entry-row="education"]').forEach(function(row){var values=Array.from(row.querySelectorAll('input')).map(function(input){return input.value.trim()}).filter(Boolean);if(values.length)education.push(values.join(' · '))});if(education.length||get('education_notes'))lines.push('EDUCATION\n'+education.join('\n')+'\n'+get('education_notes'));var projects=[];form.querySelectorAll('[data-entry-row="project"]').forEach(function(row){var values=Array.from(row.querySelectorAll('input,textarea')).map(function(input){return input.value.trim()}).filter(Boolean);if(values.length)projects.push(values.join(' · '))});if(projects.length||get('project_notes'))lines.push('PROJECTS\n'+projects.join('\n')+'\n'+get('project_notes'));[['certifications','CERTIFICATIONS'],['linkedin','LINKS'],['portfolio','PORTFOLIO']].forEach(function(pair){if(get(pair[0]))lines.push(pair[1]+'\n'+get(pair[0]))});var payload=new FormData();payload.append('resumeText',lines.join('\n'));atsController=new AbortController();try{var response=await fetch('${pageContext.request.contextPath}/ats-score',{method:'POST',body:payload,signal:atsController.signal}),result=await response.json();if(!response.ok)throw new Error(result.error||'ATS estimate unavailable');document.getElementById('builderAtsScore').textContent=result.score;var breakdown=document.getElementById('builderAtsBreakdown');breakdown.replaceChildren();Object.entries(result.breakdown).forEach(function(entry){var row=document.createElement('div');var label=document.createElement('span');label.textContent=entry[0];var value=document.createElement('strong');value.textContent=entry[1];row.append(label,value);breakdown.appendChild(row)});var advice=document.getElementById('builderAtsAdvice');advice.replaceChildren();result.suggestions.slice(0,5).forEach(function(item){var li=document.createElement('li');li.textContent=item;advice.appendChild(li)})}catch(error){if(error.name!=='AbortError'){document.getElementById('builderAtsScore').textContent='—';document.getElementById('builderAtsBreakdown').textContent='Score could not be updated. Try refreshing.'}}},350)}
function syncPreview(){document.querySelectorAll('.resume-form input[name],.resume-form textarea[name]').forEach(function(e){var out=document.querySelector('[data-out="'+e.name+'"]');if(out)out.textContent=e.value});renderEntryPreview('education');renderEntryPreview('project');var educationNotes=document.querySelector('#educationNotesPreview'),projectNotes=document.querySelector('#projectNotesPreview');if(educationNotes){var field=document.querySelector('[name="education_notes"]');educationNotes.textContent=field?field.value:''}if(projectNotes){var field2=document.querySelector('[name="project_notes"]');projectNotes.textContent=field2?field2.value:''}updateBuilderAts()}
document.querySelector('.resume-form').addEventListener('input',syncPreview);document.querySelector('.resume-form').addEventListener('click',function(event){var add=event.target.closest('[data-add-entry]');if(add){addEntry(add.getAttribute('data-add-entry'));return}var remove=event.target.closest('[data-remove-row]');if(remove){var row=remove.closest('[data-entry-row]'),kind=row.getAttribute('data-entry-row'),container=row.parentElement;if(container.querySelectorAll('[data-entry-row]').length===1){row.querySelectorAll('input,textarea').forEach(function(field){field.value=''})}else row.remove();updateCount(kind);syncPreview()}});updateCount('education');updateCount('project');syncPreview();
</script>
</body>
</html>
