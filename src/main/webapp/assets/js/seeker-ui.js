(() => {
  const menus = [...document.querySelectorAll(".seeker-account-menu")];

  const syncMenuState = (menu) => {
    const summary = menu.querySelector(":scope > summary");
    if (summary) summary.setAttribute("aria-expanded", String(menu.open));
  };

  menus.forEach((menu) => {
    syncMenuState(menu);
    menu.addEventListener("toggle", () => syncMenuState(menu));
  });

  document.addEventListener("pointerdown", (event) => {
    menus.forEach((menu) => {
      if (menu.open && !menu.contains(event.target)) {
        menu.open = false;
        syncMenuState(menu);
      }
    });
  });

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    const openMenu = menus.find((menu) => menu.open);
    if (!openMenu) return;
    openMenu.open = false;
    syncMenuState(openMenu);
    openMenu.querySelector(":scope > summary")?.focus();
  });

  const shell = document.querySelector(".seeker-shell-no-sidebar");
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  const slideshow = document.querySelector("[data-photo-slideshow]");
  const slides = slideshow ? [...slideshow.querySelectorAll(".seeker-banner-photo")] : [];

  if (slideshow && slides.length > 1 && !reducedMotion.matches) {
    let active = Math.max(0, slides.findIndex((slide) => slide.classList.contains("is-active")));
    window.setInterval(() => {
      if (document.hidden) return;
      slides[active].classList.remove("is-active");
      active = (active + 1) % slides.length;
      slides[active].classList.add("is-active");
    }, 7000);
  }

  const alertControl = document.querySelector("[data-job-alert-poll-url]");
  if (alertControl) {
    const pollUrl = alertControl.dataset.jobAlertPollUrl;
    const accountId = alertControl.dataset.jobAlertSeekerId || "current";
    const seenKey = `jobcom_seen_job_alerts_${accountId}`;
    const startupAt = Date.now();
    const toastStack = document.querySelector("[data-job-alert-stack]");
    const seen = new Set();
    let cursor = startupAt - 5000;
    let firstPoll = true;
    let audioContext = null;

    const readSeen = () => {
      try {
        const ids = JSON.parse(window.localStorage.getItem(seenKey) || "[]");
        if (Array.isArray(ids)) ids.forEach((id) => seen.add(String(id)));
      } catch (_) { /* Storage may be disabled; this page still deduplicates in memory. */ }
    };
    const saveSeen = () => {
      try { window.localStorage.setItem(seenKey, JSON.stringify([...seen].slice(-300))); } catch (_) { }
    };
    readSeen();

    const unlockAudio = () => {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextClass) return;
      try {
        if (!audioContext) audioContext = new AudioContextClass();
        if (audioContext.state === "suspended") audioContext.resume();
      } catch (_) { /* Sound is optional on browsers without Web Audio support. */ }
    };
    document.addEventListener("pointerdown", unlockAudio, { passive: true });
    document.addEventListener("keydown", unlockAudio);

    const beep = () => {
      if (!audioContext || audioContext.state !== "running") return;
      try {
        const oscillator = audioContext.createOscillator();
        const gain = audioContext.createGain();
        oscillator.type = "sine";
        oscillator.frequency.setValueAtTime(880, audioContext.currentTime);
        gain.gain.setValueAtTime(0.0001, audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.09, audioContext.currentTime + 0.015);
        gain.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + 0.2);
        oscillator.connect(gain);
        gain.connect(audioContext.destination);
        oscillator.start();
        oscillator.stop(audioContext.currentTime + 0.21);
      } catch (_) { /* Leave the visual notification available if audio is blocked. */ }
    };

    const updateUnread = (value) => {
      const count = Math.max(0, Number(value) || 0);
      const updateBadge = (selector, parent, label) => {
        let badge = document.querySelector(selector);
        if (count === 0) { badge?.remove(); return; }
        if (!badge && parent) {
          badge = document.createElement("b");
          badge.dataset[label] = "";
          parent.append(badge);
        }
        if (badge) badge.textContent = String(count);
      };
      updateBadge("[data-job-alert-badge]", alertControl, "jobAlertBadge");
      const menuLink = document.querySelector("[data-job-alert-menu-link]");
      updateBadge("[data-job-alert-menu-badge]", menuLink, "jobAlertMenuBadge");
      alertControl.setAttribute("aria-label", count ? `Notifications, ${count} unread` : "Notifications");
    };

    const showToast = (alert) => {
      if (!toastStack) return;
      const toast = document.createElement("article");
      toast.className = "seeker-job-alert-toast";
      const icon = document.createElement("span");
      icon.className = "seeker-job-alert-icon";
      icon.setAttribute("aria-hidden", "true");
      icon.textContent = "↗";
      const copy = document.createElement("div");
      copy.className = "seeker-job-alert-copy";
      const title = document.createElement("strong");
      title.textContent = "A new job matches your profile";
      const detail = document.createElement("span");
      detail.textContent = alert.title || "A company posted a new job in your field.";
      copy.append(title, detail);
      if (alert.jobUrl) {
        const link = document.createElement("a");
        link.href = alert.jobUrl;
        link.textContent = "View matching job →";
        copy.append(link);
      }
      const dismiss = document.createElement("button");
      dismiss.className = "seeker-job-alert-dismiss";
      dismiss.type = "button";
      dismiss.setAttribute("aria-label", "Dismiss job notification");
      dismiss.textContent = "×";
      dismiss.addEventListener("click", () => toast.remove());
      toast.append(icon, copy, dismiss);
      toastStack.append(toast);
      window.setTimeout(() => toast.remove(), 9000);
    };

    const claimAlertOnce = async (id) => {
      if (navigator.locks?.request) {
        return navigator.locks.request(`jobcom-job-alert-${accountId}`, async () => {
          readSeen();
          if (seen.has(String(id))) return false;
          seen.add(String(id));
          saveSeen();
          return true;
        });
      }
      readSeen();
      if (seen.has(String(id))) return false;
      seen.add(String(id));
      saveSeen();
      return true;
    };

    const pollAlerts = async () => {
      if (document.hidden) return;
      try {
        const url = new URL(pollUrl, window.location.href);
        url.searchParams.set("since", String(Math.max(0, cursor - 5000)));
        const response = await fetch(url, { credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" } });
        if (!response.ok) return;
        const payload = await response.json();
        updateUnread(payload.unreadCount);
        for (const alert of payload.alerts || []) {
          const id = String(alert.id);
          const createdAt = Number(alert.createdAt) || 0;
          cursor = Math.max(cursor, createdAt);
          if (firstPoll && createdAt < startupAt - 1000) {
            seen.add(id);
            continue;
          }
          if (!(await claimAlertOnce(id))) continue;
          showToast(alert);
          beep();
        }
        saveSeen();
        firstPoll = false;
      } catch (_) { /* Retry on the next poll; transient network errors should not consume an alert. */ }
    };

    pollAlerts();
    window.setInterval(pollAlerts, 12000);
  }

  if (!shell || !window.matchMedia("(hover: hover) and (pointer: fine)").matches) return;

  let latestPointer = null;
  let frameQueued = false;
  document.addEventListener("pointermove", (event) => {
    if (event.pointerType !== "mouse" && event.pointerType !== "pen") return;
    latestPointer = event;
    if (frameQueued) return;
    frameQueued = true;
    window.requestAnimationFrame(() => {
      frameQueued = false;
      if (!latestPointer) return;
      const { clientX, clientY } = latestPointer;
      const hue = Math.round(154 + (clientX / Math.max(window.innerWidth, 1)) * 52);
      shell.style.setProperty("--page-pointer-x", `${clientX}px`);
      shell.style.setProperty("--page-pointer-y", `${clientY}px`);
      shell.style.setProperty("--dashboard-hue", hue);

      const card = latestPointer.target.closest(
        ".seeker-home-card, .seeker-recommendations-card, .seeker-next-steps-card, .seeker-career-tools-card, .seeker-recommended-job"
      );
      if (card) {
        const bounds = card.getBoundingClientRect();
        card.style.setProperty("--card-pointer-x", `${clientX - bounds.left}px`);
        card.style.setProperty("--card-pointer-y", `${clientY - bounds.top}px`);
      }
    });
  }, { passive: true });
})();
