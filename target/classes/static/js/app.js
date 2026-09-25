const sidebar = document.querySelector('.sidebar');
const menuButton = document.querySelector('.mobile-menu');
const addButton = document.querySelector('.top-actions .quick-add:not(.top-logout)');
const taskRows = document.querySelectorAll('.task-row');
const taskProgress = document.querySelector('.progress-track i');
const progressCopy = document.querySelector('.task-progress > div:first-child span');
const progressPercent = document.querySelector('.task-progress strong');
const modal = document.querySelector('.modal-backdrop');
const modalTitle = document.querySelector('#add-title');
const leadModal = document.querySelector('.lead-modal-backdrop');
const leadForm = document.querySelector('#lead-form');
const leadPage = document.querySelector('#leads-page');
const overviewContent = document.querySelector('.page-content');
const leadTableBody = document.querySelector('#lead-table-body');
const leadResultCount = document.querySelector('#lead-result-count');
const userPage = document.querySelector('#users-page');
const customerPage = document.querySelector('#customers-page');
const userTableBody = document.querySelector('#user-table-body');
const userModal = document.querySelector('.user-modal-backdrop');
const userForm = document.querySelector('#user-form');
let users = [];
let leads = [];
let editingLeadId = null;
const loginScreen = document.querySelector('#login-screen');
const appShell = document.querySelector('#app-shell');

function showWorkspace(user) {
  const welcomeMessage = document.querySelector('#welcome-message');
  if (welcomeMessage) welcomeMessage.textContent = `Good morning, ${user.name || user.emailId || 'there'}.`;
  loginScreen.style.display = 'none';
  appShell.style.display = 'flex';
  sessionStorage.setItem('realEstateUser', JSON.stringify(user));
  openDashboard();
  loadLeads();
}

document.querySelector('#login-form')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const username = String(form.get('username') || '').trim();
  const password = String(form.get('password') || '');
  const role = String(form.get('role') || '').trim();
  const error = document.querySelector('#login-error');
  error.textContent = '';
  if (window.location.protocol === 'file:' || window.location.port === '5500') {
    error.textContent = 'Open http://localhost:8080/ to sign in. This preview does not run the application server.';
    return;
  }
  if (!username || !password || !role) {
    error.textContent = 'Enter your username, password, and select a role.';
    return;
  }
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, role })
    });
    if (!response.ok) {
      const details = await response.json().catch(() => null);
      throw new Error(details?.message || 'Invalid username, password, or role.');
    }
    const result = await response.json();
    if (result.passwordChangeRequired) {
      passwordResetToken = result.resetToken;
      showAuthForm('change-password-form');
      document.querySelector('#change-password-form [name="newPassword"]').focus();
    } else {
      showWorkspace(result);
    }
  } catch (loginError) {
    error.textContent = loginError instanceof TypeError
      ? 'Cannot reach the application server. Start the application and open http://localhost:8080/.'
      : loginError.message;
  }
});


let passwordResetToken = null;
function showAuthForm(id) {
  for (const formId of ['login-form', 'forgot-password-form', 'change-password-form']) {
    const form = document.getElementById(formId);
    form.hidden = formId !== id;
    form.style.display = formId === id ? '' : 'none';
  }
  document.querySelector('#forgot-password-link').hidden = id !== 'login-form';
}

document.querySelector('#forgot-password-link').addEventListener('click', (event) => {
  event.preventDefault();
  document.querySelector('#forgot-password-form [name="email"]').value =
    document.querySelector('#login-form [name="username"]').value;
  document.querySelector('#forgot-password-message').textContent = '';
  showAuthForm('forgot-password-form');
});

function returnToLogin() {
  passwordResetToken = null;
  document.querySelector('#change-password-form').reset();
  document.querySelector('#change-password-message').textContent = '';
  document.querySelector('#login-form [name="password"]').value = '';
  showAuthForm('login-form');
}
document.querySelector('#back-to-login').addEventListener('click', returnToLogin);
document.querySelector('#cancel-password-change').addEventListener('click', returnToLogin);

