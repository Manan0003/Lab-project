/**
 * Thapar University Dispensary Management System - Shared Client JavaScript
 */

const API_BASE = '';

// Session storage keys
const TOKEN_KEY = 'thapar_dispensary_token';
const USER_KEY = 'thapar_dispensary_user';

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getCurrentUser() {
  const u = localStorage.getItem(USER_KEY);
  return u ? JSON.parse(u) : null;
}

function setCurrentUser(token, user) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function logout() {
  const token = getToken();
  if (token) {
    fetch('/api/auth/logout', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token }
    }).catch(() => {});
  }
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = '/index.html';
}

async function apiCall(endpoint, method = 'GET', data = null) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  const options = { method, headers };
  if (data && (method === 'POST' || method === 'PUT')) {
    options.body = JSON.stringify(data);
  }

  try {
    const res = await fetch(API_BASE + endpoint, options);
    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.error || json.message || 'API error: ' + res.status);
    }
    return json;
  } catch (err) {
    console.error('API Error:', endpoint, err);
    throw err;
  }
}

// ==================== TOAST NOTIFICATIONS ====================
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : type === 'warning' ? '⚠️' : 'ℹ️';
  toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// ==================== QUICK DEMO LOGIN ====================
async function quickLogin(username, password, redirectUrl) {
  try {
    const resp = await apiCall('/api/auth/login', 'POST', { username, password });
    if (resp.success && resp.data) {
      setCurrentUser(resp.data.token, resp.data.user);
      showToast(`Logged in as ${resp.data.user.fullName}`, 'success');
      setTimeout(() => {
        window.location.href = redirectUrl || '/index.html';
      }, 400);
    }
  } catch (err) {
    showToast(err.message || 'Login failed', 'error');
  }
}

// ==================== BADGE HELPERS ====================
function getStatusBadge(status) {
  const s = (status || '').toUpperCase();
  if (s === 'AWAITING_ARRIVAL') {
    return `<span class="badge" style="background:#fffbeb; color:#b45309; border:1px solid #fde68a;">⏳ Awaiting Arrival</span>`;
  }
  if (s === 'EXPIRED') {
    return `<span class="badge" style="background:#fef2f2; color:#991b1b; border:1px solid #fecaca;">⚠️ Expired</span>`;
  }
  if (s === 'URGENT' || s === 'EMERGENCY') {
    return `<span class="badge badge-urgent">${status}</span>`;
  }
  if (s === 'CANCELLED') {
    return `<span class="badge badge-urgent">Cancelled</span>`;
  }
  if (s === 'WAITING' || s === 'PENDING') {
    return `<span class="badge badge-waiting">In Queue</span>`;
  }
  if (s === 'IN_CONSULTATION') {
    return `<span class="badge badge-consultation">In Consultation</span>`;
  }
  if (s === 'CONFIRMED' || s === 'COMPLETED' || s === 'DISPENSED') {
    return `<span class="badge badge-completed">${status}</span>`;
  }
  return `<span class="badge">${status}</span>`;
}

