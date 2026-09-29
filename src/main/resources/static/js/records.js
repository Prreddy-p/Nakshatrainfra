// Records are always read from and committed to the server, never browser storage.
window.RecordStorage = (() => {
  const definitions = {
    property: { title: 'Properties', endpoint: '/api/properties', name: 'propertyName', fields: [
      ['propertyName', 'Property name', 'text', true], ['city', 'City', 'text'],
      ['propertyType', 'Property type', 'text'], ['price', 'Price', 'number']
    ] },
    task: { title: 'Tasks', endpoint: '/api/tasks', name: 'taskName', fields: [
      ['taskName', 'Task name', 'text', true], ['assignedTo', 'Assigned to', 'text'],
      ['dueDate', 'Due date', 'date'], ['notes', 'Notes', 'text']
    ] },
    document: { title: 'Document links', endpoint: '/api/documents', name: 'documentName', fields: [
      ['documentName', 'Document name', 'text', true], ['fileUrl', 'Document URL (https://...)', 'url', true],
      ['relatedType', 'Related record type', 'text'], ['relatedId', 'Related record ID', 'text']
    ] }
  };
  const backdrop = document.createElement('div');
  backdrop.className = 'modal-backdrop';
  backdrop.setAttribute('aria-hidden', 'true');
  backdrop.innerHTML = `<section class="add-modal records-modal" role="dialog" aria-modal="true" aria-labelledby="records-title">
    <div class="modal-header"><h2 id="records-title"></h2><button type="button" class="records-close modal-close" aria-label="Close">×</button></div>
    <form class="user-form"><div class="records-fields"></div><button class="login-button" type="submit">Save record</button></form>
    <p class="records-message" role="status"></p><h3>Saved records</h3><div class="records-list"></div>
  </section>`;
  document.body.append(backdrop);
  const form = backdrop.querySelector('form');
  const message = backdrop.querySelector('.records-message');
  const list = backdrop.querySelector('.records-list');
  let current = null;
  let version = 0;
  let saving = false;
  let previousFocus;
  async function request(url, options) {
    const response = await fetch(url, options);
    const result = await response.json().catch(() => null);
    if (!response.ok) throw new Error(result?.message || 'Could not save or load records. Please try again.');
    return result;
  }
  function close() {
    if (saving) return;
    version++;
    backdrop.classList.remove('open');
    backdrop.setAttribute('aria-hidden', 'true');
    previousFocus?.focus();
  }
  async function load(definition, generation) {
    list.textContent = 'Loading saved records...';
    try {
      const records = await request(definition.endpoint);
      if (generation !== version) return;
      list.replaceChildren();
      if (!records.length) list.textContent = 'No saved records yet.';
      records.forEach(record => {
        const row = document.createElement('div');
        row.className = 'saved-record';
        const text = document.createElement('span');
        text.textContent = `${record[definition.name]}${record.status ? ' — ' + record.status : ''}${record.city ? ' · ' + record.city : ''}`;
        row.append(text);
        if (definition === definitions.task) {
          const toggle = document.createElement('button');
          toggle.type = 'button';
          toggle.textContent = record.status === 'Completed' ? 'Reopen' : 'Complete';
          toggle.addEventListener('click', async () => {
            toggle.disabled = true;
            try {
              await request(`${definition.endpoint}/${record.id}`, {
                method: 'PATCH', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ status: record.status === 'Completed' ? 'Pending' : 'Completed' })
              });
              AppNotice.success(record.status === 'Completed' ? 'Task reopened successfully.' : 'Task completed successfully.');
              if (generation === version) await load(definition, generation);
            } catch (error) {
              if (generation === version) message.textContent = error.message;
            } finally { toggle.disabled = false; }
          });
          row.append(toggle);
        }
        list.append(row);
      });
      const selector = definition === definitions.property ? '#properties' : definition === definitions.task ? '#tasks' : null;
      if (selector) {
        const badge = document.querySelector(`.nav-item[href="${selector}"] .nav-count`);
        if (badge) badge.textContent = records.length;
      }
    } catch (error) {
      if (generation === version) list.textContent = 'Could not load saved records. Close and reopen to retry.';
    }
  }
  function open(type) {
    if (type === 'property') { window.PropertiesPage.open(); return; }
    if (saving) return;
    current = definitions[type];
    if (!current) return;
    previousFocus = document.activeElement;
    const generation = ++version;
    backdrop.querySelector('h2').textContent = current.title;
    const fields = backdrop.querySelector('.records-fields');
    fields.replaceChildren();
    for (const [name, caption, type, required] of current.fields) {
      const label = document.createElement('label');
      label.textContent = caption;
      const input = document.createElement('input');
      Object.assign(input, { name, type, required: !!required });
      if (type === 'number') { input.min = '0'; input.step = '0.01'; }
      if (type === 'text') input.maxLength = name === 'notes' ? 2000 : 255;
      label.append(input); fields.append(label);
    }
    message.textContent = type === 'document' ? 'Save a link to an existing document. File upload is not available here.' : '';
    backdrop.classList.add('open');
    backdrop.setAttribute('aria-hidden', 'false');
    form.querySelector('input').focus();
    load(current, generation);
  }
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (saving) return;
    saving = true;
    const button = form.querySelector('[type="submit"]');
    button.disabled = true;
    message.textContent = 'Saving...';
    const data = Object.fromEntries(new FormData(form));
    for (const key of Object.keys(data)) if (!data[key]) delete data[key];
    try {
      const record = await request(current.endpoint, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
      });
      if (!record?.id) throw new Error('The server did not confirm that the record was saved.');
      AppNotice.success(`${current === definitions.property ? 'Property' : current === definitions.task ? 'Task' : 'Document link'} saved successfully.`);
      message.textContent = `${record[current.name]} saved.`;
      form.reset();
      await load(current, version);
    } catch (error) {
      message.textContent = error instanceof TypeError ? 'Cannot reach the server. Your form has been kept; try again.' : error.message;
    } finally { saving = false; button.disabled = false; }
  });
  backdrop.querySelector('.records-close').addEventListener('click', close);
  backdrop.addEventListener('click', event => { if (event.target === backdrop) close(); });
  backdrop.addEventListener('keydown', event => {
    if (event.key === 'Escape') close();
    if (event.key === 'Tab') {
      const items = [...backdrop.querySelectorAll('button:not(:disabled), input')];
      const first = items[0], last = items[items.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
  });
  for (const [hash, type] of [['#tasks', 'task']]) {
    document.querySelector(`.nav-item[href="${hash}"]`)?.addEventListener('click', event => {
      event.preventDefault(); open(type);
    });
    const badge = document.querySelector(`.nav-item[href="${hash}"] .nav-count`);
    if (badge) badge.textContent = '—';
  }
  return { open };
})();
