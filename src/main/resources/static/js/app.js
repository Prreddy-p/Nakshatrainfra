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
const settingsPage = document.querySelector('#settings-page');
const customerPage = document.querySelector('#customers-page');
const userTableBody = document.querySelector('#user-table-body');
const userModal = document.querySelector('.user-modal-backdrop');
const userForm = document.querySelector('#user-form');
let users = [];
let editingUserId = null;
let leads = [];
let editingLeadId = null;
let conversionAdvance = null;

function requestAdvanceConversion() {
  const checkbox = document.querySelector('#lead-advance-paid');
  const summary = document.querySelector('#lead-advance-summary');
  const entered = window.prompt('Enter the advance amount received (₹):', conversionAdvance || '');
  if (entered === null) { checkbox.checked = false; conversionAdvance = null; summary.textContent = ''; return false; }
  const amount = entered.trim();
  if (!/^\d{1,13}(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0 || Number(amount) > 9999999999999.99) {
    checkbox.checked = false; conversionAdvance = null; summary.textContent = '';
    AppNotice.error('Enter an advance greater than zero, with at most two decimal places.');
    return false;
  }
  const formatted = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(amount));
  if (!window.confirm(`Advance received: ${formatted}. Save this lead and convert it to a customer? This amount will appear in the customer's Payment Details.`)) {
    checkbox.checked = false; conversionAdvance = null; summary.textContent = ''; return false;
  }
  conversionAdvance = amount;
  summary.textContent = `Advance: ${formatted}. Conversion confirmed; saving the lead records this payment.`;
  return true;
}

document.querySelector('#lead-advance-paid').addEventListener('change', event => {
  if (!event.target.checked) {
    conversionAdvance = null;
    document.querySelector('#lead-advance-summary').textContent = '';
    return;
  }
  const existing = leads.find(lead => String(lead.id) === String(editingLeadId));
  if (existing?.advancePaidEnabled) {
    document.querySelector('#lead-advance-summary').textContent = 'Manage the saved advance in the customer payment section.';
    return;
  }
  if (requestAdvanceConversion()) leadForm.requestSubmit();
});
const loginScreen = document.querySelector('#login-screen');
const appShell = document.querySelector('#app-shell');

let greetingTimer;

function showWorkspace(user) {
  const isAssociate = String(user.role || '').trim().toLowerCase() === 'associate';
  document.querySelectorAll('[data-manager-nav]').forEach(item => { item.hidden = isAssociate; });
  const profile = document.querySelector('.sidebar-footer .user-row');
  if (profile) {
    profile.querySelector('strong').textContent = user.name || user.emailId || 'User';
    profile.querySelector('div > span').textContent = user.role || '';
    profile.querySelector('.user-avatar').textContent = String(user.name || user.emailId || 'U')
      .trim().split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase();
  }
  const welcomeMessage = document.querySelector('#welcome-message');
  const updateGreeting = () => {
    const greeting = new Date().getHours() < 12 ? 'Good morning' : 'Good evening';
    if (welcomeMessage) welcomeMessage.textContent = `${greeting}, ${user.name || user.emailId || 'there'}.`;
  };
  clearInterval(greetingTimer);
  updateGreeting();
  greetingTimer = setInterval(updateGreeting, 60000);
  loginScreen.style.display = 'none';
  appShell.style.display = 'flex';
  sessionStorage.setItem('realEstateUser', JSON.stringify(user));
  openDashboard();
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
      if (response.status === 401) {
        throw new Error('Invalid email, password, or role. Use an account registered in this hosted application.');
      }
      if (response.status === 404 || response.status === 405) {
        throw new Error('The login API is unavailable. Deploy the Spring Boot application and route /api requests to it.');
      }
      if (response.status >= 500) {
        throw new Error('The application server could not complete login. Check the hosted server logs and database connection.');
      }
      const details = await response.json().catch(() => null);
      throw new Error(details?.message || `Sign-in request failed (HTTP ${response.status}).`);
    }
    if (!response.headers.get('content-type')?.includes('application/json')) {
      throw new Error('The login API returned a web page instead of JSON. Check that /api requests reach the Spring Boot application.');
    }
    const result = await response.json();
    if (result.passwordChangeRequired) {
      passwordResetToken = result.resetToken;
      showAuthForm('change-password-form');
      document.querySelector('#change-password-form [name="newPassword"]').focus();
      AppNotice.success('Temporary password verified. Choose a new password.');
    } else {
      showWorkspace(result);
      AppNotice.success('Signed in successfully.');
    }
  } catch (loginError) {
    error.textContent = loginError instanceof TypeError
      ? 'Cannot reach the application server. Check your connection and confirm the hosted backend is running.'
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
    AppNotice.success(result.message);
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
    AppNotice.success('Password updated successfully.');
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
  ['followup-nav-count', 'visit-nav-count'].forEach(id => {
    const badge = document.getElementById(id);
    badge.textContent = '—';
    badge.title = 'Loading count...';
  });
  try {
    const response = await fetch('/api/leads');
    if (!response.ok) throw new Error('Leads could not be loaded');
    leads = await response.json();
    const normalizedStatus = lead => String(lead.leadStatus || '').toLowerCase().replace(/[\s-]/g, '');
    updateTileCount('lead-tile-count', leads.length);
    const visits = leads.filter(lead => normalizedStatus(lead) === 'sitevisit');
    updateTileCount('visit-tile-count', visits.length);
    updateTileCount('visit-nav-count', visits.length);
    const followups = leads.filter(lead => normalizedStatus(lead) === 'followup');
    updateTileCount('followup-tile-count', followups.length);
    updateTileCount('followup-nav-count', followups.length);
    renderLeads();
  } catch (error) {
    leadTableBody.innerHTML = '<div class="lead-empty">Could not load leads. Check that the server is running.</div>';
    leadResultCount.textContent = 'Unavailable';
    ['lead-tile-count', 'visit-tile-count', 'followup-tile-count', 'visit-nav-count', 'followup-nav-count'].forEach(id => updateTileCount(id, null));
  }
}