async function submitPasswordRequest(path, body) {
  const response = await fetch(`/api/auth/${path}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  const result = await response.json().catch(() => null);
  if (!response.ok) throw new Error(result?.message || 'The request failed. Please try again.');
  return result;
}

document.querySelector('#forgot-password-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = form.querySelector('[type="submit"]');
  const message = document.querySelector('#forgot-password-message');
  button.disabled = true;
  message.textContent = 'Sending email...';
  try {
    const email = form.elements.email.value.trim();
    const result = await submitPasswordRequest('forgot-password', { email });
    message.textContent = result.message + ' Return to sign in and enter the temporary password with your assigned role.';
    document.querySelector('#login-form [name="username"]').value = email;
  } catch (error) {
    message.textContent = error instanceof TypeError ? 'Cannot reach the server. Please try again.' : error.message;
  } finally { button.disabled = false; }
});

document.querySelector('#change-password-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.querySelector('#change-password-message');
  const newPassword = form.elements.newPassword.value;
  const confirmPassword = form.elements.confirmPassword.value;
  if (newPassword !== confirmPassword) {
    message.textContent = 'Passwords must match.';
    return;
  }
  const button = form.querySelector('[type="submit"]');
  button.disabled = true;
  try {
    await submitPasswordRequest('reset-password', { resetToken: passwordResetToken, newPassword, confirmPassword });
    returnToLogin();
    document.querySelector('#login-error').textContent = 'Password updated. Sign in with your new password.';
  } catch (error) {
    message.textContent = error instanceof TypeError ? 'Cannot reach the server. Please try again.' : error.message;
  } finally { button.disabled = false; }
});
showAuthForm('login-form');

document.querySelector('#dashboard-open-leads')?.addEventListener('click', openLeadsPage);

menuButton?.addEventListener('click', () => {
  sidebar.classList.toggle('open');
});

document.querySelectorAll('.nav-item').forEach((item) => {
  item.addEventListener('click', () => sidebar.classList.remove('open'));
});

async function loadLeads() {
  if (!leadTableBody) return;
  leadTableBody.innerHTML = '<div class="lead-empty">Loading your leads...</div>';
  try {
    const response = await fetch('/api/leads');
    if (!response.ok) throw new Error('Leads could not be loaded');
    leads = await response.json();
    renderLeads();
  } catch (error) {
    leadTableBody.innerHTML = '<div class="lead-empty">Could not load leads. Check that the server is running.</div>';
    leadResultCount.textContent = 'Unavailable';
  }
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>'"]/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[character]));
}

function renderLeads() {
  const search = String(document.querySelector('#lead-search')?.value || '').toLowerCase().trim();
  const status = document.querySelector('#lead-status-filter')?.value || '';
  const category = document.querySelector('#lead-category-filter')?.value || '';
  const filtered = leads.filter((lead) => {
    const searchable = `${lead.customerName} ${lead.mobileNumber} ${lead.interestedProperty}`.toLowerCase();
    return (!search || searchable.includes(search)) && (!status || lead.leadStatus === status) && (!category || lead.category === category);
  });
  leadResultCount.textContent = `${filtered.length} lead${filtered.length === 1 ? '' : 's'}`;
  const navCount = document.querySelector('#lead-nav-count');
  if (navCount) navCount.textContent = leads.length;
  if (!filtered.length) {
    leadTableBody.innerHTML = '<div class="lead-empty">No leads match your filters.</div>';
    return;
  }
  leadTableBody.innerHTML = filtered.map((lead) => {
    const initials = escapeHtml(String(lead.customerName || '?').split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase());
    const statusClass = `status-${String(lead.leadStatus || 'new').toLowerCase().replace(/\s+/g, '-')}`;
    const categoryClass = `category-${String(lead.category || 'warm').toLowerCase()}`;
    return `<div class="lead-row" data-lead-id="${lead.id}"><div class="lead-person"><span class="lead-avatar">${initials}</span><div><strong>${escapeHtml(lead.customerName)}</strong><small>${escapeHtml(lead.mobileNumber || lead.email || 'No contact details')}</small></div></div><div class="lead-interest"><strong>${escapeHtml(lead.interestedProperty || 'No property selected')}</strong><small>${escapeHtml(lead.preferredLocation || 'Location not specified')}</small></div><span class="lead-status ${statusClass}">${escapeHtml(lead.leadStatus || 'New')}</span><span class="lead-category ${categoryClass}">${escapeHtml(lead.category || 'Warm')}</span><span class="lead-follow-up">${escapeHtml(lead.nextFollowUpDate || 'Not scheduled')}</span><button class="lead-action" data-lead-id="${lead.id}" aria-label="Open ${escapeHtml(lead.customerName)}">→</button></div>`;
  }).join('');
}

function openLeadsPage() {
  closeCustomersPage();
  sessionStorage.setItem('realEstatePage', 'leads');
  userPage.classList.remove('active');
  window.scrollTo(0, 0);
  document.querySelector('.main-content').classList.remove('users-mode');
  document.querySelector('.main-content').classList.add('leads-mode');
  overviewContent.style.display = 'none';
  leadPage.classList.add('active');
  document.querySelector('.breadcrumb strong').textContent = 'Leads';
  document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.getAttribute('href') === '#leads'));
  loadLeads();
}

function closeLeadsPage() {
  document.querySelector('.main-content').classList.remove('leads-mode');
  leadPage.classList.remove('active');
  overviewContent.style.display = '';
  document.querySelector('.breadcrumb strong').textContent = 'Overview';
  document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.getAttribute('href') === '#overview'));
}

async function loadUsers() {
  if (!userTableBody) return;
  userTableBody.innerHTML = '<div class="lead-empty">Loading users...</div>';
  try {
    const response = await fetch('/api/users');
    if (!response.ok) throw new Error('Users could not be loaded');
    users = await response.json();
    userTableBody.innerHTML = users.length ? users.map((user) => {
      const initials = escapeHtml(String(user.name || '?').split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase());
      return `<div class="user-row-record"><div class="user-person"><span class="user-record-avatar">${initials}</span><strong>${escapeHtml(user.name)}</strong></div><span>${escapeHtml(user.emailId)}</span><span>${escapeHtml(user.phoneNumber || 'Not provided')}</span><span class="user-role">${escapeHtml(user.role)}</span><button class="user-action" data-user-id="${user.id}" aria-label="Delete ${escapeHtml(user.name)}">×</button></div>`;
    }).join('') : '<div class="lead-empty">No users added yet.</div>';
  } catch (error) {
    userTableBody.innerHTML = '<div class="lead-empty">Could not load users. Check that the server is running.</div>';
  }
}

function openUsersPage(event) {
  closeCustomersPage();
  sessionStorage.setItem('realEstatePage', 'users');
  event?.preventDefault();
  window.scrollTo(0, 0);
  document.querySelector('.main-content').classList.remove('leads-mode');
  document.querySelector('.main-content').classList.add('users-mode');
  overviewContent.style.display = 'none';
  leadPage.classList.remove('active');
  userPage.classList.add('active');
  document.querySelector('.breadcrumb strong').textContent = 'User management';
  document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.getAttribute('href') === '#users'));
  loadUsers();
}

function closeUsersPage() {
  document.querySelector('.main-content').classList.remove('users-mode');
  userPage.classList.remove('active');
  overviewContent.style.display = '';
  document.querySelector('.breadcrumb strong').textContent = 'Overview';
  document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.getAttribute('href') === '#overview'));
}

document.querySelector('.nav-item[href="#leads"]')?.addEventListener('click', (event) => {
  event.preventDefault();
  openLeadsPage();
});

function openDashboard(event) {
  event?.preventDefault();
  sessionStorage.setItem('realEstatePage', 'overview');
  closeLeadsPage();
  closeUsersPage();
  closeCustomersPage();
  sidebar?.classList.remove('open');
  window.scrollTo(0, 0);
}

document.querySelector('.nav-item[href="#overview"]')?.addEventListener('click', openDashboard);
document.querySelector('.brand-mark')?.addEventListener('click', openDashboard);

document.querySelector('.nav-item[href="#users"]')?.addEventListener('click', openUsersPage);

function closeCustomersPage() {
  customerPage.classList.remove('active');
  document.querySelector('.main-content').classList.remove('customers-mode');
}

function openCustomersPage(event) {
  event?.preventDefault();
  closeLeadsPage();
  closeUsersPage();
  overviewContent.style.display = 'none';
  customerPage.classList.add('active');
  document.querySelector('.main-content').classList.add('customers-mode');
  document.querySelector('.breadcrumb strong').textContent = 'Customers';
  document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.getAttribute('href') === '#customers'));
  sidebar?.classList.remove('open');
  sessionStorage.setItem('realEstatePage', 'customers');
  window.scrollTo(0, 0);
  loadCustomers();
}

async function loadCustomers() {
  const body = document.querySelector('#customer-table-body');
  body.innerHTML = '<div class="lead-empty">Loading customers...</div>';
  try {
    const response = await fetch('/api/customers');
    if (!response.ok) throw new Error('Customers could not be loaded. Please try again.');
    const customers = await response.json();
    body.innerHTML = customers.length ? customers.map((customer) =>
      `<div class="customer-record"><button type="button" data-customer-lead="${customer.leadId}"><strong>${escapeHtml(customer.name)}</strong></button><div>${escapeHtml(customer.email || 'No email')}<small>${escapeHtml(customer.mobileNumber || 'No phone')}</small></div><span>${escapeHtml(customer.interestedProperty || 'Not specified')}</span><span>${escapeHtml(customer.createdDate)}</span><button type="button" data-customer-lead="${customer.leadId}">Open customer</button></div>`
    ).join('') : '<div class="lead-empty">No customers yet. Save a lead with Advance Paid checked to convert it.</div>';
  } catch (error) {
    body.textContent = error.message;
  }
}

document.querySelector('.nav-item[href="#customers"]').addEventListener('click', openCustomersPage);
document.querySelector('#dashboard-open-customers').addEventListener('click', openCustomersPage);
document.querySelector('#customer-table-body').addEventListener('click', async (event) => {
  const button = event.target.closest('[data-customer-lead]');
  if (!button) return;
  await loadLeads();
  openLeadDetails(button.dataset.customerLead);
  document.querySelector('#lead-title').textContent = 'Customer details';
});

function closeUserModal() {
  userModal?.classList.remove('open');
  userModal?.setAttribute('aria-hidden', 'true');
}

document.querySelector('.add-new-user')?.addEventListener('click', () => {
  userForm.reset();
  document.querySelector('#user-title').textContent = 'Add new user';
  document.querySelector('.user-form-message').textContent = '';
  userModal?.classList.add('open');
  userModal?.setAttribute('aria-hidden', 'false');
  userForm.querySelector('[name="name"]')?.focus();
});
document.querySelector('.user-modal-close')?.addEventListener('click', closeUserModal);
document.querySelector('.user-cancel')?.addEventListener('click', closeUserModal);
userModal?.addEventListener('click', (event) => { if (event.target === userModal) closeUserModal(); });

userForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const message = document.querySelector('.user-form-message');
  message.textContent = 'Saving user...';
  const data = Object.fromEntries(new FormData(userForm).entries());
  try {
    const response = await fetch('/api/users', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
    if (!response.ok) {
      const details = await response.json().catch(() => null);
      throw new Error(details?.message || 'User could not be saved');
    }
    const savedUser = await response.json();
    message.textContent = `${savedUser.name} was added.`;
    await loadUsers();
    window.setTimeout(closeUserModal, 700);
  } catch (error) {
    message.textContent = error.message;
    message.style.color = '#ed725c';
  }
});

userTableBody?.addEventListener('click', async (event) => {
  const button = event.target.closest('.user-action');
  if (!button || !window.confirm('Delete this user?')) return;
  const response = await fetch(`/api/users/${button.dataset.userId}`, { method: 'DELETE' });
  if (response.ok) loadUsers();
});

function openNewLead() {
  LeadPayments.open(null);
  editingLeadId = null;
  leadForm.reset();
  renderLeadStatusRibbon('New');
  document.querySelector('#lead-title').textContent = 'Add a new lead';
  document.querySelector('.save-lead').innerHTML = 'Save lead <span>→</span>';
  document.querySelector('.delete-lead').style.display = 'none';
  leadModal?.classList.add('open');
  leadModal?.setAttribute('aria-hidden', 'false');
  leadForm?.querySelector('input[name="customerName"]')?.focus();
}

function openLeadDetails(leadId) {
  const lead = leads.find((item) => String(item.id) === String(leadId));
  if (!lead) return;
  LeadPayments.open(lead.id);
  editingLeadId = lead.id;
  leadForm.reset();
  document.querySelector('#lead-advance-paid').checked = lead.advancePaidEnabled === true;
  renderLeadStatusRibbon(lead.leadStatus || 'New');
  Object.keys(lead).forEach((field) => {
    const input = leadForm.querySelector(`[name="${field}"]`);
    if (input && input.type !== 'checkbox' && field !== 'leadStatus') input.value = lead[field] ?? '';
  });
  document.querySelector('#lead-title').textContent = 'Lead details';
  document.querySelector('.save-lead').innerHTML = 'Save changes <span>→</span>';
  document.querySelector('.delete-lead').style.display = 'inline-flex';
  document.querySelector('.lead-form-message').textContent = '';
  leadModal?.classList.add('open');
  leadModal?.setAttribute('aria-hidden', 'false');
}

document.querySelector('.add-new-lead')?.addEventListener('click', openNewLead);

function renderLeadStatusRibbon(currentStatus) {
  const select = document.querySelector('#lead-status-select');
  select.querySelector('option[data-current-status]')?.remove();
  if (!Array.from(select.options).some((option) => option.value === currentStatus)) {
    const option = new Option(currentStatus, currentStatus);
    option.dataset.currentStatus = 'true';
    select.add(option, 0);
  }
  select.value = currentStatus;
  const statuses = Array.from(select.options).map((option) => option.value);
  const currentIndex = statuses.indexOf(select.value);
  const ribbon = document.querySelector('#lead-status-options');
  ribbon.innerHTML = statuses.map((status, index) =>
    `<label class="lead-status-step${index < currentIndex ? ' is-complete' : ''}" title="${escapeHtml(status)}"><input type="radio" name="leadStatus" aria-label="${escapeHtml(status)}" value="${escapeHtml(status)}"${index === currentIndex ? ' checked' : ''}><span><b class="status-check" aria-hidden="true">&#10003;</b><span class="status-name">${escapeHtml(status)}</span></span></label>`
  ).join('');
  requestAnimationFrame(() => {
    const activeStep = ribbon.querySelector('input:checked')?.closest('.lead-status-step');
    if (activeStep) ribbon.scrollLeft = activeStep.offsetLeft - ribbon.offsetLeft;
  });
}

