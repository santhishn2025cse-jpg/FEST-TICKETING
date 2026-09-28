// Global State
let festEvents = [];
let festAttendees = [];

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
    loadEvents();
    loadAttendees();
});

// Navigation & Tab Switching
function showTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.sidebar-btn').forEach(btn => btn.classList.remove('active'));

    const selectedTab = document.getElementById(`${tabName}-tab`);
    if (selectedTab) {
        selectedTab.classList.add('active');
    }

    const activeNavBtn = Array.from(document.querySelectorAll('.sidebar-btn')).find(btn => btn.getAttribute('onclick')?.includes(tabName));
    if (activeNavBtn) {
        activeNavBtn.classList.add('active');
    }

    // Update Header Page Title
    const titleMap = {
        'dashboard': 'Dashboard Overview',
        'events': 'Events & Capacity Management',
        'ticketing': 'Issue Digital Ticket Pass',
        'validation': 'Gate Entry QR Validation',
        'attendees': 'Registered Attendees Master Data'
    };
    const titleElem = document.getElementById('page-title');
    if (titleElem && titleMap[tabName]) {
        titleElem.innerText = titleMap[tabName];
    }

    if (tabName === 'dashboard') {
        loadDashboardData();
    } else if (tabName === 'events') {
        loadEvents();
    } else if (tabName === 'ticketing') {
        loadEvents();
        loadAttendees();
    } else if (tabName === 'attendees') {
        loadAttendeesTable();
    }
}