function updateTileCount(id, count) {
  const element = document.getElementById(id);
  if (!element) return;
  element.textContent = count === null ? '—' : count.toLocaleString();
  if (count === null) element.title = 'Count could not be loaded. Reopen Overview to retry.';
  else element.removeAttribute('title');
}

let pendingPaymentsGeneration = 0;
async function loadPaymentCount() {
  const generation = ++pendingPaymentsGeneration;
  const body = document.getElementById('pending-payment-body');
  body.innerHTML = '<tr><td colspan="6">Loading pending payments...</td></tr>';
  try {
    const response = await fetch('/api/customers');
    if (!response.ok) throw new Error('Payment count unavailable');
    const customers = await response.json();
    const balances = await Promise.all(customers.map(async customer => {
      const paymentResponse = await fetch(`/api/leads/${customer.leadId}/payments`);
      if (!paymentResponse.ok) throw new Error('Customer balance unavailable');
      return { customer, details: await paymentResponse.json() };
    }));
    if (generation !== pendingPaymentsGeneration) return;
    const pending = balances.filter(({ details }) => details.remainingAmount != null && Number(details.remainingAmount) > 0);
    pending.sort((a, b) => String(a.customer.name || '').localeCompare(String(b.customer.name || '')));
    updateTileCount('payment-tile-count', pending.length);
    const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
    body.innerHTML = pending.length ? pending.map(({ customer, details }) => `<tr>
      <td>${escapeHtml(customer.name || 'Not specified')}</td><td>${escapeHtml(customer.interestedProperty || 'Not specified')}</td>
      <td>${escapeHtml(currency.format(Number(details.totalAssetValue)))}</td>
      <td>${escapeHtml(currency.format(Number(details.totalPaid)))}</td>
      <td><strong>${escapeHtml(currency.format(Number(details.remainingAmount)))}</strong></td>
      <td><button type="button" class="secondary-button" data-payment-customer-lead="${escapeHtml(customer.leadId)}">View payments</button></td>
    </tr>`).join('') : '<tr><td colspan="6">No customers with a remaining balance.</td></tr>';
  } catch {
    if (generation !== pendingPaymentsGeneration) return;
    updateTileCount('payment-tile-count', null);
    body.innerHTML = '<tr><td colspan="6">Could not load pending payments. Select Refresh to retry.</td></tr>';
  }
}
document.addEventListener('payments-updated', loadPaymentCount);
document.getElementById('pending-payment-body').addEventListener('click', async event => {
  const button = event.target.closest('[data-payment-customer-lead]');
  if (!button) return;
  button.disabled = true;
  try {
    await loadLeads();
    if (!leads.some(lead => String(lead.id) === button.dataset.paymentCustomerLead)) {
      AppNotice.error('Customer details could not be loaded. Refresh and try again.');
      return;
    }
    openLeadDetails(button.dataset.paymentCustomerLead, { customer: true });
  } finally { button.disabled = false; }
});

