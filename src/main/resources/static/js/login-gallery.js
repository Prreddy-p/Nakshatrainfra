(async () => {
  const gallery = document.getElementById('login-gallery');
  const frame = document.getElementById('login-photo-grid');
  try {
    const response = await fetch('/api/public/property-photos');
    if (!response.ok) return;
    const photos = await response.json();
    if (!Array.isArray(photos) || !photos.length) return;
    frame.classList.add('login-photo-carousel');
    frame.setAttribute('role', 'region');
    frame.setAttribute('aria-roledescription', 'carousel');
    frame.setAttribute('aria-label', 'Property photos');
    const link = document.createElement('a');
    link.target = '_blank'; link.rel = 'noopener';
    const counter = document.createElement('span');
    counter.className = 'login-photo-counter';
    counter.setAttribute('aria-live', 'polite'); counter.setAttribute('aria-atomic', 'true');
    const previous = document.createElement('button');
    const next = document.createElement('button');
    previous.type = next.type = 'button';
    previous.className = 'login-photo-arrow previous'; next.className = 'login-photo-arrow next';
    previous.textContent = '\u2039'; next.textContent = '\u203a';
    previous.setAttribute('aria-label', 'Previous property photo');
    next.setAttribute('aria-label', 'Next property photo');
    previous.disabled = next.disabled = photos.length === 1;
    let index = 0;
    function showPhoto(position) {
      index = (position + photos.length) % photos.length;
      link.href = '/api/public/property-photos/' + encodeURIComponent(photos[index].id);
      link.setAttribute('aria-label', 'Open property photo ' + (index + 1) + ' at full size');
      const image = document.createElement('img');
      image.alt = 'Property photo ' + (index + 1); image.decoding = 'async';
      image.addEventListener('error', () => {
        if (link.firstChild === image) link.textContent = 'Preview unavailable. Open photo.';
      });
      link.replaceChildren(image); image.src = link.href;
      counter.textContent = (index + 1) + ' / ' + photos.length;
    }
    previous.addEventListener('click', () => showPhoto(index - 1));
    next.addEventListener('click', () => showPhoto(index + 1));
    frame.addEventListener('keydown', event => {
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
        event.preventDefault(); showPhoto(index + (event.key === 'ArrowLeft' ? -1 : 1));
      }
    });
    frame.replaceChildren(link, previous, next, counter);
    showPhoto(0);
    gallery.hidden = false;
    document.getElementById('login-screen').classList.add('has-gallery');
  } catch { /* Sign-in stays available when the gallery cannot load. */ }
})();