document.querySelector('#lead-status-select').addEventListener('change', (event) => {
  renderLeadStatusRibbon(event.target.value);
});
document.querySelector('#lead-status-options').addEventListener('change', (event) => {
  if (event.target.matches('input[name="leadStatus"]')) renderLeadStatusRibbon(event.target.value);
});
// Include every dropdown status in the list filter as well.
for (const option of document.querySelector('#lead-status-select').options) {
  const filter = document.querySelector('#lead-status-filter');
  if (!Array.from(filter.options).some((item) => item.value === option.value)) {
    filter.add(new Option(option.text, option.value));
  }
}
leadTableBody?.addEventListener('click', (event) => {
  const row = event.target.closest('.lead-row');
  if (row) openLeadDetails(row.dataset.leadId);
});

document.querySelector('#lead-search')?.addEventListener('input', renderLeads);
document.querySelector('#lead-status-filter')?.addEventListener('change', renderLeads);
document.querySelector('#lead-category-filter')?.addEventListener('change', renderLeads);

document.querySelector('.logout-link')?.addEventListener('click', (event) => {
  event.preventDefault();
  sessionStorage.removeItem('realEstateUser');
  sessionStorage.removeItem('realEstatePage');
  appShell.style.display = 'none';
  loginScreen.style.display = 'flex';
});