function closePaymentsPage() {
  document.getElementById('payments-page').classList.remove('active');
}
function openPaymentsPage(event) {
  window.SettingsPage?.close();
  window.PropertiesPage?.close();
  event?.preventDefault();
  closeLeadsPage(); closeUsersPage(); closeCustomersPage();
  overviewContent.style.display = 'none';
  document.getElementById('payments-page').classList.add('active');
  document.querySelector('.breadcrumb strong').textContent = 'Pending payments';
  document.querySelectorAll('.nav-item').forEach(item => item.classList.toggle('active', item.getAttribute('href') === '#payments'));
  sessionStorage.setItem('realEstatePage', 'payments');
  sidebar?.classList.remove('open');
  window.scrollTo(0, 0);
  loadPaymentCount();
}
document.getElementById('dashboard-open-payments').addEventListener('click', openPaymentsPage);
document.getElementById('dashboard-open-payments').addEventListener('keydown', event => {
  if (event.key === 'Enter' || event.key === ' ') openPaymentsPage(event);
});
document.querySelector('.nav-item[href="#payments"]').addEventListener('click', openPaymentsPage);
document.getElementById('refresh-pending-payments').addEventListener('click', loadPaymentCount);

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>'"]/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[character]));
}

function renderLeads() {
  const search = String(document.querySelector('#lead-search')?.value || '').toLowerCase().trim();
  const status = document.querySelector('#lead-status-filter')?.value || '';
  const category = document.querySelector('#lead-category-filter')?.value || '';
  const filtered = leads.filter((lead) => {
    const searchable = `${lead.customerName} ${lead.mobileNumber} ${lead.interestedProperty}`.toLowerCase();
    const normalize = value => String(value || '').toLowerCase().replace(/[\s-]/g, '');
    return (!search || searchable.includes(search)) && (!status || normalize(lead.leadStatus) === normalize(status)) && (!category || lead.category === category);
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

function openLeadsPage(mode) {
  window.SettingsPage?.close();
  window.PropertiesPage?.close();
  closePaymentsPage();
  const followupsOnly = mode === 'followups';
  const visitsOnly = mode === 'visits';
  const filteredView = followupsOnly || visitsOnly;
  const statusFilter = document.querySelector('#lead-status-filter');
  document.querySelector('#lead-search').value = '';
  document.querySelector('#lead-category-filter').value = '';
  statusFilter.value = followupsOnly ? 'Follow-up' : visitsOnly ? 'Site Visit' : '';
  statusFilter.disabled = filteredView;
  document.querySelector('#leads-page-title').textContent = followupsOnly ? 'Follow-up records' : visitsOnly ? 'Site visit records' : 'Lead management';
  document.querySelector('.lead-page-header .subhead').textContent = filteredView
    ? `All leads saved with ${visitsOnly ? 'Site Visit' : 'Followup'} status. Select a record to view or update it.`
    : 'Capture, qualify, and follow every opportunity.';
  closeCustomersPage();
  sessionStorage.setItem('realEstatePage', followupsOnly ? 'followups' : visitsOnly ? 'visits' : 'leads');
  userPage.classList.remove('active');
  window.scrollTo(0, 0);
  document.querySelector('.main-content').classList.remove('users-mode');
  document.querySelector('.main-content').classList.add('leads-mode');
  overviewContent.style.display = 'none';
  leadPage.classList.add('active');
  document.querySelector('.breadcrumb strong').textContent = followupsOnly ? 'Follow-ups' : visitsOnly ? 'Site visits' : 'Leads';
  document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.getAttribute('href') === (followupsOnly ? '#follow-ups' : visitsOnly ? '#site-visits' : '#leads')));
  loadLeads();
}

document.querySelector('#dashboard-open-followups').addEventListener('click', () => openLeadsPage('followups'));
document.querySelector('#dashboard-open-visits').addEventListener('click', () => openLeadsPage('visits'));
document.querySelector('#dashboard-open-visits').addEventListener('keydown', event => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    openLeadsPage('visits');
  }
});
document.querySelector('.nav-item[href="#site-visits"]').addEventListener('click', event => {
  event.preventDefault();
  openLeadsPage('visits');
});
document.querySelector('#dashboard-open-followups').addEventListener('keydown', event => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    openLeadsPage('followups');
  }
});
document.querySelector('.nav-item[href="#follow-ups"]').addEventListener('click', event => {
  event.preventDefault();
  openLeadsPage('followups');
});

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
      return `<div class="user-row-record"><div class="user-person"><span class="user-record-avatar">${initials}</span><strong>${escapeHtml(user.name)}</strong></div><span>${escapeHtml(user.emailId)}</span><span>${escapeHtml(user.phoneNumber || 'Not provided')}</span><span class="user-role">${escapeHtml(user.role)}</span><div class="user-actions"><button type="button" class="user-edit" data-user-id="${user.id}">Edit</button><button class="user-action" data-user-id="${user.id}" aria-label="Delete ${escapeHtml(user.name)}">×</button></div></div>`;
    }).join('') : '<div class="lead-empty">No users added yet.</div>';
  } catch (error) {
    userTableBody.innerHTML = '<div class="lead-empty">Could not load users. Check that the server is running.</div>';
  }
}