// ==================== SICK LEAVE & PRESCRIPTION SLIP MODAL ====================
function showPrescriptionSlip(p) {
  let modal = document.getElementById('slip-modal-container');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'slip-modal-container';
    modal.className = 'modal-backdrop';
    document.body.appendChild(modal);
  }

  const itemsHtml = (p.items || []).map((it, idx) => `
    <tr>
      <td>${idx + 1}</td>
      <td><strong>${it.medicineName}</strong></td>
      <td>${it.dosage}</td>
      <td>${it.duration}</td>
      <td>${it.quantity}</td>
      <td>${it.instructions || '-'}</td>
    </tr>
  `).join('');

  const sickLeaveBanner = p.sickLeaveIssued ? `
    <div style="background:#fef3c7; border:1px solid #f59e0b; border-radius:6px; padding:1rem; margin:1.25rem 0;">
      <h4 style="color:#92400e; margin-bottom:0.25rem;">🏥 OFFICIAL MEDICAL REST / SICK LEAVE RECOMMENDATION</h4>
      <p style="font-size:0.9rem; color:#78350f;">
        The student is advised complete medical rest for <strong>${p.sickLeaveDays} Day(s)</strong> 
        from <strong>${p.sickLeaveFrom || '-'}</strong> to <strong>${p.sickLeaveTo || '-'}</strong>.
        <br><em>Medical Advice: ${p.sickLeaveAdvice || 'Strict bed rest advised.'}</em>
      </p>
    </div>
  ` : '';

  modal.innerHTML = `
    <div class="modal" style="max-width:750px;">
      <div class="modal-header no-print">
        <h3>Digital Prescription & Medical Slip</h3>
        <button class="btn btn-sm btn-secondary" onclick="closeSlipModal()">✕ Close</button>
      </div>
      <div class="modal-body" id="printable-slip">
        <div class="printable-document">
          <div class="doc-header">
            <h2>THAPAR INSTITUTE OF ENGINEERING & TECHNOLOGY</h2>
            <h3>STUDENT & STAFF DISPENSARY HEALTH CARE CENTER</h3>
            <p>Dispensary Building, Opp. Hostel PG, Patiala - 147004, Punjab | Phone: +91 175 2393000</p>
          </div>
          
          <div style="display:flex; justify-content:space-between; margin-bottom:1rem; font-size:0.88rem; border-bottom:1px dashed #cbd5e1; padding-bottom:0.75rem;">
            <div>
              <strong>Rx ID:</strong> ${p.id}<br>
              <strong>Date:</strong> ${p.date || '-'}<br>
              <strong>Attending Doctor:</strong> ${p.doctorName || '-'}
            </div>
            <div style="text-align:right;">
              <strong>Patient Name:</strong> ${p.studentName}<br>
              <strong>Roll / Staff ID:</strong> ${p.studentRollNo}<br>
              <strong>Status:</strong> ${p.status || 'Active'}
            </div>
          </div>

          <div style="background:#f8fafc; border-radius:6px; padding:0.75rem; margin-bottom:1rem; font-size:0.85rem; display:flex; gap:1.5rem;">
            <div><strong>Blood Pressure:</strong> ${p.bloodPressure || '-'}</div>
            <div><strong>Temperature:</strong> ${p.temperature || '-'}</div>
            <div><strong>Pulse:</strong> ${p.pulse || '-'}</div>
            <div><strong>Weight:</strong> ${p.weight || '-'}</div>
          </div>

          <div style="margin-bottom:1rem; font-size:0.88rem;">
            <p><strong>Chief Complaints:</strong> ${p.symptoms || '-'}</p>
            <p style="margin-top:0.3rem;"><strong>Diagnosis:</strong> <span style="color:#8b0000; font-weight:700;">${p.diagnosis || '-'}</span></p>
            ${p.clinicalNotes ? `<p style="margin-top:0.3rem;"><strong>Clinical Notes:</strong> ${p.clinicalNotes}</p>` : ''}
          </div>

          ${sickLeaveBanner}

          <h4 style="font-size:0.95rem; margin-bottom:0.5rem; color:#0f172a; border-bottom:1px solid #e2e8f0; padding-bottom:0.25rem;">Prescribed Medicines:</h4>
          <table class="table" style="font-size:0.82rem; margin-bottom:1.5rem;">
            <thead>
              <tr>
                <th>#</th>
                <th>Medicine</th>
                <th>Dosage</th>
                <th>Duration</th>
                <th>Qty</th>
                <th>Instructions</th>
              </tr>
            </thead>
            <tbody>
              ${itemsHtml || '<tr><td colspan="6" style="text-align:center;">No medicines prescribed</td></tr>'}
            </tbody>
          </table>

          <div class="doc-stamp">
            <div class="stamp-box">
              Verified by Dispensary Pharmacy<br>
              ${p.dispensedBy ? `Dispensed by: ${p.dispensedBy} (${p.dispensedAt})` : 'Awaiting Pharmacy Dispensation'}
            </div>
            <div style="text-align:center;">
              <div style="font-weight:700; color:#8b0000; border-top:1px solid #0f172a; padding-top:0.4rem; width:200px;">
                ${p.doctorName || 'Medical Officer'}
              </div>
              <span style="font-size:0.75rem; color:#64748b;">Signature & Stamp</span>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-footer no-print">
        <button class="btn btn-secondary" onclick="closeSlipModal()">Close</button>
        <button class="btn btn-primary" onclick="window.print()">🖨️ Print Prescription / Sick Leave</button>
      </div>
    </div>
  `;
  modal.classList.add('active');
}

function closeSlipModal() {
  const modal = document.getElementById('slip-modal-container');
  if (modal) modal.classList.remove('active');
}

// ==================== SIMULATION REFRESH & RESET ====================
async function resetSimulationData() {
  if (!confirm("Are you sure you want to refresh all simulation data to the starting state?\n\nThis will reset:\n• Active queue tokens back to starting queue (#101-#112)\n• Default student & doctor accounts\n• Original appointment schedule\n• Full medicine inventory stock levels")) {
    return;
  }
  try {
    showToast("Refreshing simulation data...", "info");
    const res = await apiCall('/api/admin/reset-data', 'POST');
    showToast(res.message || "All dispensary simulation data has been reset to starting state!", "success");
    setTimeout(() => {
      window.location.reload();
    }, 700);
  } catch (err) {
    showToast("Reset failed: " + err.message, "error");
  }
}