document.querySelector('.top-logout')?.addEventListener('click', () => {
  sessionStorage.removeItem('realEstateUser');
  sessionStorage.removeItem('realEstatePage');
  appShell.style.display = 'none';
  loginScreen.style.display = 'flex';
});

function updateTaskProgress() {
  const total = taskRows.length;
  const completed = document.querySelectorAll('.task-row.completed').length;
  const percentage = Math.round((completed / total) * 100);
  taskProgress.style.width = `${percentage}%`;
  progressCopy.textContent = `${completed} of ${total} completed`;
  progressPercent.textContent = `${percentage}%`;
}

taskRows.forEach((row) => {
  row.addEventListener('click', () => {
    row.classList.toggle('completed');
    row.querySelector('input').checked = row.classList.contains('completed');
    row.querySelector('.fake-check').textContent = row.classList.contains('completed') ? '✓' : '';
    updateTaskProgress();
  });
});

addButton?.addEventListener('click', () => {
  modal?.classList.add('open');
  modal?.setAttribute('aria-hidden', 'false');
});

function closeModal() {
  modal?.classList.remove('open');
  modal?.setAttribute('aria-hidden', 'true');
}

function closeLeadModal() {
  if (LeadPayments.isDirty() && !window.confirm('Discard unsaved payment changes?')) return;
  LeadPayments.close();
  leadModal?.classList.remove('open');
  leadModal?.setAttribute('aria-hidden', 'true');
}