function openUsersPage(event) {
  window.SettingsPage?.close();
  window.PropertiesPage?.close();
  closePaymentsPage();
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
  window.SettingsPage?.close();
  window.PropertiesPage?.close();
  closePaymentsPage();
  event?.preventDefault();
  sessionStorage.setItem('realEstatePage', 'overview');
  closeLeadsPage();
  closeUsersPage();
  closeCustomersPage();
  loadCustomers();
  loadLeads();
  loadPaymentCount();
  sidebar?.classList.remove('open');
  window.scrollTo(0, 0);
}

document.querySelector('.nav-item[href="#overview"]')?.addEventListener('click', openDashboard);
document.querySelector('.brand-mark')?.addEventListener('click', openDashboard);

document.querySelector('.nav-item[href="#users"]')?.addEventListener('click', openUsersPage);

async function loadPasswordResetEmail() {
  const input = document.querySelector('#password-reset-config-email');
  const message = document.querySelector('#password-reset-email-message');
  message.textContent = 'Loading saved sender address...';
  try {
    const response = await fetch('/api/settings/password-reset-email');
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || 'Settings could not be loaded.');
    input.value = result.email || '';
    message.textContent = '';
  } catch (error) {
    message.textContent = error.message || 'Settings could not be loaded.';
  }
}

function openSettingsPage(event) {
  window.PropertiesPage?.close();
  closePaymentsPage(); closeLeadsPage(); closeUsersPage(); closeCustomersPage();
  event?.preventDefault();
  document.querySelector('.main-content').classList.remove('leads-mode', 'users-mode', 'customers-mode');
  overviewContent.style.display = 'none';
  settingsPage.classList.add('active');
  document.querySelector('.breadcrumb strong').textContent = 'Settings';
  document.querySelectorAll('.nav-item').forEach(item => item.classList.toggle('active', item.getAttribute('href') === '#settings'));
  sessionStorage.setItem('realEstatePage', 'settings');
  sidebar?.classList.remove('open');
  window.scrollTo(0, 0);
  loadPasswordResetEmail();
}

function closeSettingsPage() {
  settingsPage?.classList.remove('active');
  if (sessionStorage.getItem('realEstatePage') === 'settings') sessionStorage.setItem('realEstatePage', 'overview');
}