// Toast Alerts
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icon = type === 'success' ? 'fa-check' : 'fa-triangle-exclamation';
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${escapeHtml(message)}</span>`;
    
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 200);
    }, 4000);
}

// Load Dashboard & Headcount Data Table
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

        if (festEvents.length === 0) {
            headcountContainer.innerHTML = '<div class="empty-state">No fest events configured in database. Use "Events & Capacity" tab to create events.</div>';
            document.getElementById('stat-total-tickets').innerText = '0';
            document.getElementById('stat-total-checkedin').innerText = '0';
            document.getElementById('stat-total-capacity').innerText = '0';
            return;
        }

        let rowsHtml = '';
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
                let statusBadge = '<span class="badge badge-neutral">AVAILABLE</span>';

                if (percent >= 100) {
                    progressClass = 'danger';
                    statusBadge = '<span class="badge badge-danger">FULL CAPACITY</span>';
                } else if (percent >= 80) {
                    progressClass = 'warning';
                    statusBadge = '<span class="badge badge-warning">HIGH OCCUPANCY</span>';
                }

                rowsHtml += `
                    <tr>
                        <td><strong>${escapeHtml(hc.eventName)}</strong></td>
                        <td>${escapeHtml(event.venue)}</td>
                        <td>${new Date(event.eventDate).toLocaleDateString()} ${new Date(event.eventDate).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</td>
                        <td>${hc.totalTicketsIssued} / ${hc.capacity}</td>
                        <td><strong>${hc.currentHeadcount}</strong> checked-in</td>
                        <td style="width: 200px;">
                            <div class="progress-container">
                                <div class="progress-track">
                                    <div class="progress-fill ${progressClass}" style="width: ${Math.min(percent, 100)}%"></div>
                                </div>
                                <span style="font-size:0.75rem; font-weight:600;">${percent}%</span>
                            </div>
                        </td>
                        <td>${statusBadge}</td>
                    </tr>
                `;
            }
        }

        document.getElementById('stat-total-tickets').innerText = totalIssuedSum;
        document.getElementById('stat-total-checkedin').innerText = totalCheckedInSum;
        document.getElementById('stat-total-capacity').innerText = totalCapacitySum;

        headcountContainer.innerHTML = `
            <table class="erp-table">
                <thead>
                    <tr>
                        <th>Event Name</th>
                        <th>Venue</th>
                        <th>Date & Time</th>
                        <th>Tickets Issued</th>
                        <th>Checked-in</th>
                        <th>Occupancy %</th>
                        <th>Capacity Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${rowsHtml}
                </tbody>
            </table>
        `;

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
        
        select.innerHTML = '<option value="">-- Select Fest Event --</option>';

        if (festEvents.length === 0) {
            container.innerHTML = '<div class="empty-state">No fest events configured yet.</div>';
            return;
        }

        let rowsHtml = '';
        festEvents.forEach(evt => {
            const dateStr = new Date(evt.eventDate).toLocaleDateString() + ' ' + new Date(evt.eventDate).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
            rowsHtml += `
                <tr>
                    <td><code>EVT-${evt.id}</code></td>
                    <td><strong>${escapeHtml(evt.name)}</strong></td>
                    <td>${escapeHtml(evt.venue)}</td>
                    <td>${dateStr}</td>
                    <td class="text-right">$${parseFloat(evt.ticketPrice).toFixed(2)}</td>
                    <td>${evt.capacity} seats</td>
                    <td><button class="btn btn-secondary btn-sm" onclick="selectEventForPurchase(${evt.id})">Issue Ticket</button></td>
                </tr>
            `;

            // Populate select dropdown
            const opt = document.createElement('option');
            opt.value = evt.id;
            opt.textContent = `${evt.name} — Cap: ${evt.capacity} ($${evt.ticketPrice})`;
            select.appendChild(opt);
        });

        container.innerHTML = `
            <table class="erp-table">
                <thead>
                    <tr>
                        <th>Event ID</th>
                        <th>Event Name</th>
                        <th>Venue</th>
                        <th>Date & Time</th>
                        <th class="text-right">Price</th>
                        <th>Capacity</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${rowsHtml}
                </tbody>
            </table>
        `;
    } catch (err) {
        console.error('Error loading events:', err);
    }
}

function selectEventForPurchase(eventId) {
    showTab('ticketing');
    const select = document.getElementById('select-event');
    if (select) select.value = eventId;
}

// Load Attendees List
async function loadAttendees() {
    try {
        const res = await fetch('/api/attendees');
        const data = await res.json();
        if (!data.success) return;

        festAttendees = data.data || [];
        const select = document.getElementById('select-attendee');
        select.innerHTML = '<option value="">-- Select Registered Attendee --</option>';

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

async function loadAttendeesTable() {
    try {
        const res = await fetch('/api/attendees');
        const data = await res.json();
        if (!data.success) return;

        festAttendees = data.data || [];
        const container = document.getElementById('attendees-table-container');

        if (festAttendees.length === 0) {
            container.innerHTML = '<div class="empty-state">No attendees registered in database.</div>';
            return;
        }

        let rowsHtml = '';
        festAttendees.forEach(att => {
            rowsHtml += `
                <tr>
                    <td><code>ATT-${att.id}</code></td>
                    <td><strong>${escapeHtml(att.name)}</strong></td>
                    <td>${escapeHtml(att.email)}</td>
                    <td>${escapeHtml(att.phone || 'N/A')}</td>
                </tr>
            `;
        });

        container.innerHTML = `
            <table class="erp-table">
                <thead>
                    <tr>
                        <th>Attendee ID</th>
                        <th>Full Name</th>
                        <th>Email Address</th>
                        <th>Phone Number</th>
                    </tr>
                </thead>
                <tbody>
                    ${rowsHtml}
                </tbody>
            </table>
        `;
    } catch (err) {
        console.error('Error loading attendees table:', err);
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
            showToast(`Event '${data.data.name}' created successfully.`);
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
            showToast(`Attendee '${data.data.name}' registered.`);
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
            showToast('Digital Ticket Pass issued.');
            renderDigitalPass(data.data);
            loadDashboardData();
        } else {
            // Display clear business rule violation error (e.g. Capacity Exceeded!)
            showToast(data.message || 'Ticket purchase failed', 'error');
        }
    } catch (err) {
        showToast('Server error while issuing ticket', 'error');
    }
}

// Render Digital Pass Preview
function renderDigitalPass(ticket) {
    const container = document.getElementById('ticket-preview-container');
    container.className = 'digital-pass-card';

    const passHtml = `
        <div class="pass-card-header">
            <span class="pass-card-title">FESTPASS DIGITAL TICKET</span>
            <span class="badge ${ticket.used ? 'badge-neutral' : 'badge-success'}">${ticket.status}</span>
        </div>
        <div class="pass-card-body">
            <div class="qr-box">
                <img src="${ticket.qrCodeImageBase64}" alt="QR Code">
            </div>
            <div class="qr-code-text">${escapeHtml(ticket.qrCode)}</div>
            
            <div style="display:flex; gap:0.5rem; width:100%;">
                <button class="btn btn-secondary btn-sm btn-block" onclick="fillQrToken('${escapeHtml(ticket.qrCode)}')">
                    <i class="fa-solid fa-arrow-right"></i> Send to Gate Scanner
                </button>
                <button class="btn btn-secondary btn-sm" onclick="window.print()">
                    <i class="fa-solid fa-print"></i> Print
                </button>
            </div>

            <table class="pass-details-table">
                <tr><td class="label">Event Name:</td><td class="val">${escapeHtml(ticket.eventName)}</td></tr>
                <tr><td class="label">Venue:</td><td class="val">${escapeHtml(ticket.eventVenue)}</td></tr>
                <tr><td class="label">Attendee:</td><td class="val">${escapeHtml(ticket.attendeeName)}</td></tr>
                <tr><td class="label">Ticket Price:</td><td class="val">$${parseFloat(ticket.ticketPrice).toFixed(2)}</td></tr>
                <tr><td class="label">Ticket ID:</td><td class="val">TKT-${ticket.ticketId}</td></tr>
            </table>
        </div>
    `;
    container.innerHTML = passHtml;
}

function fillQrToken(qrCode) {
    showTab('validation');
    document.getElementById('input-qrcode').value = qrCode;
    showToast('QR Code transferred to Gate Check-in terminal.');
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
                <div class="decision-card approved">
                    <div class="decision-header">
                        <i class="fa-solid fa-circle-check"></i>
                        <span>ENTRY APPROVED</span>
                    </div>
                    <div class="decision-body">
                        <p style="margin-bottom:0.75rem;">Valid QR Code token. Attendance recorded successfully.</p>
                        <table class="erp-table">
                            <tr><th>Attendee Name</th><td><strong>${escapeHtml(ticket.attendeeName)}</strong></td></tr>
                            <tr><th>Event</th><td>${escapeHtml(ticket.eventName)}</td></tr>
                            <tr><th>Validated Timestamp</th><td>${new Date(ticket.validatedAt).toLocaleTimeString()}</td></tr>
                            <tr><th>QR Token</th><td><code>${escapeHtml(ticket.qrCode)}</code></td></tr>
                        </table>
                    </div>
                </div>
            `;
            showToast('Gate Check-in Approved.', 'success');
            loadDashboardData();
        } else {
            // Rejection Display (Duplicate check-in or invalid QR code)
            resultContainer.innerHTML = `
                <div class="decision-card rejected">
                    <div class="decision-header">
                        <i class="fa-solid fa-circle-xmark"></i>
                        <span>ENTRY REJECTED</span>
                    </div>
                    <div class="decision-body">
                        <p style="font-weight:600; margin-bottom:0.5rem;">${escapeHtml(data.message || 'Invalid or duplicate QR code token')}</p>
                        <p style="font-size:0.8rem; color:var(--text-secondary);">
                            Single-Use Business Rule Enforced: Each ticket QR code can be validated for entry only once.
                        </p>
                    </div>
                </div>
            `;
            showToast(data.message || 'Check-in Rejected', 'error');
        }
    } catch (err) {
        showToast('Server error during QR validation', 'error');
    }
}

// Simple Global Search Filter
function handleGlobalSearch(query) {
    if (!query) return;
    const q = query.toLowerCase();
    // Search within events table rows if present
    const rows = document.querySelectorAll('.erp-table tbody tr');
    rows.forEach(row => {
        const text = row.innerText.toLowerCase();
        row.style.display = text.includes(q) ? '' : 'none';
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