document.querySelector('.modal-close')?.addEventListener('click', closeModal);
modal?.addEventListener('click', (event) => {
  if (event.target === modal) closeModal();
});

document.querySelector('.lead-modal-close')?.addEventListener('click', closeLeadModal);
document.querySelector('.lead-cancel')?.addEventListener('click', closeLeadModal);
leadModal?.addEventListener('click', (event) => {
  if (event.target === leadModal) closeLeadModal();
});

leadForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const message = document.querySelector('.lead-form-message');
  const data = Object.fromEntries(new FormData(leadForm).entries());
  data.minimumBudget = data.minimumBudget ? Number(data.minimumBudget) : null;
  data.maximumBudget = data.maximumBudget ? Number(data.maximumBudget) : null;
  data.advancePaidEnabled = document.querySelector('#lead-advance-paid').checked;
  const previousLead = leads.find((lead) => String(lead.id) === String(editingLeadId));
  const needsConfirmation = data.advancePaidEnabled && !previousLead?.advancePaidEnabled;
  if (needsConfirmation && !window.confirm('Confirm advance payment and convert this lead to a customer? A customer record will be created if this lead has not already been converted.')) return;
  const submitButtons = leadForm.querySelectorAll('button[type="submit"]');
  submitButtons.forEach((button) => { button.disabled = true; });
  message.textContent = 'Saving lead...';
  try {
    await LeadPayments.saveIfDirty();
    const endpoint = editingLeadId ? `/api/leads/${editingLeadId}` : '/api/leads';
    const response = await fetch(`${endpoint}?conversionConfirmed=${needsConfirmation}`, {
      method: editingLeadId ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    if (!response.ok) {
      const details = await response.json().catch(() => null);
      throw new Error(details?.message || 'Lead could not be saved');
    }
    const savedLead = await response.json();
    if (data.advancePaidEnabled) loadCustomers();
    message.textContent = `${savedLead.customerName} was ${editingLeadId ? 'updated' : 'added to Leads'}.`;
    leadForm.reset();
    editingLeadId = null;
    await loadLeads();
    if (needsConfirmation) message.textContent = 'Lead saved. The customer is now available under Customers.';
    window.setTimeout(closeLeadModal, 900);
  } catch (error) {
    message.textContent = error.message || 'Could not save lead. Check that the server is running.';
    message.style.color = '#ed725c';
  } finally {
    submitButtons.forEach((button) => { button.disabled = false; });
  }
});