window.SettingsPage = { close: closeSettingsPage };
document.querySelector('.nav-item[href="#settings"]')?.addEventListener('click', openSettingsPage);
document.querySelector('#password-reset-email-form')?.addEventListener('submit', async event => {
  event.preventDefault();
  const button = document.querySelector('#save-password-reset-email');
  const message = document.querySelector('#password-reset-email-message');
  const email = document.querySelector('#password-reset-config-email').value.trim();
  button.disabled = true;
  message.textContent = 'Saving...';
  try {
    const response = await fetch('/api/settings/password-reset-email', {
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || 'Settings could not be saved.');
    message.textContent = 'Password reset sender email saved.';
    AppNotice.success('Password reset sender email saved.');
  } catch (error) {
    message.textContent = error.message || 'Settings could not be saved.';
  } finally { button.disabled = false; }
});

function closeCustomersPage() {
  customerPage.classList.remove('active');
  document.querySelector('.main-content').classList.remove('customers-mode');
}

function openCustomersPage(event) {
  window.SettingsPage?.close();
  window.PropertiesPage?.close();
  closePaymentsPage();
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
  const counts = document.querySelectorAll('#customer-tile-count, #customer-nav-count');
  counts.forEach(count => { count.textContent = '—'; count.title = 'Loading customer count...'; });
  body.innerHTML = '<div class="lead-empty">Loading customers...</div>';
  try {
    const response = await fetch('/api/customers');
    if (!response.ok) throw new Error('Customers could not be loaded. Please try again.');
    const customers = await response.json();
    counts.forEach(count => {
      count.textContent = customers.length.toLocaleString();
      count.removeAttribute('title');
    });
    body.innerHTML = customers.length ? customers.map((customer) =>
      `<div class="customer-record"><button type="button" data-customer-lead="${customer.leadId}"><strong>${escapeHtml(customer.name)}</strong></button><div>${escapeHtml(customer.email || 'No email')}<small>${escapeHtml(customer.mobileNumber || 'No phone')}</small></div><span>${escapeHtml(customer.interestedProperty || 'Not specified')}</span><span>${escapeHtml(customer.createdDate)}<span class="record-audit">${renderRecordAudit(customer)}</span></span><button type="button" data-customer-lead="${customer.leadId}">Open customer</button></div>`
    ).join('') : '<div class="lead-empty">No customers yet. Save a lead with Advance Paid checked to convert it.</div>';
  } catch (error) {
    body.textContent = error.message;
    counts.forEach(count => {
      count.textContent = '—';
      count.title = 'Customer count could not be loaded. Reopen Customers to retry.';
    });
  }
}

document.querySelector('.nav-item[href="#customers"]').addEventListener('click', openCustomersPage);
document.querySelector('#dashboard-open-customers').addEventListener('click', openCustomersPage);
document.querySelector('#customer-table-body').addEventListener('click', async (event) => {
  const button = event.target.closest('[data-customer-lead]');
  if (!button) return;
  await loadLeads();
  openLeadDetails(button.dataset.customerLead, { customer: true });
});

function closeUserModal() {
  userModal?.classList.remove('open');
  userModal?.setAttribute('aria-hidden', 'true');
}

function openUserForm(user = null) {
  editingUserId = user?.id ?? null;
  userForm.reset();
  for (const field of ['name', 'emailId', 'phoneNumber', 'role']) {
    userForm.elements[field].value = user?.[field] || '';
  }
  userForm.elements.password.required = !user;
  userForm.elements.password.placeholder = user ? 'Leave blank to keep current password' : 'Minimum 6 characters';
  document.querySelector('#user-title').textContent = user ? 'Edit user' : 'Add new user';
  document.querySelector('.user-form-message').textContent = '';
  userModal?.classList.add('open');
  userModal?.setAttribute('aria-hidden', 'false');
  userForm.querySelector('[name="name"]')?.focus();
}
document.querySelector('.add-new-user')?.addEventListener('click', () => openUserForm());
document.querySelector('.user-modal-close')?.addEventListener('click', closeUserModal);
document.querySelector('.user-cancel')?.addEventListener('click', closeUserModal);
userModal?.addEventListener('click', (event) => { if (event.target === userModal) closeUserModal(); });

userForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const message = document.querySelector('.user-form-message');
  const button = userForm.querySelector('[type="submit"]');
  if (button.disabled) return;
  button.disabled = true;
  message.style.color = '';
  message.textContent = 'Saving user...';
  const data = Object.fromEntries(new FormData(userForm).entries());
  try {
    const response = await fetch(editingUserId === null ? '/api/users' : `/api/users/${editingUserId}`, { method: editingUserId === null ? 'POST' : 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
    if (!response.ok) {
      const details = await response.json().catch(() => null);
      throw new Error(details?.message || 'User could not be saved');
    }
    const savedUser = await response.json();
    AppNotice.success(editingUserId === null ? 'User created successfully.' : 'User updated successfully.');
    closeUserModal();
    await loadUsers();

  } catch (error) {
    message.textContent = error instanceof TypeError
      ? 'Cannot reach the application server. Open http://localhost:8080/ and try again.'
      : error.message;
    message.style.color = '#ed725c';
  } finally {
    button.disabled = false;
  }
});

userTableBody?.addEventListener('click', async (event) => {
  const editButton = event.target.closest('.user-edit');
  if (editButton) {
    const user = users.find(item => String(item.id) === editButton.dataset.userId);
    if (user) openUserForm(user);
    return;
  }
  const button = event.target.closest('.user-action');
  if (!button || !window.confirm('Delete this user?')) return;
  button.disabled = true;
  try {
    const response = await fetch(`/api/users/${button.dataset.userId}`, { method: 'DELETE' });
    if (!response.ok) {
      const details = await response.json().catch(() => null);
      throw new Error(details?.message || 'User could not be deleted.');
    }
    AppNotice.success('User deleted successfully.');
    await loadUsers();
  } catch (error) {
    AppNotice.error(error instanceof TypeError ? 'Cannot reach the server. Please try again.' : error.message);
  } finally { button.disabled = false; }
});

let propertyLoadGeneration = 0;
async function loadLeadProperties(selectedProperty = '') {
  const generation = ++propertyLoadGeneration;
  const select = document.getElementById('lead-interested-property');
  const help = document.getElementById('lead-property-help');
  const retry = document.getElementById('retry-lead-properties');
  select.replaceChildren(new Option('Select a property', ''));
  if (selectedProperty) select.add(new Option(selectedProperty, selectedProperty));
  select.value = selectedProperty;
  select.setAttribute('aria-busy', 'true');
  help.textContent = 'Loading properties...';
  retry.hidden = true;
  try {
    const response = await fetch('/api/properties');
    if (!response.ok) throw new Error('Could not load properties.');
    const properties = await response.json();
    if (generation !== propertyLoadGeneration) return;
    const current = select.value;
    select.replaceChildren(new Option('Select a property', ''));
    properties.sort((a, b) => String(a.propertyName || '').localeCompare(String(b.propertyName || '')));
    for (const property of properties) {
      if (!property.propertyName) continue;
      const label = property.city ? `${property.propertyName} — ${property.city}` : property.propertyName;
      select.add(new Option(label, property.propertyName));
    }
    if (current && !properties.some(property => property.propertyName === current)) {
      select.add(new Option(`${current} (previously saved)`, current));
    }
    select.value = current;
    help.textContent = properties.length ? '' : 'No properties available. Add a record under Properties first.';
  } catch {
    if (generation !== propertyLoadGeneration) return;
    help.textContent = 'Could not load properties. Your existing selection is preserved.';
    retry.hidden = false;
  } finally {
    if (generation === propertyLoadGeneration) select.removeAttribute('aria-busy');
  }
}
document.getElementById('retry-lead-properties').addEventListener('click', () => {
  loadLeadProperties(document.getElementById('lead-interested-property').value);
});

function renderRecordAudit(record) {
  const format = (name, timestamp) => {
    const date = timestamp ? new Date(timestamp) : null;
    const localTime = date && !Number.isNaN(date.getTime())
      ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'long' }).format(date) : 'Not recorded';
    return escapeHtml(name || 'Not recorded') + ' ? ' + escapeHtml(localTime);
  };
  return '<div><strong>Created by</strong><br>' + format(record.createdBy, record.createdAt)
    + '</div><div><strong>Last modified by</strong><br>' + format(record.lastModifiedBy, record.lastModifiedAt) + '</div>';
}

