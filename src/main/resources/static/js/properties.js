window.PropertiesPage = (() => {
  const page = document.getElementById('properties-page');
  const body = document.getElementById('property-list-body');
  const search = document.getElementById('property-search');
  const status = document.getElementById('property-status-filter');
  const count = document.getElementById('property-list-count');
  const dialog = document.createElement('dialog');
  dialog.className = 'property-editor add-modal';
  dialog.setAttribute('aria-labelledby', 'property-editor-title');
  dialog.innerHTML = `<div class="modal-header"><h2 id="property-editor-title">Add property</h2><button type="button" class="modal-close" aria-label="Close property form">×</button></div>
    <form class="user-form"><div class="form-grid two-columns property-fields"></div><p class="property-message" role="status"></p><div class="property-form-actions"><button type="button" class="secondary-button property-cancel">Cancel</button><button type="submit" class="quick-add">Save property</button></div></form>`;
  document.body.append(dialog);
  const form = dialog.querySelector('form');
  const message = dialog.querySelector('.property-message');
  const fields = [
    ['propertyName', 'Property name', 'text', true], ['propertyType', 'Property type', 'text'],
    ['city', 'City', 'text'], ['state', 'State', 'text'], ['price', 'Price (₹)', 'number'],
    ['status', 'Status', 'select', true], ['address', 'Address', 'text'],
    ['ownerName', 'Owner name', 'text'], ['mobileNumber', 'Phone number', 'tel'],
    ['description', 'Description', 'textarea']
  ];
  fields.forEach(([name, caption, type, required]) => {
    const label = document.createElement('label'); label.textContent = caption;
    const input = document.createElement(type === 'select' ? 'select' : type === 'textarea' ? 'textarea' : 'input');
    input.name = name; input.required = !!required;
    if (type === 'select') ['Available', 'Pending', 'Sold'].forEach(value => input.add(new Option(value, value)));
    else if (type === 'textarea') { input.rows = 3; input.maxLength = 2000; }
    else { input.type = type; input.maxLength = 255; }
    if (type === 'number') { input.min = '0'; input.step = '0.01'; }
    label.append(input); form.querySelector('.property-fields').append(label);
  });
  let records = [], editing = null, busy = false, generation = 0;
  const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
  async function request(url, options) {
    const response = await fetch(url, options);
    const data = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) throw new Error(data?.message || (options?.method === 'DELETE'
      ? 'Could not delete this property. It may be linked to a booking.' : 'Could not load or save this property. Please try again.'));
    return data;
  }
  function render() {
    const query = search.value.trim().toLowerCase();
    const filtered = records.filter(record => (!status.value || record.status === status.value)
      && `${record.propertyName} ${record.city || ''} ${record.propertyId || ''}`.toLowerCase().includes(query));
    count.textContent = `${filtered.length} properties`;
    body.replaceChildren();
    if (!filtered.length) { body.innerHTML = '<tr><td colspan="6">No properties found.</td></tr>'; return; }
    for (const record of filtered) {
      const row = document.createElement('tr');
      for (const value of [record.propertyName, record.propertyType || '—', [record.city, record.state].filter(Boolean).join(', ') || '—', record.price == null ? '—' : currency.format(Number(record.price)), record.status]) {
        const cell = document.createElement('td'); cell.textContent = value; row.append(cell);
      }
      const actions = document.createElement('td'); actions.className = 'property-row-actions';
      for (const [label, handler] of [['Edit', () => edit(record.id)], ['Delete', () => remove(record, actions)]]) {
        const button = document.createElement('button'); button.type = 'button'; button.className = 'secondary-button';
        button.textContent = label; button.setAttribute('aria-label', `${label} ${record.propertyName}`);
        button.addEventListener('click', handler); actions.append(button);
      }
      row.append(actions); body.append(row);
    }
  }
  async function load() {
    const current = ++generation;
    body.innerHTML = '<tr><td colspan="6">Loading properties...</td></tr>';
    try {
      const result = await request('/api/properties');
      if (current !== generation) return;
      records = result; render();
      const badge = document.querySelector('.nav-item[href="#properties"] .nav-count');
      if (badge) badge.textContent = records.length;
    } catch (error) {
      if (current !== generation) return;
      records = []; count.textContent = 'Unavailable';
      body.innerHTML = '<tr><td colspan="6">Could not load properties. Select Refresh to retry.</td></tr>';
    }
  }
  async function edit(id) {
    if (busy) return;
    busy = true;
    try {
      editing = id ? await request(`/api/properties/${id}`) : null;
      form.reset(); message.textContent = '';
      form.elements.status.querySelectorAll('[data-legacy]').forEach(option => option.remove());
      if (editing?.status && ![...form.elements.status.options].some(option => option.value === editing.status)) {
        const option = new Option(editing.status, editing.status); option.dataset.legacy = 'true'; form.elements.status.add(option);
      }
      fields.forEach(([name]) => { form.elements[name].value = editing?.[name] ?? (name === 'status' ? 'Available' : ''); });
      dialog.querySelector('h2').textContent = id ? 'Edit property' : 'Add new property';
      dialog.showModal(); form.elements.propertyName.focus();
    } catch (error) { AppNotice.error(error.message); }
    finally { busy = false; }
  }
  async function remove(record, actions) {
    if (!window.confirm(`Delete "${record.propertyName}"? This cannot be undone.`)) return;
    actions.querySelectorAll('button').forEach(button => { button.disabled = true; });
    try {
      await request(`/api/properties/${record.id}`, { method: 'DELETE' });
      AppNotice.success('Property deleted successfully.'); await load();
    } catch (error) { AppNotice.error(error.message); }
    finally { actions.querySelectorAll('button').forEach(button => { button.disabled = false; }); }
  }
  form.addEventListener('submit', async event => {
    event.preventDefault(); if (busy) return;
    busy = true;
    const button = form.querySelector('[type="submit"]'); button.disabled = true;
    message.textContent = 'Saving property...';
    // Retain stored fields not exposed by this form when editing.
    const data = { ...(editing || {}), ...Object.fromEntries(new FormData(form)) };
    data.propertyName = data.propertyName.trim(); data.price = data.price || null;
    try {
      await request(editing ? `/api/properties/${editing.id}` : '/api/properties', {
        method: editing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
      });
      AppNotice.success(editing ? 'Property updated successfully.' : 'Property added successfully.');
      dialog.close(); await load();
    } catch (error) { message.textContent = error.message; }
    finally { busy = false; button.disabled = false; }
  });
  const closeEditor = () => { if (!busy) dialog.close(); };
  dialog.querySelector('.modal-close').addEventListener('click', closeEditor);
  dialog.querySelector('.property-cancel').addEventListener('click', closeEditor);
  dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
  function open(event) {
    event?.preventDefault(); closeLeadsPage(); closeUsersPage(); closeCustomersPage(); closePaymentsPage();
    overviewContent.style.display = 'none'; page.classList.add('active');
    document.querySelector('.breadcrumb strong').textContent = 'Properties';
    document.querySelectorAll('.nav-item').forEach(item => item.classList.toggle('active', item.getAttribute('href') === '#properties'));
    sidebar?.classList.remove('open'); sessionStorage.setItem('realEstatePage', 'properties');
    search.value = ''; status.value = ''; page.scrollTop = 0; load();
  }
  document.querySelector('.nav-item[href="#properties"]').addEventListener('click', open);
  document.getElementById('add-property').addEventListener('click', () => edit(null));
  document.getElementById('refresh-properties').addEventListener('click', load);
  search.addEventListener('input', render); status.addEventListener('change', render);
  return { open, close: () => page.classList.remove('active') };
})();
