window.AppNotice = (() => {
  const region = document.createElement('div');
  region.className = 'notification-stack';
  region.setAttribute('aria-label', 'Action notifications');
  document.body.append(region);

  function show(message, error = false) {
    const toast = document.createElement('div');
    toast.className = `action-toast${error ? ' action-toast-error' : ''}`;
    const icon = document.createElement('span');
    icon.className = 'action-toast-icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.textContent = error ? '!' : '✓';
    const text = document.createElement('div');
    text.setAttribute('role', error ? 'alert' : 'status');
    text.setAttribute('aria-atomic', 'true');
    const close = document.createElement('button');
    close.type = 'button';
    close.setAttribute('aria-label', 'Dismiss notification');
    close.textContent = '×';
    toast.append(icon, text, close);
    region.append(toast);
    const title = document.createElement('strong');
    title.textContent = error ? 'Action failed' : 'Success';
    const copy = document.createElement('span');
    copy.textContent = message;
    text.append(title, copy);
    let timer;
    const dismiss = () => { clearTimeout(timer); toast.remove(); };
    const pause = () => clearTimeout(timer);
    const resume = () => {
      pause();
      if (!toast.matches(':hover') && !toast.contains(document.activeElement)) timer = setTimeout(dismiss, 6000);
    };
    close.addEventListener('click', dismiss);
    toast.addEventListener('mouseenter', pause);
    toast.addEventListener('mouseleave', resume);
    toast.addEventListener('focusin', pause);
    toast.addEventListener('focusout', () => setTimeout(resume, 0));
    resume();
  }
  return { success: message => show(message), error: message => show(message, true) };
})();