function openNewLead() {
  document.querySelector('#lead-audit').textContent = 'Created by and last modified by will be recorded when saved.';
  conversionAdvance = null;
  document.querySelector('#lead-advance-summary').textContent = '';
  LeadPayments.close();
  editingLeadId = null;
  leadForm.reset();
  loadLeadProperties();
  renderLeadStatusRibbon('New');
  document.querySelector('#lead-title').textContent = 'Add a new lead';
  document.querySelector('.save-lead').innerHTML = 'Save lead <span>→</span>';
  document.querySelector('.delete-lead').style.display = 'none';
  leadModal?.classList.add('open');
  leadModal?.setAttribute('aria-hidden', 'false');
  leadForm?.querySelector('input[name="customerName"]')?.focus();
}

function openLeadDetails(leadId, { customer = false } = {}) {
  const lead = leads.find((item) => String(item.id) === String(leadId));
  if (!lead) return;
  conversionAdvance = null;
  document.querySelector('#lead-advance-summary').textContent = '';
  LeadPayments.close();
  if (customer) LeadPayments.open(lead.id);
  editingLeadId = lead.id;
  document.querySelector('#lead-audit').innerHTML = renderRecordAudit(lead);
  leadForm.reset();
  document.querySelector('#lead-advance-paid').checked = lead.advancePaidEnabled === true;
  renderLeadStatusRibbon(lead.leadStatus || 'New');
  Object.keys(lead).forEach((field) => {
    const input = leadForm.querySelector(`[name="${field}"]`);
    if (input && input.type !== 'checkbox' && field !== 'leadStatus') input.value = lead[field] ?? '';
  });
  document.querySelector('#lead-title').textContent = customer ? 'Customer details' : 'Lead details';
  loadLeadProperties(lead.interestedProperty || '');
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

document.querySelector('.logout-link')?.addEventListener('click', async (event) => {
  event.preventDefault();
  await fetch('/api/auth/logout', { method: 'POST' }).catch(() => {});
  sessionStorage.removeItem('realEstateUser');
  sessionStorage.removeItem('realEstatePage');
  appShell.style.display = 'none';
  loginScreen.style.display = 'flex';
  window.location.reload();
});

document.querySelector('.top-logout')?.addEventListener('click', async () => {
  await fetch('/api/auth/logout', { method: 'POST' }).catch(() => {});
  sessionStorage.removeItem('realEstateUser');
  sessionStorage.removeItem('realEstatePage');
  appShell.style.display = 'none';
  loginScreen.style.display = 'flex';
  window.location.reload();
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
  if (needsConfirmation && conversionAdvance === null && !requestAdvanceConversion()) return;
  if (needsConfirmation) data.advanceAmount = conversionAdvance;
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
    if (data.advancePaidEnabled) { loadCustomers(); loadPaymentCount(); }
    message.textContent = `${savedLead.customerName} was ${editingLeadId ? 'updated' : 'added to Leads'}.`;
    AppNotice.success(needsConfirmation
      ? 'Lead saved. Customer is available under Customers.'
      : `${document.querySelector('#lead-title').textContent === 'Customer details' ? 'Customer' : 'Lead'} ${editingLeadId ? 'updated' : 'created'} successfully.`);
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
    AppNotice.success('Lead deleted successfully.');
    await loadLeads();
  } catch (error) {
    message.textContent = error.message;
    message.style.color = '#ed725c';
  }
});

