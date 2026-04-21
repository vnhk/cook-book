import 'https://cdn.jsdelivr.net/npm/chart.js';

window.renderDietActivityChart = (canvas, labels, activityKcal) => {
    if (!canvas) return;
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-text-color').trim();
    const line1Color = getComputedStyle(document.documentElement).getPropertyValue('--chart-line1-color').trim() || '#6366f1';
    const ctx = canvas.getContext('2d');
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Activity Calories Burned (kcal)',
                data: activityKcal.map(v => v === 'null' ? null : parseFloat(v)),
                backgroundColor: line1Color + 'b3',
                borderColor: line1Color,
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: textColor } } },
            scales: {
                x: { ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { beginAtZero: true, ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
};

window.renderDietCalorieChart = (canvas, labels, consumed, target, effectiveTdee) => {
    if (!canvas) return;
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-text-color').trim();
    const line1Color = getComputedStyle(document.documentElement).getPropertyValue('--chart-line1-color').trim() || '#6366f1';
    const line2Color = getComputedStyle(document.documentElement).getPropertyValue('--chart-line2-color').trim() || '#f97316';
    const ctx = canvas.getContext('2d');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Consumed (kcal)',
                    data: consumed.map(v => v === 'null' ? null : parseFloat(v)),
                    borderColor: line1Color,
                    backgroundColor: 'transparent',
                    borderWidth: 2,
                    pointRadius: 3,
                    tension: 0.3
                },
                {
                    label: 'Target (kcal)',
                    data: target.map(v => v === 'null' ? null : parseFloat(v)),
                    borderColor: line2Color,
                    backgroundColor: 'transparent',
                    borderWidth: 2,
                    borderDash: [6, 3],
                    pointRadius: 2,
                    tension: 0.3
                },
                {
                    label: 'TDEE / Maintenance (kcal)',
                    data: effectiveTdee.map(v => v === 'null' ? null : parseFloat(v)),
                    borderColor: '#10b981',
                    backgroundColor: 'transparent',
                    borderWidth: 2,
                    borderDash: [3, 3],
                    pointRadius: 2,
                    tension: 0.3
                }
            ]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: textColor } } },
            scales: {
                x: { ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { beginAtZero: false, ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
};

window.renderDietDeficitChart = (canvas, labels, deficit) => {
    if (!canvas) return;
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-text-color').trim();
    const ctx = canvas.getContext('2d');
    const data = deficit.map(v => v === 'null' ? null : parseFloat(v));
    const bgColors = data.map(v => v === null ? 'transparent' : (v >= 0 ? 'rgba(16,185,129,0.7)' : 'rgba(239,68,68,0.7)'));
    const borderColors = data.map(v => v === null ? 'transparent' : (v >= 0 ? 'rgba(16,185,129,1)' : 'rgba(239,68,68,1)'));
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Net Deficit (kcal)',
                data: data,
                backgroundColor: bgColors,
                borderColor: borderColors,
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: textColor } } },
            scales: {
                x: { ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
};

window.renderDietWeightProjectionChart = (canvas, labels, actualWeight, projectedWeight) => {
    if (!canvas) return;
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-text-color').trim();
    const line1Color = getComputedStyle(document.documentElement).getPropertyValue('--chart-line1-color').trim() || '#6366f1';
    const ctx = canvas.getContext('2d');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Actual Weight (kg)',
                    data: actualWeight.map(v => v === 'null' ? null : parseFloat(v)),
                    borderColor: line1Color,
                    backgroundColor: 'transparent',
                    borderWidth: 2,
                    pointRadius: 4,
                    tension: 0.3,
                    spanGaps: false
                },
                {
                    label: 'Projected Weight (kg)',
                    data: projectedWeight.map(v => v === 'null' ? null : parseFloat(v)),
                    borderColor: '#f97316',
                    backgroundColor: 'rgba(249,115,22,0.08)',
                    borderWidth: 2,
                    borderDash: [6, 4],
                    pointRadius: 3,
                    tension: 0.3,
                    spanGaps: false,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { labels: { color: textColor } },
                tooltip: {
                    callbacks: {
                        label: ctx => ctx.parsed.y !== null ? `${ctx.dataset.label}: ${ctx.parsed.y} kg` : null
                    }
                }
            },
            scales: {
                x: { ticks: { color: textColor, maxTicksLimit: 14 }, grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { beginAtZero: false, ticks: { color: textColor, callback: v => v + ' kg' }, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
};

window.renderDietWeightChart = (canvas, labels, weight) => {
    if (!canvas) return;
    const textColor = getComputedStyle(document.documentElement).getPropertyValue('--chart-text-color').trim();
    const line1Color = getComputedStyle(document.documentElement).getPropertyValue('--chart-line1-color').trim() || '#6366f1';
    const ctx = canvas.getContext('2d');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Weight (kg)',
                data: weight.map(v => v === 'null' ? null : parseFloat(v)),
                borderColor: line1Color,
                backgroundColor: 'transparent',
                borderWidth: 2,
                pointRadius: 3,
                tension: 0.3,
                spanGaps: true
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: textColor } } },
            scales: {
                x: { ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { beginAtZero: false, ticks: { color: textColor }, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
};
