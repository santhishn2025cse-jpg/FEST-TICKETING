// Global State
let festEvents = [];
let festAttendees = [];

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
    loadEvents();
    loadAttendees();
});

// Tab Navigation
function showTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    const selectedTab = document.getElementById(`${tabName}-tab`);
    if (selectedTab) {
        selectedTab.classList.add('active');
    }

    const navBtn = Array.from(document.querySelectorAll('.nav-btn')).find(btn => btn.getAttribute('onclick')?.includes(tabName));
    if (navBtn) {
        navBtn.classList.add('active');
    }

    if (tabName === 'dashboard') {
        loadDashboardData();
    } else if (tabName === 'events') {
        loadEvents();
    } else if (tabName === 'ticketing') {
        loadEvents();
        loadAttendees();
    }
}

// Toast Alerts
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icon = type === 'success' ? 'fa-circle-check' : 'fa-triangle-exclamation';
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Load Dashboard & Headcount Data
async function loadDashboardData() {
    try {
        const eventsRes = await fetch('/api/events');
        const eventsData = await eventsRes.json();
        
        if (!eventsData.success) return;
        
        festEvents = eventsData.data || [];
        document.getElementById('stat-total-events').innerText = festEvents.length;

        let totalCapacitySum = 0;
        let totalIssuedSum = 0;
        let totalCheckedInSum = 0;

        const headcountContainer = document.getElementById('headcount-list');
        headcountContainer.innerHTML = '';

        if (festEvents.length === 0) {
            headcountContainer.innerHTML = '<div class="empty-state">No fest events created yet. Go to the Events tab to create your first event!</div>';
            document.getElementById('stat-total-tickets').innerText = '0';
            document.getElementById('stat-total-checkedin').innerText = '0';
            document.getElementById('stat-total-capacity').innerText = '0';
            return;
        }

        for (const event of festEvents) {
            totalCapacitySum += event.capacity;
            const hcRes = await fetch(`/api/events/${event.id}/headcount`);
            const hcData = await hcRes.json();
            
            if (hcData.success) {
                const hc = hcData.data;
                totalIssuedSum += hc.totalTicketsIssued;
                totalCheckedInSum += hc.currentHeadcount;

                const percent = hc.occupancyPercentage.toFixed(1);
                let progressClass = '';
                if (percent >= 100) progressClass = 'danger';
                else if (percent >= 80) progressClass = 'warning';

                const itemHtml = `
                    <div class="headcount-item">
                        <div class="headcount-header">
                            <span class="headcount-title"><i class="fa-solid fa-masks-theater"></i> ${escapeHtml(hc.eventName)}</span>
                            <span class="headcount-meta">Capacity: ${hc.capacity} | Price: $${event.ticketPrice}</span>
                        </div>
                        <div class="progress-bar-bg">
                            <div class="progress-bar-fill ${progressClass}" style="width: ${Math.min(percent, 100)}%"></div>
                        </div>
                        <div class="headcount-details">
                            <span><i class="fa-solid fa-user-check"></i> Checked-in Headcount: <strong>${hc.currentHeadcount}</strong></span>
                            <span><i class="fa-solid fa-ticket"></i> Issued Tickets: <strong>${hc.totalTicketsIssued} / ${hc.capacity}</strong></span>
                            <span><i class="fa-solid fa-chart-pie"></i> Occupancy: <strong>${percent}%</strong></span>
                        </div>
                    </div>
                `;
                headcountContainer.insertAdjacentHTML('beforeend', itemHtml);
            }
        }

        document.getElementById('stat-total-tickets').innerText = totalIssuedSum;
        document.getElementById('stat-total-checkedin').innerText = totalCheckedInSum;
        document.getElementById('stat-total-capacity').innerText = totalCapacitySum;

    } catch (err) {
        console.error('Error loading dashboard data:', err);
    }
}