document.querySelectorAll('.add-options button').forEach((option) => {
  option.addEventListener('click', () => {
    closeModal();
    if (['contact', 'deal'].includes(option.dataset.action)) {
      openNewLead();
      document.querySelector('.lead-form-message').textContent = 'Save this contact or opportunity as a lead.';
    } else {
      window.RecordStorage.open(option.dataset.action);
    }
  });
});

document.querySelector('.add-contact')?.addEventListener('click', () => {
  openNewLead();
  document.querySelector('.lead-form-message').textContent = 'Save this contact as a lead.';
});

document.querySelector('.upload-document')?.addEventListener('click', () => {
  window.RecordStorage.open('document');
});

document.querySelectorAll('.date-filter, .filter-button').forEach((button) => {
  button.addEventListener('click', () => {
    button.classList.toggle('selected');
    button.style.borderColor = button.classList.contains('selected') ? '#ed725c' : '';
  });
});

// Restore this tab's workspace after all page handlers have been initialized.
(async function restoreWorkspace() {
const savedWorkspaceUser = sessionStorage.getItem('realEstateUser');
if (savedWorkspaceUser) {
  let savedUser;
  try {
    savedUser = JSON.parse(savedWorkspaceUser);
  } catch {
    await fetch('/api/auth/logout', { method: 'POST' }).catch(() => {});
  sessionStorage.removeItem('realEstateUser');
    sessionStorage.removeItem('realEstatePage');
  }
  if (savedUser && savedUser.id && savedUser.emailId) {
    const session = await fetch('/api/auth/session').then(response => response.ok ? response.json() : null).catch(() => null);
    if (!session?.active) {
      document.querySelector('#login-error').textContent = 'Please sign in again to record your name on changes.';
      return;
    }
    const savedPage = sessionStorage.getItem('realEstatePage');
    showWorkspace(session.user);
    if (savedPage === 'leads') openLeadsPage();
    if (savedPage === 'followups') openLeadsPage('followups');
    if (savedPage === 'visits') openLeadsPage('visits');
    if (savedPage === 'users') openUsersPage();
    if (savedPage === 'customers') openCustomersPage();
    if (savedPage === 'payments') openPaymentsPage();
    if (savedPage === 'properties') window.PropertiesPage.open();
    if (savedPage === 'settings') openSettingsPage();
  }
}

})();
