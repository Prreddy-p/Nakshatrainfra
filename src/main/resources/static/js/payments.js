window.LeadPayments = (() => {
  const byId = (id) => document.getElementById(id);
  const editor = byId('payment-editor');
  const asset = byId('payment-asset');
  const advance = byId('payment-advance');
  const rows = byId('payment-installments');
  const message = byId('payment-message');
  const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
  let leadId = null, saved = null, dirty = false, generation = 0, pending = null;
  const format = (paise) => currency.format(paise / 100);

  function money(value, label) {
    if (!/^\d+(\.\d{1,2})?$/.test(value)) throw new Error(`${label}: enter a non-negative amount with up to two decimals.`);
    const [whole, fraction = ''] = value.split('.');
    const amount = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
    if (!Number.isSafeInteger(amount) || amount > 999999999999999) throw new Error(`${label} is too large.`);
    return amount;
  }
  const decimal = (paise) => (paise / 100).toFixed(2);

  function read() {
    const totalAsset = money(asset.value.trim(), 'Total Asset Value');
    if (totalAsset <= 0) throw new Error('Enter the asset value before recording payments.');
    const advanceAmount = money(advance.value.trim(), 'Advance Amount');
    if (advanceAmount > totalAsset) throw new Error('Advance Amount cannot exceed Total Asset Value.');
    let installmentsTotal = 0;
    const installments = Array.from(rows.children).map((row, index) => {
      const amount = money(row.querySelector('[data-amount]').value.trim(), `Installment ${index + 1}`);
      if (amount <= 0) throw new Error(`Installment ${index + 1} must be greater than zero.`);
      installmentsTotal += amount;
      if (advanceAmount + installmentsTotal > totalAsset) throw new Error('This payment exceeds the remaining balance. Reduce the amount before saving.');
      return { amount: decimal(amount), paymentDate: row.querySelector('[data-date]').value, notes: row.querySelector('[data-notes]').value };
    });
    return { totalAsset, advanceAmount, installmentsTotal, total: advanceAmount + installmentsTotal,
      remaining: totalAsset - advanceAmount - installmentsTotal, installments };
  }

  function recalculate() {
    try {
      const state = read();
      byId('payment-installments-total').textContent = format(state.installmentsTotal);
      byId('payment-total').textContent = format(state.total);
      byId('payment-remaining').textContent = format(state.remaining);
      byId('payment-status').textContent = state.remaining === 0 ? 'Fully Paid' : 'Balance Due';
      byId('payment-add').disabled = state.remaining === 0 || rows.children.length >= 500;
      byId('payment-save').disabled = false;
      message.textContent = '';
      return state;
    } catch (error) {
      byId('payment-status').textContent = 'Check payment amounts';
      for (const id of ['payment-installments-total', 'payment-total', 'payment-remaining']) byId(id).textContent = '—';
      byId('payment-add').disabled = true;
      byId('payment-save').disabled = true;
      message.textContent = error.message;
      return null;
    }
  }

  function addRow(item = {}) {
    const row = document.createElement('div');
    row.className = 'payment-installment';
    row.innerHTML = '<strong class="installment-number"></strong><label>Amount (₹)<input type="text" inputmode="decimal" data-amount autocomplete="off"></label><label>Payment date<input type="date" data-date></label><label>Notes / reference<input type="text" maxlength="1000" data-notes></label><button type="button" class="danger-button">Delete</button>';
    row.querySelector('[data-amount]').value = item.amount ?? '';
    const today = new Date();
    row.querySelector('[data-date]').value = item.paymentDate || `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    row.querySelector('[data-notes]').value = item.notes || '';
    row.querySelector('button').addEventListener('click', () => {
      row.remove(); renumber(); dirty = true; recalculate();
    });
    rows.append(row);
    renumber();
    return row;
  }

  function renumber() {
    Array.from(rows.children).forEach((row, i) => { row.querySelector('.installment-number').textContent = `#${i + 1}`; });
  }

  function populate(details) {
    asset.value = details.totalAssetValue ?? '';
    advance.value = details.advanceAmount ?? '0';
    rows.replaceChildren();
    details.installments.forEach(addRow);
    dirty = false;
    recalculate();
  }

  async function open(id) {
    const requestGeneration = ++generation;
    leadId = id; saved = null; dirty = false;
    editor.disabled = true;
    asset.value = ''; advance.value = '0'; rows.replaceChildren();
    for (const key of ['payment-installments-total', 'payment-total', 'payment-remaining']) byId(key).textContent = '—';
    byId('payment-status').textContent = '';
    message.textContent = id ? 'Loading payments...' : 'Save this lead first, then reopen it to add payment details.';
    if (!id) return;
    try {
      const response = await fetch(`/api/leads/${id}/payments`);
      if (!response.ok) throw new Error('Could not load payments. Close and reopen this record to retry.');
      const details = await response.json();
      if (requestGeneration !== generation) return;
      saved = details; populate(details); editor.disabled = false;
    } catch (error) {
      if (requestGeneration === generation) message.textContent = error.message;
    }
  }

  async function save() {
    if (pending) return pending;
    if (!leadId || !saved) throw new Error('Load the saved lead before adding payments.');
    const state = read();
    if (state.installments.some((item) => !item.paymentDate)) throw new Error('Each installment requires a payment date.');
    const id = leadId, requestGeneration = generation;
    editor.disabled = true;
    pending = (async () => {
      try {
        const response = await fetch(`/api/leads/${id}/payments`, {
          method: 'PUT', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ totalAssetValue: decimal(state.totalAsset), advanceAmount: decimal(state.advanceAmount), installments: state.installments, revision: saved.revision })
        });
        const result = await response.json().catch(() => null);
        if (!response.ok) throw new Error(result?.message || 'Payments could not be saved. Please try again.');
        if (requestGeneration === generation) {
          saved = result; populate(result); message.textContent = 'Payment details saved.';
        }
      } finally {
        pending = null;
        if (requestGeneration === generation) editor.disabled = false;
      }
    })();
    return pending;
  }

  editor.addEventListener('input', () => { dirty = true; recalculate(); });
  byId('payment-add').addEventListener('click', () => {
    const state = recalculate();
    if (!state || state.remaining <= 0) return;
    const row = addRow(); dirty = true; recalculate(); row.querySelector('[data-amount]').focus();
  });
  byId('payment-cancel').addEventListener('click', () => { if (saved) populate(saved); });
  byId('payment-save').addEventListener('click', async () => {
    try { await save(); } catch (error) { message.textContent = error.message; }
  });
  return {
    open, isDirty: () => dirty,
    close: () => { generation++; leadId = null; dirty = false; },
    saveIfDirty: async () => { if (pending) await pending; if (dirty) await save(); }
  };
})();