document.querySelector('.delete-lead')?.addEventListener('click', async () => {
  if (!editingLeadId || !window.confirm('Delete this lead?')) return;
  const message = document.querySelector('.lead-form-message');
  try {
    const response = await fetch(`/api/leads/${editingLeadId}`, { method: 'DELETE' });
    if (!response.ok) {
      const details = await response.json().catch(() => null);
      throw new Error(details?.message || 'Lead could not be deleted');
    }
    editingLeadId = null;
    closeLeadModal();
    await loadLeads();
  } catch (error) {
    message.textContent = error.message;
    message.style.color = '#ed725c';
  }
});

document.querySelectorAll('.add-options button').forEach((option) => {
  option.addEventListener('click', () => {
    const label = option.querySelector('strong').textContent;
    modalTitle.textContent = `${label} added to workspace`;
    window.setTimeout(closeModal, 900);
  });
});

document.querySelector('.add-contact')?.addEventListener('click', () => {
  modal?.classList.add('open');
  modal?.setAttribute('aria-hidden', 'false');
  modalTitle.textContent = 'Add a contact';
});

document.querySelector('.upload-document')?.addEventListener('click', () => {
  modal?.classList.add('open');
  modal?.setAttribute('aria-hidden', 'false');
  modalTitle.textContent = 'Upload a document';
});

document.querySelectorAll('.date-filter, .filter-button').forEach((button) => {
  button.addEventListener('click', () => {
    button.classList.toggle('selected');
    button.style.borderColor = button.classList.contains('selected') ? '#ed725c' : '';
  });
});

// Restore this tab's workspace after all page handlers have been initialized.
const savedWorkspaceUser = sessionStorage.getItem('realEstateUser');
if (savedWorkspaceUser) {
  let savedUser;
  try {
    savedUser = JSON.parse(savedWorkspaceUser);
  } catch {
    sessionStorage.removeItem('realEstateUser');
    sessionStorage.removeItem('realEstatePage');
  }
  if (savedUser && savedUser.id && savedUser.emailId) {
    const savedPage = sessionStorage.getItem('realEstatePage');
    showWorkspace(savedUser);
    if (savedPage === 'leads') openLeadsPage();
    if (savedPage === 'users') openUsersPage();
    if (savedPage === 'customers') openCustomersPage();
  }
}