// Load Events List
async function loadEvents() {
    try {
        const res = await fetch('/api/events');
        const data = await res.json();
        if (!data.success) return;

        festEvents = data.data || [];
        const container = document.getElementById('events-list');
        const select = document.getElementById('select-event');
        
        container.innerHTML = '';
        select.innerHTML = '<option value="">-- Choose Fest Event --</option>';

        if (festEvents.length === 0) {
            container.innerHTML = '<div class="empty-state">No fest events created yet.</div>';
            return;
        }

        festEvents.forEach(evt => {
            const dateStr = new Date(evt.eventDate).toLocaleString();
            const card = `
                <div class="headcount-item mb-3">
                    <div class="headcount-header">
                        <span class="headcount-title">${escapeHtml(evt.name)}</span>
                        <span class="pass-qr-token">$${evt.ticketPrice}</span>
                    </div>
                    <p style="font-size:0.85rem; color: var(--text-secondary); margin-bottom: 0.5rem;">
                        <i class="fa-solid fa-location-dot"></i> ${escapeHtml(evt.venue)} &bull; <i class="fa-solid fa-clock"></i> ${dateStr}
                    </p>
                    <p style="font-size:0.85rem; color: var(--text-muted); mb-2">${escapeHtml(evt.description || '')}</p>
                    <div class="headcount-details">
                        <span>Max Capacity: <strong>${evt.capacity} seats</strong></span>
                        <span>ID: #${evt.id}</span>
                    </div>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', card);

            // Populate select option
            const opt = document.createElement('option');
            opt.value = evt.id;
            opt.textContent = `${evt.name} (Capacity: ${evt.capacity}, Price: $${evt.ticketPrice})`;
            select.appendChild(opt);
        });
    } catch (err) {
        console.error('Error loading events:', err);
    }
}

// Load Attendees List
async function loadAttendees() {
    try {
        const res = await fetch('/api/attendees');
        const data = await res.json();
        if (!data.success) return;

        festAttendees = data.data || [];
        const select = document.getElementById('select-attendee');
        select.innerHTML = '<option value="">-- Choose Attendee --</option>';

        festAttendees.forEach(att => {
            const opt = document.createElement('option');
            opt.value = att.id;
            opt.textContent = `${att.name} (${att.email})`;
            select.appendChild(opt);
        });
    } catch (err) {
        console.error('Error loading attendees:', err);
    }
}

// Create Event Submit
async function handleCreateEvent(e) {
    e.preventDefault();
    const payload = {
        name: document.getElementById('event-name').value,
        venue: document.getElementById('event-venue').value,
        capacity: parseInt(document.getElementById('event-capacity').value),
        ticketPrice: parseFloat(document.getElementById('event-price').value),
        eventDate: document.getElementById('event-date').value,
        description: document.getElementById('event-desc').value
    };

    try {
        const res = await fetch('/api/events', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok && data.success) {
            showToast(`Event '${data.data.name}' created successfully!`);
            document.getElementById('create-event-form').reset();
            loadEvents();
            loadDashboardData();
        } else {
            showToast(data.message || 'Failed to create event', 'error');
        }
    } catch (err) {
        showToast('Server error while creating event', 'error');
    }
}

// Create Attendee Submit
async function handleCreateAttendee(e) {
    e.preventDefault();
    const payload = {
        name: document.getElementById('attendee-name').value,
        email: document.getElementById('attendee-email').value,
        phone: document.getElementById('attendee-phone').value
    };

    try {
        const res = await fetch('/api/attendees', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok && data.success) {
            showToast(`Attendee '${data.data.name}' registered!`);
            document.getElementById('create-attendee-form').reset();
            loadAttendees();
        } else {
            showToast(data.message || 'Failed to register attendee', 'error');
        }
    } catch (err) {
        showToast('Server error while registering attendee', 'error');
    }
}

// Purchase / Issue Ticket Submit
async function handlePurchaseTicket(e) {
    e.preventDefault();
    const payload = {
        eventId: parseInt(document.getElementById('select-event').value),
        attendeeId: parseInt(document.getElementById('select-attendee').value)
    };

    try {
        const res = await fetch('/api/tickets/purchase', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok && data.success) {
            showToast('Digital Ticket & QR Code issued successfully!');
            renderDigitalPass(data.data);
            loadDashboardData();
        } else {
            // Display clear business rule violation error (e.g. Capacity Exceeded!)
            showToast(data.message || 'Failed to issue ticket', 'error');
        }
    } catch (err) {
        showToast('Server error while issuing ticket', 'error');
    }
}

// Render Digital Pass Preview
function renderDigitalPass(ticket) {
    const container = document.getElementById('ticket-preview-container');
    container.className = 'digital-pass';

    const passHtml = `
        <div class="pass-header">
            <span class="pass-title"><i class="fa-solid fa-ticket"></i> Fest Digital Pass</span>
            <span class="pass-status-badge ${ticket.used ? 'badge-used' : 'badge-active'}">${ticket.status}</span>
        </div>
        <div class="pass-body">
            <div class="qr-image-frame">
                <img src="${ticket.qrCodeImageBase64}" alt="QR Code">
            </div>
            <div class="pass-qr-token">${escapeHtml(ticket.qrCode)}</div>
            
            <button class="btn btn-sm btn-outline mb-2" onclick="fillQrToken('${escapeHtml(ticket.qrCode)}')">
                <i class="fa-solid fa-arrow-right-to-bracket"></i> Copy to Gate Scanner
            </button>

            <div class="pass-info-grid">
                <div class="pass-info-item">
                    <label>Event</label>
                    <span>${escapeHtml(ticket.eventName)}</span>
                </div>
                <div class="pass-info-item">
                    <label>Venue</label>
                    <span>${escapeHtml(ticket.eventVenue)}</span>
                </div>
                <div class="pass-info-item">
                    <label>Attendee</label>
                    <span>${escapeHtml(ticket.attendeeName)}</span>
                </div>
                <div class="pass-info-item">
                    <label>Price Paid</label>
                    <span>$${ticket.ticketPrice}</span>
                </div>
            </div>
        </div>
    `;
    container.innerHTML = passHtml;
}

function fillQrToken(qrCode) {
    showTab('validation');
    document.getElementById('input-qrcode').value = qrCode;
    showToast('QR Token transferred to Gate Check-in form!');
}

// Validate Ticket QR Submit
async function handleValidateTicket(e) {
    e.preventDefault();
    const qrCode = document.getElementById('input-qrcode').value.trim();

    try {
        const res = await fetch('/api/tickets/validate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ qrCode })
        });
        const data = await res.json();

        const resultContainer = document.getElementById('validation-result-container');

        if (res.ok && data.success) {
            const ticket = data.data;
            resultContainer.innerHTML = `
                <div class="validation-card approved">
                    <div class="result-icon"><i class="fa-solid fa-circle-check"></i></div>
                    <div class="result-title">ENTRY APPROVED!</div>
                    <div class="result-msg">Valid QR Code. Attendance recorded.</div>
                    <div class="pass-info-grid">
                        <div class="pass-info-item"><label>Attendee</label><span>${escapeHtml(ticket.attendeeName)}</span></div>
                        <div class="pass-info-item"><label>Event</label><span>${escapeHtml(ticket.eventName)}</span></div>
                        <div class="pass-info-item"><label>Validated At</label><span>${new Date(ticket.validatedAt).toLocaleTimeString()}</span></div>
                        <div class="pass-info-item"><label>QR Token</label><span>${escapeHtml(ticket.qrCode)}</span></div>
                    </div>
                </div>
            `;
            showToast('Gate Check-in Approved!', 'success');
            loadDashboardData();
        } else {
            // Rejection Display (Duplicate check-in or invalid QR code)
            resultContainer.innerHTML = `
                <div class="validation-card rejected">
                    <div class="result-icon"><i class="fa-solid fa-circle-xmark"></i></div>
                    <div class="result-title">ENTRY REJECTED</div>
                    <div class="result-msg">${escapeHtml(data.message || 'Invalid or duplicate QR code token')}</div>
                    <p style="font-size:0.85rem; color:var(--text-secondary); margin-top: 0.5rem;">
                        Business rule enforced: Each QR code can only be validated once.
                    </p>
                </div>
            `;
            showToast(data.message || 'Check-in Rejected', 'error');
        }
    } catch (err) {
        showToast('Server error during validation', 'error');
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
